# CodeChickenLib 兼容性说明

兼容状态：**已验证**（DE 场景回归通过；独立场景验证待补）
最后更新：2026-08-12

## 模组信息

- CodeChickenLib（modid `codechickenlib`），3.3.8（生产整合包为 3.2.3.358）
- 接入方式：late/conditional Mixin（`mixins.actinium.ccl.json`，`codechickenlib` 条件）

## 机制

- `MixinGlStateTracker`：`@Overwrite` CCL 的 `GlStateTracker.pushState()/popState()`，
  委托给 `com.dhj.actinium.compat.ccl.GlStateTrackerSnapshot`（mixin 包外的实现类）。
  CCL 的 tracker 原本读取原版 `GlStateManager` 字段缓存——在 GLSM 重定向下该缓存
  冻结，DE 每帧 HUD 的 push/pop 会把真实 GL 状态重置为初始默认值；兼容桥改为读写
  GLSM 真实 tracked 状态，恢复帧末状态为「GUI 渲染后的合理状态」。

## 验证记录

- dev 环境（Cleanroom 0.6.7，DE 2.3.28.354 + CCL 3.3.8）：云/草方块侧面/主菜单
  三症状修复回归通过（详见 draconic-evolution.md）。
- CCL 3.2.3.358（生产整合包版本）的 `GlStateTracker` 方法签名兼容性待实际验证。

## 待办

- [ ] CCL 3.2.3.358 下验证 mixin 兼容性
- [ ] 无 DE 场景下 CCL 独立验证
