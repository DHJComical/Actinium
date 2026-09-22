# Component Model Hider 兼容（component_model_hider 1.0）

兼容状态：**代码完成，实机验证待做**。
最后更新：2026-09-22。分支：`fix/mmce-component-model-hider`。

## 背景

`MMCE-ComponentModelHider`（modid `component_model_hider`，CurseForge 940949:4885858，1.12.2
客户端专用，需要 MixinBooter）是从 Multiblocked 抽出的"隐藏方块模型"API
（源码：`NovaEngineering-Source/MMCE-ComponentModelHider`，类包名仍是
`com.cleanroommc.multiblocked.*`）。Modular Machinery CE（`hellfirepvp.modularmachinery`）与
NovaEngineering-Core 的 `BlockModelHider` 在检测到该模组时调用：

- `MultiblockWorldSavedData.addDisableModel(BlockPos controller, Collection<BlockPos> poses)`
- `MultiblockWorldSavedData.removeDisableModel(BlockPos controller)`

已被隐藏位置的方块模型不再渲染，多方块结构只显示结构自身的自定义模型/TESR。
注意 ModularMachinery 的判定顺序是 `Mods.MBD`（Multiblocked 本尊）优先，其后才是
`Mods.COMPONENT_MODEL_HIDER`，因此**同时安装 Multiblocked 时本兼容不会被走到**。

## 模组机制（反编译结论）

`com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData`：

- `modelDisabled`（`public static final Set<BlockPos>`）：全部被隐藏位置；
  `multiDisabled`（`Map<BlockPos, Collection<BlockPos>>`）：按控制器记录。
- `isBuildingChunk`（`public static final ThreadLocal<Boolean>`，初值 `false`）。
- `isModelDisabled(BlockPos)`：**仅当 `isBuildingChunk` 为 true** 时返回
  `modelDisabled.contains(pos)`，否则恒为 `false`。

三个图形注入点：

| 注入 | 目标 | 作用 |
| --- | --- | --- |
| `RenderChunkMixin`（`@Redirect`） | `RenderChunk.rebuildChunk` → `BlockRendererDispatcher.renderBlock` | 隐藏位置直接 `return false`（不产出几何），并在调用前后置 `isBuildingChunk` true/false |
| `BlockModelRendererMixin` / `ForgeBlockModelRendererMixin`（`@Redirect`） | `renderModelFlat` / `renderModelSmooth` / `ForgeBlockModelRenderer.render` → `IBlockState.shouldSideBeRendered` | 邻格被隐藏时返回 `true`，让相邻方块朝向它的面仍然绘制 |
| `TileEntityRendererDispatcherMixin`（`@Inject` + cancellable） | `TileEntityRendererDispatcher.getRenderer` | 隐藏位置的 TESR 返回 null（**不经 `isBuildingChunk` 门控**，直接读 `modelDisabled`） |

`isBuildingChunk` 是隐藏链的开关：上表第 2 行的 `BlockModelRenderer` / `ForgeBlockModelRenderer`
redirect（以及 CCL 钩子）都以 `isModelDisabled` 为唯一判据；第 3 行的 TESR 注入不经门控。

反编译另有一处**在 1.12.2 无效**的注入：`ASMTransformer` + `BlockVisitor` 试图在
`net.minecraft.block.Block.doesSideBlockRendering` 方法开头插入 `BlockHooks` 钩子，但 1.12.2
的 `Block` 没有该方法（`BlockVisitor` 找不到方法名即静默跳过）。1.12.2 的面剔除在
`Block#shouldSideBeRendered`，其结尾是
`return !blockAccess.getBlockState(pos.offset(side)).isOpaqueCube();`。因此邻面规则**只**
由 `BlockModelRenderer`/`ForgeBlockModelRenderer` 的 redirect 承担；同理
`CCLBlockRendererDispatcherVisitor` 只对 CodeChickenLib 的渲染通道有效。

原版 1.12.2 `RenderChunk.rebuildChunk` 中 `VisGraph.setOpaqueCube(pos)` 位于被 redirect 的
`renderBlock` 调用**之外**，所以被隐藏的方块在原版下仍然参与 section 可见性——本兼容刻意保留
这一语义（见下）。

## 冲突分析

Actinium 的区块网格由 `ChunkBuilderMeshingTask` → `VintageBlockRenderer` 生成，
`RenderChunk.rebuildChunk` 在仓库内没有任何调用点，于是：

1. `isBuildingChunk` 永远是 false → `isModelDisabled` 恒为 false →
   `BlockModelRenderer`/`ForgeBlockModelRenderer` 的邻面 redirect 与 CCL 钩子全部空转；
2. `RenderChunkMixin` 的 skip 永不执行 → 隐藏方块照旧入网格；
3. 即使补上第 1 条也不够：快速路径 `VintageBlockRenderer#renderBlock` 自己调用
   `state.shouldSideBeRendered(...)`，不经过 `BlockModelRenderer`，所以隐藏方块仍会剔除相邻
   方块朝向它的面，留下可看穿的洞。

唯一幸存的是 TESR 那一半：`TileEntityRendererDispatcherMixin` 直接读 `modelDisabled`、不经
`isBuildingChunk` 门控，而 `ChunkBuilderMeshingTask` 收集 TESR 时正是调用
`TileEntityRendererDispatcher.instance.getRenderer(...)`。

## 兼容方案

新增 `com.dhj.actinium.compat.componentmodelhider`：

- `ComponentModelHiderCompat`：调用面，**不含任何外部模组引用**。
  `IS_LOADED = Mods.COMPONENT_MODEL_HIDER`，对外只有
  `isHidden(BlockPos)` / `isNeighbourHidden(BlockPos, EnumFacing)` / `beginBuild()` / `endBuild()`。
- `ComponentModelHiderBridge`：包私有，**唯一**引用 `MultiblockWorldSavedData` 的类，仅当
  `IS_LOADED` 为 true 时才会被加载；隐藏判据委托模组自己的 `isModelDisabled` 而不是直接读
  `modelDisabled` 字段，门控语义仍归模组所有。

接入点（等价物对照）：

| 原版钩子 | Actinium 等价实现 | 位置 |
| --- | --- | --- |
| `RenderChunkMixin` 的 `isBuildingChunk` 门控 | `beginBuild()` / `endBuild()` 包住整个 section 网格构建（`finally` 收口；线程本地，只影响该 worker 线程） | `ChunkBuilderMeshingTask#execute` |
| `RenderChunkMixin` 的 `renderBlock` skip | 隐藏位置跳过该方块**全部 layer** 的模型渲染（快速路径与 vanilla dispatcher 回退路径都跳过） | `ChunkBuilderMeshingTask` 方块循环 |
| `BlockModelRenderer` 系列的邻面 redirect（快速路径走不到） | `VintageBlockRenderer#renderBlock` 的面剔除条件：vanilla 判据为 false 且邻格隐藏时仍然绘制该面（邻格偏移只在模组在场时计算）。**该 `shouldSideBeRendered` 调用刻意留在 `renderBlock` 内而不抽成方法**——`VintageBlockRenderer` 是第三方 Mixin 绑定面，抽走会把指令移出 addon `@Redirect` 的可达范围；`VintageBlockRendererBindingContractTest#renderBlockKeepsTheFaceCullingCallSite` 锁定该不变量 | `VintageBlockRenderer#renderBlock` |
| CCL 钩子 | 无需额外代码：`beginBuild()` 置位 `isBuildingChunk` 后其原有 `CCLHooks` 判定自动生效 | — |

刻意**不**改的两处：

- `occluder.markRenderable` / `markOpaque` 仍对隐藏方块执行——原版 `VisGraph.setOpaqueCube`
  就在 redirect 之外，隐藏方块继续参与 section 可见性；改成"不参与"会让结构后方地形被错误剔除。
  Actinium 侧同样自洽：`SectionVisibilityBuilder` 的遮挡盒只由 `markOpaque` 写入的
  `blocks` 位集派生（`OccluderBoxes.from`），`markRenderable` 只影响可渲染包围盒，而后者本就被
  **每一个非空气方块**（含无形的 barrier 之类不产出几何的方块）写入，因此隐藏方块不构成新的
  假遮挡盒。
- 破坏/损伤覆盖层（`renderBlockDamage`）与选择框不受影响：`isBuildingChunk` 只在网格构建期间
  置位，与原版 MMCE 的窗口一致。
- 流体回退渲染（`FluidloggedCompat.renderFluidState`）仍对隐藏位置执行，即"既是隐藏位置、又
  fluidlogged"的方块其流体照旧绘制。这也是原版语义，且已核实注入点：Fluidlogged 的
  `PluginRenderChunk`（ASM）目标方法是 `RenderChunk.rebuildChunk`（`func_178581_b`），注入的是它
  自己的 `Hooks.renderFluidState(IBlockState, boolean[], ChunkCompileTaskGenerator,
  CompiledChunk, IBlockAccess, BlockPos, BlockPos)` 调用——**不是** MMCE 所 redirect 的那个
  `BlockRendererDispatcher.renderBlock` 调用点，所以原版+MMCE 下隐藏位置的 fluidlogged 流体同样
  会绘制。该调用位于 `if (!hidden)` 之外是刻意的，其邻面判定仍受 `isBuildingChunk` 窗口影响。

取消隐藏的刷新路径无需额外处理：`removeDisableModel` → `updateRenderChunk` →
`World.markBlockRangeForRenderUpdate` → `RenderGlobal.markBlocksForUpdate` 已被 Actinium
`@Overwrite` 为 `scheduleRebuildForBlockArea`。

## 行为差异（相对原版）

- 原版按"每个方块"置位/复位 `isBuildingChunk`，Actinium 按"整个 section 构建"置位。置位期间
  只有模组的剔除判据会读到它，且只在网格 worker 线程上，效果等价（流体回退渲染也在窗口内，
  其邻面判定同样生效）。
- 兼容层不依赖 `BlockVisitor` 的 `Block.doesSideBlockRendering` 钩子（1.12.2 无该目标方法）。
- 隐藏方块不再执行 `ForgeHooksClient.setRenderLayer(layer)` 与 `block.canRenderInLayer(blockState,
  layer)`（它们都在 `if (!hidden)` 内）。原版下这两者位于 `BlockRendererDispatcher.renderBlock`
  内部，而整个调用被 redirect 掉，因此同样不会执行——语义一致；且真正的渲染调用总会先自行设置
  当前 render layer，故无残留状态问题。

## 残余限制

- `modelDisabled` 是模组自己的普通 `HashSet`：新增/移除发生在客户端线程，而 Actinium 在区块
  worker 线程读取，构成并发读写。这一暴露与**原版 MMCE** 完全相同（原版 `RenderChunk` 的网格
  构建同样在 `ChunkRenderDispatcher` worker 线程上），兼容层不额外加重也不修复它；若日后实测
  出现偶发漏隐藏/错隐藏，应先怀疑此处而非兼容层。
- 仅覆盖 `component_model_hider`。同时安装 Multiblocked 时 MMCE 走 MBD 路径（`Mods.MBD` 优先），
  本兼容不生效；Multiblocked 自身在 Actinium 下的渲染适配未在本次范围内。
- 兼容层自身在自动化与 dev 中都不会执行（`modCompileOnly` → `Mods.COMPONENT_MODEL_HIDER` 在 dev
  为 false），只有装了 hider 的实机能覆盖；这一点无法通过改依赖配置简单解决：该模组是 coremod，
  其 `ASMTransformer` 依赖 `ObfMapping` 的静态初始化，而 `ObfMapping` 在非混淆环境下会走
  `MCPRemapper.getConfFiles()` 读取 `net.minecraftforge.gradle.GradleStart.*` 映射系统属性——
  Unimined dev 环境不提供这些属性（同类 dev-only 映射问题见
  [docs/compat/chunkanimator.md](chunkanimator.md)）。把它放进 dev 运行时需要额外供给映射，且其
  `CCLBlockRendererDispatcherVisitor` 的 `TargetClassVisitor` 找不到方法即抛
  `RuntimeException`，与 dev 环境的 CCL 版本存在额外风险，故依赖配置刻意保持 compile-only。

## 文件清单

- `src/main/java/com/dhj/actinium/compat/componentmodelhider/ComponentModelHiderCompat.java`（新增）
- `src/main/java/com/dhj/actinium/compat/componentmodelhider/ComponentModelHiderBridge.java`（新增）
- `src/main/java/com/dhj/actinium/render/terrain/compile/task/ChunkBuilderMeshingTask.java`
- `src/main/java/com/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRenderer.java`
- `src/test/java/com/dhj/actinium/render/terrain/compile/pipeline/VintageBlockRendererBindingContractTest.java`
  （新增 `renderBlockKeepsTheFaceCullingCallSite`，锁定面剔除调用点不得移出 `renderBlock`）
- `GTNHLib/src/main/java/com/gtnewhorizon/gtnhlib/compat/Mods.java`（新增 `COMPONENT_MODEL_HIDER`）
- `gradle/scripts/dependencies.gradle`（`modCompileOnly curse.maven:component-model-hider-940949:4885858`）

## 验证记录

- [x] `./gradlew compileJava`、`compileTestJava`、`check` 全部通过（2026-09-22，新依赖由
  CurseMaven 正常解析）。

  未新增兼容层自身的单元测试：本兼容是"门控 + 转发"的薄适配层，其行为只有在真实区块构建中才可
  观察；而 `Mods` 的静态初始化依赖 Cleanroom 启动期扫描（`CleanroomModDiscoverer.instance()`
  在 headless JVM 中为 null 并 NPE，已实测），仓库内所有 compat 类在单元测试中都无法加载，故按
  既有惯例交由实机验证。唯一新增的自动化覆盖是
  `VintageBlockRendererBindingContractTest#renderBlockKeepsTheFaceCullingCallSite`（ASM 字节码
  契约，非源码文本断言）。

- [ ] 实机验证待做。

### 实机验证场景（待用户确认）

环境：Cleanroom + Actinium + ModularMachinery CE + `MMCE-ComponentModelHider-1.0.jar`
（**不要**同时装 Multiblocked，否则 MMCE 优先走 MBD 路径）。建议准备一台已成型的多方块机器。

1. 结构与模型：机器成型后结构方块模型消失，只剩控制器/自定义模型；与原版（非 Actinium）
   表现一致。
2. 无破洞：站到结构内部/侧面观察，隐藏位置不应"看穿"到另一侧天空或未渲染地形——即相邻方块
   朝向隐藏位置的面仍然绘制。
3. 两条渲染路径各验一次：`useFastBlockRenderer` 开（默认，走 `VintageBlockRenderer`）与关
   （走 vanilla dispatcher 回退），行为应一致。
4. 取消隐藏：拆掉机器/使其结构失效，方块模型必须恢复显示（`removeDisableModel` 刷新路径）。
5. 光影：无光影与启用光影（如 Complementary Reimagined）各验一次，隐藏与邻面表现一致。
6. 无副作用：破坏/损伤覆盖层（挖掘裂纹）照常显示；未装 hider 的实例完全无感，不得出现
   `NoClassDefFoundError` / `ClassNotFoundException`（`ComponentModelHiderBridge` 不应被加载）。
7. 边界确认（预期**流体仍显示**）：若隐藏位置同时被 fluidlogged（例如水logged 的机壳），其流体
   照旧绘制——这与原版+MMCE 一致（见"刻意不改"一节），不是回归。
8. 若安装了 celeritasleafculling 一类的 addon：其 quad 级剔除（`renderQuadList` redirect）与
   `currentState`/`currentBlockAccess` 绑定必须照常生效。
