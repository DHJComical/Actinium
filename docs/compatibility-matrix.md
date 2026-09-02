# Actinium 兼容性矩阵

最后更新：2026-09-02。

状态定义：`已验证` 表示在记录的版本和场景中通过；`部分` 表示能运行但存在已知缺口；
`无法启用` 表示光影包不能成功开启；`未验证` 不代表不兼容。更新记录时必须填写 Actinium commit、
光影/模组版本和测试环境。

本轮验证环境：Actinium `30c7ffb`、Java 25.0.3、Cleanroom 0.5.12-alpha、Distant Horizons 3.1.2-b、
Windows 10、NVIDIA GeForce RTX 5070 Laptop GPU（驱动 610.74）。

> 2026-08-31 追加：Photon v1.3b 水面不生效（水面保持原版贴图、仅余微弱反光）的修复——
> 根因不在水面渲染路径，而在 block.properties 的版本条件求值：Photon 把全部 modern 方块映射
> 放在 `#if MC_VERSION >= 11300` 段内，`#else`（1.12 段）为空；`IdMap#hasLegacySection` 此前
> 只要见到 MC_VERSION 条件的 `#else` 分支就判定包内含 legacy 段，于是以真实 MC_VERSION=11202
> 交给 jcpp 求值，modern 映射段被整体剔除，方块 ID 映射表为空（`block-meta-map present=false`、
> 全部方块 `shaderBlockId=-1`），`mc_Entity` 全部失效，水面因此不被识别为 `MATERIAL_WATER`。
> 修复后 `#else` 分支必须含实际映射行才判 legacy 段（空 fallback 的包如 Photon 继续走
> MC_VERSION=260101 的 modern 伪装路径 + legacy 名展平）；Complementary 的 `#elif >= 10800`
> 实质段路径不受影响。验证：dev 运行，`block-meta-map present=true`、水面采样
> `blockId=10001`（water 与 flowing_water 双映射），水面效果正常（用户实机确认）。

> 2026-08-24 追加：改 mipmap 后地形方块概率性消失（无光影与光影下均出现）的修复——见下方
> [mipmap 与地形渲染](#mipmap-与地形渲染)。根因有两点：(1) terrain shader 用三参数 `texture()`
> 按片元偏导选 mip 层，mip 级别改变后 UV 接缝处偏导取到未定义层返回 alpha≈0，被 cutout 的
> alpha 测试裁掉；(2) 地形材质 `mipped` 位硬编码为 true，mipmap 关闭时仍请求 mip 采样。修复后
> 采样改为由 GL 过滤状态决定 mip 层，材质位随实时 mipmap 级别生成，并与 atlas 过滤状态一致。
> 验证：RSO 界面反复切换 mipmap 0↔4 共 10 次，无方块消失（commit `64b9c32`、`f94dfa6`、
> `43aefae`，dev 运行）。

> 2026-08-16 追加：VoxelMap 1.9.25（分支 `fix/voxelmap-minimap-black`）小地图黑屏修复验证——见下方
> [模组与环境](#模组与环境) 的 VoxelMap 行与 [docs/compat/voxelmap.md](compat/voxelmap.md)。

> 2026-08-18 追加：Depths Update（issue #68）扩展世界高度（默认 -64..320，可配 -256..512）下
> Y 范围 0-255 之外方块不渲染的修复——见下方 [模组与环境](#模组与环境) 的 Depths Update 行。
> 根因是渲染器硬编码 0-255 的 section 范围，且 Depths 自带的 celeritas 兼容 mixin 指向
> 重构前的 `org.taumc.celeritas.impl.*` 类路径而不生效；修复改为从 Depths 公开 API
> （`DepthsUpdateAPI.getHeightInfo`）推导 section 范围，并按其 storage 布局映射读取（commit
> `6d8fc24`，dev 实测 Y<0 与 Y>255 区域正常渲染）。
> 
> 2026-08-18 追加：EnderIO CEu 5.4.2 流体罐在光影开启时罐内液体不渲染的修复（#58）——见下方
> [模组与环境](#模组与环境) 的 EnderIO 行。根因是 Iris celeritas 地形接口对所有 terrain pass
> 无条件强制 `glDepthMask(true)`，translucent 层里的罐体玻璃窗因此写出深度，遮挡了其后绘制的
> TESR 液体；修复后 translucent terrain pass 在主 pass 不再写深度（与 vanilla 语义一致），
> 阴影图 pass 与不透明 pass 保持写深度。
>
> 2026-09-02 追加：上述修复曾被 #85（`da83c59`）回潮——该提交把 translucent terrain pass
> 翻转为写深度，依据的"vanilla 半透明阶段保持写深度"前提不实：vanilla 1.12.2 将整个
> translucent 阶段（半透明地形 + pass-1 方块实体重绘）包在 `depthMask(false)` 内
> （`EntityRenderer` 约 1539/1564 行），储罐玻璃先写深度便遮挡了其后渲染的流体 TESR，
> 有无光影均不显示。修复为 translucent pass 恢复不写深度
> （`VintageRenderPassConfigurationBuilder`），水 pass 保持写深度；光影路径由
> Iris celeritas 接口对 translucent 语义 pass 统一不写深度兜底。无光影/光影双路径、
> 水与玻璃叠层、mipmap 切换均回归通过。
> 
> 2026-08-18 追加：Snow! Real Magic! 0.7.4（issue #35）带雪栅栏不渲染的修复——见下方
> [模组与环境](#模组与环境) 的 Snow! Real Magic! 行。根因是 SRM 把被雪覆盖的方块替换为携带
> `SnowTile` 的 `snow_layer`，并只在 `BlockRendererDispatcher.renderBlock` 内重绘被覆盖方块，
> 而 Actinium 的快速区块渲染路径直接走 baked model、从不调用该入口；修复让 SRM 的
> `snow_layer` 块退回 vanilla dispatcher 路径（`6aee395`，dev 运行验证通过）。

> 2026-08-20 追加：TC4 Research Port: Reborn（issue #74）旧版 Tessellator 的 GUI 卡死修复——见下方
> [模组与环境](#模组与环境) 的 TC4 Research Port: Reborn 行与
> [docs/compat/oldresearch.md](compat/oldresearch.md)。dev 人工回归确认研究笔记 GUI 不再卡死，
> 优化后约 500+ FPS，与背包界面同量级；研究树视觉回归仍待补充。

> 2026-08-29 追加：HBM's Nuclear Tech - Community Edition 2.5.0.5 的机器黑色剪影、
> lightmap 和深度状态异常修复——见下方 [模组与环境](#模组与环境) 的 HBM 行。
> dev 实际场景回归覆盖 FENSU 与其他 HBM 机器，模型不再显示为黑色剪影。

> 2026-09-02 追加：BetterPortals Refitted 0.4.1 末地传送门无看穿效果（洞口只见星野贴图）+
> 视觉位置低约一格 + 修复后星野被拉成竖直条纹的修复——见下方 [模组与环境](#模组与环境) 的
> BetterPortals Refitted 行与 [docs/compat/betterportals.md](compat/betterportals.md)。
> 三层根因叠加：compat shader 预处理器条件求值 bug 致 `render_portal` 的 `sampler` 失活；
> `TileEntityEndPortalRendererIrisMixin` 无条件劫持使 BPR 合成 TE 的星野叠加失去原版
> blend 钩子语义；glsm texgen 顶点着色器的逐分量写入模式被 NVIDIA 驱动 DCE 掉
> `u_TexGenEyePlaneS`。

## 光影包

| 光影包                                | 版本            | 状态   | 已验证范围                                                          | 已知缺口      | Actinium 基线 |
|------------------------------------|---------------|------|----------------------------------------------------------------|-----------|-------------|
| MakeUp Ultra Fast                  | 9.1f          | 已验证  | 世界加载、维度切换、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载、Distant Horizons LOD | -         | `f261611`   |
| BSL                                | 10.0          | 已验证  | 世界加载、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载、Distant Horizons LOD      | -         | `f261611`   |
| Complementary Reimagined / Unbound | r5.5.1        | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `f261611`   |
| Bliss                              | 2.1.2         | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `28d976d`   |
| iterationT                         | 3.2.0         | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `30c7ffb`   |
| iterationRP                        | 0.7.7 / 0.8.7 | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `28d976d`   |
| Photon                             | v1.3b         | 部分    | 开启、世界渲染、地形、水（2026-08-31 水面修复后）、GUI                                     | 阴影/实体/维度切换/重载等场景待补充验证；选项菜单部分元素缺失（GTAO 等 profile 项告警，与水面无关） | `fix/photon-water-surface` |
| SEUS PTGI HRR                      | Test 2.1      | 无法启用 | -                                                              | 光影包不能成功开启 | `f261611`   |

## 模组与环境

| 组件               | 状态   | 接入方式                               | 备注               |
|------------------|------|------------------------------------|------------------|
| Cleanroom Loader | 必需   | Forge/Cleanroom 启动与 MixinBootstrap | 当前目标运行环境         |
| Celeritas        | 内嵌   | Gradle 子项目、最终 Jar 合并               | Actinium 的区块渲染器  |
| GLSM             | 内嵌   | Gradle 子项目、service provider        | 管理 GL 状态和固定管线兼容  |
| GTNHLib          | 内嵌   | Gradle 子项目、bridge API              | 提供底层渲染与内存工具      |
| Distant Horizons | 部分   | API、late Mixin、Iris LOD programs   | 版本变化敏感，必须按指定版本验证 |
| Lumenized        | 部分   | 条件 Mixin、自动 safe mode           | 默认有效值为 `bloomStyle=0`、`hookDepthTexture=false`；Bloom 仍不可用，详见 [docs/compat/lumenized.md](compat/lumenized.md) |
| StellarCore      | 已验证  | 无（不再需要配置规避） | HUD 缓存相关 GUI/HUD 症状实为 Draconic Evolution 引起（2026-08-12 实测归因修正）；DE 兼容桥修复后 HUD 正常，`HudCaching`/`HUDFramebuffer` 可恢复开启，详见 [docs/compat/stellarcore.md](compat/stellarcore.md) |
| Draconic Evolution | 已验证 | 条件 Mixin（CCL GlStateTracker 兼容桥） | DE 2.3.28.354 在场时云异常/草方块侧面偏绿/主菜单消失；根因为 DE 每帧 HUD 经 CCL GlStateTracker 基于冻结的原版 GlStateManager 字段重置 GL 状态，已由 `mixins.actinium.ccl.json` 兼容桥修复（dev 回归通过，生产整合包全量回归待做），详见 [docs/compat/draconic-evolution.md](compat/draconic-evolution.md) |
| Fluidlogged API  | 代码支持 | compile-only API、条件调用              | 尚缺当前运行时验证记录      |
| Gibbed           | 代码支持 | late Mixin、模型批处理路径                 | 尚缺当前运行时验证记录      |
| Chunk Animator   | 部分 | 条件桥（ChunkAnimationProvider）+ 动画 section 单独绘制 | 1.12.2-1.2.1（236484:3850023）dev 运行通过（coremod 加载、兼容层启用、进世界无异常）；动画视觉确认待补，详见 [docs/compat/chunkanimator.md](compat/chunkanimator.md) |
| VoxelMap         | 部分 | 条件 Mixin（CPU 纹理路径 + 线性过滤 + scissor 重路由 + HudCaching alpha 保护） | 1.9.25 小地图黑屏/黑块已修复（dev 验证圆内正常显示地图内容、HUD 不被缓存隐藏，见 [docs/compat/voxelmap.md](compat/voxelmap.md)）；已知缺口：与 StellarCore `HudCaching` 组合时小地图圆周仍可能残留黑块（VoxelMap 全屏清 alpha + DST_ALPHA 混合与 HUD 缓存 FBO 的第三方冲突，`HudCaching=false` 即消失，非本模组缺陷）；验证 VoxelMap 需停用 JourneyMap（二者频道冲突） |
| ModernUI         | 代码支持 | GUI scale hook                     | 尚缺当前运行时验证记录      |
| HBM's Nuclear Tech - Community Edition | 已验证 | 条件 Mixin（RenderUtil 状态栈 + TileEntityRendererDispatcher 世界 lightmap 同步） | 2.5.0.5（CurseForge 1312314:8330665）：FENSU 与其他 HBM 机器的 WaveFront raw VAO 模型在实际场景中正常显示；修复前的 stale lightmap、黑色剪影和 depth 恢复异常不再复现；Java 25.0.3、Cleanroom 0.6.12-alpha dev 回归通过 |
| Depths Update    | 已验证 | 兼容门控（`compat/depthsupdate`：公开 API 推导 section 范围 + storage 索引映射） | 1.0.0-a10：扩展世界高度（默认 -64..320）下 Y<0 与 Y>255 的方块不再缺失（渲染器原先硬编码 0-255）；dev 实测正常；无 Depths 时回退 vanilla 行为 |
| EnderIO CEu / EnderCore CEu | 已验证 | 无（核心渲染语义修复，非模组接入） | 5.4.2 + EnderCore 0.5.81：光影开启时流体罐内液体被罐体玻璃窗深度遮挡的问题已修复（`cb4feaa5`，translucent terrain pass 不再写深度）；MakeUp Ultra Fast 9.4c + Cleanroom 0.5.17-alpha 实测通过；2026-09-02 修复 #85（`da83c59`）引入的回潮——translucent pass 被错误翻转为写深度导致有无光影流体均被玻璃遮挡，已恢复 vanilla 深度语义，双路径实测通过 |
| Snow! Real Magic! | 已验证 | 兼容门控（SRM 的 snow_layer 块退回 vanilla dispatcher 路径） | 0.7.4：带雪栅栏不渲染已修复（SRM 把被覆盖方块替换为带 SnowTile 的雪层、仅在 `BlockRendererDispatcher.renderBlock` 内重绘，快速区块路径已绕过）；`6aee395`，dev 运行验证通过（MakeUp Ultra Fast 下无光影 + 光影各验一次） |
| TC4 Research Port: Reborn | 部分 | 条件 Mixin（Old Research Tessellator 转发到 streaming drawer） | 1.0.1-release（1632015:8642028）：已修复 splash 结束后 repack capacity 为 0 导致的 GUI Client thread 无限循环；dev 人工回归确认研究笔记 GUI 不再卡死，优化后约 500+ FPS，与背包界面同量级；研究树视觉回归待补，详见 [docs/compat/oldresearch.md](compat/oldresearch.md) |
| Modern Splash    | 部分 | 无侵入（替换类与 mixin 注入天然兼容）+ splash 字体 color=0 修复 | 1.5.3（629058:8487408）dev 运行通过（coremod 加载、mixin 注入保留、字体颜色按配置生效）；光影场景回归待做，详见 [docs/compat/modern-splash.md](compat/modern-splash.md) |
| Reese's Sodium Options（内嵌） | 代码支持 | 内嵌移植 UI（`me.flashyreese.mods.reeses_sodium_options`，MIT）+ embeddium 选项数据层（`org.embeddedt.embeddium.api.options.*` 自研扩展） | RSO 界面作为视频设置入口（`MixinGuiOptions` 拦截按钮 101，`enabled=false` 回退原版 `GuiVideoSettings`）；`net.caffeinemc` 设置界面与配置模型已整体删除；编译与 349 项单元测试通过，**运行期视觉对比待人工验证**，详见 [docs/rso-port.md](rso-port.md) |
| Extra Utilities 2 | 已验证 | `ModdedBlockRenderCompat` 在完整 block-render 生命周期内按 block 实例串行化 | `extrautils2@1.0`：Java 25 dev 客户端启动 10 个 chunk-builder worker，进入已有世界并触发区块重载后未复现 Issue #36 的 CME；代码提交 `44f4295`，详见 [docs/compat/extrautils2.md](compat/extrautils2.md) |
| AgriCraft | 部分 | `ModdedBlockRenderCompat` 使用共享 renderer 锁保护 crop 缓存 | 与 XU2 相同的异步第三方缓存访问模式已加入兼容层；dev 运行验证待补，详见 [docs/compat/extrautils2.md](compat/extrautils2.md) |
| Kirino Engine（Cleanroom 内建） | 部分（Headless） | early 配置 + `IMixinConfigPlugin` 门控，钉死 `isEnableRenderDelegate()` 为 false（`MixinKirinoConfigHub`） | Kirino Graphics 模式会整体替换 `EntityRenderer#renderWorld`，使 Actinium 全部渲染注入点失效；共存的唯一路径是 Kirino Headless 模式：本兼容层强制其渲染委托关闭、保留 ECS/分析运行时，Actinium 独掌渲染管线。Cleanroom 0.6.7-alpha（kirino epoch-1.a5）dev 运行通过（early 配置注册、headless installer、兼容层日志、渲染循环正常），详见 [docs/compat/kirino.md](compat/kirino.md) |
| Scannable | 已验证 | 条件 Mixin（接管 `ProxyOptiFine` 探针，扫描波走其 overlay 路径） | 1.6.3.26（266784:3146549）：使用扫描器后无光影透视 / 光影全白拖影的根因是其 INJECT 路径换装主 FBO 深度 attachment（Actinium 下 `Framebuffer.depthBuffer` 为 0，"恢复"即卸下深度）；已引导其走 OptiFine 式 overlay 渲染路径，详见 [docs/compat/scannable.md](compat/scannable.md)；dev 运行验证通过（无光影透视与光影全白均消失、扫描波区域正确；相邻结果合并为聚类大框为 Scannable 固有设计） |
| BetterPortals Refitted | 已验证 | `EndPortalRenderPolicy` 按调用来源分流（真实 TE 走替代渲染器；合成 TE 无光影走 glsm FFP/texgen、光影走替代渲染器并复刻 CONSTANT_ALPHA 淡出钩子）+ 管线按维度缓存消除看穿双 pass 的重载风暴 | 0.4.1：末地传送门看穿失效/星野条纹/光影卡顿地形消失/光影星野旁路均已修复（四层根因见 [docs/compat/betterportals.md](compat/betterportals.md)）；无光影与光影（BSL）场景看穿+星野+淡出+换维度均实机确认正常 |

## 验证记录模板

```text
日期：
Actinium commit：
Java / Cleanroom：
GPU / 驱动 / OS：
光影包与预设：
模组列表：
场景：世界加载、维度切换、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载
结果：
日志与截图：
```
