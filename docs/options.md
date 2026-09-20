# 渲染选项与实现落点

记录 Actinium 自有 GUI 选项的落点，方便定位与做取舍。选项模型与页面框架来自
`celeritas-common`，页面以 `OptionIdentifier` 绑定到 `SodiumGameOptions` 的持久化字段。

## 选项的组织方式

- 持久化字段：`celeritas-common` 的 `dhj.embeddedt.embeddium.impl.gui.SodiumGameOptions`，
  按 `Performance / Advanced / Quality / Notification / Debug / Window` 分段。
- 选项 ID：`dhj.embeddedt.embeddium.api.options.structure.StandardOptions`。
  Actinium 自有选项使用 `actinium` 命名空间（`ACTINIUM_MOD_NAME`）。
- 页面与分组：`src/main/java/com/dhj/actinium/gui/ActiniumGameOptionPages.java`
  （`general` / `quality` / `advanced` / `debug`）与
  `celeritas-common` 的 `...gui.options.CommonOptionPages`（`performance`）。
- 标签：`src/main/resources/assets/celeritas/lang/{en_us,zh_cn}.lang`。
- 构建期守卫：`MixinConfigurationTest` 要求每个已编译 Mixin 恰好在配置文件中声明一次，
  新增 Mixin 类必须同步 `src/main/resources/mixins.actinium.vintage.json`。

## 质量页 → DETAILS 分组

| 选项 | 字段 | 实现落点 |
| --- | --- | --- |
| 天气质量 | `quality.weatherQuality` | `mixin.vintage.features.options.MixinEntityRenderer`（`renderRainSnow` 重定向 `fancyGraphics`） |
| 树叶质量 | `quality.leavesQuality` | 资源重载路径 |
| 生物群系颜色噪声 | `quality.useBiomeColorNoise` 等 | 生物群系染色计算 |
| 暗角 | `quality.enableVignette` | `mixin.vintage.features.options.MixinGuiIngameForge` |
| 动态视野 | `quality.dynamicFov` | `mixin.vintage.features.options.MixinEntityRendererDynamicFov` |

## 动态视野（`quality.dynamicFov`）

默认开启；关闭后视野锁定在 `fovSetting`。

原版 1.12.2 的动态视野由三段构成，选项只作用于中间那一段：

1. `AbstractClientPlayer.getFovModifier()` 计算因子 —— 飞行 ×1.1、移动速度属性折算
   （含疾跑与速度效果）、拉弓最多 ×0.85。
2. `EntityRenderer.updateFovModifierHand()` 做缓动（系数 0.5）并钳制到 0.1~1.5。
   **这是该因子唯一的写入点，且只被 `updateRenderer()` 调用。**
3. `EntityRenderer.getFOVModifier(float, boolean)` 仅当 `useFOVSetting == true` 时把因子
   乘到 `fovSetting` 上。

`MixinEntityRendererDynamicFov` 在 `updateFovModifierHand()` 头部取消该方法并把
`fovModifierHand` / `fovModifierHandPrev` 都置为 1.0，于是第 3 步的插值恒为 1.0。
选择这个注入点而非在读取处替换表达式，是因为它无需访问局部变量、也不做"先乘后除"的
浮点往返（后者可能产生 1 ulp 偏差）。

同一方法内另外两项缩放**不受该选项影响**，与 OptiFine / Sodium 的 `dynamicFov` 语义一致：

- 死亡镜头（`getHealth() <= 0` 时的除法）；
- 水下（`Material.WATER` 的 ×60/70）。

`useFOVSetting == false` 的调用（手部渲染、Iris 的 `HandRenderer`）本来就不消费该因子。
该选项在光影包激活时同样生效——它只影响传入投影矩阵的 FOV 值。

**维护提示**：`fovModifierHand` 的写入点一旦被上游或其它补丁挪出
`updateFovModifierHand()`，该 Mixin 会静默失去作用（不会报错），需同步检查。
