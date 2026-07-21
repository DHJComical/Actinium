# Lumenized 兼容性说明

兼容状态：**部分兼容**（动态光源可用；Bloom 泛光不可用，Actinium 会规避默认配置导致的画面异常）
最后更新：2026-07-21

## 模组信息

- 版本：1.0.3（作者 Kasumi_Nova，GTCEu 发光功能的独立提取版）
- 功能：动态光源（手持发光物照明）+ 发光纹理 Bloom 泛光
- 机制：内置 GTCEu 风格 Bloom 后处理（`gregtech.client.utils.BloomEffectUtil` /
  `gregtech.client.shader.postprocessing.BloomEffect`），通过 `MixinEntityRenderer`
  在 `EntityRenderer.renderWorld` 的 TRANSLUCENT 层渲染处钩入
  `BloomEffectUtil.renderBloomBlockLayer`，执行 bloomFBO 创建/同步、主 framebuffer
  深度附件共享、多级模糊与全屏合成。

## 已知问题：进世界后画面纯色或 framebuffer 异常

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

### Actinium 自动安全模式

检测到 Lumenized 后，Actinium 会通过条件 late Mixin 加载兼容层。默认 safe mode 同时禁用
两个尚未兼容的渲染路径：

- `bloomStyle` 的运行时有效值固定为 `0`，不进入 Unity 或 Unreal Bloom 分支。
- `hookDepthTexture` 的运行时有效值固定为 `false`，不进入 Lumenized 的 depth texture
  framebuffer 重建与挂接路径。
- 兼容层不重写 `config/lumenized.cfg`。配置初始化、配置同步和渲染代码的实际读取点都会
  应用同一策略，因此不依赖配置加载时序。

需要保留 Unreal Bloom 进行实验时，可添加 JVM 参数：

```text
-Dactinium.allowLumenizedUnrealBloom=true
```

该开关是完整的实验 opt-out：启用后，Actinium 会保留配置请求的 `bloomStyle` 和
`hookDepthTexture`，包括 Unity/Unreal Bloom 与 depth texture 路径。这些 GPU 路径尚未兼容，
仍可能出现纯色画面、framebuffer 错误或其他渲染问题。兼容层只在初始化时记录一次所选策略，
不会在每帧渲染中输出日志。

### 原配置规避验证

自动兼容层实现前，以下 `config/lumenized.cfg` 配置用于定位和验证问题：

- `bloomStyle=0`（Simple Gaussian Blur）——**必要改动**，已验证可恢复正常画面。
- `hookDepthTexture=false`——建议保险项（与 `bloomStyle=0` 的组合已验证；
  `hookDepthTexture=true` + `bloomStyle=0` 的组合未单独验证）。

这些结果是自动安全模式选择有效值 `0/false` 的依据。Actinium 仅调整运行时有效值，不会
把调整结果写回配置文件。

### 残余限制

- 即使 `bloomStyle=0`，Bloom 泛光效果在 Actinium 下依然**无效**（内容收集链
  断裂，bloomFBO 无输入）；兼容层用于规避已知画面异常，预期恢复正常。动态光源功能
  不受影响、可正常使用。
- `bloomStyle=1`（Unity Bloom）未验证，默认 safe mode 同样不会进入该路径。
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

上述记录来自自动兼容层实现前的手动配置验证。最初仅在配置同步后把 `bloomStyle=2` 改为
`0`，生产环境仍可能进入独立的 depth texture 路径；开发环境还会因 Lumenized 内置旧
`GregTechTransformer` 缺少开发映射属性而在进世界时崩溃。当前开发依赖已改为
compile-only，Lumenized 应由运行实例单独安装；safe mode 则在实际字段读取点强制执行
`bloomStyle=0` 和 `hookDepthTexture=false`。Bloom 仍不可用，不应视为已兼容。
