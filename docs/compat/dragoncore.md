# DragonCore 自定义字体兼容性说明

兼容状态：**已验证**（MixinFontRenderer 对 DragonCore 字体渲染器让位；`./gradlew check` 通过）
最后更新：2026-09-24

## 验证范围

- 版本：DragonCore 2.0.1（元素之诗整合包）
- 相关功能：Actinium 字体批处理渲染器（`BatchingFontRenderer`）与 DragonCore 服务端下发的
  自定义字体（FontConfig）
- 触发环境：元素之诗整合包（Cleanroom 0.6.13），服务器下发 FontConfig 资源包

## 症状：自定义字体渲染异常

### 现象

使用 Actinium 代替 OptiFine 后，DragonCore 的自定义字体（服务器经 FontConfig 下发的字形表）
无法正常渲染。OptiFine 环境下该字体正常。

### 机制

1. DragonCore 收到服务端 FontConfig 后，用 `bt`（extends `FontRenderer`）替换
   `Minecraft.fontRendererObj`；该渲染器在**覆写的 `renderStringAtPos`** 中从自身字符表
   （`tfa.h`，`ConcurrentHashMap<Character, ...>`，`ALLATORIxDEMO` 取字）绘制自定义字形，
   并通过 ASM 改写 vanilla `FontRenderer.getCharWidth` / `renderItem` 使度量兼容
   （`FontRendererTransform`）。
2. Actinium 的字体批处理注入（`MixinFontRenderer`）拦截 `drawString` / `renderString` /
   `getCharWidth`，改用 `BatchingFontRenderer` 直接内存渲染；该路径**绕过**
   `renderStringAtPos`，因此 DragonCore 的自定义字形完全不参与绘制 → 字体异常。
3. NeoFontRender 已有同样的让位逻辑（`Mods.NEOFONTRENDER` 时禁用 batcher），DragonCore
   需要相同的待遇。

## 修复：按渲染器实例让 batcher 让位

兼容策略位于 mixin 包外的 `FontBatcherCompat.isBatcherDisabledFor(Class<?>)`，由
`MixinFontRenderer` 在 `drawString` / `renderString` / `getCharWidth` 注入点调用：

- 用 `Mods.DRAGONCORE` 存在性门控 + **按类名前缀** `eos.moe.dragoncore.*` 匹配（渲染器类是
  混淆且随版本变化的 `bt`，包前缀是唯一稳定身份；不引用任何 DragonCore 类，未安装该模组时
  包路径不会出现）。
- 命中后 `drawString` / `renderString` / `getCharWidth` 全部回退 vanilla 路径，
  与 NeoFontRender 让位行为一致。

## 后续维护

- DragonCore 渲染器类名（当前 `bt`）与混淆名可能随版本变化，检测只依赖
  `eos.moe.dragoncore.` 包前缀，无需同步升级。
