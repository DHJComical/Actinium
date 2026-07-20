# Lumenized 兼容性说明

兼容状态：**部分兼容**（动态光源可用；Bloom 泛光不可用，默认配置会导致画面异常）
最后更新：2026-07-20

## 模组信息

- 版本：1.0.3（作者 Kasumi_Nova，GTCEu 发光功能的独立提取版）
- 功能：动态光源（手持发光物照明）+ 发光纹理 Bloom 泛光
- 机制：内置 GTCEu 风格 Bloom 后处理（`gregtech.client.utils.BloomEffectUtil` /
  `gregtech.client.shader.postprocessing.BloomEffect`），通过 `MixinEntityRenderer`
  在 `EntityRenderer.renderWorld` 的 TRANSLUCENT 层渲染处钩入
  `BloomEffectUtil.renderBloomBlockLayer`，执行 bloomFBO 创建/同步、主 framebuffer
  深度附件共享、多级模糊与全屏合成。

## 已知问题：进世界后画面纯色（bloomStyle=2）

### 现象

- 进入世界后画面呈偏蓝黑色、恒定不变（转动视角无变化），世界完全不显示。
- 按 Esc 打开游戏菜单时，背景中的世界地形渲染正常。

### 根因

1. Unreal Bloom（`bloomStyle=2`，默认值）的后处理主流程与 Actinium 的 OpenGL
   core profile 渲染栈（GLSM 状态管理 + Celeritas 区块渲染）冲突：世界渲染结果
   无法上屏，画面仅剩静态清屏色。冲突点位于 bloom FBO 主流程本身，与
   `hookDepthTexture` 无关（实测关闭后 `bloomStyle=2` 仍然复现）。
2. Esc 菜单打开时世界恢复可见，与该后处理在 GUI 状态下的执行路径差异有关
   （具体断点未逐一分离）。
3. Actinium 对 Lumenized 注册的第 5 个自定义方块渲染层 `Bloom` 按未知层回退为
   cutout 处理（日志出现 `Falling back to cutout-like behavior for custom block
   render layer 'Bloom'`），其 Bloom 内容收集链（对 vanilla
   `RegionRenderCacheBuilder` 的拦截）在 Actinium 区块渲染器下不生效。
4. Lumenized 仅通过反射检测 OptiFine
   （`net.optifine.shaders.Shaders.shaderPackLoaded`）决定是否停用 Bloom 后处理；
   它不认识 Actinium 渲染栈，因此 Bloom 在 Actinium 环境下全速运行。

### 解决方案（配置级，无需修改模组）

`config/lumenized.cfg`：

- `bloomStyle=0`（Simple Gaussian Blur）——**必要改动**，已验证可恢复正常画面。
- `hookDepthTexture=false`——建议保险项（与 `bloomStyle=0` 的组合已验证；
  `hookDepthTexture=true` + `bloomStyle=0` 的组合未单独验证）。

### 残余限制

- 即使 `bloomStyle=0`，Bloom 泛光效果在 Actinium 下依然**无效**（内容收集链
  断裂，bloomFBO 无输入），配置仅保证画面正常；动态光源功能不受影响、可正常使用。
- `bloomStyle=1`（Unity Bloom）未验证，预期与 `2` 存在同类风险。
- 根治（让 Bloom 在 Actinium 下真正生效）需要 Actinium 侧实现自定义渲染层与
  Bloom FBO 流程的兼容层，目前未立项。

## 验证记录

- 日期：2026-07-20
- Actinium：2.4.0-dev（commit `8e95029`）
- 环境：Cleanroom 0.5.17-alpha、Java 25.0.3、Windows 11、NovaEngineering-World
  整合包（约 250 个模组）
- 过程与结果：
  - 默认配置（`bloomStyle=2` + `hookDepthTexture=true`）：进世界纯色，Esc 可见世界（复现）。
  - 禁用 Lumenized：画面正常。
  - `hookDepthTexture=false` + `bloomStyle=2`：仍纯色。
  - `hookDepthTexture=false` + `bloomStyle=0`：画面正常。
