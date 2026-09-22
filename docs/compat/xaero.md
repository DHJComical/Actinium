# Xaero 小地图 / 世界地图兼容

最后更新：2026-09-22。分支：`fix/xaero-map-render`。对应
[issue #175](https://github.com/DHJComical/Actinium/issues/175)（两张地图的地形渲染成纯色方块）。

版本：XaeroLib 1.7.3 + Xaero's Minimap 26.5.1 + Xaero's World Map 1.46.0。

## 症状与环境

- 环境：Cleanroom 0.6.13-alpha、Java 25、无光影包、NVIDIA GTX 1650 / 616.92（GL 4.6 core profile）。
- 症状：小地图与世界地图的**地形**渲染为每 64×64 方块一块的纯色（颜色仍像真实地形平均色，边界是
  整齐的区块阶梯）；路标图标、方向字、地图边框、按钮等**非地形**元素正常；关闭 Actinium 后正常。
- 无崩溃、无 GL 报错。

## 模组机制（反编译结论）

世界地图用固定管线多纹理绘制地形：

- `xaero.map.gui.GuiMap:87-90` 定义 `TEX_2F_1/2/3`（UV 元素 `index` = 1/2/3），`:3053-3057` 组装
  `POSITION_TEX_TEX_TEX = POSITION_3F + TEX_2F(index 0) + TEX_2F_1/2/3`，即**每个顶点带 4 组 UV，
  分别对应 legacy texture unit 0..3**；`renderTexturedModalRectWithLighting2` (`:2219-2257`) 每顶点
  连写 4 次 `tex()`。
- 逐 unit TexEnv：`setupTextureMatricesAndTextures(brightness)` (`:2495-2508`) 与
  `setupTexture0/2/3` (`:2452-2478`) 对 unit 0/2/3 设 `GL_COMBINE`（unit 0 `SUBTRACT`、
  unit 2 `ADD`、unit 3 `MODULATE`，用 `GL_PRIMARY_COLOR`/`GL_PREVIOUS`/`GL_TEXTURE` 与 alpha 操作数
  组合出 `brightness × texture`）；`bindMapTextureWithLighting3` (`:2420-2446`) 把**同一张区块贴图**
  绑到 unit 0/2/3（unit 1 关闭）。
- 贴图内容：每张 `MapTileChunk` 是 **64×64** 纹理（1 纹素/方块），由 CPU 侧地图数据经
  **pixel-unpack PBO** 上传（`TextureUpload.Normal/SubsequentNormal` 走 `glTexImage2D`/
  `glTexSubImage2D` 的 `long pboOffset` 重载）；`RegionTexture` 另有 pack PBO 回读路径。
- **小地图复用世界地图的绘制代码**：装了世界地图时 `SupportXaeroWorldmap.drawMinimap/renderChunks`
  (`:189/:404/:417`) 直接调用 `GuiMap` 的静态方法绘制 64×64 区块贴图，因此两者同因同果。
- `hasLight=false`（地图数据无光照信息，或关闭 Xaero 的 Lighting）时退回单 UV 的
  `DefaultVertexFormats.POSITION_TEX`，只涉及 unit 0，不触发本缺陷。

## 冲突分析（根因）

`src/main/java/com/dhj/actinium/render/VanillaVertexBufferRenderer` 是所有 BufferBuilder 绘制路径
（`VanillaBufferBuilderRenderer`、`BufferBuilderStreamingDrawer`、`DeferredDrawBatcher`）共用的
「顶点格式元素 → 属性位置」映射，它此前把 UV 元素按 `index` 硬编码为：

```java
case UV -> element.getIndex() == 0 ? 2 : element.getIndex() == 1 ? 3 : -1;
```

Xaero 的格式用到 `index` 2/3 → 返回 `-1` → `setupVertexFormatAttributes` 直接 `continue`，
**unit 2/3 的属性槽既不 enable 也不下发指针**。FFP 顶点着色器因此退回逐次绘制的常量
`u_CurrentTexCoord2/3`（Xaero 从不设置，即 (0,0,0,1)），unit 2/3 只采样到该纹理的一个纹素，再经上面
的 TexEnv 组合 ⇒ **每张 64×64 地图纹理塌成一个纯色**，与症状（区块级纯色、非地形元素正常）完全吻合。

关键点：`BufferBuilderMixin.tex` 按**元素游标**顺序写入（与 `index` 无关），顶点数据本来就落在各自
偏移上，唯一按 `index` 分派的就是属性位置——这解释了根因为何只在这一处。

## 兼容方案

| 冲突 | 方案 | 位置 |
| --- | --- | --- |
| UV `index` 2/3 无属性槽 → 逐顶点纹理坐标丢失 | UV 元素统一走 `Usage.uvAttributeLocation(unit)`（0→2、1→3、2→5、3→6）；FFP 顶点着色器按同一张表声明 `a_TexCoord2/3`（location 5/6），并在存在属性时不再声明 `u_CurrentTexCoord2/3` | `VanillaVertexBufferRenderer.attributeLocation`、`VertexFormatElement.uvAttributeLocation`、`VertexShaderGenerator`、`VertexKey`、`Uniforms` |
| 单元 2/3 的逐顶点可用性判定 | `VertexKey.packFromState` 查询 `VertexAttribState.isAttribEnabled(5/6)`（per-VAO 状态），而不是只有 unit 0/1 的 `VertexFlags` | `VertexKey`、`VertexAttribState` |
| 客户端数组路径的同类缺口 | `glClientActiveTexture` + `glTexCoordPointer` 也走同一张表（unit 2/3 不再被静默丢弃） | `GLStateManager.texCoordAttributeLocation`、`clientStateToAttributeLocation` |
| 自定义 GLSL 程序（CompatShaderTransformer） | `actinium_TexCoord{i}` 用同一张表；`i>3` 无槽时告警并跳过（宁可链接失败，也不静默采错坐标） | `CompatShaderTransformer` |
| 防止同类静默丢弃再现 | UV 单位无槽时一次性告警；位置表由两个测试锁定 | `VanillaVertexBufferRenderer.reportUnsupportedElement`、`VertexAttributeLayoutTest`、`VanillaVertexBufferRendererAttributeLayoutTest` |

## 验证记录

- [x] `./gradlew check --no-daemon` 通过（156 suites / 714 tests，0 失败）。
- [x] 独立审查子代理复核：反编译 Xaero 三件套 + 逐段核对 glsm FFP 链路（`VertexKey` →
  `VertexShaderGenerator` → `FragmentShaderGenerator` 的 TexEnv 链 → `Uniforms`），判定 pass。
- [x] 用户实机（dev 运行，Xaero 三件套、无光影）：小地图与世界地图地形恢复细节，纯色方块消失
  （截图对比：关 Actinium 正常 / 修复前纯色 / 修复后细节）。
- [ ] 遗留：世界地图界面内的 `GuiTexturedButton` 图标按钮仍渲染异常（白底 + 深色墨迹）。已定位到
  确定的触发开关与状态差异、并给出机制与待办修复，见下节；**修复尚未实机验证，因此未提交**。

## 遗留排查：世界地图图标按钮（2026-09-22）

结论：**不是世界地图本身的问题，而是"实体雷达"渲染留下的状态**；按钮只是第一个受害者。

- **触发开关（用户实机确认）**：小地图设置里关闭「实体雷达」后按钮立即恢复正常 ⇒ 病灶在
  `xaero.hud.minimap.radar.icon.creator.RadarIconCreator` 的每帧图标预渲染。
- **运行期证据**（同一次会话内"雷达开 → 雷达关"两段对照）：
  - 雷达开（按钮坏）：地图界面里 `POSITION_TEX` 精灵绘制处于 **`blend=false` 且 `alphaTest=false`**；
  - 雷达关（按钮正常）：同一图集的同类绘制处于 **`blend=true` / `alphaTest=true`**。
  - 其余状态在两种情况下均正常：`GL_MODULATE`、仅 unit 0 启用、图集 id/尺寸合理（256×256）、
    VAO 属性 `stride=20 / UV@12` 与 `POSITION_TEX` 格式一致、绘制时程序非 0、纹理内容未被覆盖。
- **机制（静态闭环）**：`RadarIconCreator.create` 在渲染实体图标前 `disableBlend()` + `disableAlpha()`
  （`GlStateManager.func_179084_k` / `func_179118_c`，见该方法 100-104 行），而 Xaero 的按钮图集是
  "深色图标 + 透明底"，在混合关闭时**只能靠 alpha test 丢弃透明像素**。但 glsm 的 FFP 决定是否生成
  alpha-test discard 时读的是**被 Iris 覆盖后的内部值**：
  - `AlphaTestStorage`（`shader/`）在覆盖 alpha test 期间把模组原本请求的值保存为
    `originalAlphaTestEnable` / `originalAlphaTest`，并通过 `VanillaBooleanLayer` 暴露为"有效值"；
  - 而 `FragmentKey.packFromState` 用 `getAlphaTest().isEnabled()` / `getAlphaState().getFunction()`，
    `Uniforms.uploadFragmentUniforms` 用 `getAlphaState().getReference()`（均为内部值）；
    glsm 为此准备的 `GLStateManager.isEffectiveAlphaTestEnabled()`（1495 行）与
    `getEffectiveAlphaState(AlphaState)`（1499 行）**定义了却无人调用**。
  - ⇒ 覆盖窗口内 FFP 不生成 discard ⇒ 透明像素被当不透明写入 ⇒ 白底 + 深色图标。
- **待办修复（已定位，未提交）**：把上述三处改用有效值访问器；Iris 未覆盖时有效值等于内部值，
  因此该改动在无光影覆盖时行为不变。
- **已排除的路径**（不要重复排查）：UV 属性槽（#175 已修）、TexEnv/多纹理与 unit 掩码、跨线程状态
  （日志中的非渲染线程调用只在启动 splash 期）、纹理矩阵、程序状态、GUI 纹理 mip、纹理内容/绑定、
  顶点布局与属性偏移。
- **调试安全约束**（踩过的坑）：诊断代码**不得读取 CPU 侧绘制缓冲**（`BufferBuilder` 的
  `ByteBuffer`）——streaming 路径的 `firstVertex` 是持久缓冲内偏移，用它索引上传数据会越界并使客户端
  崩溃。只需跟踪状态时，一律读 glsm 自身跟踪值（零 GL 调用）；必须做 GL 查询时，需避开显示列表编译期
  （`DisplayListManager.isRecording()`），并对纹理回读先经 glsm 解绑 pack PBO。

## 已知缺口（与本缺陷无关）

- GTNHLib `TessellatorManager.mapVertexFormat/copyVertex`（DirectTessellator 捕获路径）同样只处理
  UV `index` 0/1，UV≥2 被静默丢弃。该路径只在 GTNHLib 捕获期（如显示列表编译、DirectTessellator
  使用者）可达，Xaero 的绘制不经过它，因此与 #175 无关；修复需要扩展
  `VertexFlags`/`DefaultVertexFormat` 的格式表，建议另开条目处理。
- `org.lwjgl.opengl.EXTSeparateShaderObjects.glActiveProgramEXT` 未登记重定向：小地图/世界地图探测旧式
  能力对象后会走 EXT 分支，`xaero.map.misc.Misc.setShaderProgram(0)` 因此绕过 glsm 直接改真实程序，
  造成"跟踪程序 vs 真实程序"失步。修复方向：把该激活调用映射为 `GLStateManager.glUseProgram`
  （`glUseShaderProgramEXT` 保持未映射是无害的，EXT 语义下只有激活调用决定绘制程序）。已定位未提交。
- `GLStateManager.glPushAttrib()`（无参）只推 `GL_ENABLE_BIT`，而原版 1.12.2 `GlStateManager.pushAttrib()`
  推的是 `8256 = GL_ENABLE_BIT | GL_CURRENT_BIT` ⇒ 经重定向走该入口时会漏恢复"当前颜色 / 当前纹理坐标"。
  修复方向：让无参版本推 `8256`。已定位未提交。
- `GLStateManager.glGetTexImage(int,int,int,int,long)` 的 PBO 偏移读回错误地套用了
  `suspendPixelPackBuffer()/restorePixelPackBuffer()`（偏移会被当成客户端指针）。已定位未提交。

## 复现配方（dev 运行）

`gradle/scripts/dependencies.gradle` 的本地（未提交）改动把三件套按 issue 版本加入 dev 运行时；
JourneyMap 同时在场会与其 `world_id`/`world_info` 频道冲突，需降为 compileOnly：

```
modImplementation 'curse.maven:xaerolib-1417462:8849823'         // XaeroLib 1.7.3
modImplementation 'curse.maven:xaeros-minimap-263420:8863814'    // Minimap 26.5.1
modImplementation 'curse.maven:xaeros-world-map-317780:8849947'  // World Map 1.46.0
modCompileOnly   'curse.maven:journeymap-32274:5172461'          // 与上者不可共用运行时
```

## 参考

- 反编译产物：`%APPDATA%/minecraft-dev-mcp/decompiled-mods/{xaerominimap,xaeroworldmap,xaerolib}/`
  （Xaero 未混淆，可直接读）。
- 相关先例：`docs/compat/voxelmap.md`（另一个小地图模组的固定管线/状态跟踪缺口）。
