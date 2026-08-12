# Better Foliage 兼容性说明

兼容状态：**代码支持**（机制从代码推断，运行验证待记录）
最后更新：2026-08-12

## 模组信息

- BetterFoliage（modid `betterfoliage`），1.12.2 版本
- 接入方式：late/conditional Mixin（`mixins.actinium.betterfoliage.json`，`betterfoliage` 条件）

## 机制

- `MixinChunkBuilderMeshingTaskBetterFoliage`：区块构建 meshing 任务中适配 Better Foliage
  的 foliage 生成路径（BF 会替换原版方块模型生成逻辑，需在 Celeritas 的 meshing 任务
  中兼容）。

## 验证记录

- 待补：dev 环境启用 BetterFoliage 后进世界，验证植被渲染正常、无崩溃。

## 待办

- [ ] 运行验证并回填验证记录
