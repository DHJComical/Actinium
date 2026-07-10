# 上游代码维护

最后更新：2026-07-10。

Actinium 以源码形式内嵌了多个大型上游项目。当前仓库能确定本地首次导入 commit，
但早期导入没有保存精确的上游 SHA。`THIRD_PARTY_NOTICES.md` 是当前来源清单，
本文件规定后续更新流程。

## 当前导入边界

| 组件 | 本地首次/主要导入 commit | 当前目录 |
| --- | --- | --- |
| Celeritas 基础 | `6167e49`（独立项目提取时已存在） | `celeritas-common/`、部分 `src/main/java/org/taumc/` |
| Iris 管线 | `79306ba` | `shader/`、Iris Mixin 与资源 |
| Angelica GLSM | `119f607`、`83ea1b7` | `glsm/`、部分 `src/main/java/com/gtnewhorizons/angelica/` |
| GTNHLib | `83ea1b7` | `GTNHLib/` |
| mitchej123 GL/LWJGL service | `4826cf8` | service 接口、provider 和相关 GL bridge |

这些 commit 只能证明代码何时进入 Actinium，不能替代上游 SHA。

## 更新流程

1. 在临时分支或独立 worktree 中检出上游的明确 tag/SHA。
2. 列出要同步的文件，不做整个目录的无差别覆盖。
3. 先提交机械同步，提交说明包含上游 URL、old SHA、new SHA、路径和许可证变化。
4. 再提交 Minecraft 1.12/Cleanroom、LWJGL、Mixin 和 Actinium bridge 适配。
5. 更新 `THIRD_PARTY_NOTICES.md`，保留新增或变更的文件头与许可证文本。
6. 运行 `gradlew build --no-daemon`，确认测试、模块边界和 remap Jar 校验通过。
7. 启动客户端验证无光影、目标光影包、维度切换、资源重载和条件兼容模组。

## 禁止事项

- 不使用包名推断许可证。
- 不删除或统一改写上游版权头。
- 不在同一个提交中混合大规模上游同步和无关重构。
- 不用 `duplicatesStrategy = EXCLUDE` 解决同步产生的重复 class。
- 不在没有运行时验证记录时把兼容状态标记为“已验证”。

## 待追溯

- Angelica/GLSM 在 `119f607`、`83ea1b7` 对应的精确上游 SHA。
- GTNHLib 在 `83ea1b7` 对应的精确上游 SHA。
- Iris 在 `79306ba` 对应的分支、tag 和 SHA。
- 早期 Celeritas、Embeddium、Sodium 各自进入当前代码树的文件范围和 SHA。
- mitchej123 service 层的上游仓库 URL、SHA 和许可证文件。

追溯完成后，应补充到 Notice，并将完整许可证文本放入 `third-party/licenses/`。
