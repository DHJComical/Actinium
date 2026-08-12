# Revo UI 兼容性说明

兼容状态：**代码支持**（运行验证待记录）
最后更新：2026-08-12

## 模组信息

- Revo UI（modid `neofontrender_ui_enhancements`），1.12.2 版本
- 接入方式：late/conditional Mixin（`mixins.actinium.revoui.json`，
  `neofontrender_ui_enhancements` 条件）

## 机制

- `MixinHudWindowCompositor`：HUD 窗口合成器适配。
- `MixinScreenEffectsGradientRelocation`：屏幕特效渐变重定位。
- `ScreenEffectsRendererInvoker`：屏幕特效渲染器的 Invoker 访问器。

## 验证记录

- 待补：启用 Revo UI 后验证 HUD 合成与屏幕特效渲染正常。

## 待办

- [ ] 运行验证并回填验证记录
