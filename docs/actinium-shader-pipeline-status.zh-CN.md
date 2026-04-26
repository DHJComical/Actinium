# Actinium 光影管线状态记录

最后更新：2026-04-26

本文档记录到目前为止 Actinium 光影管线的工作进展、当前项目状态、核心架构、参考项目，以及后续开发需要重点关注的潜在问题。

## 当前状态

Actinium 目前已经可以以可用状态运行 MakeUp Ultra Fast 光影包。核心世界渲染、天空、水面、后处理、GUI 隔离、TAA 兼容、体积云贴图采样和阴影路径都已经经过多轮修复。最新反馈状态是：场景渲染正常，体积云不再向四个方向无限拉伸，实时阴影在当前 MakeUp 测试配置下也已经恢复到可用状态。

当前实现还不是完整的 Iris 等价实现，而是一套兼容管线：它已经支持足够多的 OptiFine/Iris 风格行为，使 MakeUp 能够工作；但在完整 deferred 渲染还没接通的地方，仍然使用选择性 fallback 和受保护的集成点。

当前工作基线已经包含下文描述的 MakeUp 阴影接通结果。最近这一轮最关键的变更，主要集中在阴影兼容、实体阴影语义，以及为保证运行稳定性所做的最后清理。

## 已完成进展

### 稳定性和 GUI 隔离

- 修复了 post/final 渲染污染 GUI 的多种情况，包括白屏、物品栏消失、菜单残留、世界 pass 后 framebuffer 状态泄漏。
- 在 post/final 之后恢复主 framebuffer 状态，避免 GUI 和第一人称渲染受到 shader framebuffer 绑定影响。
- 稳定了第一人称手部和手持物品渲染，解决了某些方向面消失的问题。

### 地形和水面

- 从多次“地形不可见、地形变黑、地形奶白色”的回归中恢复了正常地形渲染。
- 修复透明地形/水的 block metadata 和 shader block mapping，使原版水能被光影包逻辑正确识别。
- 将水面从黑色恢复到正常状态，并改善了全屏下水反异常的问题。
- 对 solid/cutout 地形禁用了大范围外部 terrain redirect，因为它会导致场景丢失；translucent 仍然是水面和后处理交互中最敏感的路径。

### 阴影和实体

- 经过多轮迭代后，MakeUp 阴影路径已经恢复到可用状态，期间处理过地形全黑、阴影缺失、噪点/黑线、以及随镜头晃动的阴影抖动等问题。
- 当前已验证状态下，方块、草、树叶/植物，以及动物阴影都已经正常渲染。
- 修复了动态植物阴影，使摆动植物不再投射静止轮廓。
- 将 shadow 实体渲染接回外部 `shadow` program，并在实体绘制期间刷新 `entityId` / `entityColor` 语义，使其更接近 shader-core / Iris 的预期。
- 为避免 `RenderGlobal.renderEntities` 带来的副作用，实体侧 shader 绑定经过多次收敛，最终落在逐实体绑定路径上，不再阻碍最终阴影结果。

### 天空和云

- 多轮重写天空处理，解决白天空盒、四周闪烁、太阳/月亮贴图异常、下半天幕异常等问题。
- 用 managed sky 路径替代不稳定的原版天空抑制逻辑，以更好地保留光影包天空颜色和天体处理。
- 云颜色同步已经修复到当前可用状态。
- 增加 shader pack `clouds=off/fast/fancy/on` 对原版云渲染的覆盖支持。
- 修复自定义光影包贴图的 wrapping：`gaux2` 和 `noisetex` 不再使用 `CLAMP_TO_EDGE`，而是对云/噪声贴图使用 `REPEAT`。这修复了 MakeUp 体积云向四个方向无限拉伸的问题。

### 后处理和 deferred 兼容

- 启用了外部 scene post programs 和 final pass，不再硬编码禁用 post 链。
- 增加常见 OptiFine/Iris post 输入的 sampler/unit 兼容，包括 `colortex4..7`、`gaux1..4`、`depthtex0..2`、`shadowtex0/1`、`shadowcolor0/1` 和 `noisetex`。
- 扩展 post 深度目标，支持 `depthtex2`。
- 调整 `prepare` 和 `deferred` 的执行时机。旧的 world 早期执行路径已被禁用；兼容执行现在发生在真实场景已经存在之后的 post phase。
- 分离 `prepare` 和 `deferred` 的处理：`prepare` 不再覆盖场景 `colortex1`，`deferred` 可以基于当前真实场景运行并合并输出。这修复了 sky/prepass 数据替换世界颜色导致的奶白色场景。

### TAA

- 参考 Iris 26.1 改善了 MakeUp 的 TAA 行为。
- 静态模糊、移动模糊、重影和相机相对浮动都已降低到可用标准。
- TAA 仍然是高风险区域，因为它依赖 previous matrices、previous camera position、depth snapshot 和 history buffer 语义。

### 光影包 GUI

- 做了更完整的光影包配置 GUI，整体参考 Iris 风格配置界面。
- 修复配置项滚出可见区域后仍可点击的问题。
- 移除了游戏内光影配置面板的泥土背景，方便玩家边观察画面边调整光影选项。
- 修复光影包选择界面按钮在横向和纵向上的对齐问题。

## 当前架构

### 主管线入口

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumRenderPipeline.java`

这是当前光影集成的核心单例，负责：

- 跟踪渲染阶段：world、sky、clouds、weather、shadow、post、final。
- 捕获 camera、矩阵、雾颜色、上一帧数据、frame counter 和 TAA jitter。
- 解析并编译外部光影包 programs。
- 管理 world-stage targets、post targets、shadow targets、fallback textures、自定义 shader pack textures 和 terrain input textures。
- 执行 `prepare/deferred/composite/final` programs。
- 在 shader pass 后恢复 OpenGL/framebuffer 状态。
- 管理地形与实体的 shadow pass 集成，以及当前面向 MakeUp 的阴影兼容逻辑。

### Post Targets

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumPostTargets.java`

管理 OptiFine/Iris 风格的 post render targets：

- `colortex0..3`
- `gaux1..4`
- `depthtex0..2`
- target clear directives
- ping-pong source/write textures
- post draw buffers 的 framebuffer 绑定
- scene、pre-translucent depth 和部分中间输出的 copy

当前兼容层仍然假设 8 个 color targets。更复杂的光影包可能需要更多 color attachment、image bindings、compute support 或更精确的 buffer flip 语义。

### Post Shader Interface

`src/main/java/com/dhj/actinium/shader/pipeline/ActiniumPostShaderInterface.java`

负责绑定外部 post programs 需要的 uniforms 和 samplers：

- Scene samplers：`colortex0..7`、`gcolor`、`gdepth`、`gnormal`、`composite`、`gaux1..4`
- Depth samplers：`depthtex0`、`depthtex1`、`depthtex2`
- Shadow samplers：`shadow`、`watershadow`、`shadowtex0`、`shadowtex1`、`shadowcolor0`、`shadowcolor1`
- Temporal inputs：previous matrices、camera position、previous camera position、frame counters、frame time
- Environment inputs：fog color、sky color、rain、world time、moon phase、sun/moon/shadow light positions、eye brightness、water state

### Mixin 入口

`src/main/java/org/taumc/celeritas/mixin/features/render/EntityRendererActiniumPipelineMixin.java`

主要 world render hooks：

- 开始 world pass。
- 保留旧的 prepare/deferred hooks，但当前会被 pipeline guard 禁用。
- 捕获 world state，并在第一人称手部渲染前执行 post。
- 在世界渲染前执行 shadow pipeline。
- 开始/结束 weather stage。

`src/main/java/org/taumc/celeritas/mixin/features/render/RenderGlobalActiniumPipelineMixin.java`

主要 world geometry hooks：

- 开始/结束 managed sky。
- 跟踪 sky texture 和 celestial segments。
- 开始/结束 cloud stage。
- 替换或抑制不稳定的原版 sky/horizon 行为。
- 稳定 selection box 渲染，避免选中方块框出现多余斜线。

`src/main/java/org/taumc/celeritas/mixin/features/render/GameSettingsActiniumCloudMixin.java`

云模式覆盖 hook：

- 拦截 `GameSettings.shouldRenderClouds()`。
- 应用 shader pack 的 `clouds` 属性，同时保留原版低渲染距离下不渲染云的行为。

### Shader Pack Properties

`src/main/java/com/dhj/actinium/shader/pack/ActiniumShaderProperties.java`

解析 shader pack metadata 和 directives：

- `clouds`
- shadow flags 和 shadow settings
- `prepareBeforeShadow`
- program enable/disable directives
- 自定义贴图声明，例如 `texture.gbuffers.gaux2`
- 显式 buffer flip 指令，例如 `flip.composite.colortex1`
- shader option screen metadata

### 地形集成

地形 shader 集成当前仍然比较保守：

- solid 和 cutout 地形在安全默认路径下通常使用 Celeritas 默认 shader。
- translucent/water 路径有自定义 context，用于 shader block IDs 和水面处理。
- `ENABLE_EXTERNAL_TERRAIN_REDIRECT` 当前为 false，因为大范围 terrain redirect 曾经导致不可见地形、黑屏和 GUI 污染。

## 当前关键行为

### Prepare 和 Deferred

当前 `prepare/deferred` 路径是兼容折中，不是完整 Iris 复刻：

- 早期 `renderPreparePipeline` 和 `renderDeferredPipeline` 会被 `ENABLE_PRE_SCENE_PIPELINES = false` 阻止。
- `prepare` 和 `deferred` 通过 `executePreSceneProgramsInPostPhase` 在 post phase 执行。
- `prepare` 输出不会把 `colortex1` 合并回主 post targets。
- `deferred` 从真实 scene input 运行，并可以合并它自己的 draw-buffer 输出。

原因：

- MakeUp 的 `prepare/deferred` 如果运行太早，`depthtex0` 大量区域会像 sky，导致错误体积云和泛白输出。
- 合并 `prepare` 的 `colortex1` 会用 sky/prepass 颜色替换真实场景，使大部分几何变成奶白色。
- 让 `deferred` 从真实场景运行，可以恢复效果，同时避免重新引入白场景。

### 贴图 Wrapping

之前自定义 pack textures 统一以 `GL_CLAMP_TO_EDGE` 上传，这对重复采样的 noise/cloud textures 是错误的。

当前行为：

- `gaux2` 使用 repeat wrapping。
- `noisetex` 使用 repeat wrapping。
- 路径包含 `noise` 或 `cloud` 的贴图使用 repeat wrapping。
- 其他自定义贴图默认仍使用 clamp-to-edge。

这对 MakeUp 很重要，因为它的体积云会在世界空间采样 `gaux2`。如果使用 clamp，边缘像素会被无限拉伸，正好对应之前看到的四方向云拉丝。

## 参考项目

### Iris 26.1

路径：`D:\Github Desktop\Iris-26.1`

用途：

- `composite/deferred/final` pass 顺序。
- `CompositeRenderer` 中的 buffer flip 行为。
- cloud setting override 行为。
- TAA previous matrix 和 camera-position 预期。
- OptiFine/Iris sampler 命名和 render target 约定。

关键参考文件：

- `common/src/main/java/net/irisshaders/iris/pipeline/IrisRenderingPipeline.java`
- `common/src/main/java/net/irisshaders/iris/pipeline/CompositeRenderer.java`
- `common/src/main/java/net/irisshaders/iris/pipeline/FinalPassRenderer.java`
- `common/src/main/java/net/irisshaders/iris/mixin/sky/MixinOptions_CloudsOverride.java`
- `common/src/main/java/net/irisshaders/iris/shaderpack/properties/ShaderProperties.java`

### shader-core-src-mod

路径：`D:\Github Desktop\CleanBoom\shader-core-src-mod`

用途：

- 旧 shader-core 天空行为。
- horizon wall 和天空下半部分行为。
- 理解旧 shader-core 兼容代码期望如何绘制 world-stage sky。

注意：后续继续比较前应重新确认路径。早期搜索中使用过 `D:\Github Desktop\shader-core-src-mod`，该路径不存在。

### Minecraft 1.12.2 源码

路径：`D:\Github Desktop\CleanBoom\minecraft-src`

用途：

- 原版 `RenderGlobal.renderSky` 和 `RenderGlobal.renderClouds` 行为。
- `GameSettings.shouldRenderClouds()` 行为。
- `EntityRenderer.renderWorldPass` 附近的第一人称、GUI 和 framebuffer 顺序。

### glsl-transformation-lib

路径：`D:\Github Desktop\glsl-transformation-lib`

潜在用途：

- 后续做更稳健的 GLSL 解析和转换。
- 用结构化 shader transform 替代当前部分 regex/source-string 兼容补丁。

当前 Actinium 实现还没有把它作为核心依赖。

## 当前主测试光影包

主测试包：

```text
D:\Github Desktop\Actinium\run\client\shaderpacks\MakeUp-UltraFast-9.4c
```

重点检查过的 MakeUp 文件：

- `shaders/common/prepare_fragment.glsl`
- `shaders/common/deferred_fragment.glsl`
- `shaders/common/composite_fragment.glsl`
- `shaders/common/composite1_fragment.glsl`
- `shaders/common/composite2_fragment.glsl`
- `shaders/common/final_fragment.glsl`
- `shaders/lib/volumetric_clouds.glsl`
- `shaders/src/writebuffers.glsl`
- `shaders/shaders.properties`

MakeUp 相关结论：

- `deferred_fragment.glsl` 写入 `DRAWBUFFERS:14`，对应 `colortex1` 和 `gaux1`。
- `final_fragment.glsl` 注释中说明 `gaux1` 是 SSR/Bloom 辅助，`gaux2` 是 clouds texture，`gaux3` 是 exposure 辅助，`gaux4` 是 fog 辅助。
- `shaders.properties` 声明了 `texture.gbuffers.gaux2` 和 `texture.deferred.gaux2` 作为云贴图。
- MakeUp 的体积云依赖 `gaux2` 的重复采样。

## 已知风险

### Prepare/Deferred 语义

当前 `prepare/deferred` 路径是兼容折中。它适用于当前 MakeUp 测试状态，但可能不满足依赖精确 Iris 时序的光影包：

- Iris 在 shadows 后运行 `prepare`，在 translucents 前运行 `deferred`。
- Actinium 当前在 post 兼容执行中运行它们。
- depth 和 scene input 可能不符合要求精确 pre-translucent 或 pre-hand 时序的光影包。

后续应在 framebuffer state 和 terrain output 足够稳定后，逐步迁移到更接近 Iris 的真实阶段时序。

### Buffer Flip 语义

Actinium 已实现 explicit flips 和 ping-pong targets，但还不是完整的 Iris `BufferFlipper` 等价实现。

潜在失败模式：

- 从 target pair 错误一侧读取 stale data。
- history targets 不符合 TAA 预期。
- 简单 `flip.*` 指令能工作，但复杂多 pass 光影包失败。

### Render Target 覆盖范围

当前只建模了 legacy 8 个 color targets：

```text
colortex0, colortex1, colortex2, colortex3, gaux1, gaux2, gaux3, gaux4
```

可能缺失的功能：

- legacy alias 之外的更多 `colortexN` targets。
- image load/store bindings。
- compute programs。
- per-target resolution scaling。
- 更完整的 target format handling。
- 当前支持之外的 per-texture filtering 和 mipmap directives。

### Terrain G-Buffer 完整度

solid/cutout 地形在安全默认路径下还没有完整运行外部 terrain programs。

这仍然会限制：

- material-specific deferred data。
- normal/specular data。
- 依赖更完整 terrain gbuffers 的光影包水面和反射行为。

当前保守方案避免了大量严重回归，但也意味着许多高阶效果仍然不会出现；不过当前 MakeUp 阴影结果已经达到了可用标准。

### 天空和云边界情况

天空已经稳定很多，但仍然敏感，因为当前实现结合了：

- 原版 1.12.2 sky geometry。
- shader-core-like horizon rendering。
- 外部 `gbuffers_skybasic` 和 `gbuffers_skytextured`。
- post-stage volumetric clouds。

需要继续关注：

- 日出/日落颜色不匹配。
- 极高处的下半天空异常。
- 天体贴图深度/顺序问题。
- 云颜色随视角变化。

### GL 状态泄漏

之前许多回归都来自状态泄漏到 GUI、手部渲染或 framebuffer presentation。

高风险状态：

- active framebuffer
- draw/read buffers
- 高 texture unit 上绑定的 textures
- active shader program
- blend/depth/alpha/cull/scissor state
- viewport
- VAO binding

任何新的 pass 都应显式恢复或隔离这些状态。

### Debug 日志

Debug 日志很有用，但很容易过量。现有 debug 路径已经会采样 framebuffer/texture 中心像素并记录 pass bindings，新增 debug 应继续保持节流。

常用日志位置：

```text
D:\Github Desktop\Actinium\run\client\logs\latest.log
```

## 建议下一步

### 短期

- 提交当前这轮阴影修复，并持续同步文档中的已验证运行状态。
- 继续用 MakeUp 的高设置测试：体积云、水反、阴影、TAA、Bloom、DOF、volumetric light。
- 仅在出现新的回归时增加定向 debug；这轮阴影接通过程中加入的临时 debug 路径应继续服从运行稳定性。

### 中期

- 将 `prepare` 和 `deferred` 的执行时机改得更接近 Iris，而不是继续依赖 post-phase 兼容执行。
- 改进 buffer flip tracking，使其更接近 Iris `CompositeRenderer` 和 `BufferFlipper`。
- 扩展 legacy 8 aliases 之外的 render target 支持。
- 改进 shader-pack 自定义贴图解析，包括显式 texture formats、filtering 和 wrap directives。

### 长期

- 从 regex/string GLSL 修补迁移到结构化 GLSL transformation，可考虑 `glsl-transformation-lib`。
- 在不破坏 Celeritas 稳定性的前提下实现更完整的 solid/cutout terrain gbuffer 路径。
- 增加 compute/image 支持，以兼容更多现代光影包。
- 建立小型兼容测试矩阵：MakeUp 加至少一个 BSL/Complementary 风格光影包。

## 当前验证命令

Java 层验证使用：

```powershell
.\gradlew compileJava --no-daemon
```

运行时验证仍然需要启动客户端并手动检查当前启用的光影包。
