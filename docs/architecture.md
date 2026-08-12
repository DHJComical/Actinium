# Actinium 架构说明

最后更新：2026-07-31。

## 运行时组成

Actinium 是一个客户端模组和 coremod。`com.dhj.actinium.Actinium` 处理 Forge 生命周期，
`com.dhj.actinium.mixins.MixinEarly` 注册启动转换器和基础 Mixin，`MixinLate` 根据已加载模组
增加 Distant Horizons 与 Gibbed 兼容 Mixin。

渲染栈由五部分组成：

- `src/main/java/com/dhj/actinium/`：生命周期、配置、兼容层和 Actinium 自有渲染代码。
- `celeritas-common/`：区块构建、上传、裁剪和绘制。
- `glsm/`：OpenGL 状态跟踪、重定向、固定管线模拟和调试设施。
- `shader/`：Iris 风格的光影包解析、GLSL 变换、render targets、uniform 和渲染管线。
- `GTNHLib/`：内存、buffer、后处理和旧版渲染工具。

四个内嵌库（`shader`/`celeritas-common`/`glsm`/`GTNHLib`）是 Gradle 子项目，
最终 class 由根项目合并进同一个 Actinium Jar。

## 主要执行链路

1. `MixinEarly` 在 Minecraft 类加载前安装 redirector 和 Mixin 配置。
2. Forge construction 阶段建立 Celeritas、GTNHLib、GLSM 与 Actinium 配置之间的 bridge。
3. Iris 初始化光影包目录、配置和 shader provider。
4. `EntityRendererIrisMixin` 在世界渲染中驱动 pipeline：开始世界、prepare、shadow、
   半透明阶段和最终合成。
5. `CeleritasTerrainPipeline` 为区块 pass 提供变换后的 shader program；无光影时使用
   `FixedFunctionWorldRenderingPipeline`。
6. pipeline 在维度切换或光影重载时销毁 GL 资源并重建，最后恢复 Minecraft 主 framebuffer。
7. `AdaptiveShadowBoundsTransformer` 在可识别的 PCF helper 入口加入 shadow-map bounds
   early return；仅在 GLSM 性能调试开启、且 SSBO/GLSL 4.30 可用时，
   `DeferredWorldRenderingPipeline` 才分配独立 SSBO 并注入运行期 `atomicAdd` 统计。
   统计通过 `GLSM perf:` 输出并在每个报告窗口清零，不能作为无调试开销的 FPS 基线。

## 构建模型

`shader`、`GTNHLib`、`glsm` 和 `celeritas-common` 是四个独立 Gradle 子项目，
根项目通过 `mergeEmbeddedLibraryClasses` 合并输出。
`jar` 生成开发命名空间产物，Unimined 的 `remapJar` 生成可安装的 SRG 产物。

开发约束：

- 子项目之间的依赖方向是 `shader -> celeritas-common -> glsm -> GTNHLib`（shader 另依赖 glsm/GTNHLib）。
- 子项目不应引用 `com.dhj.actinium` 实现类；跨边界行为通过 bridge、provider 或小接口注入
  （桥的登记与新增约定见 `docs/bridges.md`；`DependencyDirectionTest` 在字节码层兜底该约束）。
- 新增 render pass 必须明确 framebuffer、program、texture unit、viewport 和混合/深度状态归属。
- 新增 Mixin 必须加入 `MixinConfigurationTest` 覆盖的配置文件。
- 发布前运行 `build`；`check` 会验证自动化测试及 remap Jar 结构。

## 模块所有权

多人协作下，每个子项目有明确的默认 owner 与变更流程。当前由维护者认领，
正式分工后在此登记：

| 模块 | 职责边界 | 变更流程 |
| --- | --- | --- |
| 根项目 `src/main` | 生命周期、配置、兼容层、mixin 注册 | 兼容改动经 `docs/compat/*.md` 留痕；mixin 遵循「Mixin 组织约定」 |
| `shader/` | 光影管线（Iris 移植）：解析、GLSL 变换、render targets、uniform、pipeline | 上游同步走 `docs/upstream-maintenance.md`；对根项目零引用（架构测试强制） |
| `celeritas-common/` | 区块构建、上传、裁剪、绘制 | 渲染算法改动需 dev 回归 + 兼容矩阵更新 |
| `glsm/` | GL 状态跟踪、重定向、FFP 模拟 | 状态语义改动影响面大，需 DE/光影双场景回归；第三方包边界见 upstream-maintenance.md |
| `GTNHLib/` | 渲染原语库（tessellator/VBO/VAO/后处理） | API 改动需同步全部调用方；新增原语须先评估与现有实现的重叠 |

通用规则：改接口需 cross-review；新增桥遵循 `docs/bridges.md` 五条约定；
PR 原则上只动一个子项目 + 必要桥接。

Mixin 组织约定：

- **mixin 类只做注入**，业务逻辑放在 mixin 包之外的实现类（`compat/`、功能包或
  工具类），mixin 类保持薄。
- **注入接口与注入类分离**：访问器/调用器（Accessor/Invoker）按
  `core.terrain.AccessorEntityRenderer` 模式组织；需要跨类暴露的注入能力，接口
  定义放对应 `mixinterface/` 子包，实现留在 mixin 类。
- **mixin 包隔离规则**：mixin 包内的辅助类（含内部类/静态嵌套类）不得被 mixin
  目标类或其调用方直接引用——会触发 `IllegalClassLoadError`。该检查**仅在非
  dev 环境强制，dev 验证无法覆盖**（2026-08-12 生产崩溃实例：
  `MixinGlStateTracker$Saved`）。辅助实现一律放 mixin 包外。

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
