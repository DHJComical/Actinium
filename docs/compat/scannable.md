# Scannable 兼容（issue #94）

## 概述

- 模组：Scannable（CurseForge project `266784`，dev 依赖 `curse.maven:scannable-266784:3146549`，即 `Scannable-MC1.12.2-1.6.3.26.jar`）
- 症状：使用扫描器后——无光影时世界透视（可透过地面看到地下空洞）；开光影（Solas Shader）时画面全白并有拖影，无报错。
- 修复：条件 Mixin（`mixins.actinium.scannable.json`，mod id 门控 `scannable`）把 Scannable 的
  OptiFine 集成探针接到 Actinium，使其走自带的"外部 shader mod"渲染路径。

## 根因

Scannable 的扫描波渲染有两条路径（`ScannerRenderer`）：

1. **INJECT**（默认，`Settings.injectDepthTexture=true`）：扫描期间把主 framebuffer 的
   `GL_DEPTH_ATTACHMENT` 偷换成自己的 depth texture（`installDepthTexture`，帧首
   `RenderTickEvent.Phase.START` 执行），在 `RenderWorldLastEvent` 里全屏采样该纹理画扫描波，
   扫描结束（`uninstallDepthTexture`）用 `glFramebufferRenderbuffer(..., Framebuffer.depthBuffer)`
   "恢复"深度 attachment。
2. **OPTIFINE**：仅当 `ProxyOptiFine.isShaderPackLoaded()`（反射 OptiFine 的
   `net.optifine.shaders.Shaders.shaderPackLoaded`）为 true 时启用——world 渲染后把
   OptiFine 的 depth texture 复制到私有 FBO（`copyDepthTexture`），在
   `RenderGameOverlayEvent.Pre(ALL)` 阶段画扫描波。全程不触碰主 framebuffer 的 attachment。

Actinium 的 `FramebufferIrisMixin` 把主 framebuffer 的深度缓冲从 vanilla renderbuffer 换成了
共享 depth texture（`actinium$createDepthTexture`），并通过 `@Redirect` 跳过 vanilla 的
depth renderbuffer 创建——因此 `Framebuffer.depthBuffer` 恒为 0。由此产生两个破坏：

- **无光影透视**：INJECT 模式扫描结束的"恢复"实际执行
  `glFramebufferRenderbuffer(GL_DEPTH_ATTACHMENT, depthBuffer=0)`，把主 framebuffer 的深度
  attachment 完全卸下，此后地形渲染没有深度缓冲、失去自遮挡（透视）。扫描期间
  `installDepthTexture` 还会把 Actinium 的共享深度纹理从 attachment 上顶掉。
- **光影全白拖影**：Scannable 不认识 Actinium 的 Iris 管线（只反射 OptiFine），走 INJECT 且
  在 `RenderWorldLastEvent` 时机对"当前绑定的 FBO"（Iris render target）做 attachment 换装，
  并把 additive 混合的全屏扫描 quad 直接画进管线目标——gbuffer 深度被破坏、错误颜色逐帧
  叠加残留，表现为全白与拖影。

`ScanManager` 的扫描结果标记框（实体/方块高亮，即 issue 中预期的"扫描框"）同样按
`isShaderPackLoaded` 在 `RenderWorldLastEvent` 直画与 overlay 阶段绘制之间切换，受同一探针控制。

## 修复方案

条件 Mixin `MixinProxyOptiFine` 注入 Scannable 的 `ProxyOptiFine`：

- `isShaderPackLoaded()`：OptiFine 反射结果为 true 时放行（Actinium 环境不与 OptiFine 共存，
  实际不会发生）；否则当 Actinium 能提供深度纹理时改答 true，使 Scannable 全程走
  OPTIFINE 路径。
- `getDepthTexture()`：OptiFine 未提供纹理（返回 0）时改答主 framebuffer 的共享深度纹理
  （`IRenderTargetExt.iris$getDepthTextureId()`，即 `FramebufferIrisMixin.actinium$depthTextureId`）。

该纹理正是 Iris 光影管线 gbuffer 深度的落点（`DeferredWorldRenderingPipeline` 用它构造
`RenderTargets`），因此无论是否开光影，Scannable 采样到的都是世界深度。OPTIFINE 路径：

- 不触碰主 framebuffer 的 depth attachment（INJECT 的两处破坏点不再执行）；
- `copyDepthTexture` 采样深度纹理时绑定的是 Scannable 私有 FBO，无 feedback loop；
- 扫描波与扫描框统一推迟到 overlay 阶段绘制，与 OptiFine 用户所见行为一致。

业务逻辑集中在 `compat/scannable/ScannableShaderCompat`（mixin 只做注入）。

## 验证

- 编译：`gradlew compileJava` 通过；`MixinConfigurationTest` 已登记
  `mixins.actinium.scannable.json`。
- dev 运行验证（待执行）：装 Scannable 使用扫描器，无光影下世界不再透视、扫描框正常渲染；
  开 Solas Shader 后画面不全白、无拖影。
- 诊断开关：`-Dactinium.debug.scannable=true` 输出深度纹理探针与接管通知日志。

## 已知边界

- 扫描进行中开启/关闭光影（mode 切换窗口）属于 Scannable 自身在 OptiFine 下同样存在的
  边界情况，未做额外处理。
- OPTIFINE 路径的深度副本格式（R32F）由 Scannable 自有 shader 决定，与 OptiFine 环境一致，
  未做改动。

## Follow-up：深度副本纹理格式修正（dev 验证反馈）

首版修复后 dev 实测发现：绑定大量方块（如草方块，开阔地表场景）时扫描波覆盖区域不正确。
根因是 Scannable 自身 bug——其深度副本纹理按 `(GL_R32F, GL_RED, GL_UNSIGNED_BYTE)` 分配，
而 `GL_R32F` 仅接受 `GL_FLOAT`/`GL_HALF_FLOAT` 像素类型，无效组合使纹理分配失败、无存储，
copy pass 写入不完整 FBO，overlay 阶段采到未定义深度 → 反投影错乱、扫描波区域错位。
`MixinScannerRenderer`（`@ModifyArgs`）将该分配的像素类型修正为 `GL_FLOAT`；INJECT/RENDER
路径的深度纹理分配使用合法组合，不受影响。修正后 R32F 副本携带全精度深度，扫描波反投影
恢复正确。
