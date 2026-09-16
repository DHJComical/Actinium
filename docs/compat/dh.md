# Distant Horizons 兼容性说明

兼容状态：**部分兼容**（版本敏感，必须按指定版本验证）
最后更新：2026-09-16

## 模组信息

- Distant Horizons（modid `distanthorizons`），按兼容矩阵指定版本
- 接入方式：**不注入 DH**。DH 自持 Iris 集成（`IIrisAccessor` 注册与延迟透明 LOD 开关），
  Actinium 只通过 DH 的公开 API（`DhApi`、render proxy、override、events）与其渲染入口接线

## 机制

- Actinium 不在 DH 的类上做任何 Mixin 注入：`mixin/mod/dh`（8 个类）与 `mixins.actinium.dh.json`
  已整体移除，`DistantHorizonsIrisAccessorState` 及其访问器过滤 Mixin 随之删除。
- Iris 侧的 DH 接管位于 `shader` 子项目的 `net.coderbot.iris.compat.dh`：`LodRendererEvents` 通过
  DH 的 API events 注册 Iris 的 LOD / generic override program、framebuffer 与深度纹理，
  `DHCompatInternal` 保存相应状态。
- `DistantHorizonsCompat`（`src/main/java/com/dhj/actinium/compat/dh/`）只做 Actinium 侧的渲染状态
  同步（GLSM 投影/模型矩阵、雾色、lightmap），并在 Iris 的 `beginTranslucents()` 之后驱动 DH 的
  延迟透明 LOD pass；DH 侧开关关闭时该调用是惰性的（`LodRenderer.renderTerrain` 在
  `runningDeferredPass && !deferTransparentRendering` 时直接返回）。
- DH 控制云与 LOD 地形渲染（`8de0eef` 起的历史提交），版本变化敏感。

## 由 DH 持有的开关

- `IIrisAccessor`：DH 的 1.12.2 Iris 支持（上游提交 `b15b57cf`）在检测到 `actinium` 已加载时
  自行绑定；Actinium 不再注册（重复绑定会触发 `ModAccessorInjector` 的 `IllegalStateException`）。
- `renderProxy.setDeferTransparentRendering()`：由 DH 侧配置决定，Actinium 只读取、不再每帧写回。
- 因此 Actinium 需搭配含该提交的 DH。`gradle/scripts/dependencies.gradle` 里的
  `distant-horizons-508933:8389134`（3.2.0-b）早于该提交，升级前 dev 环境既不会有 Iris 访问器
  注册，也不会启用延迟 LOD。

## 移除注入后回归 DH 自身行为的部分

以下行为此前由 Actinium 注入 DH 实现，现在交由 DH 侧接管：

- **far clip / far fade / AA**：DH 的 `RenderUtil.getFarClipPlaneDistanceInBlocks()` 与
  `LodRenderer.renderTerrain()` 只判断 `IRIS_ACCESSOR != null`，而 DH 自注册的访问器恒非 null，
  无光影包时也会走 Iris 分支（far clip 缩短为 `√2/2`）。建议 DH 上游改判 `isShaderPackInUse()`。
- **启动顺序**：原 `MixinConfig` / `MixinDependencySetup` 让 Actinium 提前建立 DH 客户端绑定、
  并让 DH 自身的绑定调用跳过；移除后 Actinium 不再提前绑定 DH 依赖。
- **多线程 LOD 构建**：方块状态缓存查询的加锁（原 `MixinFullDataToRenderDataTransformer`）。
- **雾色**：DH 取雾色的路径（原 `MixinFogRenderParamFactory`）。
- **F3 覆盖层**：DH 渲染目标状态的调试行（原 `InvokerGlDhMetaRenderer`）。

## 验证记录

- 兼容矩阵记录：光影包 + DH LOD 场景已验证（MakeUp/BSL/Complementary/Bliss/
  iterationT/iterationRP）。
- DH 3.1.2-b + 光影 + 进出世界/维度切换回归通过。

## 待办

- [ ] DH 发布含 `b15b57cf` 的版本后升级 gradle 依赖并做一次完整回归
- [ ] 与 DH 侧确认上节各项由 DH 接管后的实际表现（尤其 far clip 与雾色）
- [ ] 新版本 DH 验证后更新兼容矩阵
