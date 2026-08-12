# Distant Horizons 兼容性说明

兼容状态：**部分兼容**（版本敏感，必须按指定版本验证）
最后更新：2026-08-12

## 模组信息

- Distant Horizons（modid `distanthorizons`），按兼容矩阵指定版本（3.1.2-b 等）
- 接入方式：late/conditional Mixin（`mixins.actinium.dh.json`，`distanthorizons` 条件）
  + API event / framebuffer / depth texture / LOD shader

## 机制

- Mixin（7）：`MixinClientApi`（客户端 API 接线）、`MixinConfig`、`MixinDependencySetup`
  （依赖初始化）、`MixinFogRenderParamFactory`（雾参数）、
  `MixinFullDataToRenderDataTransformer` / `MixinRenderUtil`（LOD 数据变换）、
  `MixinLodRenderer`（LOD 渲染器集成）。
- 实现（4）：`DistantHorizonsCompat`（事件/framebuffer 接线）、
  `ActiniumDHIrisAccessor` / `ActiniumDHIrisCompat` / `DistantHorizonsIrisAccessorState`
  （光影下 LOD 状态访问）。
- DH 控制云与 LOD 地形渲染（`8de0eef` 起的历史提交），版本变化敏感。

## 验证记录

- 兼容矩阵记录：光影包 + DH LOD 场景已验证（MakeUp/BSL/Complementary/Bliss/
  iterationT/iterationRP）。
- DH 3.1.2-b + 光影 + 进出世界/维度切换回归通过。

## 待办

- [ ] 新版本 DH 验证后更新兼容矩阵
