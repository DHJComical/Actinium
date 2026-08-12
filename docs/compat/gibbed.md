# Gibbed 兼容性说明

兼容状态：**代码支持**（运行验证待记录）
最后更新：2026-08-12

## 模组信息

- Gibbed（modid `gibbed`），1.12.2 版本
- 接入方式：late/conditional Mixin（`mixins.actinium.gibbed.json`，`gibbed` 条件）

## 机制

- `BasicGibMixin`：注入 Gibbed 的模型渲染路径，走 Actinium 的快速路径。
- `ActiniumModelRenderer`：Gibbed 模型渲染的兼容实现（模型批处理路径）。

## 验证记录

- 待补：启用 Gibbed 后验证实体/方块模型渲染正常。

## 待办

- [ ] 运行验证并回填验证记录
