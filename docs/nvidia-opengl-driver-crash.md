# NVIDIA OpenGL 原生崩溃：`0xC0000409` / `LiveKernelEvent 141`

> 当前状态：触发条件尚未确定。本文记录的是已经确认的崩溃特征和排查方法，不能据此认定某个 Actinium 功能、光影包或 NVIDIA 驱动版本是根因。

## 症状

在 Windows 和 NVIDIA GPU 环境中，Minecraft 进程可能突然退出。通过 Gradle 启动时，终端只报告：

```text
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':runClient' (registered in build file 'build.gradle').
> Process 'command 'D:\Program Files\Zulu\zulu-25\bin\java.exe'' finished with non-zero exit value -1073740791 (NTSTATUS 0xC0000409)
```

已核对的 2026-07-15 运行中，Minecraft 日志在 10:10:28 中断，末尾没有对应的 Java 异常堆栈；NVIDIA OpenGL Driver 错误出现在 10:10:39。当前保存的三次事件材料中均未发现 Minecraft crash report 或 `hs_err_pid*.log`，但现有证据不足以判断 JVM 未写出 `hs_err_pid*.log` 的具体原因。

`-1073740791` 是 Windows 状态码 `0xC0000409` 的有符号十进制表示。Windows Error Reporting 将该进程故障归类为 `BEX64`，参数 `P8` 为 `c0000409`、`P9` 为 `0000000000000007`。

## 已确认的系统事件

以下时间均为本机时间（UTC+8）。三次崩溃具有相同的关键事件组合：

| 时间                  | NVIDIA 驱动 | NVIDIA OpenGL Driver             | `nvlddmkm`                               | Application Error                                   |
|---------------------|----------:|----------------------------------|------------------------------------------|-----------------------------------------------------|
| 2026-06-21 22:24:03 |    610.62 | Event 1，Error code 3 / subcode 7 | Event 153，`Error occurred on GPUID: 100` | 22:24:04，`java.exe` / `nvoglv64.dll` / `0xc0000409` |
| 2026-06-21 22:31:18 |    610.62 | Event 1，Error code 3 / subcode 7 | Event 153，`Error occurred on GPUID: 100` | 22:31:19，`java.exe` / `nvoglv64.dll` / `0xc0000409` |
| 2026-07-15 10:10:39 |    610.74 | Event 1，Error code 3 / subcode 7 | Event 153，`Error occurred on GPUID: 100` | 10:10:41，`java.exe` / `nvoglv64.dll` / `0xc0000409` |

NVIDIA OpenGL Driver 的原始错误为：

```text
Unable to recover from a kernel exception. The application must close.

Error code: 3 (subcode 7)
(pid=... tid=... java.exe 64bit)
```

同一故障时间窗还关联到 Windows `LiveKernelEvent 141`，故障桶为：

```text
LKD_0x141_Tdr:C_IMAGE_nvlddmkm.sys_Blackwell
```

2026-07-15 事件对应的 watchdog dump 为：

```text
C:\WINDOWS\LiveKernelReports\WATCHDOG\WATCHDOG-20260715-1010.dmp
```

两次 2026-06-21 事件对应的 dump 文件名分别为 `WATCHDOG-20260621-2224.dmp` 和 `WATCHDOG-20260621-2231.dmp`。WER 可能延后登记或重复登记同一个 dump，因此应以 dump 文件名和同一时刻的 OpenGL、`nvlddmkm`、Application Error 事件关联，而不是把 WER 的登记时间当作实际崩溃时间。

驱动 610.62 的 `nvoglv64.dll` 文件版本为 `32.0.16.1062`，驱动 610.74 的版本为 `32.0.16.1074`。同型崩溃跨越了这两个驱动版本；这只能说明问题不局限于其中一个已测试版本，不能证明驱动本身是唯一根因。

### 2026-07-15 对照样本

后续自动捕获确认，不同光影包可以落入相同的故障桶：

| 光影包 | 维度 | 本次连续运行时间 | 已确认结果 |
|---|---|---:|---|
| MakeUp Ultra Fast 9.1f | 主世界 | 约 53 秒 | NVIDIA OpenGL Driver error 3 / subcode 7，`nvoglv64.dll` 偏移 `0x10cba3d`，`0xC0000409`，并关联 `LiveKernelEvent 141` |
| iterationT 3.2.0 | 主世界 | 约 15 分钟 | 与 MakeUp 样本相同的驱动错误、模块偏移、异常代码和 `LiveKernelEvent 141` |

因此，现有证据不支持把故障限定为 MakeUp，也不能把 iterationT 3.2 视为不会触发该故障的负对照。

同型崩溃还在以下 JVM 和流式上传配置下复现：`-Xmx8G`、`-XX:MaxDirectMemorySize=8G`，并通过 `-Dactinium.glsm.forceOrphanStreaming=true` 禁用 persistent streaming buffer 路径。对应会话中没有 Distant Horizons direct-buffer OOM。该结果排除了“2 GiB Java heap 上限”“2 GiB direct-memory 上限”“Distant Horizons direct-buffer OOM”或“persistent streaming buffer”分别作为该故障的必要条件，但不能证明这些变量在其他组合中完全无关。

GPU command breadcrumb 捕获到的最后暴露点并不固定：有样本停在 texture copy 调用中，后续样本则停在真实的 `SwapBuffers` 调用中。用户态 dump 和 breadcrumb 只能说明 CPU 线程在驱动报告故障时暴露于哪个调用。由于 OpenGL 命令由驱动异步提交和执行，不能把最后暴露的 API 直接认定为产生无效 GPU 工作或触发 TDR 的根因。

## 游戏日志的证据边界

现有事件显示，Windows 将 `java.exe` 的故障模块记录为 NVIDIA OpenGL 原生驱动 `nvoglv64.dll`，异常代码为 `0xC0000409`，同一时间窗还关联到 GPU watchdog 的 `LiveKernelEvent 141`。从现有结果只能确认该次退出未进入 Minecraft 常规的 Java 异常和 crash-report 流程，不能进一步断言 JVM 未生成 `hs_err_pid*.log` 的内部机制。

所以，`latest.log` 中没有 Java 堆栈并不代表正常退出，也不能单独用于判断最后一条游戏日志就是触发点。2026-07-15 日志的最后一条 Gibbed single-immediate 记录与驱动错误相隔约 11 秒，当前没有证据证明二者存在因果关系。需要将游戏日志时间线与 Windows 事件查看器和 WER 记录一起分析。

## 已知负对照

以下操作已经成功完成，未触发崩溃，因此目前不能将它们视为充分触发条件或已经确定的根因：

- 据用户报告，在末地传送门附近连续切换约 20 次光影，未崩溃。
- 另一轮测试的光影加载编号到 `Load #35`，其中 34 次记录了 `Total shaderpack load time`，`Load #17` 没有完成记录；该会话于 10:36:37 正常退出。
- Gibbed 的两种 immediate 渲染路径均执行过并正常退出。
- 加入 GPU command breadcrumb 和非阻塞 checkpoint 后，用户使用 iterationT 3.2 在主世界等待 2 小时未崩溃。该结果目前只能记录为单次“暂未复现”，不能据此宣称问题已经修复。

2026-06-21 的两次同型崩溃早于当前末地传送门渲染重写，也不支持将重写后的传送门实现直接认定为根因。

这些结果只能排除“执行一次就必然崩溃”或“达到已测试切换次数就必然崩溃”之类的简单条件；它们不能排除这些路径与其他状态组合后参与触发。

## 当前未知项

目前还没有稳定复现步骤，以下关系均未建立：

- 是否依赖特定维度，或特定的维度切换方向；
- 是否依赖某个光影包、预设或光影开启状态；
- 是否与游戏持续时间、显存压力、资源重载或场景复杂度有关；
- 是否需要特定模组组合或特定渲染路径共同出现；
- 是否只发生于特定 GPU、驱动分支、Java 版本或 Windows 版本；
- 最后一条游戏日志与原生驱动故障之间是否存在因果关系。

在取得可重复对照结果或 dump 分析证据前，以上项目都只能作为待测变量，不能写成根因。

## 建议复现矩阵

每次启动只改变一个变量，固定世界、视角、停留时间、Actinium commit、模组列表和 JVM 参数。每个场景至少重复三次，并记录未崩溃的负结果。

| 编号 | 光影状态 | 维度流程 | 目的 |
|---|---|---|---|
| A | 关闭 | 主世界停留 | 无光影基线 |
| B | 固定 MakeUp，不切换 | 主世界停留 | 单一光影基线 |
| C | 固定 MakeUp，不切换 | 末地停留 | 隔离末地场景 |
| D | 固定 MakeUp，不切换 | 主世界 → 末地 | 测试进入末地 |
| E | 固定 MakeUp，不切换 | 末地 → 主世界 | 测试离开末地 |
| F | 关闭 | 末地 → 主世界 | 区分维度切换与光影状态 |
| G | 固定另一款已验证光影，不切换 | 按首次可疑的维度流程 | 区分光影包相关性 |

若其中一个场景能够重复触发，再分别替换光影包、关闭非必要模组或更换驱动版本。不要在同一次运行中同时改变多个变量，否则无法从成功或失败结果中确定有效差异。

## 自动捕获工具

仓库提供 [`tools/capture-native-crash.ps1`](../tools/capture-native-crash.ps1)，用于把一次 `runClient` 会话的用户态进程、GPU 跟踪和系统事件放进同一个时间线。脚本通过 `Win32_Process` 的父子关系和 Java main class 识别真正的 Minecraft 客户端，排除 Gradle daemon；确认该客户端 PID 完全消失后，才读取 `latest.log` 和 `debug.log`。

先执行 dry run 检查路径和可用工具。该命令不会启动进程，也不会创建输出目录：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\capture-native-crash.ps1 -DryRun
```

默认启动 `crash` 模式的 GL flight recorder，同时收集日志、Windows 事件和新 dump 元数据：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\capture-native-crash.ps1
```

脚本默认使用 `-GradleUserHome D:/gradle`。它只在启动 Gradle 子进程的瞬间临时设置当前脚本进程的 `GRADLE_USER_HOME`，`Start-Process` 成功或失败后都会立即恢复原值；实际路径也会写入 manifest。需要覆盖时显式传入其他路径，例如 `-GradleUserHome 'D:/gradle'`。Dry run 只显示该路径，不会创建目录。

脚本通过 Gradle 的 `extra_jvm_args` 向客户端传递 `-Dactinium.glFlightRecorder=crash`。可用 `-FlightRecorderMode off` 关闭。附加客户端 JVM 参数可使用空格分隔的 `-ExtraClientJvmArgs '-Xmx6G -Dexample=value'`；最终生成的 Gradle 参数和客户端 JVM 参数会完整写入 `manifest.json`。

当前 flight recorder 记录 frame、render stage、pipeline、swap、GPU command 和非阻塞 checkpoint 等 CPU 侧语义 breadcrumb，不是可重放的逐条 OpenGL API trace。`GPU_CHECKPOINT` 会记录进入和返回 native fence/wait/delete 的生命周期，包括 `FENCE_CALL_BEGIN`、`FENCE_CALL_RETURNED`、`WAIT_CALL_BEGIN`、`WAIT_CALL_PENDING`、`WAIT_CALL_COMPLETED`、`WAIT_CALL_FAILED`、`DELETE_CALL_BEGIN` 和 `DELETE_CALL_RETURNED`；由此可以区分 native 调用没有返回、零超时轮询仍在等待、已经完成、驱动报告失败或 `glDeleteSync` 是否返回。非阻塞 checkpoint 不会等待 GPU 完成，`FENCE_CALL_RETURNED` 也只证明 `glFenceSync` 返回到 Java，不能证明此前提交的 GPU 工作执行成功；`DELETE_CALL_RETURNED` 同样只描述 `glDeleteSync` 的调用边界。GPU command 元数据只有在当前 OpenGL context 的缓存状态可信时才记录具体值；不可信时写入 `UNKNOWN=-1`，该值表示无法安全归属，而不是 OpenGL 对象 `-1` 或新的驱动错误。OpenGL 命令由驱动异步提交和执行，因此崩溃前最后一个 `BEGIN`、未配对事件、最后暴露的 API 或最后一条已写入记录只能界定候选区间，不能直接认定为触发 GPU/驱动故障的根因调用。需要逐调用参数和 replay 时，仍应在缩小复现场景后使用专门的 OpenGL API trace 工具。

同时启用 WPR GPU 内存循环跟踪并附加 ProcDump：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\capture-native-crash.ps1 `
  -EnableWpr `
  -EnableLwjglDebug `
  -ProcDumpPath 'D:\Tools\Sysinternals\procdump64.exe'
```

每次运行会在 `run/crash-captures/<时间戳>/` 建立独立会话，写入 `manifest.json`、捕获器日志、Gradle stdout/stderr、游戏日志、筛选后的 Windows 事件和新 dump 的路径/时间/大小元数据。客户端在 `run/client/diagnostics/` 生成的本次 `gl-flight-*.bin` 会在 Minecraft 进程完全退出后复制到会话的 `diagnostics/` 目录；脚本使用启动前后快照，只复制本次新增或变化的文件。启用 WPR 后还会写入 `gpu-trace.etl`；ProcDump 可用时，用户态 dump 位于该会话的 `procdump/` 目录。未安装 ProcDump 时脚本会明确记录并继续，不会修改 Windows Error Reporting 注册表设置。

复制完成后，脚本会使用本次 Minecraft 的 Java 可执行文件和 `build/classes/java/main` 中的已编译类自动运行 decoder，在同一目录生成同名 `.json`。decoder 失败不会删除原始 `.bin`，会在 manifest 中记录错误并把会话标记为 `capture-partial`，随后仍继续收集 Windows 事件和 dump。若需手工解析，必须先完成编译，并确认 Minecraft 客户端已经退出：

```powershell
.\gradlew.bat classes --no-daemon
java.exe -cp .\build\classes\java\main `
  com.dhj.actinium.debug.flight.GlFlightRecordingDecoder `
  .\run\crash-captures\<会话时间戳>\diagnostics\gl-flight-<时间戳>-<PID>.bin `
  > .\run\crash-captures\<会话时间戳>\diagnostics\gl-flight-decoded.json
```

捕获脚本运行 `runClient` 时会先触发所需编译；如果单独移动 `.bin` 到其他工作树，仍需在该工作树先执行上面的 `classes` 任务。

使用时需要注意：

- `runClient` 由复现问题的用户执行；分析人员应等待 Minecraft / `java.exe` 完全退出和捕获脚本显示会话结束后再读取本次日志与证据。
- `-EnableWpr` 通常需要从“以管理员身份运行”的 PowerShell 启动。读取 `C:\Windows\LiveKernelReports\WATCHDOG` 也可能需要管理员权限；权限不足只会使对应 dump 元数据缺失。
- KHR_debug callback 默认不开启；必须显式传入 `-EnableLwjglDebug`，脚本才会追加 `-Dactinium.lwjglDebug=true`。Debug context 和 callback 会增加检查工作并可能改变崩溃时序，因此复现记录中必须注明该开关状态。
- 脚本只会停止自己成功启动的 WPR 会话。若系统已有 WPR 记录，新的 WPR 启动通常会失败；不要为了运行脚本而取消其他人的跟踪会话。
- WPR 使用内存模式的 GPU profile，但停止时生成的 ETL 仍可能很大。ProcDump 的 `-ma` full dump 可能接近 Java 进程提交内存大小，单次会话应预留数 GB 磁盘空间，并及时清理无用会话。
- full dump 可能包含 Java 堆、账号信息、服务器地址、聊天内容和其他进程内数据；游戏日志、命令行、Windows 事件及 ETL 也可能暴露用户名和本地路径。公开上传前必须检查并按需要脱敏。不要把未经检查的 dump 直接附到公开 Issue。
- 捕获期间不要关闭脚本所在的 PowerShell 窗口。正常退出、原生崩溃或脚本异常都会进入清理流程，保存或取消本次 WPR，并终止本次附加后仍未退出的 ProcDump。
- 如果脚本在识别 Minecraft PID 前超时或无法读取 CIM，它会尝试清理本次启动且仍存活的 Gradle 进程树。清理严格核对根进程和各 PID 的创建时间，并按子进程到根进程的顺序执行；核对失败时会停止清理并写入 manifest，避免因 PID 复用终止无关进程。

## 报告此问题时请提供

请先等待 Minecraft / `java.exe` 进程完全退出，再读取和打包日志。Issue 中至少包含：

- Actinium commit，以及工作树是否有未提交修改；
- 崩溃的本地时间和时区；
- Windows 版本和 OS build；
- GPU 完整型号、NVIDIA 驱动版本、MUX / 独显直连状态；
- GPU 是否超频、降压或设置了非默认功耗限制；
- Java 发行版、完整版本、实际 `java.exe` 路径和完整 JVM 参数；
- Cleanroom Loader 版本、完整模组列表和配置；
- 光影包名称、精确版本、预设和修改过的光影选项；
- 崩溃前所在维度、维度切换顺序、传送门是否在视野内；
- 从启动到崩溃的操作时间线，包括资源重载和光影切换；
- Gradle 报告的十进制退出码，以及终端最后一段输出；
- 同次运行的 `run/client/logs/latest.log` 和 `run/client/logs/debug.log`；
- 事件查看器中同一时刻的 `NVIDIA OpenGL Driver` Event 1、`nvlddmkm` Event 153、Application Error Event 1000 和 Windows Error Reporting Event 1001；
- 如果存在，附上 `C:\WINDOWS\LiveKernelReports\WATCHDOG\WATCHDOG-*.dmp` 的文件名、时间和 WER Report ID。上传 dump 前应先确认其中不包含不希望公开的信息。

事件查看器中应保留完整的事件文本，不要只截取 `0xC0000409`。即使某次没有崩溃，也请报告测试场景、持续时间和重复次数；负结果对于缩小触发条件同样重要。

## 当前诊断限制

- `use_no_error_g_l_context` 当前没有源码读取点，切换该配置不会形成有效对照，不建议将其作为排查步骤。
- `actinium.lwjglDebug` 现已接入 debug context 和 KHR_debug callback，但 callback 只能报告驱动实际发出的诊断消息；没有消息不代表此前不存在异步 GL/GPU 错误，也不能替代 flight recorder 或 dump。
- 在没有 Java crash report 的情况下，应优先保存 Windows 事件和 watchdog dump；不要根据日志中最后出现的模组名称直接归因。
