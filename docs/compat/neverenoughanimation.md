# NeverEnoughAnimation 兼容性说明

兼容状态：**已验证**（GUI 开/关淡入与 Actinium 快速物品渲染路径共存）
最后更新：2026-09-20

## 验证范围

- 版本：1.0.7（CurseForge 1062347:7289408）
- 相关功能：GUI 开/关淡入（OpeningAnimation）
- 相关 issue：#145

## 症状：箱子／背包 GUI 中的物品不可见（issue #145）

### 现象

同时安装 NeverEnoughAnimation（下称 NEA）后，打开箱子或背包时格子里的物品**永久不可见或极度发虚**，
GUI 面板与文字正常，无崩溃、无日志异常。只在**未启用光影包**时出现——`IrisApi.getInstance().isShaderPackInUse()`
为 true 时 Actinium 不接管物品路径，症状不出现。

### 机制

1. NEA 的 GUI 开/关动画在动画窗口内把**物品顶点 alpha** 乘以 `NEA.getCurrentOpenAnimationValue()`
   （0→1，`Opening/Closing animation time` 默认 90ms），注入点为 `LightUtil.renderQuadColor` 与
   `BufferBuilder.color`。
2. Forge 的 `ForgeModContainer.allowEmissiveItems` 默认为 **true**，因此
   `RenderItem.renderModel(model, color, stack)` 改道 `ForgeHooksClient.renderLitItem`，**GUI 槽位物品也走
   这条路径**（`color = -1`；已在本地 Cleanroom 0.6.12 的 `RenderItem` 源码与 Forge 1.12.x patch 中确认）。
3. Actinium 默认接管该路径（`advanced.useFastLitItemRendering`、`advanced.useFastLitItemDisplayLists`
   默认均为 `true`），两条捷径都假定顶点颜色只由 baked quad 数据决定：
   - **display list 分支**：`FastLitItemDisplayListCache.compile()` 内部用 `renderItem.renderQuads(...)`
     编译顶点，**会经过 `LightUtil.renderQuadColor`**，于是编译那一帧的动画 alpha 被**烘焙进 display list**。
     缓存 key 仅为 `(模型身份, quad tint 颜色)`，不含任何动画状态，之后 `glCallList` 永久回放该 alpha——
     打开 GUI 的第一帧 alpha≈0，物品因此持续透明。这是"永久不可见"的直接来源。
   - **raw append 分支**：`actinium$appendRawItemQuads` 用 `addVertexData` 直接搬运 baked quad 字节，
     **整条绕过 `LightUtil`／`BufferBuilder.color`**，表现为 NEA 的淡入动画消失。
4. NEA 注入原版 `GlStateManager.color(FFFF)` 的那条路径在 Actinium 下本就不生效：`GLSMRedirector` 已把
   所有调用点重定向到 `GLSM.glColor4f`（dev 日志逐条可见，例如
   `Redirecting call in net.minecraft.client.gui.inventory.GuiChest from GlStateManager.color(FFFF)V ...`）。
   因此物品顶点 alpha 只剩 `LightUtil`／`BufferBuilder` 两条通道，这两条恰好都被快速路径绕过或固化。

### 修复：顶点 alpha 覆写扩展点（2026-09-20）

Actinium 侧新增一个通用查询点，让"正在缩放物品顶点 alpha"的模组能被快速路径感知：

- `com.dhj.actinium.render.ItemVertexAlphaOverride`：接口，返回当前施加在物品顶点 alpha 上的乘数，
  `1.0F` 表示无覆写；
- `com.dhj.actinium.render.ItemVertexAlphaOverrides`：注册与查询点，`isActive()` 在客户端渲染线程逐物品
  轮询（`CopyOnWriteArrayList`，无锁、无分配）；
- `com.dhj.actinium.compat.neverenoughanimations.NeverEnoughAnimationsAlphaOverride`：NEA 实现，
  `install()` 由 `Actinium.onInit` 调用，经 `Mods.NEVERENOUGHANIMATIONS` 门控后才加载 NEA 类；
- `ForgeHooksClientIrisMixin`：`isActive()` 为真时**跳过 raw append 与 display list 缓存两条捷径**，
  改走 `renderItem.renderQuads(...)`——NEA 的 `LightUtil` alpha 缩放照常生效，同时不写入也不读取会固化
  alpha 的缓存。动画结束后快速路径自动恢复，无 NEA 时该方法恒为 false。

不采用"整体让位给 Forge 的 `renderLitItem`"作为修复：该路径自带 lightmap/lighting 分段逻辑
（`OpenGlHelper.setLightmapTextureCoords`、`GlStateManager.enableLighting/disableLighting`），会把
Actinium 不拥有的光照状态写进非光影的固定管线模拟，实测使 GUI 持续变暗。因此兼容层只放弃两条捷径，
保留 Actinium 自己的 `renderQuads` 分支，把状态差异降到零。

### 验证

- `./gradlew check --no-daemon` 通过（含新增 `ItemVertexAlphaOverridesTest`：无来源、乘数为 1、
  激活、激活状态往返、多来源任一激活、清理后复位）。
- dev 实机（Cleanroom 0.6.12-alpha + NEA 1.0.7 + 47 模组环境，无光影）：打开箱子后物品随 GUI 淡入并最终
  完全可见，反复开关与切换不同箱子正常；背包与世界物品渲染无回归。
- 无 NEA 时 `isActive()` 恒 false，物品渲染分支与改动前完全一致。

## 顺带发现：NEA dev 环境下箱子面板变暗（非 Actinium 缺陷）

排查 #145 期间曾观察到"只有箱子 GUI 面板变暗、背包正常"。最终归因于 **NEA 自身的 dev-only 调试代码**：

- `NEA.drawScreenDebug` 挂在 `GuiScreenEvent.BackgroundDrawnEvent` 上，末尾调用
  `GlStateManager.enableLighting()` 与 `RenderHelper.enableStandardItemLighting()`，并不恢复调用前状态
  （开头是 `disableLighting()`）；
- `BackgroundDrawnEvent` 由 `GuiScreen.drawDefaultBackground()` post，而 `GuiChest` 是少数**覆写
  `drawScreen` 并先调 `drawDefaultBackground()`** 的容器，紧接着 `GuiContainer.drawScreen` 的第一件事
  就是绘制面板纹理——面板因此在"标准物品光照"下绘制而变暗；`GuiInventory` 不调用
  `drawDefaultBackground()`，所以正常；
- 该方法由 `FMLLaunchHandler.isDeobfuscatedEnvironment()` 门控，**只在 dev 环境执行**，正式运行不受影响。

处理建议：向 NEA 上游反馈（应改为恢复进入前状态，或移到 `DrawScreenEvent.Post`）；Actinium 侧不为
上游调试代码做状态兜底（Fail Fast）。

## 残余限制

- NEA 的 `BufferBuilder.color` 注入落在 Actinium `BufferBuilderMixin` 的 `@Overwrite` 方法体上
  （dev 日志无 Critical injection failure），其 `@Local` 捕获语义未逐一验证；本修复不依赖该注入。
- NEA 中只有 GUI 开/关淡入会走顶点 alpha 通道（`withAlpha` 仅用于该路径与背景渐变），item move／hover
  动画不受本机制影响。
- 上述"NEA dev 环境箱子面板变暗"未做上游修复，仅记录归因。
