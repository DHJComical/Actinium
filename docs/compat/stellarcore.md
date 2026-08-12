# StellarCore 兼容性说明

兼容状态：**已验证**（GUI/HUD 症状实为 Draconic Evolution 引起，与 StellarCore 无关）
最后更新：2026-08-12

## 验证范围

- 版本：1.5.22（`StellarCore-1.5.22.jar`）
- 整合包：NovaEngineering-World-cleanroom
- 相关功能：HUD 缓存与 InGameInfoXML HUD framebuffer
- 初始排查日期：2026-07-21；归因修正：2026-08-12

## 历史记录：GUI 闪烁或 Esc 菜单消失

### 现象与当时的验证结果

2026-07-21 在生产整合包中观察：同时启用以下两个 StellarCore 配置时，游戏内 GUI
会闪烁，按 Esc 打开的菜单可能消失：

```text
general.performance.vanilla.HudCaching=true
general.performance.ingameinfoxml.HUDFramebuffer=true
```

A/B 验证将两个选项同时设为 `false` 后症状恢复，当时归因于 StellarCore。日志确认
`mixins.stellar_core_hudcaching.json` 正常加载；HUDCaching 会接管 `EntityRenderer`、
`GuiIngame` 和 `Framebuffer` 并拦截 blend/depth/color 状态，存在冲突风险假设。

### 归因修正（2026-08-12）

**实测表明 GUI 闪烁 / Esc 菜单消失 / 背包与 JEI 闪烁 / 左上角 HUD 异常均不是
StellarCore 引起**，而是 Draconic Evolution 的渲染污染（DE 每帧 HUD 经 CCL
`GlStateTracker` 基于冻结的原版 `GlStateManager` 字段重置 GL 状态，见
[draconic-evolution.md](draconic-evolution.md)）。DE 兼容桥修复后，HUD 症状消失；
StellarCore 的两个 HUD 选项（`HudCaching`、`HUDFramebuffer`）无需再关闭。

### 当时的排除验证（保留备查）

2026-07-21 保持两个 HUD 选项关闭时：临时禁用 Alfheim、Lumenized、StellarCore
整包，云层消失/草方块侧面颜色异常均无变化——该排除结论仍有效（这些症状最终
归因于 DE，见 draconic-evolution.md）。

## 推荐配置

不再需要关闭 StellarCore 的 HUD 缓存选项。可恢复：

```text
general.performance.vanilla.HudCaching=true
general.performance.ingameinfoxml.HUDFramebuffer=true
```

恢复后建议按下方验证范围重测一次（DE 在场场景），确认 HUD 缓存与 Actinium
GUI 管线共存无回归。

## 残余限制

- 归因修正基于 DE 兼容桥修复后的生产实测；StellarCore 版本升级后需重测。
- 本说明不代表 StellarCore 的其他功能均已验证。
