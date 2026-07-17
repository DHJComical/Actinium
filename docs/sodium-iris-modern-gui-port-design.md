# Sodium / Iris 现代 GUI 移植设计规格

## 1. 背景与目标

Actinium 面向 Minecraft 1.12.2，目前需要将现代 Sodium 与 Iris 的配置界面能力移植到现有模组，同时维持可持续同步上游、清晰声明许可和继续扩展页面的能力。

本次实现以以下上游版本为固定基准：

- Sodium：提交 `3c4c4b29`。
- Iris：提交 `844542b84`。

目标包括：

- 移植现代 Sodium 的 Config API、配置模型、搜索、页面导航、控件、Tooltip、Apply、Undo、Reset 和状态提示。
- 保持 Sodium 上游的包结构，使后续比较和同步上游变更时边界清晰。
- 将 Actinium 现有配置与渲染重载机制接入新 GUI，不创建第二套配置来源。
- 以 Iris 的现代集成方式提供 Shader Pack external page，并维护其独立的应用与放弃生命周期。
- 隔离 GPL 资源与现代 Sodium 的 PolyForm Shield 资源，保证许可来源可审计。
- 移植完成并验证后，删除现有未跟踪的 `com.dhj.actinium.gui.modern` 原型。

## 2. 非目标

本次不包含：

- 移植 Sodium 的渲染器、平台启动器或现代 Minecraft runtime。
- 引入第二份 Actinium、Sodium 或 Iris 配置文件。
- 迁移 donation、narration、现代 fullscreen 特有逻辑和 Minecraft 1.12.2 无对应能力的选项。
- 迁移当前 Actinium shader core 不支持的 Iris Color Space 功能。
- 通过反射发现配置入口、页面或测试目标。
- 为兼容未知调用方增加静默兜底、占位实现或空 `catch`。
- 在本次范围内重新设计现代 Sodium GUI 的视觉和交互。

## 3. 许可与来源边界

现代 Sodium 源码和资源遵循上游 `PolyForm Shield License 1.0.0`，项目必须保留许可证全文、上游来源、固定提交号、迁移文件清单和必要声明。

许可说明应引用 Sodium issue 2400 中主维护者的评论 `2038560656`：该评论说明面向非常旧 Minecraft 版本的移植不与 Sodium 构成竞争。本项目将其作为 Minecraft 1.12.2 移植适用性的公开依据，但该记录不构成法律意见，也不替代发布者自行评估许可义务。

资源边界如下：

- 当前 `src/main/resources/assets/sodium` 下已有的 GPL 资源迁移到 `src/main/resources/assets/actinium`。
- 所有引用旧资源的位置同步改为 `actinium` namespace，迁移后不得再通过 `sodium` namespace 访问这些 GPL 资源。
- 从现代 Sodium `3c4c4b29` 移植的 PolyForm Shield 资源放入 `src/main/resources/assets/sodium`。
- 新增的 Sodium GUI 资源必须记录上游原始路径、提交号和本地目标路径。
- Actinium 自有资源继续使用 `assets/actinium`，不得混入上游 Sodium 来源清单。
- 构建产物可以同时包含两个 namespace，但源码树中的来源和许可记录必须能独立审计。

## 4. 代码所有权与包结构

### 4.1 Sodium 上游代码

Sodium GUI 相关源码保持上游包结构，统一位于：

`src/main/java/net/caffeinemc/mods/sodium`

内部按职责分层：

- `net.caffeinemc.mods.sodium.api.config`：公开 Config API、builder、binding、dependency、theme 与 external page 契约。
- `net.caffeinemc.mods.sodium.client.config`：配置模型、builder 实现、页面与 option 标识、搜索索引、pending state 和存储事务。
- `net.caffeinemc.mods.sodium.client.gui`：主界面、侧栏、选项列表、搜索框、控件、Tooltip、状态栏以及键鼠输入处理。

只允许为 Minecraft 1.12.2 API、渲染、输入和生命周期适配做必要改写。能够保持的类型名、职责和公开契约应与基准提交一致。

### 4.2 Actinium 适配层

Actinium 专属适配代码使用包 `com.dhj.actinium.compat.sodium`，位于：

`src/main/java/com/dhj/actinium/compat/sodium`

该层负责：

- 将 Minecraft 1.12.2 的界面、绘制、输入、文本、资源和生命周期 API 映射到 Sodium GUI。
- 声明 Actinium 页面并绑定现有配置对象。
- 将配置变更 flags 映射到现有 renderer、resource 或其他重载动作。
- 提供旧 `OptionGUIConstructionEvent` 的兼容桥，使既有扩展能接入新页面模型。
- 处理新 GUI 与现有入口 Mixin 的切换。

Sodium 上游包不得反向依赖 Actinium 的具体配置实现；跨边界依赖通过公开接口与 binding 完成。

### 4.3 Iris 集成层

Iris 集成代码使用包 `net.irisshaders.iris.compat.sodium`，位于：

`src/main/java/net/irisshaders/iris/compat/sodium`

该层以 Iris `844542b84` 的入口模式为基准，显式注册 Shader Pack external page、Iris settings 和动态依赖状态。它复用现有 `IrisConfig` 和 shader pack 生命周期，不复制 shader core。

## 5. 注册与扩展模型

页面、entry point、option 和 external page 全部采用显式注册。注册表必须在 GUI 创建前完成初始化，禁止 classpath 扫描和反射。

每个注册项具有稳定且唯一的 ID。重复 ID、缺失页面、无效 binding、缺失资源或不完整依赖在初始化阶段立即失败，并记录冲突 ID、来源和注册入口。

扩展顺序如下：

1. 注册 Sodium Config API 基础能力。
2. 注册 Actinium 内建页面与 binding。
3. 通过兼容桥接收 `OptionGUIConstructionEvent` 扩展。
4. 注册 Iris Shader Pack external page 与 Iris settings。
5. 冻结注册表并构建搜索索引和界面模型。

注册表冻结后不得动态修改。需要新增功能时，通过新显式入口在冻结前注册，避免运行中页面结构漂移。

## 6. 配置与事务模型

新 GUI 始终读写现有 `GameSettings`、`SodiumGameOptions` 和 `IrisConfig`。Config API 的 model 只表达页面、option、依赖、当前值和 pending value，不成为持久化来源。

标准 Apply 流程为：

1. 控件将用户输入写入 option 的 pending value。
2. binding 验证 pending value；验证失败时保留界面并报告具体 option ID 和原因。
3. 所有验证通过后，由 binding 写回现有配置对象。
4. 按 storage 去重并持久化配置，避免同一配置文件重复写入。
5. 汇总所有已应用 option 的 flags。
6. 按确定顺序执行 renderer、resource 和其他重载动作。
7. 更新基准值并清除 dirty state。

Undo 恢复为当前会话最近一次已应用的基准值；Reset 将 pending value 设置为该 option 定义的默认值。两者都不立即持久化，只有 Apply 才写回配置并执行 flags。

关闭界面或按 ESC 时，未应用的 pending value 被放弃。已经成功 Apply 的值保持不变。

Shader Pack external page 继续使用 Iris 自身的 apply/discard 生命周期。返回 Sodium 主页面时必须正确反映 Iris 已应用状态；离开未应用的 Shader Pack 页面时不得污染全局配置。

## 7. GUI 行为

主界面包含侧栏、当前页面标题、搜索入口、滚动选项区域、Tooltip 层和底部操作区。页面切换时维持侧栏选中状态，并清理不再有效的 hover、focus 和 Tooltip 状态。

搜索索引覆盖页面名、option 名和可搜索描述，使用当前语言的已解析文本。语言切换后重新构建索引，不复用旧语言缓存。

控件应覆盖移植页面实际需要的开关、滑块、枚举、数值和外部页面入口。控件必须支持键盘与鼠标操作，低分辨率和不同 GUI scale 下不得发生文本覆盖、不可达按钮或滚动区域越界。

Tooltip 只展示当前可见且可交互项的说明；依赖条件禁用 option 时，界面需表现禁用状态，并在上游契约允许时说明原因。

## 8. 错误处理与日志

实现遵循 Fail Fast：

- 注册冲突、无效页面结构、缺失资源和不可能执行的 binding 在首次确定时立即抛出有语义的异常。
- 日志包含页面 ID、option ID、入口来源、资源路径或 storage 标识，避免只有通用错误文本。
- 禁止空 `catch`、吞异常或以默认页面静默替代损坏页面。
- 第三方页面注册失败时记录具体入口与完整异常，并隔离该入口，不能破坏此前已成功注册的页面。
- 核心业务运行期间可以在进程边界捕获异常，记录后阻止危险的 Apply，避免未捕获异常直接导致整个客户端进程崩溃。
- 配置文件损坏时沿用项目现有策略：记录异常并进入只读默认配置状态；GUI 禁止 Apply，不伪造保存成功。

## 9. 测试策略

逻辑实现采用 TDD，测试必须直接调用目标类型并验证完整行为，禁止源码文本 `contain` 测试、反射和“证明功能已删除”的长期测试。

自动化测试至少覆盖：

- 注册表唯一 ID、冻结行为、入口失败隔离和依赖解析。
- Config builder 与 model 的有效、无效构造路径。
- binding 的读取、验证、保存与 storage 去重。
- pending value、Apply、Undo、Reset 和 ESC discard 状态转换。
- 多 option flags 汇总、执行顺序及失败日志。
- 搜索在页面名、option 名、描述和语言切换后的结果。
- `OptionGUIConstructionEvent` 兼容桥注册的页面与 option 行为。
- Iris Shader Pack external page 的 apply/discard 隔离。

Gradle 验证统一使用 `GRADLE_USER_HOME=D:/gradle`，依次执行目标测试、`compileJava` 和 `check`。测试失败必须先定位原因，不允许通过放宽断言或添加静默兜底规避。

## 10. 客户端验收

在 Minecraft 1.12.2 客户端中验证：

- 从原视频设置入口打开新 GUI，并能正确返回父界面。
- 所有页面、侧栏状态、滚动、搜索和语言切换工作正常。
- 开关、滑块、枚举、数值、Tooltip、键盘与鼠标输入行为正确。
- Apply、Undo、Reset、ESC discard 与 dirty state 提示符合事务模型。
- 配置保存到原有文件，重启客户端后值保持一致。
- flags 只触发必要的 renderer、resource 或其他重载，且同类动作去重。
- 不同 GUI scale、低分辨率和窗口缩放下无重叠、越界或不可操作元素。
- Iris 页面往返、shader pack apply/discard 和动态依赖状态正确。
- 资源只从预期 namespace 加载，不出现 GPL 旧资源继续占用 `assets/sodium` 的情况。

## 11. 迁移阶段

### 阶段一：资源与许可隔离

迁移现有 GPL `assets/sodium` 到 `assets/actinium`，修改全部资源引用并验证旧界面功能不回退；随后建立 PolyForm Shield 许可证、来源清单和现代 Sodium `assets/sodium` 目录。

### 阶段二：Config API 与模型

移植公开 Config API、builder、binding、dependency、theme、模型、注册表和搜索索引，以单元测试固定 ID、事务和依赖行为。

### 阶段三：Minecraft 1.12.2 GUI 适配

移植 Sodium GUI 组件并完成绘制、文本、资源、输入、焦点、滚动和界面生命周期适配，接入 Actinium 页面和现有配置对象。

### 阶段四：兼容与 Iris 集成

接入 `OptionGUIConstructionEvent` 兼容桥，移植 Iris external page 注册和 shader pack 生命周期，再完成页面间状态同步。

### 阶段五：入口切换与清理

切换现有选项界面入口，完成自动化与客户端验收后，删除未跟踪的 `src/main/java/com/dhj/actinium/gui/modern` 原型及其不再引用的资源。

## 12. 主要风险与控制

- 现代 Minecraft GUI API 差异：限制在 Actinium 适配层处理，避免上游模型混入 1.12.2 细节。
- 配置双写或重载过度：只绑定现有配置对象，以 storage 去重持久化并集中汇总 flags。
- 许可资源混淆：迁移前后扫描 namespace 引用，并用来源清单逐项核对现代 Sodium 文件。
- Iris 生命周期泄漏：external page 保持独立事务，通过往返和取消测试验证状态隔离。
- 扩展兼容性回退：保留 `OptionGUIConstructionEvent` 桥，但将输出转换成稳定 Config API 模型。
- 搜索缓存失效：语言变化时明确重建索引，并覆盖非英文客户端验证。
- 上游同步困难：保持 Sodium 原包、职责和可保持的类型契约，所有 Actinium 特有逻辑集中到适配包。

## 13. 完成验收标准

只有同时满足以下条件，移植才视为完成：

- Sodium `3c4c4b29` 与 Iris `844542b84` 的目标 GUI 能力均已按本规格接入。
- Sodium 源码、Actinium 适配和 Iris 集成分别位于约定包结构，无业务反射和完全限定名滥用。
- GPL 旧资源已迁到 `assets/actinium` 并改完引用，现代 PolyForm Shield 资源独占 `assets/sodium`。
- 许可证全文、提交号、issue 评论依据、来源清单和非法律意见声明完整。
- 新 GUI 复用现有配置文件，Apply、Undo、Reset、flags 和 Iris external page 行为通过逻辑测试。
- `OptionGUIConstructionEvent` 兼容桥可用，重复或无效注册按 Fail Fast 规则处理并记录日志。
- 目标测试、`compileJava` 和 `check` 在 `GRADLE_USER_HOME=D:/gradle` 下通过。
- 客户端验收项目全部通过，未发现资源 namespace、布局、输入或配置持久化回归。
- 新入口稳定运行，未跟踪的 `com.dhj.actinium.gui.modern` 原型已删除。
