# Distant Horizons 兼容性说明

兼容状态：**部分兼容**（版本敏感，必须按指定版本验证）
最后更新：2026-09-16

## 模组信息

- Distant Horizons（modid `distanthorizons`），按兼容矩阵指定版本（3.1.2-b 等）
- 接入方式：late/conditional Mixin（`mixins.actinium.dh.json`，`distanthorizons` 条件）
  + API event / framebuffer / depth texture / LOD shader

## 机制

- Mixin（7）+ Invoker（1）：`MixinClientApi`（客户端 API 接线）、`MixinConfig`、
  `MixinDependencySetup`（依赖初始化）、`MixinFogRenderParamFactory`（雾参数）、
  `MixinFullDataToRenderDataTransformer` / `MixinRenderUtil`（LOD 数据变换）、
  `MixinLodRenderer`（LOD 渲染器集成）、`InvokerGlDhMetaRenderer`（LOD 元渲染器调用器）。
- 实现（2）：`DistantHorizonsCompat`（事件/framebuffer 接线）、`DistantHorizonsIrisAccessorState`
  （无激活光影包时把 DH 读到的 `IIrisAccessor` 过滤为 null）。
- 访问器过滤：`MixinRenderUtil` / `MixinLodRenderer` 把 `RenderUtil` 与 `LodRenderer` 读取的
  `IIrisAccessor` 交给 `DistantHorizonsIrisAccessorState` 过滤。DH 为 `actinium` 注册的访问器
  **恒非 null**（与是否启用光影包无关），而 DH 仍有只判 `!= null` 的读取点（如
  `RenderUtil.getFarClipPlaneDistanceInBlocks()`），这层过滤因此不可删。`MixinLodRenderer`
  在含 `b15b57cf` 的 DH 上已与上游自带的 `isShaderPackInUse()` 判断等价，保留它是为了兼容旧
  DH 以及上游判据回退时的防护。
- DH 控制云与 LOD 地形渲染（`8de0eef` 起的历史提交），版本变化敏感。

## Iris 访问器归属

- `IIrisAccessor` 的注册归 Distant Horizons 所有：DH 的 1.12.2 Iris 支持（上游提交
  `b15b57cf`，记录版本 3.2.1-b）在 `CleanroomMain.initializeModCompat()` 中检测到
  `actinium` 已加载时，自行绑定 `cleanroom.modAccessor.IrisAccessor`。
- Actinium 不再注册自己的访问器（原 `ActiniumDHIrisCompat` / `ActiniumDHIrisAccessor` 已移除）。
  `ModAccessorInjector` 禁止同一接口重复绑定，且 FML 按 modid 字母序先执行 Actinium 的 `init`：
  Actinium 抢先注册会让 DH 随后的 bind 抛 `IllegalStateException`（上游 DH 侧无重复检查）。
- 因此 Actinium 需搭配含该提交的 DH。`gradle/scripts/dependencies.gradle` 里的
  `distant-horizons-508933:8389134`（3.2.0-b）早于该提交，升级到含 Iris 支持的版本前，
  dev 环境不会有任何 Iris 访问器注册。

## 验证记录

- 兼容矩阵记录：光影包 + DH LOD 场景已验证（MakeUp/BSL/Complementary/Bliss/
  iterationT/iterationRP）。
- DH 3.1.2-b + 光影 + 进出世界/维度切换回归通过。

## 待办

- [ ] DH 发布含 `b15b57cf` 的版本后升级 gradle 依赖并做一次完整回归
- [ ] 新版本 DH 验证后更新兼容矩阵
