# BetterPortals Refitted 兼容性说明

兼容状态：**已验证**（无光影与光影（BSL）场景下穿看 + 星野 + 淡出 + 换维度均正常）
最后更新：2026-09-02

## 模组信息

- betterportals-0.4.1.jar（modid `betterportals`，BetterPortals Refitted）
- 依赖：forgelin-continuous（Kotlin 运行时）
- BPR jar 自带 `_Actinium`/`_Celeritas` 定向 mixin（`mixins.betterportals.view.json`）：
  celeritas 侧 mixin 负责看穿渲染的地形就绪门控（`CeleritasTerrainDetail`），与本模组协同正常。

## 现象（修复前）

- 末地传送门没有"看穿看到末地"的视觉效果，洞口只能看到传送门（星野）贴图；传送功能正常。
- 传送门视觉位置比平时低约一格。
- 第一阶段的修复（放行星野渲染路径）后，星野出现但被拉伸成竖直条纹。

## 根因机制（三层叠加，2026-09-01 ~ 09-02 定位）

### 1. 看穿管线的 surface shader 失活

BPR 的看穿表面 quad 用 `betterportals:render_portal` shader 采样 view FBO。该 shader
依赖 `#ifdef GL_ES` / `#if BP_GL_ES && !BP_MOBILEGLES` 等预处理器条件分支；Actinium 的
compat shader 变换器此前对条件求值有 bug，桌面分支采样代码被剔除，链接后 program 缺少
活跃的 `sampler` uniform，Forge 无法喂入 view FBO 纹理，看穿画面采样失败（日志特征：
`Shader betterportals:render_portal could not find sampler named sampler`）。
由 `c7f3d45 fix(glsm): evaluate preprocessor conditionals in compat shaders` 修复，
回归断言锁定 `sampler`/`screenSize`/`opacity` 等 Forge 按名查找的 uniform 必须存活
（`CompatShaderTransformerTest`）。

### 2. 星野叠加渲染被整体劫持

BPR 的 `EndPortalRenderer.renderPortalBlocks` 自建一个**无 world 的合成 dummy
TileEntityEndPortal**，直接调原版 `TileEntityEndPortalRenderer.render` 六参方法来画
星野叠加层，并依赖原版路径上的精确 blend 钩子（`shouldRenderFace` 回调里
`blendFunc(CONSTANT_ALPHA,…)` + `glBlendColor` 淡出）与投影式 texgen。

Actinium 的 `TileEntityEndPortalRendererIrisMixin` 此前无条件 cancel 该入口并改走自研
core-profile 渲染器，BPR 的淡出语义被破坏（星野不透明化/消失）。

修复：`render/EndPortalRenderPolicy` 以「TE 是否属于世界」区分调用来源——dispatcher 对
真实世界 TE 的调用继续走替换渲染器；无 world 的合成 TE 调用**放行原版路径**，由 glsm
FFP/texgen 模拟执行，恢复 blend 钩子语义。判据不引用 BPR 类，对任何以合成 TE 走原版
TESR 的模组通用。折跃门（`TileEntityEndGatewayRenderer`）覆写路径不受影响。

### 3. glsm texgen 顶点着色器触发 NVIDIA 驱动 uniform 消除

放行后星野走 glsm 的 FFP 眼线性 texgen 模拟。生成的顶点着色器原写法为：

```glsl
vec4 texGenCoord = vec4(0.0, 0.0, 0.0, 1.0);
texGenCoord.s = dot(eyePos, u_TexGenEyePlaneS);
texGenCoord.t = dot(eyePos, u_TexGenEyePlaneT);
texGenCoord.r = dot(eyePos, u_TexGenEyePlaneR);
```

NVIDIA 驱动（RTX 5070 Laptop / 610.74 实测）把「先初始化再单分量覆写」模式中
`u_TexGenEyePlaneS` 的 feed 链误判为可消除——`GL_ACTIVE_UNIFORMS` 枚举里 S 缺失而
T/R 存活，眼平面 S 恒为 0，纹理映射塌掉一个基向量，星野沿一个方向被拉成条纹。

修复（`VertexShaderGenerator`）：改为单表达式构造，语义完全等价但不再给驱动逐分量
消除的机会：

```glsl
vec4 texGenCoord = vec4(dot(eyePos, u_TexGenEyePlaneS), dot(eyePos, u_TexGenEyePlaneT),
    dot(eyePos, u_TexGenEyePlaneR), 1.0);
```

修复后枚举确认 `u_TexGenEyePlaneS` 回到活跃列表（12 → 13 个活跃 uniform），视觉恢复。
回归测试 `VertexShaderGeneratorTest#eyeLinearTexGenFeedsEveryPlaneFromSingleConstructor`
（ANTLR 解析生成物断言 AST）锁定该结构。

### 「低约一格」的解释

非独立缺陷：BPR 按设计把星野画在洞底（`translateY(-0.75)`，星野落在 y+0 而非原版的
y+0.75）。看穿失败时洞口唯一可见的就是这层星野，于是表现为「整体低一格」；看穿恢复后
该错觉随之消失。

### 4. 光影下的两个叠加缺陷（2026-09-02 定位修复）

**管线重载风暴**：BPR 的看穿视图是**顺序**渲染（先 renderDeps 末地视图、换
`mc.world`+`mc.framebufferMc` 后再 renderSelf 主世界），同一帧内维度名往返切换。
`PipelineManager.preparePipeline` 此前把维度切换当世界卸载处理——每次切换
destroyPipeline() 再重建，每帧两次，且每次重建杀 celeritas worker 线程，导致地形永远
来不及重建（现象：门进入视角即 5 fps、主世界地形全部消失，日志 `Dimension changed …
reloading pipeline` 每帧刷屏）。

修复：`preparePipeline` 改为 `pipelinesPerDimension` 缓存切换（维度间翻零成本），
地形 program 缓存同步改为按管线实例分键
（`IrisCeleritasChunkProgramOverrides.programsPerPipeline`），`MinecraftFramebufferHelper`
动态读 `mc.getFramebuffer()` 使 Iris 合成自动写入 BPR 换入的传送门 FBO——两个世界各自
获得完整光影效果。EntityRenderer 注入点守卫回退为纯深度 `RenderWorldRecursionGuard`
（真嵌套防御，顺序双 pass 不再触发跳过）。缓存的维度管线在停止渲染闲置 30 秒后自动
销毁回收（含地形 program 惰性清理），显存不随门/维度数量累积。

**星野叠加层被旁路**：光影下实体渲染阶段 Iris 的 gbuffers program 已绑定，glsm 按设计
对外部 program 让位（`ShaderManager.preDraw` 提前 return），原版 TESR 的 texgen FFP
仿真永远不执行，Iris program 按实体属性格式误读 `POSITION_COLOR` 顶点流 → 星野几乎
不可见且随视角转动闪烁。

修复：无 world 合成 TE 在光影下改走替代渲染器（`EndPortalRenderer`，CPU 烘焙投影 UV +
实体兼容顶点格式，本就为光影设计）；BPR 的淡出钩子（`shouldRenderFace` 首调里
`blendFunc(CONSTANT_ALPHA,…)` + 裸 `glBlendColor(0,0,0,opacity)`）完整复刻——检测到
CONSTANT_ALPHA 因子对即认定钩子触发，从真实 GL 状态读回常量 alpha（该裸调用绕过 glsm
跟踪），按「CONSTANT_ALPHA ≡ SRC_ALPHA + 首层顶点 alpha=opacity」折入第 0 层顶点
alpha，后续叠加层保持全强度并跳过 precompose（单层合成无法只淡出 sky 层）。无光影路径
与真实传送门行为零改动。

## 验证记录

- 环境：实例 `crl_t/1.12.2-Cleanroom`（HMCL 布局）、Cleanroom + forgelin-continuous、
  存档 `新的世界`、传送门 `(-337,70,290)`、NVIDIA RTX 5070 Laptop（驱动 610.74）。
- 无光影：看穿（洞内可见末地黑曜石柱）+ 星野旋涡形状 + 淡出 + 位置均正常（用户实机确认，
  构建 `alpha-0.0.6-c7f3d45-portalfix6`）。
- 光影（BSL）：看穿与主世界各自带光影效果、帧数与地形正常（构建
  `alpha-0.0.6-1bae36f-portalfix10`）；星野叠加层可见、转动视角无闪烁、淡出正常、换维度
  正常（构建 `alpha-0.0.6-1bae36f-portalfix12`，2026-09-02 用户实机确认）。
- 不装 BPR 的原版传送门/折跃门回归待补。

## 注意事项

- 排查期间用的 BPR jar 是带临时插桩的构建（`[BP surface]`/`[tpdbg]` 日志不在其源码树
  当前版本中）；换成正式 BPR 构建不影响本兼容层的行为（Actinium 侧不依赖这些日志）。
- glsm 的 program 绑定跟踪与真实 GL 可能不同步（`ShaderManager.commitVariant` 直连
  backend `useProgram`，不更新 `GLStateManager` 的跟踪值）；用
  `GLStateManager.getActiveProgram()` 做诊断时需注意其值可能是跟踪值而非真实绑定。
