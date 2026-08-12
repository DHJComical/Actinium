# Actinium 项目结构优化计划

最后更新：2026-08-12
状态：**草案，待审阅**
约束前提：**项目预计多人维护**——所有结构决策以「模块边界 = 协作契约」为准，
不采用削弱编译期隔离的方案。

## 1. 现状快照

### 1.1 模块规模

| 模块 | 规模 | 定位 | 结构状态 |
|------|------|------|----------|
| `shader/`（根项目附加源码目录） | 48k 行 / 448 文件 | Iris 风格光影管线 | **最大模块，混编在根项目编译单元内，无依赖约束** |
| 根项目 `src/main` | 30k 行 / 382 文件（含 102 个 mixin、15 个顶层包） | 生命周期、配置、兼容层、自有渲染 | mixin 过半，兼容层散落 |
| `glsm/` | 28k 行 / 145 文件 | GL 状态跟踪、重定向、固定管线模拟 | 内含第三方包（`com.mitchej123.glsm`、`net.minecraftforge.eventbus` 重写） |
| `celeritas-common/` | 21k 行 / 278 文件 | 区块构建、上传、裁剪、绘制 | 依赖方向正确，职责清晰 |
| `GTNHLib/` | 15k 行 / 100 文件 | 渲染原语库 | 最底层，稳定 |

### 1.2 现有约束（保持不动）

- 依赖方向固定：`celeritas-common -> glsm -> GTNHLib`
- 子项目不得引用 `com.dhj.actinium` 实现类；跨边界经 bridge、provider 或小接口注入
- 新增 Mixin 必须加入 `MixinConfigurationTest` 覆盖的配置文件
- 兼容代码由模组存在性检查保护（late/conditional 配置）

多人维护含义：以上约束是**编译期物理强制**的（子项目 classpath 不含根项目），
对新人最有效；后续任何结构调整不得把强制约束降级为文档/纪律。

## 2. 结构压力点（有代码证据）

1. **GLSM 的 interface 语义不完整**（本次 DE 兼容修复的直接教训）
   - GLSM 重定向了原版 `GlStateManager`，但「原版字段缓存已冻结」这一不变量未作为 interface 的一部分被声明，第三方状态追踪代码（如 CCL `GlStateTracker`）读取即踩坑，只能逐个打补丁
   - `depthMask`/`cullMode` 无 tracked 状态，`glGetInteger` 对部分枚举返回近似值——「真实状态快照」查询能力缺失，兼容桥被迫绕行 `RENDER_BACKEND`

2. **条件 mixin 注册管线手写**
   - 9 个配置 json + `MixinEarly`/`MixinLate` 手写分支 + 3 个测试共同维护；新增一个兼容配置需同步修改 4 个文件（本次 DE/CCL 配置即为实例）

3. **mixin 包隔离规则无约束记录**
   - mixin 包内辅助类被外部引用触发 `IllegalClassLoadError`，且该检查只在非 dev 环境强制（本次 `MixinGlStateTracker$Saved` 生产崩溃）；dev 验证无法覆盖

4. **shader/ 混编**
   - 48k 行 Iris 移植与 `com.dhj.actinium` 代码处于同一编译单元，依赖方向约束形同虚设；若 shader 已反向引用根项目类，说明边界已经打破

5. **debug 设施分散**
   - `GLSMDebug`/`GLSMPerfDebug`/`ActiniumGLDebug`/`IrisGlDebug`/`ActiniumFontDebug` 多处独立，临时诊断代码直接塞进业务文件（本次 TEMP-DEBUG 即为例）

6. **glsm 内第三方包未归一化**
   - `com.mitchej123.glsm`、`net.minecraftforge.eventbus` 重写类与 `com.gtnewhorizons.angelica.glsm` 并存，来源边界无文档

7. **兼容层散落**
   - `compat/`、`mixin/mod/*`、`shadows/`、`loading/` 各管一段，缺少统一的「外部模组兼容 = 配置 + mixin + 实现 + 文档 + 测试」形态

## 3. 可借鉴的项目结构（D:/Code 调研结论）

### 3.1 「核心 + 平台薄壳」——Iris / celeritas / sodium 共识

- **Iris**：`common`（pipeline/shaderpack/gl/parsing）+ `fabric`/`neoforge` 薄壳；`api` source set 独立编译；平台差异经 `IrisPlatformHelpers` 接口抽象
- **celeritas**：核心 `common` 完全抽象于 Minecraft、可独立发布 Maven；Stonecutter 条件源集替代分支维护，覆盖 1.7.10~1.16.5
- **sodium**：`common` 内 `api/boot/desktop` 三个 source set（启动期代码不碰 MC 类）；自定义 configuration 跨项目合并产物

### 3.2 「mixin 与 mixinterface 分离 + 配置细分」——Iris

- `mixin/` 放被注入类，`mixinterface/` 放注入接口
- mixin 配置按兼容目标细分（`mixins.iris.compat.dh.json` 等十余个），条件加载与排查简单

### 3.3 「服务接口 + ServiceLoader 解耦」——sodium

- `common` 定义 `services` 接口，平台经 `META-INF/services` 注册实现，核心零平台依赖

### 3.4 其他

- **GTNHLib**：克制的最小 `api` 包；Multi-Release Jar（`main17` source set）
- **lwjgl3ify**：生成代码与生成器分离（`generated`/`util` source set）
- ~~单模块多 source set（lwjgl3ify 式整体形态）~~：多人维护约束下**不采用**——会丢失子项目级的并行开发隔离与编译期强制

> 多人维护结论：多子项目结构是本项目正确的协作形态；借鉴重点放在
> 「模块内部组织」（Iris 的 mixin 分层、sodium 的 services 解耦），
> 而非「折叠模块边界」。

## 4. 分阶段计划

### Phase 1：低风险收编（不动依赖拓扑）

**1a. mixin/mixinterface 分层**（✅ 2026-08-12 完成）
- 摸底结论：94 个 `@Unique` 方法无任何外部跨类调用，13 个 Accessor/Invoker 已是
  注入接口先例形态——无需大规模迁移，降级为**约定文档化**
- 产出：`AGENTS.md` 新增「Mixin 组织约定」（mixin 只做注入、接口与注入类分离、
  mixin 包隔离规则——含 `IllegalClassLoadError` 仅在非 dev 环境强制的事实）

**1b. mixin 配置声明 modid 条件，加载器扫描注册**（✅ 2026-08-12 完成）
- ~~配置 json 增加 `"mods"` 声明~~（mixin 加载器不接受自定义字段，已否决）
- 改为独立声明文件 `mixins.actinium.conditions.properties`（`配置名=modid[,modid]`）；
  `MixinLate.configsFor` 扫描该文件，测试验证声明与加载器输出一致
- 收益：新增兼容 = 1 个 json + 1 个 mixin + properties 一行，不再改 `MixinLate`

**1c. GLSM 状态查询补全**（✅ 2026-08-12 完成）
- `DepthState` 新增 `maskEnabled`（depthMask 独立 tracked，修复与 depth test 共用
  字段的状态混用）；`glGetInteger` 补全 `GL_DEPTH_WRITEMASK`/`GL_CULL_FACE_MODE`/
  `GL_FRONT_FACE`
- 兼容桥 `GlStateTrackerSnapshot` 改用正式查询，不再绕行 `RENDER_BACKEND`；
  顺带修复其 `depthTest` 误读 mask 值的潜在 bug 及 3 处同类读取方

**1d. debug 门面统一**
- 单一 debug 门面 + 统一开关；新诊断只加一行，业务代码不再出现诊断输出
- 风险：低

**1e. glsm 第三方包来源文档化**
- 在 `glsm` 的 README 或 `docs/` 记录 `com.mitchej123.glsm`、`net.minecraftforge.eventbus` 的来源与维护边界
- 风险：无（纯文档）

### Phase 2：shader 边界（优先级最高——多人维护下最大风险点）

1. **前置依赖分析**（2026-08-12 完成）：`shader/` 对根项目 `com.dhj.actinium.*` 共 **18 个文件**引用，集中在 4 类：
   - **配置静态字段访问（7 文件 / 8 处）**：`ActiniumConfig.enableIris/enableCeleritas/defineIsIris/enableHardcodedCustomUniforms/disableF3Additions`——全部是布尔开关，收敛为共享配置接口即可
   - **`AccessorEntityRenderer`（5 处）**：cast 后调 `getLightmapTexture()`——mixin accessor 接口，下沉到 glsm（渲染栈底层能力）
   - **debug 类（6 处）**：`PBRDebug`/`GlFlightRecording`/`ShaderRegressionDebug`——随 Phase 1d debug 门面收敛
   - **少量运行时/渲染类（4 处）**：`ActiniumRuntime`/`ActiniumWorldRenderer`/`InternalShadowRenderingState`/`Rfp2Compat`——逐一收敛或下沉
   - **结论：子项目化难度中等偏低**——无结构性环依赖，全部为可收敛的简单引用
2. **依赖收敛（✅ 2026-08-12，`48daa99`）**：shader 对根项目引用 **18 → 2 个文件**：
   - debug 全家（`flight` 包 20 类、`PBRDebug`、`ShaderRegressionDebug`）下沉到 `net.coderbot.iris.debug[.flight]`，测试同步迁移
   - `InternalShadowRenderingState` → glsm（`glsm/shadow`，渲染栈底层）
   - `Rfp2Compat` → shader（`net.coderbot.iris.compat.rfp2`）
   - 配置开关经 **`IrisDebugOptions` 桥**（shader 定义 Bridge 接口 + 根项目注册实现，热更新保持；`Iris.enabled` 等静态求值点以「桥未注册返回配置默认值」保证时序无回归）
   - `AccessorEntityRenderer` 能力经 **`PostProcessingBridge`**（GTNHLib）桥接：lightmap texture / night vision brightness，根项目 onConstruct 注册
   - 剩余 2 文件（`IrisDebugScreenHandler`/`ShadowRenderer`）对 `ActiniumWorldRenderer` 的引用属**渲染核心集群**（ShadowRenderer 用它绘制阴影地形），归入步骤 B
3. **步骤 B（✅ 2026-08-12，`11aaf8b`）——ActiniumWorldRenderer 集群**：
   - `WorldRendererCompat` 接口（shader 定义，含阴影 pass 所需的绘制/查询方法）+ `WorldRendererCompatBridge` 注册桥
   - `ActiniumWorldRenderer implements WorldRendererCompat`（根项目 → shader，组装方合法）；onConstruct 注册
   - `ShadowRenderer`/`IrisDebugScreenHandler` 全部经桥调用；`getRenderSectionManager()` 返回根项目类型，接口压平为 `markSectionGraphDirty()`
   - **完成标准达成：`shader/` 对 `com.dhj.actinium.*` 引用清零**
4. **步骤 C（✅ 2026-08-12，`08f1907`）——Gradle 子项目化**：
   - `settings.gradle` 注册 `shader` 子项目；`shader/build.gradle` 依赖 `glsm`/`GTNHLib`/`celeritas-common`（只读）
   - 根项目：`embeddedLibraryProjects` 加入 shader（合并进 jar）、main sourceSet 移除附加源码目录、`compileOnly project(':shader')`
   - 前置整理：`Desugar` 注解定义随 Iris 移植代码进入 shader；零依赖的 angelica 栈类（`Tags`/`AngelicaMod`/`ModStatus`/`compat.mojang`/`compat.toremove`/`rendering`）下沉 glsm；依赖 shader 的 `TextureTracker`/`ModdedBiomeDetector`/`BiomeCategoryCache`/`NativeImage` 随迁 shader；`ClientProxy.animationsMode` 经 `IrisDebugOptions` 桥
   - **完成标准达成：`shader/` 对根项目引用清零 + 独立子项目 + `:check`（含 remap）通过**
5. 决策：**必须子项目化**（多人维护约束下无备选；「接口化」只是过渡手段而非终点）
6. 参照 celeritas 的 `common` 模式：渲染核心与平台/生命周期分离
7. 完成标准：`shader/` 不再出现 `com.dhj.actinium.*` 引用（✅）；构建/remap 无回归（✅ `:check`）；dev 运行回归（✅ 2026-08-12，光影开关/阴影/DE 三症状/进出世界均正常）

### Phase 3：兼容层统一 seam

- **每个外部模组兼容收敛为：配置 json + mixin + 实现 + `docs/compat/` 文档 + 测试**（✅ 2026-08-12）：
  - 配置 json + conditions.properties 声明（Phase 1b ✅）；7 个条件模组的
    `docs/compat/*.md` 补齐（betterfoliage/ccl/dh/gibbed/ichunutil/revoui，
    dh/ichunutil/lumenized 已有测试）
- **bridge 机制系统化（✅ 2026-08-12）**：`docs/bridges.md` 登记全部桥（跨模块桥
  6 项 + ServiceLoader 3 项 + 模块内桥若干），并定义「新增桥的约定」5 条：
  接口放调用方模块、注册集中在 `Actinium.onConstruct`、门面未注册时返回确定默认值、
  不暴露被注入方具体类型、必须有单元测试
- **测试缺口（待补）**：betterfoliage/ccl/gibbed/revoui 尚无模组逻辑测试；
  建议随各模组实际验证一并补（避免为凑测试写低价值用例）

## 5. 验证方式

- 每阶段：`./gradlew :test` 全绿（含 `MixinConfigurationTest`/`MixinLateTest`）
- dev 环境回归：进世界 + 退出回主菜单（无光影 + 光影各一次）
- 涉及渲染路径的改动：DE 在场回归（云/草/主菜单）+ 全量 250 场景
- 发布前 `./gradlew build`

## 6. 明确不做

- 不重构 glsm/GTNHLib 内部实现（本次计划只动边界与组织）
- 不改变既有依赖方向
- 不做「为了对称而对称」的拆分；每阶段必须有可验证的收益
- 不在本计划内处理 Celeritas 渲染算法本身

## 7. 建议执行顺序（多人维护约束下）

1. **Phase 2 前置依赖分析**（立即启动——shader 边界是多人协作的最大风险点）
2. Phase 1a + 1b（mixin 组织，收益直观、风险可控）
3. Phase 1c + 1d（GLSM 查询补全与 debug 门面）
4. 协作机制（见 8.4）：架构依赖方向测试 + 模块所有权文档，与上述并行落地
5. Phase 2 子项目化实施（依赖分析产出后）
6. Phase 3 视 Phase 2 结果排期

## 8. 最终项目结构（目标形态）

重构完成后的目标布局如下。与现状的差异用 `+`/`~`/`-` 标注。

```
Actinium
├── src/main/java/com/dhj/actinium/          — 根项目：收窄为「生命周期 + 配置 + 入口 + 兼容层」
│   ├── Actinium.java                        — Forge 生命周期入口（维持）
│   ├── api/                                 — 对外接口定义（桥接 seam 的接口侧）
│   ├── bridge/                              + 子项目注入实现（提供者侧；替代散落的 bridge/provider）
│   ├── config/                              — 配置（维持）
│   ├── command/                             — 命令（维持）
│   ├── runtime/                             — 运行时状态（维持）
│   ├── debug/                               ~ 统一 debug 门面（收敛 GLSM/Iris/Actinium 分散调试输出）
│   ├── compat/                              ~ 统一兼容层：每外部模组一个包
│   │   ├── draconicevolution/               + 实现类（GlStateTrackerSnapshot 等迁入）
│   │   ├── stellarcore/
│   │   ├── distanthorizons/
│   │   └── ...                              （对应 mixin/mod/* 配置 + docs/compat/*.md + 测试）
│   ├── mixin/                               ~ 只放注入类，按目标分类（维持 vintage/features 粒度）
│   │   ├── vintage/
│   │   ├── features/
│   │   └── mod/<modid>/                     — 外部模组 mixin（维持）
│   └── mixinterface/                        + 注入接口层（与 mixin 分离；Iris 模式）
├── shader/                                  + 独立子项目（从根项目附加源码目录迁出）
│   ├── src/main/java/net/irisshaders/iris/… — 光影管线核心（移植代码，命名空间不变）
│   ├── src/api/java/…                       + 公开接口 source set（独立编译，不被实现污染）
│   └── 依赖：glsm、GTNHLib、celeritas-common（只读）；对根项目零引用，经 bridge 注入
├── celeritas-common/                        — 区块渲染核心（维持；依赖方向不变）
├── glsm/                                    — 状态跟踪 / FFP（维持；补全状态查询；归一化第三方包来源）
└── GTNHLib/                                 — 渲染原语库（维持；api 包保持克制）
```

### 8.1 依赖方向（最终）

```
               ┌──────────────┐
               │   根项目      │  ← 组装方：生命周期、兼容层、mixin 注册
               └──────┬───────┘
                      │ 依赖（只读）
        ┌─────────────┼──────────────┐
        ▼             ▼              ▼
   ┌─────────┐   ┌─────────┐   ┌──────────┐
   │ shader  │   │celeritas│   │  glsm    │
   └────┬────┘   │ -common │   └────┬─────┘
        │        └────┬────┘        │
        └──────┬──────┘             ▼
               ▼              ┌──────────┐
                         ┌───►│ GTNHLib  │
                         │    └──────────┘
                         │
   子项目 → 根项目：禁止（经 api/bridge 注入，维持既有约束）
```

- 既有链 `celeritas-common -> glsm -> GTNHLib` **不变**
- `shader -> glsm / GTNHLib / celeritas-common`：只读依赖，纳入依赖方向约束
- 根项目是唯一允许依赖所有模块的组装方；子项目对根项目的需求一律经 `api/` + `bridge/` 注入

### 8.2 关键规则（目标态）

1. **mixin 只做注入**：mixin 类不承载逻辑；实现位于 `compat/<modid>/` 或对应功能包（mixin 包隔离规则下，辅助类一律放 mixin 包外）
2. **兼容层一处式**：新增外部模组兼容 = `compat/<modid>/` 实现 + `mixin/mod/<modid>/` 注入 + 条件配置 json（声明 modid）+ `docs/compat/<modid>.md` + 测试，无需改动 `MixinLate`
3. **调试统一门面**：业务代码不直接输出诊断，统一走 debug 门面（开关控制）
4. **shader 零反向引用**：`shader/` 不出现 `com.dhj.actinium.*` 引用；需要生命周期/配置时经 bridge
5. **测试对齐结构**：`src/test` 镜像各模块包结构；`MixinConfigurationTest` 等结构测试随 Phase 1b 演进为「配置声明驱动」

### 8.3 与阶段的对应

| 阶段 | 对应目标态变更 |
|------|----------------|
| Phase 1a | `mixinterface/` 出现；mixin 类瘦身 |
| Phase 1b | 配置 json 声明 modid，`MixinLate` 扫描化 |
| Phase 1c/1d | glsm 查询补全；debug 门面收敛 |
| Phase 1e | glsm 第三方包来源文档化 |
| Phase 2 | `shader/` 独立为子项目，`api/` source set 出现 |
| Phase 3 | `compat/<modid>/` 形态统一；`bridge/` 收敛 |

> 注：最终结构是增量可达的目标，不要求一次性迁移；每阶段完成后目标态自动推进一格。

### 8.4 协作边界（多人维护机制）

在结构落地前即可并行启动的协作机制，与代码结构同等重要：

1. **模块所有权**：`docs/architecture.md` 为每个子项目声明 owner（或至少「默认 owner = 渲染栈组」）、职责边界、变更流程（改接口需 cross-review）
2. **依赖方向架构测试**：子项目边界由 Gradle 物理强制；根项目内部与「合法引用但错误方向」由架构测试兜底——推广 `MixinConfigurationTest` 先例，自写类引用扫描（或引入 ArchUnit），断言如：
   - `shader` 包不引用 `com.dhj.actinium.*`
   - 根项目 `compat/` 不反向引用 `mixin/` 内部实现细节
   - `glsm`/`GTNHLib`/`celeritas-common` 不引用根项目（与 Gradle 强制互为备份）
3. **PR 粒度按模块**：单 PR 原则上只动一个子项目 + 必要桥接；跨模块改动需要计划文档先行
4. **CI 分层**：`check` 按模块跑测试 + 架构测试；模块级构建缓存
5. **兼容层 ownership**：每个 `compat/<modid>/` 可由单人认领；`docs/compat/*.md` 为交接文档
6. **重复代码红线**：第 9 节清单落地后，新增代码不得重建双轨（如新的 uniform 上传器必须并入既有实现）——写进 review checklist

## 9. 重复代码精简清单（2026-08-12 调查）

> 依据：对 glsm / GTNHLib / shader / 根项目的引用关系与职责比对（explore 调查），
> 行数为调查时点数据。

### 9.1 死代码——直接可删（约 350 行）

| 类 | 行数 | 证据 |
|----|------|------|
| `GTNHLib/.../renderer/LocalTessellator` | 24 | 全仓库零引用，内容与 `CapturingTessellator` 逐字重复（`storeTranslation/restoreTranslation`） |
| `GTNHLib/.../vao/VertexArrayBuffer` + `VAOManager.createVAO()` | ~70 | 整条 `@Deprecated` 死代码链，已被 `BaseVAO` 组合式实现取代，无外部调用 |
| `GTNHLib/.../opengl/VaoAppleLwjgl3Fallback` | 235 | 零外部引用；`UniversalVAO.getImplementation` 只查 GL30/ARB，Apple 支持从未接线 |
| `src/main/.../rendering/StateAwareTessellator` | 23 | 无任何实现类；`shader/Iris.java` 两处 `instanceof` 恒为 false（原实现 mixin 已随 2026-05 重构移除） |

删除前用 `git grep` 复核零引用；删除后同步修正相关注释（如 `UniversalVAO` 声称支持 Apple fallback 的 javadoc）。

### 9.2 高重叠——合并（✅ 2026-08-12 完成 A-C，结论修正见下）

**A. 双份「FFP 状态 → GLSL uniform 上传器」（两套各 ~470 行）——修正结论**
- `ffp/Uniforms` vs `CompatUniformManager`：逐行细读后确认**定位机制完全不同**（Uniforms 面向自有 `Program` 的 loc 字段；Compat 按 `angelica_*`/`iris_*` 名字查 location 数组），真正逐字重复仅 scene color 计算与 clip plane 循环。强行合并需引入「上传目标抽象」，接口复杂度 ≈ 实现复杂度，**合并收益为负，维持双轨**（与 8.4 协作边界的「不得重建双轨」红线不冲突——此为既有设计事实）
- ✅ 配套 State 合并：`ffp/ProgramUniformState` + `CompatProgramUniformState` → 公共基类 `glsm/states/GenerationTrackedState`（bitmask + generation 表，~100 行样板消除；值比较类别 lightmap/lineWidth/viewport 保留子类实现）

**B. ✅ `VertexBuffer` vs `VertexBufferStorage`（已合并，`VertexBufferStorage` 删除）**
- `VertexBuffer` 增加 storage 模式：`storageFlags` 字段（-1 = 可变 `glBufferData`；≥0 = 不可变 `glBufferStorage`），`allocate`/`update` 按模式分支；`VAOManager` 工厂改用带 flags 构造

**C. ✅ `MatrixUniform` vs `MatrixFromFloatArrayUniform`（已删除后者，优于合并）**
- `uniformMatrixFromArray` 接口方法无任何调用方——死代码入口；删除 `MatrixFromFloatArrayUniform` 类 + 接口方法 + 3 处实现/桩，`ProgramUniforms` 的 `instanceof` 双判断简化为单判断

**D. GLSL 变换双轨（shader 最大重复面，未做）**
- `glsm/.../CompatShaderTransformer`（708 行）vs `shader/.../pipeline/transform/*`（TransformPatcher/CommonTransformer/CompatibilityTransformer 等 8 个类）：固定管线 builtin 与光影包的同源变换两套手写规则
- 已出现交叉引用（shader `ShaderTransformer` 调 `CompatShaderTransformer.fixupQualifiers`），合并方向已开始；建议统一 builtin 变换规则，两入口只保留目标语义差异

### 9.3 待裁定去留

**✅ `CapturingTessellator` 整条捕获管线裁定：废弃删除（2026-08-12）**
- 核实：`new CapturingTessellator` 零实例化点；`TessellatorManager.startCapturingDirect(DirectDrawCallback)` 无外部调用；`DirectDrawCallback` 无实现方——整条 capture 管线（int[] 缓冲 + 对象池提取）是死代码
- 删除 6 个文件：`CapturingTessellator`/`LegacyTessellator`/`QuadExtractor`/`PrimitiveExtractor`/`DrawCallback`/GTNHLib `NormalHelper`（~700 行）
- 连带：`ModelQuad.setState`/`ModelTriangle.setState` 方法删除（仅 extractor 使用）；`ITessellatorInstance` 保留（`MixinTessellator` 活跃实现，`gtnhlib$isCompiling` 被 `TessellatorManager.shouldInterceptDraw` 使用）
- `TessellatorManager` 的 `CallbackTessellator`/`DirectDrawCallback` 路径保留（仍为活跃 API）

**✅ `compat/toremove` 清理（2026-08-12）**
- `MatrixStack` → `net.coderbot.iris.gl.MatrixStack`（ShadowRenderer/ShadowMatrices 活跃使用，随 Iris 移植归位）
- `RenderLayer` → `net.coderbot.iris.layer.RenderLayer`（WrappingMultiBufferSource 使用）
- `VertexConsumer` 零引用 → 删除；`compat/toremove` 包清空移除

### 9.4 不建议动（架构事实）

- `Lwjgl2GLRenderBackend` vs `src/lwjgl3/.../Lwjgl3GLRenderBackend`：同一 `RenderBackend` 抽象的两个平台实现，方法级重复是预期设计
- celeritas-common `GlVertexArray` vs GTNHLib VAO：记录式 vs 立即式双轨，且依赖方向约束下合并成本高
- shader `AlphaTestStorage`/`BlendModeStorage`/`DepthColorStorage`：是 glsm `DeferredHandler` 接口的实现（设计如此）；其「另存 original 状态」可复用 glsm 状态栈快照原语，属渐进优化
- `DebugMessageHandler`（glsm backend vs lwjglCommon）：仅同名，签名/职责不同
- `GLDebug`（callback 安装）vs `GLSMDebug`（draw 日志转储）：职责不同，仅命名相似

### 9.5 建议顺序

1. **9.1 死代码删除**（零风险，先清理）
2. **9.3 MatrixStack**（迁移引用后删除）
3. **9.2C → 9.2A → 9.2B**（glsm 内部合并，收益最大）
4. **9.2D**（GLSL 变换统一，随 Phase 2 shader 边界工作一并做）
5. **9.3 CapturingTessellator 裁定**（需功能确认，独立排期）

> 与 Phase 1c（GLSM 查询补全）、1d（debug 门面）互不冲突，可并行推进。

