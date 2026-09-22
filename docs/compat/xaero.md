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
- [ ] 遗留：世界地图界面内的 `GuiTexturedButton` 图标按钮仍渲染异常（白底 + 深色墨迹，所采纹理
  与图标不符）。已确认**不是本次修复引入**（issue 截图里 Actinium 开启时按钮已是同样状态，关闭时
  为干净图标），且**与地图的 TexEnv/多纹理路径无关**（关闭 Xaero 的 Lighting 仍复现）。待运行时
  状态取证后单独处理。

## 已知缺口（与本缺陷无关）

- GTNHLib `TessellatorManager.mapVertexFormat/copyVertex`（DirectTessellator 捕获路径）同样只处理
  UV `index` 0/1，UV≥2 被静默丢弃。该路径只在 GTNHLib 捕获期（如显示列表编译、DirectTessellator
  使用者）可达，Xaero 的绘制不经过它，因此与 #175 无关；修复需要扩展
  `VertexFlags`/`DefaultVertexFormat` 的格式表，建议另开条目处理。

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
