# 同步上游 celeritas 方案（f15085d4 → 7190f87d）

> 状态：**修复类同步完成**。上游 `f15085d4b..7190f87d8` 共 25 个提交：16 个纯构建系统变更
> （unimined→RFG/MDG 迁移、Loom/Stonecutter 升级，Actinium 自有工具链，不适用），
> 9 个运行时提交中 6 个已收编（含 1 个部分收编），2 个不适用，1 个观望。
> 编译（根项目 + `celeritas-common` + `shader`）与 `test` 通过。planar fog 雾观感待 dev 目测。

## 完成记录

| 上游 commit | 本地 commit | 内容 |
|---|---|---|
| `1c473b2eb` | `bc3dfb0a` | 区块 vsh `gl_Position` 标记 invariant，防 solid/cutout 变体间 z-fighting |
| `09f4da755` | `b3deb706` | 移除 shadow pass near/far 深度裁剪平面（pack 重映射阴影深度时误剔除投射物） |
| `927e7a09b` | `f44fe6d2` | `Vector2Uniform`/`Vector4ArrayUniform` 改私有副本缓存（另两个 Joml 整型类此前已修） |
| `5e6c6e2b0` | `779c6c92` | 拖动控件真实鼠标捕获：`InteractableContainer` 捕获子项 + `SliderControl` 由 `sliderHeld` 驱动拖动 |
| `53f41b67c` | `b937007c` | planar fog 支持：形状常量 + fsh 按形状沿视轴测距 + 雾剔除保守回退 |
| `31455a039` | `9393f890` | **部分收编**：final pass memory barrier 加 `ranCompute` 门控；parity 双缓冲重构未收编 |

## 上游提交清单（f15085d4b..7190f87d8，stonecutter 分支）

| commit | 内容 | 处置 |
|---|---|---|
| `53f41b67c` | planar fog 基础支持 | 收编（`b937007c`） |
| `5e6c6e2b0` | 拖动控件鼠标捕获 (#37) | 收编（`779c6c92`） |
| `3d993a136`…`15dea5977`（16 个） | unimined→RFG/MDG、Loom/Gradle/Stonecutter/RFG 升级、conf cache 修复、deprecation 清理 | 不适用（Actinium 用自有 unimined 工具链） |
| `09f4da755` | 移除 shadow near/far 平面优化 | 收编（`b3deb706`） |
| `1c473b2eb` | `gl_Position` invariant | 收编（`bc3dfb0a`） |
| `927e7a09b` | uniform 上传缓存修复 | 收编（`f44fe6d2`） |
| `3dca9685a` | 实验性 bilinear/FSR 世界渲染缩放 | 不适用：三个 mixin 注入点（`getMainRenderTarget`、blaze3d `RenderTarget#bindWrite`、`GameRenderer#renderLevel`）均为 1.20+ API；如需 render-scale 应独立立项 |
| `05e5b6629` | batched entity rendering 重写（-3565 行） | 不适用：全部在 `modern/src/main/batching_java/`，1.12.2 无该基础设施 |
| `31455a039` | composite 管线 parity 双缓冲 | 部分收编（`9393f890`，仅 barrier 子项） |
| `7190f87d8` | 可选光栅化遮挡剔除 (#38) | **观望**，见下 |

## 关键决策

1. **shader 侧不能 cherry-pick**：上游 shader 代码在 `net.irisshaders.iris` 包（modern 平台
   `shaders_java`），Actinium 是 `net.coderbot.iris` 包的 Iris 1.6.x 基线，逐文件手动移植。
2. **compat-bridge 的 `SliderControl` 副本保持不动**：已解包扫描 celeritasleafculling jar，
   无任何对 `SliderControl`/gui/options 的注入目标；主 GUI 运行时走 `celeritas-common` 实现，
   桥副本是旧 Celeritas 2.4.0 行为快照，按桥契约不凭直觉变更。
3. **planar fog 不接真实投影矩阵**：上游两处 `createTerrainRenderList` 均传 `null`
   （`getMaximumFrustumSecant` 得 0，`getEffectiveRenderDistance` 回退完整渲染距离），
   即 planar 形状下雾剔除安全禁用。忠实跟随上游，若后续上游接入真实矩阵再跟进。
4. **`SHADOW_CAMERA_OFFSET` 收回 private**：`addDepthPlanes` 删除后无外部消费者
   （Actinium 本无 `ReversedAdvancedShadowCullingFrustum`，该文件部分跳过）。
5. Actinium 的 `init()` 方法形态（无参构造 + `init` 重载）保持，未照搬上游带参构造。

## Actinium 触碰点对照（53f41b67c planar fog）

| 上游文件 | Actinium 对应 |
|---|---|
| `common/.../RenderSectionManager.java` | `celeritas-common/.../render/chunk/RenderSectionManager.java`（结构一致，直改） |
| `common/.../fog/FogService.java` | 同路径（加 3 个 `FOG_SHAPE_*` 常量） |
| `common/.../terrain/SimpleWorldRenderer.java` | 同路径（删一行注释） |
| `assets/sodium/shaders/include/fog.glsl` | `src/main/resources/assets/actinium/shaders/include/fog.glsl`（命名空间不同） |
| `assets/sodium/shaders/blocks/block_layer_opaque.fsh` | `src/main/resources/assets/actinium/shaders/blocks/block_layer_opaque.fsh` |
| `forge1710/.../VintageRenderSectionManager.java`（上游老平台接线） | `src/main/java/com/dhj/actinium/render/terrain/fog/GLStateManagerFogService.java`（`getFogShapeIndex` 返回 `FOG_SHAPE_PLANAR`，1.12.2 的 GL_FOG 即视平面深度语义） |

## 遗留验证项

- **planar fog 雾观感**（行为对齐变更）：dev 起客户端，对比区块雾与 vanilla 实体/粒子雾在
  屏幕边缘的一致性；确认雾剔除回退后大视距无性能异常回退。
- **shadow pass**：`09f4da755` 后 advanced 阴影剔除少 2 个平面，用目标光影包
  （MakeUp/BSL/Complementary）确认阴影正常、shadow pass 帧率无可感知回退。
- **GUI 滑块**：设置界面拖动滑块越过热区不丢跟随；拖动期间其他控件不显示 hover。
- 上一轮 `sync-celeritas-b8c1079a.md` 的 C 类运行专项（fadd 调度、Occlusion/Lattice、
  Iris frustum 三态等）仍未执行，可与本轮验证合并起客户端。

## 下一轮提醒

- `7190f87d8` 光栅化遮挡剔除：约 5500 行（grondag bitraster 光栅化器 ~4000 行 +
  `occlusion/geometry` 包 ~820 行 + 接入 ~200 行），默认关闭、独立 tick box、
  Apache 2.0 许可证兼容。Actinium 的 occlusion 包与上游基线几乎同源（仅注释与本地
  snapshot 适配差异），接入面有 `forge1710` 老平台 `VintageRenderSectionManager` 可参照。
  该提交刚发布、剔除开销约翻倍，**建议等上游出现后续修复提交后再评估**。
- `31455a039` 其余部分（ParityFramebuffer/惰性 alt/depthtex2 采样门控）：依赖上游新
  target 架构，Actinium 1.6.x 基线需全量手改，帧间时序敏感（TAA 类包回归风险），
  除非 parity 复制的帧开销实测成为瓶颈，否则维持不收编。
- `3dca9685a` 中可复用件：FFX `ffx_a.h`/`ffx_fsr1.h` 纯 GLSL 头与
  `Vector4UnsignedIntegerUniform`，若独立立项 render-scale 可参考。
