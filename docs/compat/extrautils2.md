# 异步区块构建中的第三方方块渲染兼容

最后更新：2026-08-19。相关 issue：[Actinium #36](https://github.com/DHJComical/Actinium/issues/36)。

## 问题

Issue #36 报告 Extra Utilities 2 的 drum 在异步区块网格构建期间崩溃。历史堆栈指向
`HashMap.computeIfAbsent` 与 `com.rwtema.extrautils2.backend.XUBlockStatic$3.getQuads`，
表现为第三方模型的惰性缓存被多个 chunk-builder worker 同时修改。

Issue 中引用的 mclo.gs 日志目前已过期，因此本文只保留从 XU2 与 AgriCraft 字节码/源码
确认的缓存结构，不把这些地址作为当前可复查的日志证据。

## 根因

Extra Utilities 2 的 `XUBlockStatic` 为每个静态方块实例维护普通 `HashMap`：

- `layerMap` 缓存 `canRenderInLayer` 的结果；
- `cachedModels` / `cachedInvModels` 缓存模型；
- `XUBlockStatic$3.cachedLists` 以方块状态、面方向和 render layer 为键，使用三级
  `HashMap.computeIfAbsent` 缓存 baked quads。

原版区块构建主要在单线程中访问这些缓存。Actinium 的 `ChunkBuilder` 使用多个 worker，
所以同一个 XU2 block 的模型初始化会产生并发 `HashMap` 访问。

AgriCraft 的 `BlockCrop` 也暴露了相同类别的问题：`RenderCrop` 持有 renderer 级别的
非线程安全 crop-quad 缓存。它不能按 block 实例分别加锁，必须使用共享 renderer 锁。

## 修复

`ModdedBlockRenderCompat` 是异步区块构建调用侧的统一边界：

- XU2 通过 `Loader.isModLoaded("extrautils2")` 和类名前缀门控，并按 block 实例加锁；
- AgriCraft 通过 `Loader.isModLoaded("agricraft")` 和类名前缀门控，并使用共享锁保护
  renderer 级缓存；
- 模组状态在实际调用时查询，不在可能过早加载的静态初始化阶段缓存；
- `VintageBlockRenderer.renderBlock` 的完整生命周期在同一锁内执行，覆盖模型查询及其
  后续 quad 处理；
- `canRenderInLayer`、vanilla `BlockRendererDispatcher.renderBlock` 和 Fluidlogged API
  路径均经过同一兼容层；
- BetterFoliage 的调用侧 Mixin 目标同步到新的兼容入口，保留其自身的渲染包装逻辑。

锁只作用于已加载且类名匹配的第三方 renderer；普通 Minecraft 方块不进入兼容锁。

## 验证

已完成：

- `ModdedBlockRenderCompatTest` 直接调用锁边界，用两个 worker、`HashMap.computeIfAbsent`
  和 barrier 验证共享 renderer/cache 不会并发进入；
- XU2、AgriCraft 类名识别及无关类排除测试；
- 根项目 `:test --tests com.dhj.actinium.compat.blockrender.ModdedBlockRenderCompatTest`；
- 根项目 `test`、`compileJava`、`check`、`build`、`verifyCompatBridgeJar`、
  `verifyModuleBoundaries` 和 `verifyRemapJar`；
- 2026-08-19 dev 实测：加载 `extrautils2@1.0`，启动日志显示 10 个 chunk-builder worker，
  进入已有世界并触发区块重载；未出现 `ConcurrentModificationException` 或
  `XUBlockStatic` 错误，客户端正常退出。

待完成：

- 在包含 AgriCraft crop 的实例中触发相同路径；
- 记录 AgriCraft 的实际运行版本、模组列表和日志。
