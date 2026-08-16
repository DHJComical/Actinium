# VoxelMap 兼容（voxelmap 1.9.25）

最后更新：2026-08-16。分支：`fix/voxelmap-minimap-black`。对应
[issue #34](https://github.com/ActiniumMC/Actinium/issues/34)（VoxelMap 小地图黑屏）。

## 模组机制（反编译结论，CFR 0.152）

VoxelMap（CurseForge 项目 225179，文件 3029445，modid `voxelmap`，版本 1.9.25）
在 `RenderGameOverlayEvent.Post`（`TickHandler.onRenderOverlay`）里调用
`Map.onTickInGame` 绘制小地图：

- `GLUtils` 类初始化时探测 `hasAlphaBits`（默认 framebuffer 是否有 alpha 位）与
  `fboEnabled`（EXT_framebuffer_object + OpenGL14）。
- `hasAlphaBits=true` → 走**直接纹理路径**：`renderMap` 内
  `glColorMask(alpha-only) → glClear(alpha) → 画 circle/square mask → 画地图纹理`。
- `hasAlphaBits=false` → 走**旧 FBO 路径**（EXT framebuffer object + 固定管线光栅化）。
- 地图纹理上传（`LiveGLBufferedImage.write`）：CPU raster（`TYPE_4BYTE_ABGR`）bytes
  直传 `glTexImage2D(GL_RGBA, GL_UNSIGNED_BYTE)`。

### 直接纹理路径的 alpha 语义（本兼容的关键）

1. 把**整个** color buffer 的 alpha 清 0（`glColorMask(false,false,false,true)` +
   `glClear(16384)`，无 scissor）；
2. 以 `(770,771)`（SRC_ALPHA, ONE_MINUS_SRC_ALPHA）画 mask 纹理：圆内 alpha=255
   + 黑色 RGB，圆外 alpha 保持 0；
3. 以 `(772,773)`（**GL_DST_ALPHA**, ONE_MINUS_DST_ALPHA）画地图纹理 quad
   （64 GUI px，可旋转/平移）：**只在 dst alpha > 0（mask 圆内）落笔**，
   且 alpha 通道本身不被修改——mask 圆形状由此保持；
4. 之后在 scissor 盒内画圆框/罗盘/路径点。

`GLShim.glEnable/glDisable(3089)` 与 `glScissor` 直调 `GL11`，绕过 glsm 状态缓存；
`glTexParameteri` 直调 `GL11.glTexParameteri(III)V`。

## 冲突分析（Actinium 环境）

Actinium 的窗口由 LWJGL2 兼容层创建、无 alpha 位 → `hasAlphaBits=false` →
VoxelMap 选旧 FBO 路径，在 core profile 上渲染全黑（issue #34 黑屏根因一）。

`glTexParameteri(10241, 9987)`（GL_LINEAR_MIPMAP_LINEAR）依赖遗留
`GL_GENERATE_MIPMAP` 生成 mipmap 链；core profile 忽略该参数 → 纹理只有 level 0，
采样不完整纹理返回不透明黑（黑屏根因二）。

StellarCore `HudCaching`（`B:HudCaching=true`）把整个 HUD（含小地图）渲染进缓存
FBO，之后按 alpha 混合 blit 回屏幕（`HUDCaching.renderCachedHud`，
`EntityRendererMixin_HUDCaching` 重定向 `renderGameOverlay`）。小地图的
"全屏清 alpha + DstAlpha 依赖混合"在缓存 FBO 上会互相冲突：
跳过 clear 则缓存 alpha 保持不透明 → 地图 quad 在整个矩形内绘制（圆周黑块）；
不跳过则清掉已绘制 HUD 元素的 alpha → 缓存 blit 令它们变透明（HUD 消失）。
该交互是 VoxelMap 与 StellarCore 之间的第三方冲突：`HudCaching=false` 时黑块
即消失（2026-08-16 实测），与本模组渲染管线无关。

另有环境性频道冲突：VoxelMap 与 JourneyMap 都注册 `world_id`/`world_info`
SimpleNetworkWrapper（`ForgeModVoxelMap.init`），同时加载会撞车；
验证 VoxelMap 时必须禁用 JourneyMap。

## 兼容方案

| 冲突 | 方案 | 位置 |
| --- | --- | --- |
| 无 alpha 位 → FBO 老路径黑屏 | `GLUtils.<clinit>` 后强制 `hasAlphaBits=true, fboEnabled=false`，选直接纹理路径 | `MixinVoxelMapGLUtils` → `VoxelMapCompat.forceCpuMinimapPath()` |
| mipmap 纹理不完整 → 采样黑 | 把 `(10241, 9987)` 降为 `(10241, 9729)`（线性过滤，无 mipmap 也完整） | `MixinVoxelMapGLShim.glTexParameteri`（ModifyArgs，`GL11;glTexParameteri(III)V`）|
| scissor 直调 GL11 绕过 glsm 缓存 → core profile 无 scissor，地图 quad 洒满 HUD | 把 `glEnable/glDisable(3089)`、`glScissor` 转交 `GLStateManager`（真实 GL 调用由 glsm 缓存发出） | `MixinVoxelMapGLShim` |
| HudCaching 窗口内全屏清 alpha → HUD 透明 | 窗口内跳过 VoxelMap 的 `glClear(16384)`，保护已绘 HUD 的缓存 alpha | `MixinVoxelMapGLShim.glClear`（`GLSMConfig.hudCacheOverride` 门控） |
| 跳过 clear 后缓存 alpha 保持不透明 → 矩形内圆外也画上地图（黑块） | 把 alpha clear 用 scissor **限定到地图矩形**（镜像 `Map.renderMap` 的 scissor 盒），圆外保持透明 | `MixinVoxelMapRenderMap.renderMap` HEAD/RETURN + `VoxelMapCompat.mapScissorActive` 共享标志 |

跨 mixin 共享状态（`mapScissorActive`）放在 `VoxelMapCompat`：mixin 类不能带
非 private 静态字段。

所有 mixin 为 `remap=false`，经 `mixins.actinium.voxelmap.json`（late/conditional，
门控 mod id `voxelmap`）加载；引用 StellarCore 的路径以
`GLSMConfig.hudCacheOverride`（仅当 StellarCore HudCaching 活动时为 true）为运行时
存在性门控。

## 文件清单

- `src/main/java/com/dhj/actinium/compat/voxelmap/VoxelMapCompat.java`（新增，桥/共享状态）
- `src/main/java/com/dhj/actinium/mixin/mod/voxelmap/MixinVoxelMapGLUtils.java`（新增）
- `src/main/java/com/dhj/actinium/mixin/mod/voxelmap/MixinVoxelMapGLShim.java`（新增）
- `src/main/java/com/dhj/actinium/mixin/mod/voxelmap/MixinVoxelMapRenderMap.java`（新增）
- `src/main/resources/mixins.actinium.voxelmap.json`（新增 late/conditional 配置）
- `src/main/resources/mixins.actinium.conditions.properties`（`...voxelmap.json=voxelmap`）
- `src/test/.../mixin/MixinConfigurationTest.java`（覆盖新配置）
- `src/test/.../mixins/MixinLateTest.java`（configsFor 期望更新）
- `gradle/scripts/dependencies.gradle`（VoxelMap `modImplementation`，dev 验证后还原
  `compileOnly`；JourneyMap 保持 `modImplementation`——验证 VoxelMap 时手动停用）

## 验证记录

- [x] `compileJava` / `test` / `build` 通过（分支 `fix/voxelmap-minimap-black`，
  JourneyMap 依赖在场，`JourneyMapRedirectorTest` 转绿）。
- [x] dev 运行（2026-08-16，`runClient`，Cleanroom，`HudCaching=true`，JourneyMap 停用）：
  - 小地图黑屏消失：CPU 直接纹理路径 + 线性过滤回退生效（dump 显示圆内为真实
    地图内容：水面、地形、玩家标记）；
  - HUD 不再被缓存隐藏（跳过 clear 保护缓存 alpha）；
  - scissor 盒四角与圆周外部为场景内容（天空/地形），地图内容不溢出圆外
    （scoped clear 生效，`framebuffer-tick-*`/`framebuffer-frame-*` dump 像素统计）。

### 已知缺口（第三方交互，非本模组缺陷）

- StellarCore `HudCaching` 开启时，小地图圆周可能仍有黑块残留。根因为 VoxelMap
  依赖"全屏清 alpha + DST_ALPHA 混合"的旧式绘制协议，与 HUD 缓存 FBO 的 alpha
  语义天然冲突；`HudCaching=false` 时黑块消失（隔离实验确认）。选用方（用户）可在
  VoxelMap 与 HudCaching 二选一，或在 `stellar_core.cfg` 关闭
  `B:HudCaching`。Actinium 侧该问题不属于渲染管线缺陷，不再投入。

## 参考

- 反编译产物：`.tmp/voxelmap-src/`（CFR 0.152，临时目录不提交）；
  StellarCore mixin 反编译 `.tmp/stellarcore-src/`。
- 中断与恢复：dev 客户端在资源配置下载期间偶尔需要重启（`runClient` 网络阶段）。