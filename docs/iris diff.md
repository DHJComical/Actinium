# Actinium 与 Iris 实现差异分析

基于 `ffa0e1d` 代码阅读，对照 Iris 规范整理。

---

## 一、渲染阶段时序

### prepare / deferred

**Iris 的标准行为**

- `prepare` 在 gbuffers 渲染之前执行，输入是上一帧的场景颜色，用于生成当前帧所有 gbuffers 程序需要的预计算数据
- `deferred` 在不透明地形渲染完成、半透明地形渲染之前执行，输入是本帧的深度和不透明颜色
- 两个阶段各自拥有独立的输入快照，互不污染

**Actinium 的现状**

`prepare` 和 `deferred` 都在 post pipeline 的 `executePreSceneProgramsInPostPhase()` 里执行，时机在主场景渲染完成之后、composite 之前。代码注释已明确说明这是折中方案：

```
// Match Iris-style stage separation more closely: deferred should
// operate on the current scene inputs, not on prepare's colortex1 result.
```

`deferred` 已单独调用 `preparePreSceneTargetsForDeferred()` 获取独立输入，不再复用 prepare 的 colortex1，这是对 Iris 行为的近似，但执行时机仍在场景渲染之后而非半透明之前。

**实际影响**

依赖 `prepare` 输出驱动 gbuffers 行为（如动态材质切换）的光影包会拿不到正确数据。依赖 `deferred` 在半透明前插入的效果（如屏幕空间折射）时序会错乱。MakeUp 不强依赖这两点，BSL 和 Complementary 可能会触发。

---

### gbuffers_clouds / gbuffers_entities

**Iris 的标准行为**

`gbuffers_clouds` 和 `gbuffers_entities` 是独立的 gbuffers 程序，分别在云渲染和实体渲染时绑定。

**Actinium 的现状**

```java
private static final boolean ENABLE_EXTERNAL_CLOUDS_STAGE = false;
private static final boolean ENABLE_EXTERNAL_ENTITIES_STAGE = false;
```

两个阶段的程序对象已在 `ActiniumWorldStage` 中定义并有编译逻辑，但运行时开关关闭，实际走原生渲染路径。实体会命中 `gbuffers_entities` 对应的 fallback，云不会接入任何 shader 程序。

**实际影响**

依赖 `gbuffers_entities` 的实体光照效果（如 PBR 材质、自定义高光）无法生效。BSL 的实体渲染效果在 Actinium 上会降级为原生渲染。

---

### gbuffers 程序覆盖范围

**Iris 支持的完整列表**

`gbuffers_basic`、`gbuffers_textured`、`gbuffers_textured_lit`、`gbuffers_skybasic`、`gbuffers_skytextured`、`gbuffers_clouds`、`gbuffers_terrain`、`gbuffers_damagedblock`、`gbuffers_block`、`gbuffers_beaconbeam`、`gbuffers_entities`、`gbuffers_entities_glowing`、`gbuffers_armor_glint`、`gbuffers_spidereyes`、`gbuffers_hand`、`gbuffers_hand_water`、`gbuffers_weather`、`gbuffers_water`

**Actinium 的现状**

`ActiniumWorldStage` 枚举只定义了六个：`ENTITIES`、`PARTICLES`、`SKY`、`SKY_TEXTURED`、`CLOUDS`、`WEATHER`。terrain 和 water 走 Celeritas chunk 渲染器路径（translucent 已接通，solid 关闭）。

缺失的程序：`gbuffers_block`（方块实体表面）、`gbuffers_beaconbeam`（信标光柱）、`gbuffers_damagedblock`（破坏进度贴图）、`gbuffers_armor_glint`（附魔光效）、`gbuffers_spidereyes`（发光眼睛）、`gbuffers_hand_water`（水中第一人称手部）、`gbuffers_entities_glowing`（发光实体轮廓）。

缺失的程序会降级到 fallback 链（`gbuffers_textured_lit` → `gbuffers_textured` → `gbuffers_basic`），但如果光影包没有写这些 fallback，对应渲染会完全没有 shader 效果。

---

## 二、buffer flip 语义

**Iris 的标准行为**

Iris 用 `BufferFlipper` 统一管理所有 target 的 ping-pong 状态。每个 composite 程序执行后，根据该程序的 `drawBuffers` 声明自动翻转被写入的 target，读端和写端始终明确。`flip:false` 指令可以阻止特定 target 被翻转。

**Actinium 的现状**

`flipWrittenTargets()` 和 `explicitFlips` 机制已实现，但有两处游离在外的手动调用：

```java
// 第 2356 行
targets.flipTarget(ActiniumPostTargets.TARGET_COLORTEX1);

// 第 2368 行
targets.flipTarget(ActiniumPostTargets.TARGET_COLORTEX8);
```

这两处直接调用 `flipTarget` 而不经过 `flipWrittenTargets`，意味着这两个 target 的翻转不受 `explicitFlips` 控制，也不和 `drawBuffers` 联动。如果光影包在 `shaders.properties` 里对 colortex1 声明了 `flip:false`，这个指令会被忽略。

**实际影响**

使用多 pass 历史采样（TAA、SSAO 历史帧、动态模糊）的光影包如果依赖 colortex1 的精确 flip 语义，可能读到错误的历史帧数据，表现为 TAA 闪烁或历史帧叠影。

---

## 三、uniform 覆盖

### 已实现

以下 uniform 在 `ActiniumWorldShaderInterface` 和 `ActiniumOptiFineUniforms` 中已实现：

`gbufferModelView`、`gbufferModelViewInverse`、`gbufferProjection`、`gbufferProjectionInverse`、`gbufferPreviousModelView`、`gbufferPreviousProjection`、`shadowModelView`、`shadowModelViewInverse`、`shadowProjection`、`shadowProjectionInverse`、`shadowLightPosition`、`frameTimeCounter`、`frameCounter`、`viewWidth`、`viewHeight`、`aspectRatio`、`near`、`far`、`sunAngle`、`shadowAngle`、`worldDay`、`worldTime`、`rainStrength`、`nightVision`、`blindness`、`eyeBrightness`、`eyeBrightnessSmooth`、`isEyeInWater`、`heldItemId`、`heldItemId2`、`heldBlockLightValue`、`heldBlockLightValue2`、`entityId`、`blockEntityId`、`playerMood`、`fogMode`、`fogStart`、`fogEnd`、`fogDensity`、`screenBrightness`、`hideGUI`、`centerDepthSmooth`、`atlasSize`（通过 `atlasWidth` / `atlasHeight`）

### 已知缺失

以下 uniform 在 OptiFine 规范中存在，但 Actinium 尚未实现：

| uniform                 | 说明      | 使用场景       |
|-------------------------|---------|------------|
| `wetnessHalflife`       | 潮湿过渡半衰期 | 雨后地面干燥动画   |
| `dryingHalflife`        | 干燥过渡半衰期 | 同上         |
| `thunderStrength`       | 雷暴强度    | 区分雨天和雷暴天气  |
| `biomeCategory`         | 群系类别    | 沙漠、雪地等群系特效 |
| `biomeTemperature`      | 群系温度    | 植被颜色、降水类型  |
| `biomePrecipitation`    | 群系降水类型  | 雨/雪判断      |
| `lightningBoltPosition` | 闪电位置    | 闪电光源效果     |
| `isSpectator`           | 是否旁观者模式 | 特殊视角处理     |
| `isCreative`            | 是否创造模式  | UI 差异处理    |
| `isSneaking`            | 是否潜行    | 摄像机高度修正    |
| `isSprinting`           | 是否疾跑    | FOV 动画配合   |
| `isOnGround`            | 是否在地面   | 落地震动效果     |
| `teamColor`             | 队伍颜色    | 多人游戏队伍标识   |

`thunderStrength` 和 `biomeCategory` 是 BSL 和 Complementary 的常用 uniform，缺失会导致这两个包的天气和群系特效失效。

---

## 四、solid terrain gbuffer

**Iris 的标准行为**

所有地形（solid、cutout、cutout_mipped、translucent）都走外部 gbuffers 程序，顶点着色器可以访问 `mc_Entity`、`mc_midTexCoord`、`at_midBlock`、`at_tangent` 等扩展顶点数据，用于植被动画、法线贴图、PBR 材质等效果。

**Actinium 的现状**

```java
// ActiniumTerrainPhase.java
private static final boolean ENABLE_EXTERNAL_TERRAIN_REDIRECT = false;
```

solid / cutout / cutout_mipped 三个 pass 走原生渲染路径，不绑定任何外部 program。translucent 已接通（`ENABLE_EXTERNAL_TRANSLUCENT_TERRAIN_REDIRECT = true`）。

顶点扩展数据（`at_midBlock`、`at_tangent`）已在 `ActiniumExtendedChunkVertexEncoder` 中编码进 VBO，数据已经在那里，只是渲染路径没有把 program 接上去。

**实际影响**

所有依赖 solid terrain gbuffers 的效果均不生效：植被风吹动画、积雪法线贴图、石头 PBR 材质、地形自发光（岩浆块、荧石）的自定义处理。这是与 Iris 最大的功能差距。

---

## 五、colortex8–15 写入路径

**Iris 的标准行为**

所有 16 个 color target（colortex0–15）在 gbuffers 和 composite 阶段均可写入，通过 `/* DRAWBUFFERS:XX */` 指令选择写入目标。

**Actinium 的现状**

读和采样已就绪：`PostTargets` 定义了全部 16 个常量，`PostShaderInterface` 绑定了 colortex8 sampler，`RenderPipeline` 做了 `bindPipelineTextureAliases`（第 2476–2483 行）。

缺失的是 gbuffers 阶段往 colortex8+ 写入的 FBO color attachment 配置。composite 阶段写入 colortex8+ 是否工作取决于 `postTargets` 的 FBO 构建，需要验证。

**实际影响**

使用 colortex8+ 存储自定义数据（PBR 法线图、自定义 ID 缓冲、辅助深度）的光影包会写入失败，读到全白或未初始化纹理。BSL 和 Complementary 均使用 colortex8+。

---

## 六、TAA jitter

**Iris 的标准行为**

Iris 使用 Halton 序列生成 TAA 抖动偏移，注入到投影矩阵，保证收敛质量。

**Actinium 的现状**

使用固定的 8 帧偏移序列（`TAA_OFFSET_SEQUENCE_X/Y`，第 120–127 行），缩放因子 0.35（`TAA_JITTER_SCALE`）。不是 Halton 序列，但对大多数光影包来说视觉差异不明显。属于低优先级的细节差异。

---

## 总结

| 差异项                          | 严重程度 | 对 BSL 的影响           | 修复难度                      |
|------------------------------|------|---------------------|---------------------------|
| solid terrain 未接通            | 🔴 高 | 植被动画、PBR 材质全失效      | 高（需对接 vertex format）      |
| gbuffers_entities 关闭         | 🔴 高 | 实体光照效果降级            | 中（开关已有，需验证稳定性）            |
| colortex8+ 写入缺失              | 🔴 高 | BSL 自定义 buffer 写入失败 | 中（FBO attachment 补全）      |
| thunderStrength 等 uniform 缺失 | 🟡 中 | 天气/群系特效失效           | 低（数值计算，直接补）               |
| prepare/deferred 时序折中        | 🟡 中 | 依赖预计算的效果错位          | 高（需改渲染主循环时序）              |
| buffer flip 两处游离调用           | 🟡 中 | TAA 历史帧可能读错         | 低（统一到 flipWrittenTargets） |
| gbuffers_block 等程序缺失         | 🟢 低 | 特定方块效果降级            | 中（逐个接入）                   |
| TAA jitter 非 Halton          | 🟢 低 | 视觉差异极小              | 低（换序列）                    |