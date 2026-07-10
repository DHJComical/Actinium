# Actinium 开发路线图

最后更新：2026-07-10，基于 Iris 管线移植和 Gradle 子项目拆分后的代码。

## 当前基线

- Iris 风格的 world、shadow、prepare、deferred、composite 和 final 管线已经进入 `shader/`。
- Celeritas、GLSM 和 GTNHLib 已拆成 Gradle 子项目，但仍作为同一个模组发布。
- MakeUp 是已有验证基础；Distant Horizons、Bliss 和现代方块 ID 映射是近期活跃区域。
- `build`、16 个自动化测试和 remap Jar 内容校验可通过。

## 近期

1. 建立可复现的光影兼容验证流程。
   - 固定光影包版本、预设、显卡/驱动、场景与验证 commit。
   - 更新 `compatibility-matrix.md`，把“已加载”和“功能正确”分开记录。
   - 每个兼容性修复附带可在无图形环境运行的解析、映射或转换测试。

2. 明确发布物。
   - CI 和 Release 只上传经过 MCP 到 SRG 重映射的 `Actinium-<version>.jar`。
   - 保持 `verifyRemapJar` 对入口、Mixin、内嵌模块和 manifest 的检查。

3. 稳定 Iris/Celeritas 边界。
   - 将跨模块运行时配置和回调继续收敛到小型 bridge/API。
   - 对地形材质、shader program provider 和 framebuffer ownership 建立明确契约。
   - 防止子模块直接依赖 Actinium 主模块实现类。

4. 降低调试配置噪声。
   - 区分面向用户的配置、开发 JVM 属性和构建期选项。
   - 默认关闭高频日志，只保留一次性环境、Mixin 和兼容层诊断。

## 中期

- 扩大 BSL、Complementary、Bliss 的验证范围，优先处理 solid/cutout terrain、实体、天气和 `colortex8+`。
- 为 pipeline 创建/销毁、维度切换、buffer flip 和 render target ownership 增加更多纯逻辑测试。
- 建立 minimal、DH 和兼容模组组合的 CI 配置，避免依赖脚本靠手工注释切换。
- 对 terrain rebuild、shader transform、shadow 和 DH LOD pass 做可重复的 CPU/GPU 性能基线。
- 跟进 Unimined 的 Gradle 10 兼容性；在此之前固定 Gradle 9.6。

## 长期

- 在 Celeritas 扩展顶点格式上完成稳定的 solid/cutout gbuffer 路径。
- 补全高级 render target、compute/image 和现代光影包特性，但必须保留硬件能力降级路径。
- 将内嵌上游更新流程标准化：记录基准版本、导入范围、本地补丁和许可证。

## 非目标

- 不保证兼容所有替换区块渲染器的模组。
- 不以牺牲无光影路径稳定性换取单一光影包效果。
- 不把“可以编译”视为光影兼容完成；运行时验证仍是发布条件。
