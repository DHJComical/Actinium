# Actinium 兼容性矩阵

最后更新：2026-08-18。

状态定义：`已验证` 表示在记录的版本和场景中通过；`部分` 表示能运行但存在已知缺口；
`无法启用` 表示光影包不能成功开启；`未验证` 不代表不兼容。更新记录时必须填写 Actinium commit、
光影/模组版本和测试环境。

本轮验证环境：Actinium `30c7ffb`、Java 25.0.3、Cleanroom 0.5.12-alpha、Distant Horizons 3.1.2-b、
Windows 10、NVIDIA GeForce RTX 5070 Laptop GPU（驱动 610.74）。

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
> 2026-08-18 追加：Snow! Real Magic! 0.7.4（issue #35）带雪栅栏不渲染的修复——见下方
> [模组与环境](#模组与环境) 的 Snow! Real Magic! 行。根因是 SRM 把被雪覆盖的方块替换为携带
> `SnowTile` 的 `snow_layer`，并只在 `BlockRendererDispatcher.renderBlock` 内重绘被覆盖方块，
> 而 Actinium 的快速区块渲染路径直接走 baked model、从不调用该入口；修复让 SRM 的
> `snow_layer` 块退回 vanilla dispatcher 路径（`6aee395`，dev 运行验证通过）。

## 光影包

| 光影包                                | 版本            | 状态   | 已验证范围                                                          | 已知缺口      | Actinium 基线 |
|------------------------------------|---------------|------|----------------------------------------------------------------|-----------|-------------|
| MakeUp Ultra Fast                  | 9.1f          | 已验证  | 世界加载、维度切换、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载、Distant Horizons LOD | -         | `f261611`   |
| BSL                                | 10.0          | 已验证  | 世界加载、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载、Distant Horizons LOD      | -         | `f261611`   |
| Complementary Reimagined / Unbound | r5.5.1        | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `f261611`   |
| Bliss                              | 2.1.2         | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `28d976d`   |
| iterationT                         | 3.2.0         | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `30c7ffb`   |
| iterationRP                        | 0.7.7 / 0.8.7 | 已验证  | 开启、世界渲染、Distant Horizons LOD、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载   | -         | `28d976d`   |
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
| Depths Update    | 已验证 | 兼容门控（`compat/depthsupdate`：公开 API 推导 section 范围 + storage 索引映射） | 1.0.0-a10：扩展世界高度（默认 -64..320）下 Y<0 与 Y>255 的方块不再缺失（渲染器原先硬编码 0-255）；dev 实测正常；无 Depths 时回退 vanilla 行为 |
| EnderIO CEu / EnderCore CEu | 已验证 | 无（核心渲染语义修复，非模组接入） | 5.4.2 + EnderCore 0.5.81：光影开启时流体罐内液体被罐体玻璃窗深度遮挡的问题已修复（`cb4feaa5`，translucent terrain pass 不再写深度）；MakeUp Ultra Fast 9.4c + Cleanroom 0.5.17-alpha 实测通过 |
| Snow! Real Magic! | 已验证 | 兼容门控（SRM 的 snow_layer 块退回 vanilla dispatcher 路径） | 0.7.4：带雪栅栏不渲染已修复（SRM 把被覆盖方块替换为带 SnowTile 的雪层、仅在 `BlockRendererDispatcher.renderBlock` 内重绘，快速区块路径已绕过）；`6aee395`，dev 运行验证通过（MakeUp Ultra Fast 下无光影 + 光影各验一次） |
| Modern Splash    | 部分 | 无侵入（替换类与 mixin 注入天然兼容）+ splash 字体 color=0 修复 | 1.5.3（629058:8487408）dev 运行通过（coremod 加载、mixin 注入保留、字体颜色按配置生效）；光影场景回归待做，详见 [docs/compat/modern-splash.md](compat/modern-splash.md) |
| Reese's Sodium Options（内嵌） | 代码支持 | 内嵌移植 UI（`me.flashyreese.mods.reeses_sodium_options`，MIT）+ embeddium 选项数据层（`org.embeddedt.embeddium.api.options.*` 自研扩展） | RSO 界面作为视频设置入口（`MixinGuiOptions` 拦截按钮 101，`enabled=false` 回退原版 `GuiVideoSettings`）；`net.caffeinemc` 设置界面与配置模型已整体删除；编译与 349 项单元测试通过，**运行期视觉对比待人工验证**，详见 [docs/rso-port.md](rso-port.md) |

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
