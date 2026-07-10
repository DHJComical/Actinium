# Actinium 兼容性矩阵

最后更新：2026-07-10。

状态定义：`已验证` 表示在记录的版本和场景中通过；`部分` 表示能运行但存在已知缺口；
`未验证` 不代表不兼容。更新记录时必须填写 Actinium commit、光影/模组版本和测试环境。

## 光影包

| 光影包 | 版本 | 状态 | 已验证范围 | 已知缺口 | Actinium 基线 |
| --- | --- | --- | --- | --- | --- |
| MakeUp Ultra Fast | 9.4c | 部分 | 基础世界、天空、水、阴影、post、TAA | 高设置场景和多模组组合需复测 | 历史验证，待在当前 Iris 管线上重建基线 |
| BSL | 未固定 | 未验证 | - | - | - |
| Complementary | 未固定 | 未验证 | - | - | - |
| Bliss + Distant Horizons | 未固定 | 开发中 | LOD/cloud 路径有近期诊断与修复 | 尚无完整可复现验证记录 | `c3d4ac2` 附近 |

## 模组与环境

| 组件 | 状态 | 接入方式 | 备注 |
| --- | --- | --- | --- |
| Cleanroom Loader | 必需 | Forge/Cleanroom 启动与 MixinBootstrap | 当前目标运行环境 |
| Celeritas | 内嵌 | Gradle 子项目、最终 Jar 合并 | Actinium 的区块渲染器 |
| GLSM | 内嵌 | Gradle 子项目、service provider | 管理 GL 状态和固定管线兼容 |
| GTNHLib | 内嵌 | Gradle 子项目、bridge API | 提供底层渲染与内存工具 |
| Distant Horizons | 部分 | API、late Mixin、Iris LOD programs | 版本变化敏感，必须按指定版本验证 |
| Fluidlogged API | 代码支持 | compile-only API、条件调用 | 尚缺当前运行时验证记录 |
| Gibbed | 代码支持 | late Mixin、模型批处理路径 | 尚缺当前运行时验证记录 |
| ModernUI | 代码支持 | GUI scale hook | 尚缺当前运行时验证记录 |

## 验证记录模板

```text
日期：
Actinium commit：
Java / Cleanroom：
GPU / 驱动 / OS：
光影包与预设：
模组列表：
场景：世界加载、维度切换、地形、实体、方块实体、水、天空、天气、阴影、手部、GUI、重载
结果：
日志与截图：
```
