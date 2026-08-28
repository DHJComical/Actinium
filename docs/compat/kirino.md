# Kirino Engine 兼容（Cleanroom 内建，epoch-1.a5/a6）

最后更新：2026-08-28。分支：`feat/kirino-headless-compat`。

## 背景

Kirino Engine（`com.cleanroommc.kirino`，CleanroomMC 项目，3 个 `DummyModContainer`：
`kirino_engine`/`kirino_ecs`/`kirino_gl`，父模块 `kirino_engine`）是一个随 Cleanroom
Loader 滚动发布的实验性渲染引擎。它以源码 patch 方式（非 Mixin）在
`EntityRenderer#updateCameraAndRender` 中用 if/else 把 `renderWorld` 调用替换为
`KirinoClientCore.EntityRenderer$renderWorld`，并向 `renderWorldPass` 注入
`runHeadlessly` 帧相位（PREPARE/PRE_UPDATE/UPDATE/RENDER_OPAQUE/RENDER_TRANSPARENT/
POST_UPDATE/RENDER_OVERLAY）。

参考仓库：

- CleanroomMC/Kirino-Engine（独立开发仓库，epoch-1.a6；`onKirinoOneTimeConfig` 强制
  `enableRenderDelegate=true`）
- Cleanroom MC 1.12.2 源码树内嵌的 kirino 副本（`com/cleanroommc/kirino`）
- 实际运行环境（dev 验证）：Cleanroom 0.6.7-alpha 内嵌 kirino `epoch-1.a5`
  （`kirino_ecs` a3 / `kirino_gl` a2）

## 冲突分析

| 维度 | Actinium | Kirino | 冲突 |
| --- | --- | --- | --- |
| 渲染入口 | 锚定 vanilla `renderWorldPass(IFJ)V` 内调用点（`EntityRendererIrisMixin`）+ `RenderGlobal` 地形接管 | `EntityRenderer$renderWorld` 整体替换 vanilla `renderWorld` | Kirino Graphics 模式下 vanilla `renderWorldPass` 不执行，Actinium 全部注入点静默失效 |
| GL 状态 | glsm `GLStateManager`（字节码重定向器统一接管） | 裸 `GL11/GL30/GL43/GL45` 调用 + 自研 `glKnowledge`（commit/claim/require） | glsm 会把 kirino 已登记的方法改写到 GLStateManager；未登记方法（`glMapBufferRange`/MDI/DSA/`glFenceSync`）绕过 → 状态跟踪失步 |
| 地形 | celeritas 区块管线（build/upload/draw） | GPU-driven meshlet（persistent buffer + compute + MDI） | 两份地形渲染，互斥 |
| 线程 | `RenderDevice.enterManagedCode()` 周期性保护 | 自研 JobScheduler + staging | 独立，但都响应 RenderGlobal 方块/光照更新 |
| 配置 | `ActiniumConfig`（静态开关） | `KirinoConfigHub`：`enable`/`enableRenderDelegate`（默认 true，无配置文件写入端） | kirino 的 `onKirinoOneTimeConfig` 强制 `enableRenderDelegate`（旧版 a5 强制 false；新版 a6 强制 true），用户不可配置 |

## 兼容方案（路径 1：Kirino Headless 共存）

选定的共存路径：Kirino **Headless 模式**（`enable=true`、渲染委托关闭）下，
Kirino 不接管 `renderWorld`、不分配 GL 资源、运行 `runHeadlessly`（仅 headless
world 的 ECS/分析回调），Actinium 独掌渲染管线。

### 实现机制

1. **`MixinKirinoConfigHub`**（`src/main/java/com/dhj/actinium/mixin/mod/kirino/`，
   `@Mixin(targets = "com.cleanroommc.kirino.config.KirinoConfigHub", remap = false)`）：
   `@Overwrite public boolean isEnableRenderDelegate()` 恒返回 `false`。
   - 这是**读取点**拦截：无论 kirino 的 `onKirinoOneTimeConfig` 如何赋值，所有
     `isEnableRenderDelegate()` 调用点（`KirinoClientCore.postInit`、
     `EntityRenderer` patch、`RenderGlobal$notifyBlockUpdate` 等）都拿到 `false`。
   - `isEnable()` 不做改动：kirino 的 ECS/分析运行时照常初始化（路径 1 语义）。
   - 目标类以字符串引用（`@Mixin(targets=...)`）且 `remap=false`，无编译期依赖。
2. **`mixins.actinium.kirino.json` + `KirinoMixinConfigPlugin`**（early 注册）：
   - 配置由 `MixinEarly` 注册（early 阶段），`plugin` 字段指向
     `com.dhj.actinium.mixins.KirinoMixinConfigPlugin`（`IMixinConfigPlugin`）。
   - **为什么不能是 late/conditional**：`KirinoConfigHub` 由
     `KirinoCommonCore` 静态初始化在 `FMLClientHandler#beginMinecraftLoading` 的
     `configEvent()`（源码 line 231）被加载——**早于 mixinbooter 注册 late 配置**。
     late 方案实测崩溃：`MixinTargetAlreadyLoadedException: target
     com.cleanroommc.kirino.config.KirinoConfigHub was loaded too early`
     （`runClient` 日志 2026-08-28 10:08 实例），并连锁触发 StellarCore
     `ReEntrantTransformerError`（`NoClassDefFoundError: ModelManager`）。
   - **为什么 early 配置不需要条件门控**：plugin 的 `shouldApplyMixin` 只对
     目标类**被加载转换时**才被调用；`KirinoConfigHub` 仅当 kirino 安装时才会
     被加载（`KirinoCommonCore` 静态初始化），未安装时目标类永不加载、
     `shouldApplyMixin` 永不询问、配置为无操作。插件只做类名匹配，无 kirino
     类引用。
3. **`KirinoCompat`**（`src/main/java/com/dhj/actinium/compat/kirino/`）：
   - 纯诊断层：`isKirinoPresent()`/`isHeadlessPinned()`/`install()`（F3 与日志）。
   - 不引用任何 kirino 类、不做状态修改；`Loader.isModLoaded` 延迟到方法调用
     （类的静态初始化不依赖 Forge 环境，防止类加载失败扩散到 `Actinium` 主类）。
4. **挂载**：`Actinium.onInit` 调用 `KirinoCompat.install()`；F3 debug 屏幕显示
   `Kirino: headless (render delegate pinned off)`。

### 时序依据（实测 + 源码）

FML 启动序列（`FMLClientHandler.java` 源码证据）：

```
beginMinecraftLoading()                      [:216]
  KirinoCommonCore.configEvent()             [:231]  kirino 强制 enableRenderDelegate（事件已发）
  Loader.loadMods()                          [:236]
    identifyMods()                           [:380]  kirino mods 识别
    distributeStateMessage(CONSTRUCTING)     [:591]  Actinium.onConstruct
  preinitializeMods()                        [:256]
  KirinoClientCore.init()                    [:293]  读取 isEnable() 决定是否初始化
finishMinecraftLoading()                     [:335]
  KirinoClientCore.postInit()                [:402]  读取 isEnableRenderDelegate() 决定 Graphics/Headless
```

early 配置在 Launch/coremod 阶段注册，早于 `KirinoConfigHub` 首次加载；
mixin 于目标类加载时（configEvent → KirinoCommonCore static init）应用。

## dev 验证记录（2026-08-28，Cleanroom 0.6.7-alpha + kirino epoch-1.a5）

- [x] `compileJava` / `test` 通过（分支 `feat/kirino-headless-compat`）。
- [x] `runClient` 启动成功：`[CleanMix]: Adding [mixins.actinium.kirino.json] mixin configuration`
  （10:12:09，无 `MixinTargetAlreadyLoadedException`）。
- [x] `[Kirino Core]: Registered headless module installer "AnalyticalWorldInstaller"` ——
  kirino 走 Headless 模式（Graphics 安装器未出现）。
- [x] `[ActiniumKirinoCompat]: Kirino Engine detected: pinning render delegate OFF (Headless mode)...`
  —— 兼容层确认。
- [x] 客户端进入渲染循环并持续输出 GLSMPerfDebug（画面正常，无崩溃）。

## 文件清单

- `src/main/java/com/dhj/actinium/mixin/mod/kirino/MixinKirinoConfigHub.java`（新增）
- `src/main/resources/mixins.actinium.kirino.json`（新增 early 配置 + plugin 字段）
- `src/main/java/com/dhj/actinium/mixins/KirinoMixinConfigPlugin.java`（新增运行时门控）
- `src/main/java/com/dhj/actinium/mixins/MixinEarly.java`（注册 kirino 配置）
- `src/main/java/com/dhj/actinium/compat/kirino/KirinoCompat.java`（新增诊断层）
- `src/main/java/com/dhj/actinium/Actinium.java`（onInit 挂载 + F3 状态行）
- `src/test/java/com/dhj/actinium/mixin/MixinConfigurationTest.java`（配置清单断言）
- `src/test/java/com/dhj/actinium/mixins/MixinLateTest.java`（门控选择断言，回复原样）

## 残余风险

- **kirino 版本漂移**：`KirinoConfigHub.isEnableRenderDelegate()` 签名变化会导致
  mixin 应用失败（`@Overwrite` 不符 — `InvalidMixinException`）。兼容矩阵锁定
  `epoch-1.a5/a6`，升级 kirino 版本时需重新核对。
- **Mixin 应用时机**：early 注册保证早于 `KirinoConfigHub` 首次加载。若未来 kirino
  把 `configEvent()` 提前到 coremod/Launch 阶段，需要重新评估（当前无此迹象）。
- **用户主动开启 Kirino Graphics**：若未来 kirino 提供配置文件写入端，本 mixin
  仍接管（恒 false）——这是选定的路径 1 语义；如后续 kirino 增加"用户显式开启
  Graphics 模式"的配置，需重新评估是否保留本强制。

## 参考

- CleanroomMC/Kirino-Engine `README.md`（定位：替换整个渲染管线，不兼容 major render mods）
- CleanroomMC/Kirino-Engine `docs/engine_overview.md`（Strangler Pattern、Headless/Graphics 模式）
- CleanroomMC/Kirino-Engine `PATCHES.md`（EntityRenderer/renderWorldPass/RenderGlobal patch 点）
- CleanroomMC/Kirino-Engine `src/main/java/com/cleanroommc/kirino/config/KirinoConfigHub.java`
- CleanroomMC/Kirino-Engine `src/main/java/com/cleanroommc/kirino/engine/KirinoEngine.java`（run/runHeadlessly 分支）
- Cleanroom MC 1.12.2 源码 `net/minecraftforge/fml/client/FMLClientHandler.java`（configEvent/init/postInit 时序）
