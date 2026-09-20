# Angelica GLSM 覆盖面同步记录：02f0b0fc

## 同步目标

- 上游仓库：`D:/Code/Angelica`（只读参照），glsm 树为
  `glsm/src/main/java/com/gtnewhorizons/angelica/glsm/`
- 基准 SHA：`02f0b0fca17808303d9a5cdad7f245232219198a`（"Fix mipmaps bleeding (#2112)"）
- 目标：以覆盖面差异分析为输入，把 Angelica GLSM 相对 Actinium `glsm/` 的差异逐包落地，
  排除「已有等价替代」项；sdl-gpu 前置件、多上下文架构、GLES 支持三个方向按用户决策纳入。

## 分支与日期

- 分支：`feat/glsm-angelica-sync`（自 PR #157 合并点 `498574a7` 检出）
- 日期：2026-09-18（Asia/Shanghai）
- 工作约束：coder 子代理严格串行（共享工作树 + Gradle 互相干扰）；所有提交由主代理审阅
  diff 后执行；每个 WP 以 `./gradlew test`（最终全量 `check`）为门禁。

## 已阅读的规范文件

- `AGENTS.md`（禁 FQN、注释英文、UTF-8 无 BOM、Fail Fast、禁反射、模块边界、DEBUG 开关
  走 `GLSMDebug`、Conventional Commits）
- `docs/architecture.md`、`docs/upstream-maintenance.md`
- `docs/implementation-differences/overview.md`、`coverage-status.md`（差异现状）

## 提交清单（9 个，按落地顺序）

| 提交       | WP   | 内容                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|------------|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `f23e68ce` | WP1  | **多上下文架构**：新增包私有 `GLContextState`（`glsm/.../GLContextState.java`），替代静态单例状态；`ctx()` 按线程取上下文（`GLStateManager.java`），splash 迁移时经 `replayStateToBackend` 全量重放；GLStateManager 与上游基底同步约 117 个方法；`RenderBackend` +45 方法、`Lwjgl3GLRenderBackend` 实现；新状态类 `PixelUnpackState`/`ImageUnitArray`/`ImageUnitBinding`；14 个新测试。23 文件 +3494/−1122                                                                                 |
| `0bb23726` | WP9  | shader/：`GlVersion` +GL_33/GL_41；`InternalTextureFormat` +RGBA2/RGBA4/RGB565/RGB10_A2UI                                                                                                                                                                                                                                                                                                                                                                                                  |
| `29b5b1dc` | WP2  | `GLSMRedirector` 857→1156 行：GL 表 +69、owner 表 +68（17 新 owner）、新增 `GL_DESC_REDIRECTS` desc 精确匹配机制 +28 条、扫描前缀放宽至 `org/lwjgl/opengl/`、unmapped GL 检测改走 `GLSMDebug.isEnabled()`                                                                                                                                                                                                                                                                                  |
| `0df107c0` | WP5  | FFP 生成扩展：line stipple 片元模拟（`RenderBackend.provokingVertex`→`GL32C.glProvokingVertex`，随 `prepareWideLineEmulation` 切换）、overlay 叠加色、GL_COLOR_SUM（含 enable/isEnabled case 补齐）、unit 2/3 texcoord varying 全链、`FragmentKey` Mesa 门控、`ShaderManager.warmUp()` 接入 `enable()`。15 文件 +553/−169                                                                                                                                                                  |
| `4fb04cce` | WP8  | sdl-gpu 前置件 9 件：`glsm/shader/`（ShaderType、UniformType、GlslVulkanPreprocess、SpirvCompiler、SpirvShaderTranslator）、`glsm/hooks/`（ShaderWorkSubmitter、ShaderTransformPostProcessor、PerFrameUniformBlock）、`hooks/events/LoadingCheckpointEvent`；`GlslTransformUtils` +`parseFullQuiet`/`parsePreQuiet`；`glsm/build.gradle` +compileOnly `lwjgl-shaderc`/`lwjgl-spvc` 3.4.1；剥除 `@Lwjgl3Aware`。12 文件 +1032                                                               |
| `dbe50cc0` | WP6  | 事件接线：新事件 AlphaStateChange/ShaderColorChange/ProgramDelete + 发射点（glAlphaFunc、alpha test 开关、setShaderColor、glDeleteProgram）；splash 完成时发 LOADING_CHECKPOINT；Iris 侧 `StateUpdateNotifiers` +alphaFunc/alphaTest/colorModulator，`IrisInternalUniforms` 接入 `iris_currentAlphaFunc`/`iris_ColorModulator`（经 `IrisGLSMBridge`）；CaptureGate 移植 + `GLDebug.isActive()` 快路径；`glDebugMessageCallback(KHRDebugCallback)` 全链 + 3 条 desc 重定向。17 文件 +458/−8 |
| `affbdf36` | WP7  | GLES 支持：`GLESCaps`/`GLESFormatRemap`/`GLTypes`/`LTWWorkaround`；`remapTexImageForGLES` 接入全部 glTexImage/glTexSubImage/glReadPixels；GLES 下 `glGetTexImage` 走 FBO readback（含 BGRA→RGBA 交换）；`RenderSystem.isGLES()/isLTW()` 懒检测（失败不闩锁），GLES 下跳过 DSA 探测、禁用 bufferStorage；Lwjgl3 后端 enable/disable 经 `GLESCaps.isCapAllowed` 门控。10 文件 +679/−28                                                                                                       |
| `0c1c96bb` | 修复 | `RenderSystem.initRenderer` 的 ARB-DSA 回退分支误查 `capabilities.OpenGL45`（死代码）；lwjglx 的 `ContextCapabilities` 无 `GL_ARB_direct_state_access` 字段，改为扩展串探测                                                                                                                                                                                                                                                                                                                |
| `f3d1356b` | WP4  | recording 编译期烘焙：`AttribSnapshot`/`AttribLayoutKey`/`IndexedDrawCapture`/`IndexedDrawBatch(+Builder)`/`BatchedIndexedDrawCmd`/`PixelDataSnapshot`；4 个 client-index glDrawElements 重载与 VAO/EBO offset 变体不再抛 UOE，改为录制期烘焙（回放不依赖存活 VBO/VAO，修掉陈旧数据隐患）；`TexImage2DCmd`/`TexSubImage2DCmd` 快照 `PixelUnpackState` + 离堆像素深拷贝；`recordSecondaryColor` opcode 57；重录制 display list 时释放旧编译产物。23 文件 +1816/−262                         |

合计 90 文件，+8431/−1677；测试 633 个全绿（新增约 40 个）；全量 `./gradlew check --no-daemon` 通过。

## 关键架构变化

- **静态状态 → 多上下文**：GL 状态不再挂 `GLStateManager` 静态字段，改为包私有
  `GLContextState` 按线程上下文存取；splash/异步线程切换经 `stateSeedPending` +
  `replayStateToBackend` 重放。该架构比原静态机制更通用，按用户决策替代之。
- **redirect 机制**：新增 `GL_DESC_REDIRECTS`（按方法描述符精确匹配），与名称级表并存；
  unmapped GL 调用检测仅在 `GLSMDebug.isEnabled()` 下告警（遵守 DEBUG 开关约定）。
- **FFP 生成轴扩展**：`VertexKey` 位布局重排为 4 位基址 per-unit 编码（`MAX_UNITS=4`、
  `FFP_LIGHT_COUNT=2`），`FragmentKey` global 位 10→13（overlay/lineStipple/colorSum）；
  纹理单元 enable 以 fragment 侧 unit mask 为准，顶点侧只发会被采样的 varying。
- **recording 烘焙语义**：编译 display list 时把索引/顶点数据烤进共享 (VAO,VBO,EBO) 三元组，
  命令流只存 `BatchedIndexedDrawCmd` 占位符（complex object 引用）；`CompiledDisplayList`
  持有并负责释放批次。

## 剥离与适配记录

- `@Lwjgl3Aware`（lwjgl3ify 注解）：SpirvCompiler/SpirvShaderTranslator 各一处，直接删除。
- `SystemProperties`：CaptureGate 内联 `angelica.debug.markers`/`org.lwjgl.util.Debug` 属性键；
  LTWWorkaround 开关内联为 `-Dangelica.disableLtwWorkaround`。
- `GLDebug.adaptDebugCallback` 使用 MethodHandle：lwjglx 与 lwjgl3 的 `KHRDebugCallback`
  类形不同（final Handler 制、无 `invoke`），静态不可达，属规范允许的「穷尽正常入口后」
  情形，javadoc 已记录原因。
- `TexImage2DCmd` 等改挂 `GLTypes.name()`（WP7 移植件）做类型名输出。

## 排除项（既定决策，未落地）

VAOManager（Actinium 用 `VertexAttribState` 等价替代）、GlFence/GlStreamingRing 上游新版、
EarlyRedirectorCore、GlintColorHandler、Lwjgl2 后端/VSync/Display 改写、
config/SystemProperties 包、Tracy、FFPVertexLighting/InstancedAttribs/uploadGeneration、
texture 六枚举与 AlphaTestFunction 的 glsm 副本、FFPUniformBlock+UBO ring（纯性能路径，
逐 uniform 上传为等价机制）、awareOwnerRedirects/AwareGLDebugShim。

## 遗留与风险

- WP4 烘焙的 display list VBO/EBO 路径（夜晚星空、AR 天穹 display list）尚未单独冒烟，
  建议发布前 `runClient` 夜观星空一次。
- GLES 着色器重发光（330 core → 320 es，`SpirvShaderTranslator.glslToGlslEs`）未接线：
  shaderc/spvc natives 为 compileOnly、不随包发布；差异已写入 `CompatShaderTransformer`
  javadoc，待 sdl-gpu 后续 WP 决策。
- LOADING_CHECKPOINT 目前只有 splash 完成一个发布点且暂无订阅者（消费端在 sdl-gpu 侧）；
  上游 Mixin 侧发布点（MixinFMLClientHandler/MixinSimpleReloadableResourceManager）在
  Actinium 无对应物。
- `enterWorkerContext`/`exitWorkerContext` 上游仅测试 harness（GLCoreTest）使用，Actinium
  无生产调用点可接。
- PROGRAM_DELETE 事件暂无订阅者（上游用于 per-frame uniform dedup 表，Actinium 无此表）。
- 观察项：`GLTypes` 与 `VertexAttribState.Attrib.glTypeSizeBytes`、`GLDebug.getDataTypeName`
  功能重叠，可后续合并去重。
- 参照基准是上游仓当前检出（变动目标）；后续同步应先核对上游新提交。

## 验证状态

- 每个 WP：`./gradlew :glsm:compileJava compileJava test --no-daemon` 通过。
- 收尾：`./gradlew check --no-daemon` 通过（含 remap Jar 结构校验）。
- 实机（2026-09-19）：dev 客户端 `runClient` 进世界，无光影包 FFP 模拟下天空、天体、
  地形、信标光柱渲染正常。期间「太阳/月亮变小圆球」的疑似回归报告经排查确认为测试
  环境差异，非本分支回归：dev 实例装有 Advanced Rocketry 且 `overworldSkyOverride=true`，
  圆球即 AR 天体的设计外观（混合/纹理上传/FFP 生成/录制烘焙/矩阵/拦截面逐项排查均无
  行为性回归）；关闭该覆写后原版方形太阳渲染正确。
- 编码合规：新文件 UTF-8 无 BOM、无 FQN、无空 catch、无 `com.dhj.actinium` 反向引用。
- 未做：真实光影包、GLES 环境、LTW 环境验证。
