# GalaxySpace 渲染兼容

最后更新：2026-09-23。相关 issue：[Actinium #164](https://github.com/DHJComical/Actinium/issues/164)。
验证分支：`fix/galaxyspace-negative-sky-color`。

## 模组与源码入口

目标模组为 GalaxySpace（Galacticraft 4 addon），源码仓库
[`BlesseNtumble/GalaxySpace`](https://github.com/BlesseNtumble/GalaxySpace) `dev_1.12.2` 分支，
依赖 `AsmodeusCore-1.12.2-1.0.4`（仓库 `libs/` 内嵌 dev jar）。自定义天空不属于 GalaxySpace
本体，而在 AsmodeusCore 中：

- `asmodeuscore.core.event.AsmodeusClientEvent#onTick`：维度 0 且
  `AsmodeusConfig.enableSkyOverworld`（默认 `true`，注释自述 "Not support shaders"）时
  `world.provider.setSkyRenderer(new SkyProviderOverworld())`——即 issue 所述 "usually the
  custom GS skybox is enabled by default"。
- `asmodeuscore.core.astronomy.sky.SkyProviderBase#render`（Forge `IRenderHandler`）：天空
  dome、星星、银河背景图（`textures/environment/background/`）、日月的总绘制入口；大量
  `org.lwjgl.opengl.GL11` 直调，运行时被 `GLSMRedirector` 字节码改写进 `GLStateManager`。
- Cleanroom patched `RenderGlobal#renderSky` 开头 `skyRenderer != null` 即
  `renderer.render(...); return;`，vanilla 天空分支整段跳过，天空完全由 `SkyProviderBase`
  绘制。

## 问题与证据

Issue #164：夜晚 Overworld 看向天空，自定义 skybox 显示错误——整体发白的夜空、银河背景
贴图呈灰色块（用户截图与复现步骤：`/time set night` 后看天，"custom GS skybox is enabled
by default"）。白天不复现。

字节码取证（`javap` AsmodeusCore dev jar）：`SkyProviderBase#render` 在画天空 dome
（`glSkyList`，顶点格式 `POSITION`、无顶点色）前执行：

```java
float f9 = mc.player.posY / 400.0F;            // y=64 → 0.16
GL11.glColor3f((float) skyColor.x - f9,        // 夜晚 getSkyColor ≈ 0，
               (float) skyColor.y - f9,        // 0 - 0.16 → 负值
               (float) skyColor.z - f9);
```

## 根因

GL 规范要求 `glColor*` 的浮点分量在 API 边界钳到 [0,1]（原版/Forge 下该负值变 `(0,0,0)`
黑，夜空正常）。Actinium 下该调用被重定向进 `GLStateManager#changeColor`，而它把原始负值
原样存入 `ctx().color` 缓存；dome 顶点无顶点色，FFP 模拟从
`Uniforms#sanitizeUniformColor(GLStateManager.getColor())` 取 `u_CurrentColor`，该函数把
**任何负通道**当作 `clearCurrentColor` 的 `(-1,-1,-1,-1)` dirty sentinel 归一成
**不透明白色**（原为 JourneyMap 负 alpha 全透明问题而设，见
`UniformColorSanitizeTest`）。于是夜空 dome 以白色绘制：截图的灰白天空即 dome 本体，
灰块是银河暗纹贴图叠在白底上的观感。白天 `skyColor - f9 > 0` 不触发，故只夜晚复现。

## 修复

- `Color4.clamp01(float)`：把单个 `glColor` 分量钳到 [0,1] 的纯函数（GL API 边界语义）。
- `GLStateManager#changeColor`：入口对 RGBA 四通道统一 `clamp01`，缓存与后续
  `u_CurrentColor` 上传（含 `ImmediateModeRecorder`、`dirtyColorAttrib` 路径）只见到
  [0,1] 内的值。
- `clearCurrentColor` 的 sentinel 直写 `ctx().color`、不经 `changeColor`，
  `sanitizeUniformColor` 及其 4 个既有测试保持原契约不变。
- `hudCacheOverride` 的 alpha 覆写顺序不变（clamp 后仍 `alpha < 1` 即覆写为 1，与原行为
  等价）。

## 验证

- `./gradlew check --no-daemon` 通过；新增 `Color4ClampTest` 4 例（负值钳 0、超 1 钳 1、
  区间透传、钳后夜空色经 sanitize 保持黑色不被洗白），`UniformColorSanitizeTest` 4 例不回归。
- 用户实机确认（2026-09-23）：GalaxySpace + Galacticraft 夜晚天空恢复正常，银河贴图显示
  于暗背景，白天天空无回归；JourneyMap 网格与 GUI 字体颜色（同走 `changeColor` 路径）无
  异常。

## 已知边界

- `AsmodeusConfig.enableSkyOverworld` 自述不支持光影包；本修复针对无光影路径下的错误
  颜色，光影包下 GS 天空的表现不在本次范围。
