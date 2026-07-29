# 光影路径性能优化分析

最后更新：2026-07-29。

## 目的与范围

本文对照 Iris (D:\Code\Iris)、Sodium (D:\Code\Sodium) 和 Angelica (D:\Code\Angelica) 三个上游项目，
分析 Actinium 光影路径中可以借鉴的性能优化。分析覆盖 GLSL 着色器、Java 渲染管线、缓冲区管理、
着色器编译缓存四个层次。所有结论都标注了上游参考文件路径，便于直接查阅源码。

## 上游项目与 Actinium 的对应关系

```
Actinium 渲染栈
├── src/main/java/com/dhj/actinium/     → 自有渲染代码（ActiniumWorldRenderer 等）
├── shader/                              → Iris 管线移植（TransformPatcher, CommonUniforms 等）
├── celeritas-common/                    → Sodium/Embeddium 区块渲染（ChunkBuilder, CompactChunkVertex 等）
├── glsm/                                → Angelica GLSM（GLStateManager, VertexKey, ShaderCache 等）
└── GTNHLib/                             → 底层 OpenGL/VBO/VAO/后处理工具
```

---

## 一、阴影路径优化（影响最大）

### 1.1 AdvancedShadowCullingFrustum — 高级阴影视锥体裁剪

**优先级：P0**

**现状：** `VintageRenderSectionManager` 已支持基础遮挡裁剪 (`useFogOcclusion`, `shouldUseOcclusionCulling`)，但在阴影 pass 期间没有使用紧致裁剪。这意味着阴影 pass 会渲染大量玩家背后或视野外的区块。

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\shadows\frustum\advanced\AdvancedShadowCullingFrustum.java:39-98`

核心算法来自 L. Spiro 的定向光裁剪技术：
- 从玩家视锥体推导阴影 pass 的紧致包围体
- **背平面消除** (`addBackPlanes`): 法线朝向光源的平面（点积 > 0）标记为"背平面"——玩家背后的内容不可能在可见区域投下阴影，直接裁剪
- **边缘平面** (`addEdgePlanes`): 从背平面与前平面的交叉线计算额外裁剪面，最多 **13 个裁剪平面**
- 搭配 `BoxCuller` 做第一级距离预筛选（快速轴分离测试）

```java
// AdvancedShadowCullingFrustum.java:124-128
float dot = planeNormal.dot(shadowLightVectorFromOrigin);
boolean back = dot > 0.0;  // 法线朝向光源 → 背面 → 可安全裁剪
```

`BoxCuller` 实现（`D:\Code\Iris\common\src\main\java\net\irisshaders\iris\shadows\frustum\BoxCuller.java:29-44`）是简单的 AABB 距离盒，每个轴独立 early-out：

```java
public boolean isCulled(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    if (maxX < this.minAllowedX || minX > this.maxAllowedX) return true;  // X 轴快速剔除
    if (maxY < this.minAllowedY || minY > this.maxAllowedY) return true;  // Y 轴快速剔除
    return maxZ < this.minAllowedZ || minZ > this.maxAllowedZ;            // Z 轴
}
```

**可迁移性：** 高。`AdvancedShadowCullingFrustum` 已实现 Sodium 的 `ViewportProvider` 接口，可以直接与 Actinium 的 `VintageRenderSectionManager` 集成。Actinium 已在 `ActiniumWorldRenderer.java:80-83` 中有阴影 pass 检测逻辑：

```java
if (this.renderSectionManager != null && this.renderSectionManager.isInShadowPass()) {
    return new ChunkRenderMatrices(ShadowRenderer.PROJECTION, ShadowRenderer.MODELVIEW);
}
```

只需在此分支中切换到 `AdvancedShadowCullingFrustum` 作为视锥体即可。

---

### 1.2 ShadowMatrices — 阴影贴图间隔更新 + 网格对齐

**优先级：P0**

**现状：** 如果 Actinium 每帧都渲染阴影贴图，是不必要的开销。Iris 通过 `intervalSize` 控制更新频率，配合网格对齐消除阴影抖动（shimmering）。

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\shadows\ShadowMatrices.java:71-106`

```java
public static void snapModelViewToGrid(PoseStack target, float shadowIntervalSize,
        double cameraX, double cameraY, double cameraZ) {
    if (Math.abs(shadowIntervalSize) == 0.0F) return;

    // 计算在网格单元中的偏移，然后吸附到单元中心
    float offsetX = (float) cameraX % shadowIntervalSize;
    float offsetY = (float) cameraY % shadowIntervalSize;
    float offsetZ = (float) cameraZ % shadowIntervalSize;

    float halfIntervalSize = shadowIntervalSize / 2.0f;
    offsetX -= halfIntervalSize;
    offsetY -= halfIntervalSize;
    offsetZ -= halfIntervalSize;

    target.last().pose().translate(offsetX, offsetY, offsetZ);
}
```

`ShadowRenderer.java:91` 的 `intervalSize` 字段控制频率。`intervalSize = 4.0` 意味着每 4 帧才完全重渲染阴影贴图——60fps 下约 15 次/秒，视觉差异通常不可察觉。

**可迁移性：** 高。结合 Actinium 的 `ShadowRenderingState`，只需增加帧计数器和网格吸附逻辑。

---

### 1.3 对数极坐标阴影贴图扭曲（Log-Polar Shadow Map）

**优先级：P1**

**现状：** Eclipse/Bliss 参考光影包已有完整 GLSL 实现，可作为 Actinium 默认阴影 shader 的技术参考。

**参考实现：** `D:\Code\Actinium\.tmp\bliss\shaders\lib\Shadow_Params.glsl`

```glsl
const float k = 1.8;
const float d0 = 0.04 + max(64.0 - shadowDistance, 0.0) / 64.0 * 0.26;
const float d1 = 0.61;
float a = exp(d0);
float b = (exp(d1) - a) * 150.0 / 128.0;

vec4 BiasShadowProjection(vec4 projectedShadowSpacePosition) {
    float distortFactor = log(length(projectedShadowSpacePosition.xy) * b + a) * k;
    projectedShadowSpacePosition.xy /= distortFactor;
    return projectedShadowSpacePosition;
}
```

**原理：** 对阴影贴图像素坐标施加对数极坐标扭曲，将纹素密度集中在近处，远处自然降低精度——无需级联阴影贴图 (CSM)。`d0` 随 `shadowDistance` 自适应增大扭曲强度。

**配合技术 — GriAndEminShadowFix：** `D:\Code\Actinium\.tmp\eclipse\Eclipse-Shader-Unstable\shaders\lib\Shadows.glsl`

```glsl
void GriAndEminShadowFix(inout vec3 WorldPos, vec3 FlatNormal, float transition) {
    transition = 1.0 - transition;
    transition *= transition*transition*transition*transition*transition*transition;
    float zoomLevel = mix(0.0, 0.5, transition);
    if (zoomLevel > 0.001 && isEyeInWater != 1)
        WorldPos = WorldPos - (fract(WorldPos + cameraPosition - WorldPos*0.0001)
               * zoomLevel - zoomLevel*0.5);
}
```

结合 FlatNormal 距离衰减 + 区块网格中心缩放，同时解决 peter-panning 和 light leaking。

---

### 1.4 Adaptive PCF — 阴影采样数自适应

**优先级：P1**

Eclipse 阴影 pass 的 PCF 采样数随 filter radius 动态调整，并由 early-bounds check 限制 per-pixel cost：

```glsl
// 采样步长与 filter radius 成正比，保持采样数在合理范围
rdMul / SHADOW_FILTER_SAMPLE_COUNT * shadowMapResolution * distortFactor / 2.7

// 阴影贴图外的片元直接返回全亮——零采样
if (abs(projectedShadowPosition.x) < 1.0 - 1.5/shadowMapResolution && ...)
    return 1.0;
```

**建议：** 在 Actinium 的阴影 shader 中加入类似 bounds check，避免阴影贴图外的片元浪费 PCF 采样。

---

## 二、着色器编译与缓存优化

### 2.1 ShaderMap 惰性加载 + Uniform 引用计数去冗余

**优先级：P1**

**Iris 实现：**
- `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\pipeline\programs\ShaderMap.java` — 惰性加载，shader 仅在首次使用时编译
- `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\uniforms\custom\CustomUniforms.java:76-175` — 拓扑排序 + 未使用 uniform 移除

拓扑排序确保 uniform 按依赖顺序求值，循环引用检测抛出 `IllegalStateException`，断裂 uniform 会传播给依赖链上的所有 uniform。

`optimise()` 方法统计每个 uniform 被 render pass 引用的次数，移除引用数为零的 uniform：

```java
// CustomUniforms.java 引用计数统计逻辑
for (CachedUniform uniform : this.variables.values()) {
    if (uniform.getReferenceCount() == 0) {
        // 安全移除，连带清理依赖链
    }
}
```

**可迁移性：** 中。Actinium 已集成 stareval 表达式解析器和 TransformPatcher，需确认 `ShaderMap` 的惰性加载和 `optimise()` 调用已正确启用。

---

### 2.2 FormatAnalyzer — 动态顶点格式精简

**优先级：P1**

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\pipeline\programs\SodiumPrograms.java`

```java
// 根据 shader 是否使用特定属性来裁剪顶点格式
FormatAnalyzer.createFormat(hasBlockId, hasNormal, hasMidUv, hasMidBlock);
```

如果 shader 不读取 `midBlock` 或 `tangent`，这些属性就不会写入 VBO——直接减少顶点步长（stride）和显存带宽。

**可迁移性：** 中。Actinium 使用 Celeritas 的 `ChunkVertexType`（`COMPACT` 20 字节 / `VANILLA_LIKE` 28 字节），但 Iris shader 集成时可以根据 `SodiumCoreTransformer` 的分析结果动态选择最小格式。

---

### 2.3 TransformPatcher 两阶段解析缓存

**优先级：P2**

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\pipeline\transform\TransformPatcher.java`

```java
private static final ParsingCacheStrategy PARSING_CACHE_STRATEGY = ParsingCacheStrategy.TWO_TIER;
private static final int PARSING_CACHE_MAX_ENTRIES = 400; // LRU cache
```

两阶段缓存：先检查源码字符串是否变化，再检查变换参数是否变化。400 条目的 LRU 缓存避免重复解析未修改的 shader。

**可迁移性：** 高。Actinium 的 `shader/` 模块已包含相同的 `TransformPatcher`，只需对齐缓存参数。

---

## 三、区块构建与缓冲区管理

### 3.1 ChunkBuilder — 线程池 + 保守任务预算 + Work Stealing

**优先级：** 已集成，建议验证配置

**Sodium 实现：** `D:\Code\Actinium\celeritas-common\src\main\java\org\embeddedt\embeddium\impl\render\chunk\compile\executor\ChunkBuilder.java`

关键参数：

```java
private static final int MBS_PER_CHUNK_BUILDER = 64;     // 每线程 64MB 堆上限
private static final int TASK_QUEUE_LIMIT_PER_WORKER = 2; // 保守队列预算

// 线程数 = min(CPU核心数, 堆内存MB/64, 10)
int getOptimalThreadCount() {
    int desired = Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6);
    return Math.min(Math.max(desired, 1), 10);
}
```

`TASK_QUEUE_LIMIT_PER_WORKER = 2` 是刻意保守的设计——防止相机移动时任务积压导致响应滞后。注释明确指出 "2 seems to be a decent value, and is what Sodium 0.2 used."

**Work Stealing**（`ChunkBuilder.java:153-159`）：主线程可以在等待时"偷取"队列中的任务执行，避免 CPU 空闲：

```java
public void tryStealTask(ChunkJob job) {
    if (!this.queue.stealJob(job)) return;
    executeJobWithLocalContext(job);
}
```

---

### 3.2 CompactChunkVertex — 20 字节紧凑顶点格式

**优先级：** 已集成

Celeritas-Common 的 `CompactChunkVertex` 将顶点从 vanilla 28 字节压缩到 20 字节（节省 28.6% VRAM）。材质 ID (8-bit) + Section Index (8-bit) 内联到 `a_PosId` 属性的 padding 字节中，避免额外的顶点属性 fetch。

**注意：** 当 Iris shader pack 声明使用 `midBlock`、`tangent` 等扩展属性时，celeritas 会自动切换到 `VANILLA_LIKE` 格式。通过 2.2 节的 `FormatAnalyzer` 可以进一步避免不必要的格式升级。

---

### 3.3 Sodium 遮挡图 BFS 遍历

**优先级：** 已集成

每个 section 存储 64-bit `visibilityData`（6×6 位矩阵，编码"能否从面 F_from 穿透到面 F_to"）。`OcclusionCuller.findVisible()` 从相机所在 section 做 BFS 向外扩展：
- **只向外遍历**：朝向相机反方向的边被 mask 掉
- **遮挡 masking**：通过 `VisibilityEncoding.getConnections()` 动态计算可达面
- **距离检查**：手写 `nearestToZero()` 避免 `Math.min`/`Math.max` 的装箱开销

---

## 四、OpenGL 状态管理优化

### 4.1 VertexKey — 单 long 打包 FFP 状态键

**优先级：** 已集成，持续优化中

**实现：** `D:\Code\Actinium\glsm\src\main\java\com\gtnewhorizons\angelica\glsm\ffp\VertexKey.java`

整个顶点着色器 permutation 状态打包为一个 `long`（bit field，0-35 位覆盖所有 FFP 状态轴）：

```java
// 零对象分配的比较
@Override public int hashCode() { return Long.hashCode(packed); }
@Override public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof VertexKey other)) return false;
    return packed == other.packed;
}
```

`packFromState()` 从 `GLStateManager` 缓存读取状态——**零 GL 调用**。

纹理矩阵先检查 JOML identity property，再做带容差的值比较：

```java
public static boolean isIdentity(Matrix4f matrix) {
    return (matrix.properties() & Matrix4f.PROPERTY_IDENTITY) != 0
        || matrix.equals(IDENTITY, 1e-6f);
}
```

2026-07-29 的实机诊断中，稳定场景每秒约有 1.35 万至 1.7 万次纹理矩阵检查，全部命中
`PROPERTY_IDENTITY`，没有出现值比较或非 identity 矩阵。因此没有引入独立的纹理矩阵 generation
缓存；现有 property 检查已经是常数时间快路，增加缓存状态不会带来可测收益。

---

### 4.2 ShaderCache — Mesa 风格开放地址哈希

**优先级：** 已集成

Angelica 的 `ShaderCache` 受 Mesa `prog_cache.c` 启发：
- 6-long 槽位布局 `[vk, (fkLen<<48)|hash, fk0, fk1, fk2, fk3]`
- **单槽快速路径**：比较 raw longs inline，处理"连续 draw call 状态相同"的最常见情况
- **Fragment 源码共享**：`findFragSource()` 扫描现有槽位匹配 fragment key——多个 vertex key 变体共享同一 GLSL 源码

---

### 4.3 ShaderManager — 跨 program 的 FFP variant 复用

**优先级：** 已集成

Minecraft 与模组会高频交替使用用户 shader program 和固定管线 program 0。原实现每次
`glUseProgram(0)` 都会丢弃当前 FFP variant，并以通用顶点格式强制查找和绑定 program；下一次 draw
又会按真实顶点格式切换一次。

当前实现包含两级快路：

- 当 program 已经是 0 且 FFP 处于 active 状态时，重复 `glUseProgram(0)` 直接返回。
- 从用户 program 切回 FFP 时保留上一个 `Program` 和 packed vertex/fragment key；key 未变化时只重新
  绑定已有 program，状态变化时才进入 `ShaderCache`。

```java
if (variantChanged) {
    commitVariant(vertexKey, fragmentKeyLength);
} else {
    RENDER_BACKEND.useProgram(currentProgram.getProgramId());
}
```

2026-07-29 的同场景实测中，activation rebind 命中约 70%，每秒 variant commit 从约
`10.6k-10.9k` 降至约 `5.8k-7.5k`；纯 GUI 阶段 activation 几乎全部复用，commit 约为 `90/s`。

---

### 4.4 CompatUniformManager — 分类脏追踪

**优先级：** 中

Angelica 的 `CompatUniformManager` 管理 `angelica_*` 和 `iris_*` uniform。按类别（mv/proj/texMat/lighting/fragment/color/clipPlane）存储最后上传的 generation counter，上传时逐类别比较，未变化则跳过整批上传：

```java
final boolean mvChanged = mvGen != st.mvGen;
if (mvChanged || projChanged || texMatChanged) {
    uploadMatrices(...);
    st.mvGen = mvGen; st.projGen = projGen; ...
}
```

Actinium 的 Iris 集成已有类似机制（`CommonUniforms`），但 GLSM FFP 路径的 uniform 上传可以借鉴此 pattern。

---

### 4.5 GLSMPerfDebug — 分段性能分析

**优先级：** 已集成

`D:\Code\Actinium\glsm\src\main\java\com\gtnewhorizons\angelica\glsm\debug\GLSMPerfDebug.java`

通过 `-Dactinium.glsmPerfDebug=true` 启用。跟踪 20+ 个 stage（stream draw、FFP uniforms、buffer upload、fence 操作等），每秒自动报告。

FFP variant、compat uniform 等专项统计只在验证具体假设时临时加入。完成一次实机验证后移除，避免
长期增加热路径分支和日志维护成本；稳定保留的 stage 计时用于后续回归比较。

---

## 五、着色器内高级技术（参考 Eclipse/Bliss 光影包）

> 以下技术的参考实现位于 `D:\Code\Actinium\.tmp\eclipse\` 和 `D:\Code\Actinium\.tmp\bliss\`。
> 这些是 Actinium 用于研究和移植的参考光影包，不是 Actinium 自身的代码。

### 5.1 GTAO — 极低样本数下的高质量环境光遮蔽

**参考：** `D:\Code\Actinium\.tmp\eclipse\Eclipse-Shader-Unstable\shaders\lib\PhotonGTAO.glsl`

| 参数 | 值 | 常规 GTAO 参考值 |
|------|---|-----------------|
| `GTAO_SLICES` | **2** | 4-8 |
| `GTAO_HORIZON_STEPS` | **3** | 5-8 |
| `GTAO_RADIUS` | **2.0** | 0.5-2.0 |

**总采样数：2 slices × 3 steps = 6 次深度采样/像素**

核心优化技术：
- `fast_acos` 多项式近似：`res = (C2*|x| + C1)*|x| + C0` — 避免 `acos()` 的高三角函数开销
- `integrate_arc` 解析积分：用 `cos()` 直接计算弧段积分，不做数值积分
- 距离衰减：`linear_step(GTAO_FALLOFF_START * GTAO_RADIUS, GTAO_RADIUS, len_sq * norm)`
- Hand depth early-out：跳过第一人称手模型

---

### 5.2 LPV Compute Shader — 共享内存光照传播

**参考：** `D:\Code\Actinium\.tmp\bliss\shaders\world0\shadowcomp.csh`

```glsl
layout (local_size_x = 8, local_size_y = 8, local_size_z = 8) in;
shared vec4 lpvSharedData[10*10*10];   // 共享内存缓存 LPV 辐射度
shared uint voxelSharedData[10*10*10]; // 共享内存缓存体素 block ID
```

关键优化：
- `PopulateSharedIndex()` 预加载 10³ 邻居到 shared memory
- `barrier()` 同步后，所有 invocation 通过 `sampleShared()` 快速读取邻居
- `mixNeighbours()` 使用 **light mask bitmask** (`mask >> mask_index & 1u`) 选择性混合——只有有光照的面才参与混合计算
- Ping-pong 双缓冲：`(frameCounter % 2) == 0 ? texLpv1 : texLpv2`

**配合 — LPV 边缘衰减**（`diffuse_lighting.glsl:116-119`）：

```glsl
float fadeLength = 10.0; // meters
vec3 cubicRadius = clamp(min((LpvSize3-1.0 - lpvPos)/fadeLength, lpvPos/fadeLength), 0.0, 1.0);
float voxelRangeFalloff = cubicRadius.x * cubicRadius.y * cubicRadius.z;
```

在 LPV 体素网格边缘用三次方平滑过渡到 vanilla 光照，避免硬截断。

---

### 5.3 体积云层级裁剪

**参考：** `D:\Code\Actinium\.tmp\eclipse\Eclipse-Shader-Unstable\shaders\lib\volumetricClouds.glsl`

```glsl
// 每层自适应采样数——水平视线时减少采样
samples = int(clamp(maxSamples / exp2(abs(NormPlayerPos.y)), minSamples, maxSamples));

// 分层序贯裁剪——上一层已完全遮挡时跳过下一层
if (clouds.a > 1e-5) { /* 继续计算下一层 */ }

// 低密度早期丢弃
densityThresholdCheck varies by layer type and dither value
```

5 层云（Cirrus / Altostratus / Cumulonimbus / Large Cumulus / Small Cumulus）各有独立的采样策略和 early-out 条件。

---

### 5.4 SSRT 手电筒阴影

**参考：** `D:\Code\Actinium\.tmp\eclipse\Eclipse-Shader-Unstable\shaders\lib\diffuse_lighting.glsl:7-66`

屏幕空间射线步进检测手持光源遮挡。两种质量档位：

```glsl
#if LPV_HANDHELD_SHADOWS_QUALITY == 0
    float samples = 10.0; float div = 0.0015;   // 低质量
#else
    float samples = 20.0; float div = 0.0005;   // 高质量
#endif
```

**性能要点：** 固定步长 (`direction *= 6.0`) + depth early exit + hand model 跳过 + NdotL 预测试避免非表面法线方向的片元参与射线步进。

---

### 5.5 Blue Noise — 全局抖动

**参考：** `D:\Code\Actinium\.tmp\bliss\shaders\texture\noises.png` (512×512 RGBA, alpha 通道存储 blue noise)

```glsl
float blueNoise(){
    return fract(texelFetch2D(noisetex, ivec2(gl_FragCoord.xy)%512, 0).a
         + 1.0/1.6180339887 * frameCounter);  // 黄金比例旋转，避免 temporal 固定模式
}
```

**使用场景：**

| 用途 | 参考文件 | 效果 |
|------|----------|------|
| 阴影 pass stochastic transparency | `shadow.fsh` | 半透明物体在阴影贴图中的软边缘 |
| POM 射线起点抖动 | `gbuffers_terrain.fsh` | 消除视差贴图 banding |
| GTAO 射线偏移 | `PhotonGTAO.glsl` | 旋转采样网格，隐藏采样方向性 |
| TAA jitter | `TAA_jitter.glsl` | 子像素抖动 |
| 体积云 early rejection | `volumetricClouds.glsl` | 低密度区域随机丢弃 |
| FP16/FP10 颜色量化抖动 | `color_dither.glsl` | 减少 banding in post |

**可迁移性：** 高。确保 `noises.png` 在 Actinium 资源包中可用，并在 deferred shader 中提供标准 `blueNoise()` 函数。

---

## 六、LightDataCache 已有优化

**实现：** `D:\Code\Actinium\src\main\java\com\dhj\actinium\render\terrain\compile\light\LightDataCache.java:36-40`

```java
// OPTIMIZE: Do not calculate light data if the block is full and opaque and does not emit light.
if (fo && lu == 0) {
    bl = 0;
    sl = 0;
}
```

对于完全不透明且不发光的方块，跳过 lightmap 查询——这是 chunk build 阶段的显著优化，因为大多数方块属于此类。

---

## 七、优先级排序总览

| 优先级 | 优化项 | 预期收益 | 参考文件 | 状态 |
|--------|--------|----------|----------|------|
| **P0** | AdvancedShadowCullingFrustum | 阴影 pass 裁剪 30-60% 区块 | `AdvancedShadowCullingFrustum.java` | 待集成 |
| **P0** | 阴影贴图间隔更新 + 网格对齐 | 减少 50-70% 阴影 pass 开销 | `ShadowMatrices.java` | 待集成 |
| **P1** | Blue noise 纹理 + 标准函数 | 全局 dithering 质量提升 | `noises.png`, `color_dither.glsl` | 待集成 |
| **P1** | CustomUniforms.optimise() 验证 | 减少无用 uniform 上传 | `CustomUniforms.java` | 需验证 |
| **P1** | Adaptive PCF early-bounds check | 阴影片元剔除 | `Shadows.glsl` (Eclipse) | 待移植 |
| **P1** | FormatAnalyzer 顶点格式精简 | 减少顶点带宽 | `SodiumPrograms.java` | 需配合 Iris shader |
| **P2** | GTAO fast_acos 参考实现 | 低成本屏幕空间 AO | `PhotonGTAO.glsl` | 参考储备 |
| **P2** | LPV shared memory compute | 间接光照质量 | `shadowcomp.csh` | 参考储备 |
| **P2** | 体积云层级裁剪 | 云雾 pass GPU 时间 | `volumetricClouds.glsl` | 参考储备 |
| **P2** | SSRT 手电筒阴影 | 手持光源真实遮挡 | `diffuse_lighting.glsl` | 参考储备 |
| **P3** | TransformPatcher 缓存调参 | 减少 shader 重编译 | `TransformPatcher.java` | 需验证 |
| **P3** | CompatUniformManager 分类脏追踪 | FFP 路径 uniform 上传优化 | Angelica CompatUniformManager | 可选 |

---

## 八、已有等价项（不重复移植）

| 上游特性 | Actinium 现状 |
|----------|--------------|
| Persistent ring + fence 分段 | `PersistentStreamingBuffer` + `BufferBuilderStreamingDrawer` |
| Work Stealing ChunkBuilder | `ChunkBuilder.tryStealTask()` |
| 20-byte CompactChunkVertex | `CompactChunkVertex`（已集成） |
| 遮挡图 BFS 遍历 | `OcclusionCuller`（已集成） |
| Multi-draw emitter (Direct/Indirect) | `DirectMultiDrawEmitter` / `IndirectMultiDrawEmitter` |
| VAO 隔离 vertex state | streaming drawer 按 format 分别持有 VAO |
| VertexKey 单 long 打包 | `VertexKey.packFromState()`（持续优化中） |
| LightDataCache 全 opaque 跳过 | `LightDataCache.compute()`（已实现） |
| GLSMPerfDebug 分段统计 | `GLSMPerfDebug`（已集成） |
