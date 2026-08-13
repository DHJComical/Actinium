# 贡献指南

感谢你对 Actinium 的兴趣！本指南面向所有形式的贡献：报告问题、验证光影包兼容性、提交代码或文档。在开始之前，请先通读本指南与仓库根目录的 `AGENTS.md`——那里记录了更完整的架构约束与开发规范。

## 项目简介

Actinium 是一个面向 Minecraft 1.12.2 / Cleanroom Loader 的渲染与光影兼容模组，目标是让旧版客户端运行现代化渲染管线，同时让光影包、经典模组内容与性能渲染共存。渲染栈由以下子项目组成：

- `src/`：生命周期、配置、兼容层与 Actinium 自有渲染代码
- `celeritas-common/`：区块构建、上传、裁剪与绘制
- `glsm/`：OpenGL 状态跟踪、重定向与固定管线模拟
- `shader/`：Iris 风格的光影包解析、GLSL 变换与渲染管线
- `GTNHLib/`：内存、buffer、后处理与旧版渲染工具

改动渲染管线前，先读 `docs/architecture.md`；光影包兼容现状见 `docs/compatibility-matrix.md`。

## 开发环境

- **JDK 25**：构建工具链（本项目 Gradle 需要 Java 25，请勿使用系统默认的低版本 JVM）
- **Gradle**：使用仓库自带的 `./gradlew` 包装器，无需单独安装
- **IDEA**：直接用 IDEA 打开项目根目录，Gradle 会自动导入子项目；dev 环境通过 IDEA 的 `2. Run Client` 运行配置启动（`run/client`）

## 构建与验证

以下命令在项目根目录执行（Windows 用 `.\gradlew.bat`，其他平台用 `./gradlew`）：

```powershell
# 完整构建（发布前必须执行）
.\gradlew.bat build --no-daemon

# 快速编译检查
.\gradlew.bat compileJava --no-daemon

# 自动化测试 + remap jar 结构校验（改动后至少跑一次）
.\gradlew.bat check --no-daemon
```

可安装的产物是 `build/libs/Actinium-<version>.jar`；随主模组一起构建的兼容桥是 `celeritas-compat-bridge-<version>.jar`。

## 开发规范

### 编码与风格

- 所有文件使用 **UTF-8 无 BOM** 编码。
- **代码注释使用英文**；交流（issue、PR、提交信息）可使用中文。
- 代码中禁止使用完全限定名，优先使用 `import` 导入。
- 面向接口编程：逻辑层依赖接口而非具体实现，接口命名为 `Xxx`，实现类命名为 `XxxImpl`；接口及成员需说明动机与作用。

### 错误处理（Fail Fast）

- 遵循 Fail Fast 原则，不为兼容或兜底刻意编写防御代码。
- 出现异常必须输出日志，严禁空 `catch` 块；核心业务逻辑应避免未捕获异常导致进程崩溃。

### 测试

- 项目使用 JUnit（`src/test`），`check` 会运行全部测试。
- 测试应直接调用目标代码进行完整逻辑验证，**禁止**只做文本包含关系检查的 "contain" 类测试。
- 简单到不值得测试的代码可以不写测试；不要为验证"某功能已被删除"而写测试。
- 禁止使用反射编写测试或实现业务功能（除非用户明确要求）。

### Mixin 组织

- **mixin 类只做注入**，业务逻辑放在 mixin 包之外的实现类中，mixin 类保持薄。
- 注入接口与注入类分离：访问器/调用器按现有 `core.terrain.AccessorEntityRenderer` 模式组织。
- 新增 Mixin 必须加入 `MixinConfigurationTest` 覆盖的配置文件。
- 引用外部模组类的 Mixin 放在 late/conditional 配置中，避免未安装对应模组时触发类加载；兼容代码必须由模组存在性检查保护。
- **mixin 包隔离**：mixin 包内的辅助类（含内部类）不得被 mixin 目标类或其调用方直接引用，否则会触发 `IllegalClassLoadError`（该问题只在生产环境出现，dev 验证覆盖不到）。辅助实现一律放 mixin 包外。

### 渲染管线改动

- 新增 render pass 必须明确 framebuffer、program、texture unit、viewport 与混合/深度状态的归属。
- 子项目依赖方向固定为 `celeritas-common -> glsm -> GTNHLib`；子项目不得引用 `com.dhj.actinium` 实现类，跨边界行为通过 bridge、provider 或小接口注入。
- 改动会影响光影包表现时，请在 dev 环境用目标光影包（如 MakeUp、BSL、Complementary）做一次手动验证，并更新 `docs/compatibility-matrix.md`。

## 文档

- 渲染管线相关的结构性改动，同步更新 `docs/architecture.md`、`docs/roadmap.md` 或 `docs/compatibility-matrix.md`。
- 引用到的第三方代码来源与许可证变更，同步更新 `THIRD_PARTY_NOTICES.md`。

## 提交与 PR

### Commit 风格

采用 **Conventional Commits** 格式，便于多人协同与工具化处理（语义化版本、变更日志生成等）：

```
<type>(<scope>): <subject>
```

- `type` 使用小写英文：`feat`（新功能）、`fix`（修复）、`docs`（文档）、`refactor`（重构）、`test`（测试）、`chore`（杂项）、`perf`（性能）、`build`（构建）、`ci`（CI）、`revert`（回滚）。
- `scope` 为可选模块名（如 `shader`、`glsm`、`compat`、`build`），可省略。
- `subject` 简短、英文、祈使句，例如 `fix(shader): keep option sliders visible`；不使用 emoji 或中文。
- 破坏性变更在 type 后加 `!`，并在提交正文中写明 `BREAKING CHANGE` 说明。

### 提交原则

- 一个提交只做一件事，保持改动小而可审阅。
- 提交前确保 `:check` 通过；涉及渲染行为的改动需要在 dev 环境回归验证后再提交。
- 不要提交构建产物（`build/`、`out/`、`run/` 等已在 `.gitignore` 中）。

### Pull Request

- PR 标题使用英文，简短描述改动目的；描述中说明：改了什么、为什么改、如何验证（测试命令 + dev 回归结果）。
- 若改动影响光影包或模组兼容性，附上验证过的光影包/模组列表。
- 引用相关 issue（如 `Closes #123`）。

## 报告问题

- 请使用对应的 issue 模板：[Bug 反馈](.github/ISSUE_TEMPLATE/bug_report.md)、[光影适配](.github/ISSUE_TEMPLATE/shader_pack_compat.md) 或 [功能请求](.github/ISSUE_TEMPLATE/feature_request.md)。
- 崩溃问题请附上 `crash-reports/` 下的崩溃报告与 `logs/latest.log`；渲染异常请附截图与光影包名称、预设、版本。
- 报告前先搜索是否已有相同 issue。

## 许可

提交代码即表示你同意你的贡献按照仓库 `LICENSE` 分发。来自其他项目的代码需保留各自上游的许可证与署名。
