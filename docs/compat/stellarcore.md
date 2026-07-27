# StellarCore 兼容性说明

兼容状态：**部分兼容**（HUD 缓存相关功能需要关闭）
最后更新：2026-07-21

## 验证范围

- 版本：1.5.22（`StellarCore-1.5.22.jar`）
- 整合包：NovaEngineering-World-cleanroom
- 相关功能：HUD 缓存与 InGameInfoXML HUD framebuffer
- 日期：2026-07-21

本记录来自生产整合包的单变量 A/B 排查。现有记录未保存精确的 Actinium commit、
Cleanroom、Java、GPU、驱动和操作系统版本，因此结论只适用于下述症状与配置组合，
不能替代新版本发布前的完整兼容性验证。

## 已知问题：GUI 闪烁或 Esc 菜单消失

### 现象

在 Actinium 渲染管线下，同时启用以下两个 StellarCore 配置时，游戏内 GUI
会闪烁，按 Esc 打开的菜单可能消失：

```text
general.performance.vanilla.HudCaching=true
general.performance.ingameinfoxml.HUDFramebuffer=true
```

### 验证结果

2026-07-21 在生产整合包中进行 A/B 验证：将上述两个选项同时设为 `false` 后，
GUI 闪烁和 Esc 菜单消失均恢复正常。当前尚未逐项单独恢复测试，因此不能断言
问题由某一个开关单独造成，或只能由两者组合造成。

日志和字节码检查确认 `mixins.stellar_core_hudcaching.json` 正常加载。HUDCaching
会接管 `EntityRenderer`、`GuiIngame` 和 `Framebuffer`，创建自有 FBO，并拦截
blend/depth/color 状态；这与 Actinium 的 GUI 和渲染 pipeline 注入存在冲突风险。

### 推荐配置

在使用 Actinium 的生产实例中关闭两个配置：

```text
general.performance.vanilla.HudCaching=false
general.performance.ingameinfoxml.HUDFramebuffer=false
```

该规避只针对 GUI/HUD 症状。关闭这两个选项后，云层直接消失，草方块侧面颜色仍略微
异常；这些剩余症状尚未归因于 StellarCore。

### 剩余渲染症状排除验证

2026-07-21 保持 `HudCaching=false` 和 `HUDFramebuffer=false`，临时禁用
`Alfheim-1.5.jar` 后，云层消失和草方块侧面颜色异常均无变化。因此，当前验证排除
Alfheim 是这两个症状的直接原因；验证后已恢复 Alfheim。

同日保持上述两个 HUD 选项为 `false`，临时禁用 `Lumenized-1.0.3.jar` 进行第二轮
A/B 验证，云层消失和草方块侧面颜色异常仍无变化。因此，当前验证同时排除
Alfheim 与 Lumenized 是这两个剩余症状的直接原因；验证后已恢复两者启用。

随后在保持上述两个 HUD 选项为 `false` 的情况下，完整禁用
`StellarCore-1.5.22.jar` 进行 A/B 验证，云层消失和草方块侧面颜色异常仍无变化。
因此，当前验证将 StellarCore 整包排除为这两个剩余症状的直接原因；验证后已恢复
StellarCore，并继续保留两个 HUD 选项关闭。

完整禁用 StellarCore 后两个剩余症状均无变化，因此它们不归因于 StellarCore。本说明
不继续记录其他模组和渲染选项的未完成排障轨迹；相关调查应在独立问题记录中完成后，
再把可复现结论回填到兼容性矩阵。

## 残余限制

- HUD 缓存和 HUD framebuffer 关闭后，StellarCore 的对应性能优化不可用。
- 尚未完成两个开关的单变量 A/B 矩阵，后续若需要缩小规避范围应补充独立验证。
- Alfheim 与 Lumenized 的排除结果仅覆盖本次生产整合包中的云层和草方块侧面颜色症状。
- 本次生产记录缺少精确的软件与硬件基线，升级 Actinium、Cleanroom 或 StellarCore 后需重测。
- 本说明不代表 StellarCore 的其他功能均已验证。
