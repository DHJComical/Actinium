# SDL GPU 后端的 Cleanroom GLFW 兼容层（撤除清单）

> 状态：进行中（2026-08-15）。本文档记录 Actinium 为在 Cleanroom Loader 上运行
> Angelica 的 `sdl-gpu` 后端而添加的 **GLFW 窗口兼容代码**。这些代码是为绕过
> Cleanroom 的 lwjglxx 窗口创建而写的**临时适配**，**未必符合上游 Angelica 的
> 代码规范**（大量反射、事件队列节流、handleInput 接管等）。当 Cleanroom 把窗口
> 换成原生 SDL 窗口（不再强制 GLFW+GL context）后，应**撤掉或大幅简化**本文档
> 列出的兼容层。

## 背景

- Angelica 的 `sdl-gpu` 在 GTNH 运行于 lwjgl3ify：`DisplayEvents` 可以抑制 GL
  context 创建，SDL 后端直接认领 GLFW 窗口（`SDL_ClaimWindowForGPUDevice`）。
- Cleanroom Loader 的 lwjglxx **总是**创建「GLFW 窗口 + GL context」。Vulkan 无法
  认领带 GL context 的窗口（`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR`），所以 Actinium
  必须**绕过 lwjglxx 的窗口创建**：自己用 `glfwCreateWindow(GLFW_NO_API)` 建窗口，
  再让 SDL 认领。
- 绕过 lwjglxx 窗口创建 = 绕过了 lwjglxx 在 `Display.create()` 里做的**所有窗口
  相关初始化**（GLFW 回调注册、Mouse/Keyboard 创建、Display 静态字段）→ 需要
  手动补齐，这就是本文档的「兼容层」。

## 兼容层清单

| 文件 | 改动 | 原因 / 备注 |
|---|---|---|
| `SDLGPUGate.createSDLGPUDisplay` | 用 `Minecraft.displayWidth/Height` 建 GLFW_NO_API 窗口（**不用 vidMode**），`adoptWindow` 后 `claimPlatformWindow` | vidMode 尺寸会与 Minecraft 的 framebuffer/视口错位，画面只占左上角。窗口尺寸必须匹配 `displayWidth` |
| `SDLGPUDisplayBridge.adoptWindow` | VarHandle 镜像 lwjglxx `Display` 静态字段（handle/created/width/height/fbSize/title/resizable） | lwjglxx 的 `Display.*` 查询在绕过创建后全部失效 |
| `SDLGPUDisplayBridge.installLwjglxWindowCallbacks` | **自建 GLFW 回调**（cursor/button/scroll/key/char）转发到 lwjglxx 公开输入 API（`Mouse.addMoveEvent` 等），反射写入 `Display$Window` 静态字段后调 `setCallbacks()` | lwjglxx 的回调对象在 `Display.create()` 里创建，被绕过后字段为 null；必须 setAccessible 才能进入 package-private 内部类。回调对象要强引用（GLFW 经 native 指针回调） |
| `SDLGPUDisplayBridge.initializeLwjglxInput` | 补调 `Mouse.create()` / `Keyboard.create()` | 两者在 `Display.create()` 里初始化 |
| `SDLGPUDisplayBridge.flushPendingMove` | cursorPos 回调只记录坐标，每帧由 SDL pump 合并为 1 个 move 事件 | lwjglxx 输入队列只有 32 个槽，Minecraft 每 tick 才 poll 一次；move 事件流会挤掉按钮事件 → 点击丢失 |
| `MixinMinecraft.actinium$sdlUpdateDisplay` | `updateDisplay` HEAD 替换：`glfwPollEvents` + `SDL_PumpEvents` + `flushPendingMove` + `Mouse.poll()` + `Keyboard.poll()` + cancel | lwjglxx 在 `Display.update()` 里 poll 输入；SDL 路径替换了 updateDisplay，必须补 poll，否则输入队列永不排空 |
| `MixinMinecraft` 帧钩子（`beginRenderFrame`/`endStreamingFrame`/`actinium$finishDisplaySwap`） | `onFrameBegin`/`onFrameEnd` 挂到 `runGameLoop` 的 `updateDisplay` 调用前后 | 对齐 Angelica 的 `MixinMinecraft_FrameHook`；present 由帧钩子驱动 |
| `MixinGuiScreenSDLInput.sdlHandleInput` | 接管 `GuiScreen.handleInput`：直接 `while(Mouse.next())` → 按钮按下事件经 `@Invoker` 直调 `mouseClicked`；move/键盘事件走原 `handleMouseInput`/`handleKeyboardInput` | vanilla 的循环里 Forge `GuiScreenEvent.MouseClickedEvent` 会吞掉点击（SDL 事件时序下），所以绕过 Forge 事件门 |
| `LWJGLServiceProvider` / `SDLGPULWJGLService` | Embeddium 的 `LWJGLService` 桥：所有 GL 调用转发 `BackendManager.RENDER_BACKEND` / `GLStateManager` | 不接这个桥，Embeddium 的 GlShader 会拿到 SENTINEL 指针崩溃 |
| `glsm/.../PersistentStreamingBuffer`（writeAt/writeAtStart） | memCopy 写 mapped buffer 后调 `RENDER_BACKEND.onPersistentBufferWrite` | SDL 后端的 `mapBufferRange(persistent)` 返回 **CPU staging**（非真实 GPU 映射）；不通知后端，GPU 永远看不到顶点数据 → 所有 persistent 路径的绘制黑屏。OpenGL 后端该调用是空实现，无副作用 |
| `Device.claimWindow` | 加 `SDL_ShowWindow` | SDL3 Vulkan 的 swapchain acquire 在窗口未 SHOWN 时无限阻塞 |
| `FrameManager.presentBlit` / `blitNamedFramebuffer` | `drawFramebuffer==0` 时目标用 `finalTarget` | SDL 无默认 framebuffer 语义 |
| `build.gradle` | `runClientSdl` 任务：继承 `runClient` 的完整启动配置（含 `${mcp_to_srg}` 属性表）+ `-Dangelica.sdlgpu.enable=true` | Unimined 只对固定名 `client`/`server` 应用完整 run 模板 |

## 已知的「不规范」点（撤除时重点核对）

1. **反射密集**：`SDLGPUDisplayBridge` 用 `VarHandle` / `Method.setAccessible` 操作
   lwjglxx 内部（`Display$Window` 是 package-private 内部类，只能反射）。
2. **事件节流依赖 lwjglxx 内部容量**（32 槽）与「每 tick poll 一次」的时序；
   CRL 原生 SDL 窗口后输入路径完全不同，节流应删除。
3. **`handleInput` 接管绕过 Forge 事件门**：`MouseClickedEvent.Pre/Post` 不再发出，
   依赖 Forge 事件的 mod 在 SDL 路径上收不到点击事件。
4. **hover 无效**（按钮 hover 变色不生效）——用户已知悉、暂不处理；撤除时若
   CRL 提供标准输入，应验证 hover 是否恢复。
5. **`MixinDisplayCompat` 的 target `org.lwjgl.opengl.Display` 不存在**（Cleanroom
   用 lwjglxx，没有 lwjglx 的 Display 类）→ CleanMix 启动警告「target not found」，
   无害但应清理。
6. **`glfwGetWindowUserPointer(0)` 等 GLFW 窗口句柄相关代码**（曾用于诊断）已清理。
7. 依赖 `Display.getPixelScaleFactor()`、`Display.getHeight()` 等 lwjglxx 语义做
   坐标换算，与原生 SDL 窗口的坐标语义未必一致。

## CRL 换 SDL 窗口后的撤除计划（建议顺序）

1. **删除** `SDLGPUDisplayBridge` 的 Display 镜像 + 自建回调 + `flushPendingMove` +
   `initializeLwjglxInput`（若 CRL 原生 SDL 窗口自带输入/Display 查询）。
2. **简化** `MixinMinecraft.actinium$sdlUpdateDisplay`：事件处理交给 CRL/SDL，
   只保留 present 相关（或整个移除，帧钩子保留）。
3. **删除** `MixinGuiScreenSDLInput`（若 CRL 输入不触发 Forge 事件吞点击问题）；
   若仍触发，则把 `mouseClicked` 直调下沉到 CRL 侧。
4. **保留**（架构性，与 GLFW 无关）：
   - 帧钩子（onFrameBegin/onFrameEnd）与 present 链路
   - `PersistentStreamingBuffer` 的 `onPersistentBufferWrite` 通知（SDL 后端 staging
     语义不变）
   - 窗口尺寸匹配原则（SDL 窗口尺寸 = `displayWidth/Height`）
   - `Device.claimWindow` 的 `SDL_ShowWindow`、`blitNamedFramebuffer` finalTarget 修复
5. **清理** `MixinDisplayCompat`（target 不存在）。
6. 回归验证：主菜单显示、鼠标/键盘、点击切换界面、窗口 resize、进入世界渲染。

## 相关提交

- `fc5f88d` fix(sdl-gpu): get the SDL GPU backend through game startup
- `51117b2` fix(sdl-gpu): rename GLSL reserved words in the SDL shader pipeline
- `326a0ad` feat(sdl-gpu): embed the SDL GPU backend into the mod jar
- `1868a52` / `382994f` build: runClientSdl 启动配置
- `52be235` fix(sdl-gpu): present menu frame via persistent buffer sync and window size match
- `e69c638` fix(sdl-gpu): restore mouse and keyboard input on the SDL window
