# Actinium Mod

## 1. 项目简介

Actinium 是一个专为 Minecraft 客户端设计的开源性能优化模组，旨在通过改进渲染管线和着色器处理，显著提升游戏性能和视觉体验。该项目深度整合了现代渲染技术，并致力于与主流着色器包（如 BSL Shaders 和 Complementary Shaders）实现兼容，为玩家提供更流畅、更美观的游戏世界。

## 2. 主要特性

*   **高性能渲染管线**：Actinium 优化了 Minecraft 的渲染流程，包括 gbuffers、shadow、prepare、deferred、composite 和 final 等阶段，以减少渲染延迟并提高帧率。
*   **着色器兼容性**：项目核心目标之一是实现与流行着色器包的广泛兼容，允许玩家在享受性能提升的同时，也能体验高质量的视觉效果。
*   **基于 Mixin 的架构**：通过 Mixin 技术，Actinium 能够无缝地修改 Minecraft 客户端的渲染逻辑，实现深度优化而不直接修改游戏核心文件。
*   **LWJGL3 集成**：利用最新版本的 Lightweight Java Game Library (LWJGL3) 提供更高效的图形渲染能力。
*   **灵活的配置选项**：提供多种配置选项，允许用户根据自己的硬件和偏好调整模组行为，例如启用/禁用 Iris 和 Celeritas 功能、线程块构建等。

## 3. 路线图

Actinium 项目的开发遵循明确的路线图，以下是当前及未来的主要目标：

### 3.1. 当前状态

MakeUp Ultra Fast 9.4c, BSL_v10.1.3 在 Cleanroom Loader 环境下稳定运行。核心渲染管线已完整跑通。最近的冲刺完成了 `mc_midTexCoord` 修复、半透明重定向开启、`drawBuffers` 动态解析、测试基线建立、`TerrainPhase` 拆分和 `GlStateGuard` 提取等关键任务，所有架构目标均已达成。

### 3.2. 即将进行

*   **兼容性测试**：使用 BSL 或 Complementary 着色器包进行首次兼容性测试，记录并解决发现的问题。
*   **MakeUp 高设置场景验证**：补全对体积云、水面反射、DOF、体积光、雨雪粒子、飞溅粒子、雾过渡等 MakeUp 高设置场景的手动验证，确保无回归。
*   **兼容性矩阵文档**：建立详细的兼容性矩阵文档，明确不同着色器包与各项功能的验证状态。

### 3.3. 中期目标

*   **`prepare` / `deferred` 时序优化**：进一步向 Iris 标准靠拢，优化 `shadow-to-prepare` 时机对齐、`deferred` 的 `scene/depth` 输入精确性以及逐阶段的 `buffer ownership`。
*   **`buffer flip` 语义统一**：将所有 `flip` 路径收敛到统一的 `flipWrittenTargets` 逻辑中，与 Iris 的 `BufferFlipper` 对齐，消除潜在的 `ping-pong` 错误。
*   **性能分析与优化**：量化半透明重定向引入的 FBO 切换和 blit 开销，并进行优化。
*   **`RenderPipeline` 进一步拆分**：沿 `phase` 边界继续拆分 `RenderPipeline`，例如 shadow 渲染逻辑和 `post program` 调度。
*   **补全测试覆盖**：增强对薄弱区域的测试覆盖，确保新发现的 bug 都有对应的测试用例。

### 3.4. 长期目标

*   **`solid / cutout terrain` 完整 `gbuffer` 路径**：实现 `ENABLE_EXTERNAL_TERRAIN_REDIRECT` 的完整功能，包括 `material/normal/specular data` 接入 `Celeritas vertex format`。
*   **`colortex8–15` 完整写入路径**：补全 `gbuffers` 阶段往 `colortex8+` 写入的 FBO `color attachment` 路径。
*   **`VintageHorizons` 兼容层**：实现对 `VintageHorizons` 的兼容，包括 `softLod uniform` 注入和 `dh_terrain / dh_water program`。
*   **`uniform` 懒更新**：优化 `uniform` 的上传机制，减少不必要的开销。

## 4. 安装

由于 Actinium 是一个 Minecraft 模组，其安装通常涉及以下步骤：

1.  确保您已安装Cleanroom。
2.  下载最新版本的 Actinium 模组文件（通常为 `.jar` 格式）。
3.  将下载的模组文件放入 Minecraft 游戏目录下的 `mods` 文件夹中。
4.  启动 Minecraft 客户端，模组应自动加载。

## 5. 贡献

我们欢迎社区成员对 Actinium 项目做出贡献。如果您有兴趣参与开发、提交 bug 报告或提出功能建议，请访问项目的 GitHub 仓库并贡献。

## 6. 许可证

Actinium 项目采用 GNU General Public License (GPL) 许可证。这意味着您可以自由地使用、分发和修改本软件，但必须在分发时附带相同的许可证。

## 7. 致谢

Actinium 项目的开发受到了 Celeritas 项目的启发和影响，在此向 Celeritas 项目及其贡献者表示衷心感谢。

---
