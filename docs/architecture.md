# Actinium 架构说明

最后更新：2026-07-10。

## 运行时组成

Actinium 是一个客户端模组和 coremod。`com.dhj.actinium.Actinium` 处理 Forge 生命周期，
`com.dhj.actinium.mixins.MixinEarly` 注册启动转换器和基础 Mixin，`MixinLate` 根据已加载模组
增加 Distant Horizons 与 Gibbed 兼容 Mixin。

渲染栈由四部分组成：

- `src/main/java/com/dhj/actinium/`：生命周期、配置、兼容层和 Actinium 自有渲染代码。
- `celeritas-common/`：区块构建、上传、裁剪和绘制。
- `glsm/`：OpenGL 状态跟踪、重定向、固定管线模拟和调试设施。
- `shader/`：Iris 风格的光影包解析、GLSL 变换、render targets、uniform 和渲染管线。

`GTNHLib/` 提供内存、buffer、后处理和旧版渲染工具。三个内嵌库是 Gradle 子项目，
最终 class 仍由根项目合并进同一个 Actinium Jar。

## 主要执行链路

1. `MixinEarly` 在 Minecraft 类加载前安装 redirector 和 Mixin 配置。
2. Forge construction 阶段建立 Celeritas、GTNHLib、GLSM 与 Actinium 配置之间的 bridge。
3. Iris 初始化光影包目录、配置和 shader provider。
4. `EntityRendererIrisMixin` 在世界渲染中驱动 pipeline：开始世界、prepare、shadow、
   半透明阶段和最终合成。
5. `CeleritasTerrainPipeline` 为区块 pass 提供变换后的 shader program；无光影时使用
   `FixedFunctionWorldRenderingPipeline`。
6. pipeline 在维度切换或光影重载时销毁 GL 资源并重建，最后恢复 Minecraft 主 framebuffer。

## 构建模型

`shader/src/main/java` 是根项目的附加源码目录。`GTNHLib`、`glsm` 和
`celeritas-common` 独立编译，根项目通过 `mergeEmbeddedLibraryClasses` 合并输出。
`jar` 生成开发命名空间产物，Unimined 的 `remapJar` 生成可安装的 SRG 产物。

开发约束：

- 子项目之间的依赖方向是 `celeritas-common -> glsm -> GTNHLib`。
- 子项目不应引用 `com.dhj.actinium` 实现类；跨边界行为通过 bridge、provider 或小接口注入。
- 新增 render pass 必须明确 framebuffer、program、texture unit、viewport 和混合/深度状态归属。
- 新增 Mixin 必须加入 `MixinConfigurationTest` 覆盖的配置文件。
- 发布前运行 `build`；`check` 会验证自动化测试及 remap Jar 结构。

## 兼容层

- Distant Horizons：API event、framebuffer、depth texture、LOD shader 和条件 Mixin。
- Fluidlogged API：world slice 中的 fluid state 快照与渲染。
- Gibbed：模型渲染快速路径及条件 Mixin。
- ModernUI 和若干 HUD/地图模组：GUI scale 或编译期兼容接口。

兼容代码应由模组存在性检查保护。引用外部类的 Mixin 必须放在 late/conditional 配置中，
避免未安装对应模组时触发类加载。

## 验证边界

普通单元测试适合覆盖属性解析、GLSL 变换、ID 映射、fallback 和打包契约。
OpenGL 状态恢复、画面正确性、驱动差异和性能仍需运行客户端验证，并记录到兼容矩阵。

五月的移植方案和旧管线状态已移入 `docs/archive/`，仅用于追溯历史决策。
