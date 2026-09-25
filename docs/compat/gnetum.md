# Gnetum 兼容性说明

兼容状态:**部分**(HUD 缓存经兼容桥适配;残余限制见下文)
最后更新:2026-09-07

## 验证范围

- 版本:1.4.3(CurseForge 1220460 / Modrinth `7MoE34WK`,sha1
  `f726eaa6b6a536802ce65cd6b015652456b36c0d`)
- 环境:Cleanroom 0.6.x-alpha、LWJGL3 后端、无光影与光影(BSL)双路径
- 相关功能:HUD 分帧缓存(`enabled`,默认开启,3 passes,`maxFps=60`)

## 症状:GUI/HUD 闪屏(2026-09-07)

### 现象

Gnetum 默认配置与 Actinium 共存时,半透明 HUD 元素(聊天背景、字幕、BossBar
等)透明度错误并随缓存 pass 轮转(默认每元素 20Hz)隔帧变化,表现为 HUD 区域
闪烁。共存会话日志零 ERROR/WARN——纯渲染行为问题,不产生日志信号。

### 根因

Gnetum 自建两个 vanilla `Framebuffer` 做 HUD 缓存,最终 blit 固定以
`tryBlendFuncSeparate(ONE, ONE_MINUS_SRC_ALPHA, ZERO, ONE)` 合成——**假设缓存
纹理是预乘 alpha**。为保证这一点,Gnetum 在 `Gnetum.rendering` 窗口内用自己的
mixin 拦截 vanilla `GlStateManager.blendFunc` 与 `OpenGlHelper.glBlendFunc`,
强制 alpha 因子 `(ONE, ONE_MINUS_SRC_ALPHA)`。

Actinium 的 `GLSMRedirector`(`AngelicaRedirectorTransformer`,最后执行)把
Gnetum 全部调用方对 `GlStateManager`/`OpenGlHelper` 的**调用点**改写到
`GLStateManager`,Gnetum 挂在 vanilla 方法体上的钩子不再触发 → 缓存写入直
alpha → blit 按预乘合成 → 半透明元素透明度错误;3-pass 轮转重建使不同元素在
不同帧刷新,形成闪烁。

与 StellarCore HudCaching 冲突同族(见 [stellarcore.md](stellarcore.md) 的
"GLSM 状态镜像"模式)。

### 修复:镜像渲染窗口(2026-09-07)

- `GnetumHudCachingCompatTransformer`(launchwrapper transformer,注册于
  `MixinEarly.getASMTransformerClass`):重写
  `me.decce.gnetum.mixins.early.GuiIngameForgeMixin`,在每处常量
  `Gnetum.rendering` 赋值(1.4.3 恰有 `= true` / `= false` 两处,均位于
  `gnetum$renderGameOverlay`)后镜像写入 `GLSMConfig.hudCacheOverride`。
- GLSM 已有的 `hudCacheOverride` 分支(`GLStateManager.glBlendFunc` /
  `tryBlendFuncSeparate` / `changeColor`)语义与 Gnetum 的覆盖一致,直接复用。
  唯一偏差:混合关闭时写入半透明颜色会被强制 alpha=1——这复刻的是 vanilla
  混合关闭时的不透明覆盖语义,保持 blit 的覆盖率不变量成立,不向 Gnetum
  原生行为劣化。
- 为什么用 transformer 而不是 mixin:与 StellarCore 同理,Gnetum 经
  MixinBooter early loader 在 tweak 引导阶段注册其 mixin 配置,launchwrapper
  transformer 在类 define 前介入,与加载时机无关。

单元测试:`GnetumHudCachingCompatTransformerTest`(用 Modrinth 依赖 jar 的真实
`GuiIngameForgeMixin` 字节码验证两处镜像写入的位置与常量;`renderingCanceled`
不被误镜像)。

### 验证

- 字节码级单测:`GnetumHudCachingCompatTransformerTest` 通过。
- 生产整合包(`crl_t` 实例,Gnetum 默认配置)实机回归:**已确认**(2026-09-07,
  半透明 HUD 闪烁消失)。

## 症状:GUI 背景渐变闪屏(2026-09-07,与 Revo UI 共存)

### 现象

Gnetum + Revo UI(NeoFontRender UI Enhancements,`neofontrender_ui_enhancements`)
+ Actinium 三者共存时,打开/关闭背包等 GUI 过程中全屏背景渐变(uie
`effects.gradient`)随缓存 pass 轮转闪烁。用户 A/B 实测:关闭 uie
`effects.blur` 仍闪;禁用 Gnetum 不闪;关闭 uie `effects.gradient` 不闪。

### 根因

Actinium 既有 revoui 兼容
(`MixinScreenEffectsGradientRelocation` + `RevoScreenEffectsGradient`)把 uie
`ScreenEffectsRenderer.afterGameOverlay`(`RenderGameOverlayEvent.Post(ALL)`
监听器)里的 `drawGradient` 重定向为"登记到 pending,下一帧 pre-HUD 边界
(`setupOverlayRendering` 后)回放",避免渐变压暗 HUD。

Gnetum 把 uie 的 POST 监听器收入分帧缓存:只在所属 pass(默认 3 pass 轮转
之一)执行,其余帧不重放。于是 `defer()` 每 3 帧才登记一次,pending 只在
1/3 的帧非空,渐变以 1/3 帧率出现 → GUI 开关过渡动画期间背景闪烁。

### 修复:缓存窗口内直接画入缓存 FBO(2026-09-07,第二版)

第一版方案(已回滚)曾注入 `UncachedElements.has(String, ElementType)` 把 uie
POST 监听器列为不缓存,但实机验证无效。根因:Gnetum 的 modid 解析
(`ASMEventHandlerHelper.tryGetModId`)依赖 `ASMEventHandler.toString()` 的
legacy Forge `"ASM: "` 前缀(`substring(5)`),Cleanroom 的 `readable` 字段
没有该前缀,类名被削掉 5 个字符后 `Class.forName` 必然失败——**Cleanroom 下
所有 modded 监听器都落入 `gnetum_unknown` 桶**,按 modid 匹配的排除是空操作。

现行修复(`MixinScreenEffectsGradientRelocation`,commit `edf7c6f2`):
`actinium$deferGradient` 先检查 `GLSMConfig.hudCacheOverride`(即问题 1 镜像的
HUD 缓存捕捉窗口)——

- 缓存窗口激活时(uie 监听器正被收进 Gnetum/StellarCore 缓存):**不 defer,
  经 `ScreenEffectsRendererInvoker` 直接调用原始 `drawGradient` 画进缓存
  FBO**。这精确复刻"无 Actinium 时 Gnetum + uie 正常"的已知良好路径:渐变
  被烘进缓存纹理,每帧随 blit 显示,不再依赖 defer 管线的执行频率。
- 无缓存窗口时(Gnetum 缺席/关闭):保持 defer/replay 不变,渐变仍在 pre-HUD
  边界回放、不压暗 HUD。

### 验证

- `./gradlew build --no-daemon` 全绿。
- 生产整合包实机回归:**待用户确认**(恢复 `effects.gradient=true`、Gnetum
  默认配置,开关 GUI 观察背景渐变过渡)。

### 次生发现:Gnetum modid 解析在 Cleanroom 下整体失效

上述 `tryGetModId` 缺陷影响 Gnetum 全部按 modid 区分的功能(per-mod 缓存开关、
自带 `moddedPre/moddedPost` 硬编码排除、配置界面元素列表),在 Cleanroom 下均
不生效。未在 Actinium 侧修复(Gnetum 自身缺陷,应报上游);此处仅记录。

## 用户侧规避(修复不可用时的退路)

在 `config/gnetum.json` 中设置 `"enabled": "OFF"` 整体关闭 HUD 缓存(退化为
纯 vanilla HUD 路径);或对个别元素在 Gnetum 配置界面/`mapVanillaElements` /
`mapModdedElementsPre/Post` 中按元素关闭缓存。`"maxFps": 125` 可让缓存每帧
重绘,用于判断症状是否来自缓存内容。

## 残余限制

- **手部缓存(`gnetum:minecraft_hand`,默认关闭)未适配**:Gnetum 的
  `FramebufferTracker` 依赖 `OpenGlHelper.glBindFramebuffer` TAIL 钩子记账,
  该钩子同样被重定向架空;开启手部缓存可能在第一人称手渲染后错误恢复 FBO
  绑定。保持默认关闭即可。
- Gnetum 的危险混合检测(`OpenGlHelperMixin` 中对 `(DST_COLOR, ZERO)` 等 alpha
  因子的放行与逐元素禁用缓存)在 Actinium 下不生效;使用此类混合的第三方 HUD
  元素会以强制预乘因子缓存,可能与 Gnetum 原生表现有差异(罕见)。
- transformer 按 owner+name+desc 精确匹配 Gnetum 1.4.3 字节码;其他版本若
  失配仅记录 WARN 不崩,需重测。
- 本说明不代表 Gnetum 的其他功能(配置界面、性能分析等)均已验证。
