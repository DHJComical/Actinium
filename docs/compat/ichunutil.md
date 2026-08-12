# iChunUtil 兼容性说明

兼容状态：**代码支持**（Portal 渲染集成，验证记录待补）
最后更新：2026-08-12

## 模组信息

- iChunUtil（modid `ichunutil`），1.12.2 版本
- 接入方式：late/conditional Mixin（`mixins.actinium.ichunutil.json`，`ichunutil` 条件）

## 机制

- Mixin（3）：`MixinPortalFrustum`（传送门视锥）、`MixinRenderGlobalProxy`（渲染
  代理）、`MixinWorldPortalRenderer`（传送门渲染器）。
- 实现（5）：`PortalRenderState`（传送门渲染状态）、`PortalChunkRenderMatrices`
  （传送门视口下的区块矩阵）、`PortalViewportFactory` / `PortalViewportProvider`
  （视口构造/注入）、`WorldBoxVisibility`（世界包围盒可见性）。

## 验证记录

- 待补：启用 iChunUtil 后验证传送门渲染（视口/矩阵/遮挡）正常。

## 待办

- [ ] 运行验证并回填验证记录
