# 光影路径性能优化分析

最后更新：2026-07-31。

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

**现状：已接入。** `ShadowRenderer` 会在 shader pack 允许高级裁剪时，复用
`AdvancedShadowCullingFrustum`，由玩家视锥、阴影光向量和 `BoxCuller` 构造阴影 terrain
frustum；距离条件变化时只更新缓存的距离盒。`VintageRenderSectionManager` 通过该 frustum
执行 shadow terrain 裁剪，因此玩家背后或视野外、且不可能影响可见区域的区块会被提前排除。

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

**验证：** `ShadowOptimizationRegressionTest` 直接覆盖高级视锥的可见/剔除契约，以及
`BoxCuller` 的距离盒边界。后续只应针对包声明的 `ShadowCullState`、体素化和安全区语义调整
选择策略，不能绕过这一已接入的裁剪路径。

---

### 1.2 ShadowMatrices — 网格对齐（不是阴影贴图更新间隔）

**优先级：P0**

**现状：已接入网格对齐。** `ShadowRenderer#getShadowModelView()` 每个 shadow pass 都会调用
`ShadowMatrices.createModelViewMatrix(...)`。其中 `intervalSize` 表示**以方块为单位的吸附网格大小**：
相机在同一格内移动时，固定世界点的阴影投影保持稳定；越过格边界时才按一个格宽平移，
从而消除阴影抖动（shimmering）。

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

`ShadowRenderer` 的 `intervalSize` **不是更新帧间隔**，也不表示 `4.0` 时每 4 帧渲染一次。
当前实现不会任意跳过阴影 pass；这样做会破坏实体、方块实体和动态光照阴影的时序正确性，
除非未来建立完整的失效追踪和 shader-pack 兼容契约，否则不应把它作为性能优化建议。

**验证：** `ShadowOptimizationRegressionTest` 覆盖同格稳定、跨格平移和负坐标三种吸附情形。

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

**现状：已接入，并已完成运行期命中验证。**

`AdaptiveShadowBoundsTransformer` 在识别到 `texture2DShadow2x2` 或 `SampleFilteredShadow`
时，在 helper 入口加入统一的 shadow-map bounds check。坐标的 `x/y` 必须位于
`1.5 / shadowMapResolution` 与 `1.0 - 1.5 / shadowMapResolution` 之间，`z` 必须位于
`(0, 1)`；越界时直接返回全亮结果，从而跳过后续 PCF 纹理访问。已经带有等价检查的 helper
不会重复注入，无法确认签名或采样语义的函数也不会被改写。

当 GLSM 性能调试由 GUI 或 `-Dactinium.glsmPerfDebug=true` 开启，并且运行环境支持 SSBO 与
GLSL 4.30 以上时，`DeferredWorldRenderingPipeline` 会为当前 shader pack 选择未占用的 SSBO
binding。变换器额外注入 `atomicAdd` 计数，`GLSM perf:` 日志会输出调用数、拒绝数和估算的
采样节省量；关闭性能调试时不注入这些计数器，生产路径只保留 bounds check。

**2026-07-31 实机验证（BSL_v10.0）：** `run/client/logs/latest.log` 记录 runtime stats
使用 binding `95`，并持续输出 14 个有效统计窗口（忽略启动预热窗口）。累计
`709,042,107` 次 helper 调用中有 `73,586,511` 次提前拒绝，拒绝率 `10.38%`，加权通过率
`89.62%`；后期窗口的拒绝率稳定在 `13.2% - 13.4%`。本次场景全部命中
`SampleFilteredShadow`，`texture2DShadow2x2` 的调用数为 `0`，且未出现
`readbackError=true`。这证明 bounds 优化已经在实际渲染片元中执行，而不是只有编译期注入。

`estimatedPcfSamplesSaved` 是保守下界：当前每次拒绝只计 `1`，而 BSL 的
`SampleFilteredShadow` 至少包含一组 9 次深度采样，条件满足时还会执行第二组 9 次采样及
颜色纹理读取。因此该字段不能直接当作真实 FPS 或实际纹理 tap 数。由于 Debug 计数器自身
会增加 GPU 原子操作开销，FPS 收益仍需在相同场景下关闭统计并对比启用/禁用 bounds check
的 A/B 基线后再量化。

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

**Actinium 现状：已启用，但作用域有限。** `DeferredWorldRenderingPipeline` 在构建完 eager
composite/prepare/deferred/final renderer，并强制创建所需 shadow pass 后调用
`customUniforms.optimise()`。它只根据调用当时 `locationMap` 中已分配的 program location，保留被
引用的 uniform 和依赖，并从求值顺序中移除其余节点；它不是按帧的上传缓存，也不会优化之后才
惰性创建的 pass。

因此不应重复增加 `optimise()` 调用，更不能把它当作无条件减少所有 uniform 上传的优化。后续应以
实际 shader pack 验证 eager/惰性 pass 的 location 覆盖范围，再决定是否需要调整调用时机或上传策略。

---

### 2.2 FormatAnalyzer — 动态顶点格式精简

**优先级：P1**

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\pipeline\programs\SodiumPrograms.java`

```java
// 根据 shader 是否使用特定属性来裁剪顶点格式
FormatAnalyzer.createFormat(hasBlockId, hasNormal, hasMidUv, hasMidBlock);
```

如果 shader 不读取 `midBlock` 或 `tangent`，这些属性就不会写入 VBO——直接减少顶点步长（stride）和显存带宽。

**Actinium 现状：已实现。** `TerrainVertexFormatRequirements` 使用 GLSL lexer 检查转换后的
terrain、water、shadow 和 shadow-water vertex shader，并以 pack 级并集决定共享 section VBO
需要的扩展属性。`ExtendedChunkVertexType` 与 `ExtendedChunkVertexEncoder` 从同一需求对象生成
layout、stride、offset 和条件写入；布局可在基础 28 字节和完整 48 字节之间收缩。shader reload
导致需求变化时，会通过 `BlockRenderingSettings` 触发现有 renderer reload，确保旧 VBO 不会与新
program layout 混用。源码缺失或无法分析时会记录警告并保守回退完整格式。

**验证：** `TerrainVertexFormatRequirementsTest` 覆盖跨 pass 属性并集、仅声明属性剔除、不可分析
源码的完整格式回退，以及动态 layout 的对齐和 offset。

2026-07-29 实机连续切换 BSL 10.0、Complementary Reimagined/Unbound r5.5.1、Eclipse、
iterationRP 0.8.7、iterationT 3.2.0、MakeUp Ultra Fast 9.1f 和春 v2，画面表现与完整 48 字节
格式一致，且未出现 vertex format 分析失败、shader transform 异常或切换后的渲染错误。日志记录到
三种实际 layout：iterationRP/iterationT 为 40 字节，BSL/Complementary/MakeUp/春为 44 字节，
Eclipse 因使用全部扩展属性保留 48 字节。相对原完整格式，前两组分别减少约 16.7% 和 8.3% 的
terrain vertex 数据；本轮只确认兼容性与 layout 命中，尚未用同场景 A/B 隔离 GPU 带宽收益。

---

### 2.3 TransformPatcher 统一变换缓存

**优先级：已接入，待实机量化**

**Iris 实现：** `D:\Code\Iris\common\src\main\java\net\irisshaders\iris\pipeline\transform\TransformPatcher.java`

```java
private static final ParsingCacheStrategy PARSING_CACHE_STRATEGY = ParsingCacheStrategy.TWO_TIER;
private static final int PARSING_CACHE_MAX_ENTRIES = 400; // LRU cache
```

Actinium 现在以单个 400 条目的 access-order LRU 缓存 graphics 与 compute 变换结果。缓存键覆盖 graphics
的 vertex/geometry/tessControl/tessEval/fragment source，或 compute source，以及完整的变换参数；结果 map
在发布前冻结，避免调用方修改污染后续命中。`ShaderTransformer` 不再持有重复的 100 条目结果缓存。

在 GUI 的“渲染计时 DEBUG”中开启 Actinium 性能调试，或以 `-Dactinium.glsmPerfDebug=true` 强制开启后，
可在游戏日志中观察 `[ShaderTransformCache]`：每次
`graphics` / `compute` 的 `hit`、`miss`、`raceReuse` 和 `cleared` 都会记录 cache size，实际变换还会记录
`transformMs`。同一 shader pack 连续 reload 时，命中数上升且 miss 的总变换时间下降即为该优化生效的直接证据。

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

### 4.4 CompatUniformManager — 按 program 分类脏追踪

**优先级：** 已接入，已完成兼容性验证

Angelica 的 `CompatUniformManager` 管理 `angelica_*` 和 `iris_*` uniform。当前实现按 linked program
分别保存类别（mv/proj/texMat/lighting/fragment/color/clipPlane）的最后上传 generation counter；程序切换回来时，
generation 未变化的类别会跳过整批上传：

```java
final boolean mvChanged = state.needsModelViewUpload(mvGen);
if (mvChanged || projChanged || texMatChanged) {
    uploadMatrices(...);
    if (mvChanged) state.markModelViewUploaded(mvGen);
    if (projChanged) state.markProjectionUploaded(projGen);
    ...
}
```

重链时会先清理该 program 的旧 state，避免新的链接结果不再包含 compat uniform 时复用失效 location。

2026-07-30 已在实机 compat 日志中确认：切回已上传过的 program 时，未变化类别会跳过上传，且没有出现画面或
兼容性回归。本轮只验证功能正确性，未采集 A/B 性能数据。为完成这次验证临时加入的 `compat.uniforms` 专项计数
已经移除，避免长期在 `onUseProgram()` 热路径保留额外分支、计数和日志维护成本。

SharedDrawable 不拥有 `GLStateManager` 的缓存状态。本轮不会在该 context 上传 compat uniform 或更新 program
generation，避免以主 context 可能已过期的缓存状态作为 uniform 数据源；位置缓存和上传 scratch buffer 分别受同步和
线程局部存储保护。

---

### 4.5 GLSMPerfDebug — 分段性能分析

**优先级：** 已集成

`D:\Code\Actinium\glsm\src\main\java\com\gtnewhorizons\angelica\glsm\debug\GLSMPerfDebug.java`

通过 `-Dactinium.glsmPerfDebug=true` 启用。跟踪 20+ 个 stage（stream draw、FFP uniforms、buffer upload、fence 操作等），每秒自动报告。

FFP variant、compat uniform 等专项统计只在验证具体假设时临时加入。本轮 compat uniform 专项统计已在完成
实机验证后移除；稳定保留的 stage 计时用于后续回归比较。

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

**Actinium 现状：** 已完整提供 shader-pack 的 `noisetex` sampler。`texture.noise` 指向的自定义
纹理优先使用；未声明时 `CustomTextureManager` 按 `noiseTextureResolution`（默认 256）生成固定种子的
RGBA 随机噪声纹理，并注入 terrain、shadow composite、composite 与 final pass。它不是内置的
`noises.png`/blue-noise 资源，也没有向所有转换后的 shader 注入标准 `blueNoise()` 函数。

因此 blue noise 目前是 shader pack 可选择的质量方案，不是应直接写入 Actinium 资源包的既有优化；
若要引入，必须先定义采样格式、时序策略和对现有 `texture.noise` 覆盖语义的兼容方式。

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
| **P0** | AdvancedShadowCullingFrustum | 阴影 pass 裁剪 | `AdvancedShadowCullingFrustum.java` | 已接入，需实机量化 |
| **P0** | 网格对齐 | 消除 shadow shimmering | `ShadowMatrices.java` | 已接入；`intervalSize` 不跳帧 |
| **P1** | Blue noise 纹理 + 标准函数 | 全局 dithering 质量提升 | `noises.png`, `color_dither.glsl` | shader pack 自定义 `noisetex` 已支持；标准 blue noise 尚未设计 |
| **P1** | CustomUniforms.optimise() 验证 | 减少无用 uniform 上传 | `CustomUniforms.java` | 已在 pipeline 构建末尾执行 |
| **P1** | Adaptive PCF early-bounds check | 阴影片元剔除 | `Shadows.glsl` (Eclipse) | 已接入；BSL 10.0 实测命中 10.38% 越界调用；需关闭 Debug 的同场景 A/B 量化 |
| **P1** | FormatAnalyzer 顶点格式精简 | 扩展格式由 48 字节按需收缩，最低 28 字节 | `TerrainVertexFormatRequirements.java` | 已实机验证 40/44/48 字节布局；需同场景 A/B 量化 |
| **P2** | GTAO fast_acos 参考实现 | 低成本屏幕空间 AO | `PhotonGTAO.glsl` | 参考储备 |
| **P2** | LPV shared memory compute | 间接光照质量 | `shadowcomp.csh` | 参考储备 |
| **P2** | 体积云层级裁剪 | 云雾 pass GPU 时间 | `volumetricClouds.glsl` | 参考储备 |
| **P2** | SSRT 手电筒阴影 | 手持光源真实遮挡 | `diffuse_lighting.glsl` | 参考储备 |
| **P3** | TransformPatcher 缓存调参 | 减少 shader 重编译 | `TransformPatcher.java` | 已接入；用 GUI 性能调试或 `actinium.glsmPerfDebug` 量化 |
| **P3** | CompatUniformManager 分类脏追踪 | FFP 路径 uniform 上传优化 | Angelica CompatUniformManager | 已接入，已完成兼容性验证 |

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
