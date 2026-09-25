# 同步上游 celeritas 方案（7190f87d8 → fe57c60fa）

> 状态：**同步完成并已通过实机验证**。上游 `7190f87d8..fe57c60fa` 共 11 个提交：6 个收编（含光栅化
> 遮挡剔除三件套合并收编、1 个部分收编），5 个不适用。本地产出 7 个代码 commit。
> 编译（根项目 + 全部子项目 + compatBridge）与全量测试（565 项）通过。
> dev 实机验证结果见文末「遗留验证项」。

## 完成记录

| 上游 commit | 本地 commit | 内容 |
|---|---|---|
| `7190f87d8`+`6169005d8`+`261f9f685` | `1db4945c`+`df7c44ce` | 可选光栅化遮挡剔除：导入 grondag bitraster 引擎与 geometry 包（含许可登记），集成 SectionLattice/OcclusionCuller/Viewport/meshing task 并接选项（后续提交默认开启） |
| `88a2bfa8c` | `5dae89dd` | 修复模组 directional 纹理（`_e`/`_w`）被误判为 normal/specular 贴图 |
| `38ee3f207` | `2faa57f4` | **部分收编**：zip filesystem 泄漏（Iris.java）+ DH compat GL 资源泄漏（DHCompatInternal/IrisGenericRenderProgram）；colorspace/ExtendedShader 等 5 文件不适用 |
| `8398bc281` | `070e7721` | 自适应任务调度器（ChunkBuilder 重写 + ChunkJobMetricsTracker） |
| `bc8373c65` | `4b756e7f` | 重建列表按相机欧氏距离排序 |
| `fe57c60fa` | `b13523e3` | meshing 线程改优先级队列 |

## 上游提交清单（7190f87d8..fe57c60fa，stonecutter 分支）

| commit | 内容 | 处置 |
|---|---|---|
| `7190f87d8` | 可选光栅化遮挡剔除 (#38) | 收编（`1db4945c`+`df7c44ce`） |
| `88a2bfa8c` | [shaders] directional 纹理误判修复 | 收编（`5dae89dd`） |
| `6169005d8` | 空 section 跳过 raster 测试 + bitraster 死代码清理 (#39) | 随 #38 收编 |
| `261f9f685` | 遮挡剔除 occluder bounds 边距与 stale-mesh 回退修复 (#40) | 随 #38 收编 |
| `8d63cb7d4` | [shaders] BufferBuilder 大重构 | 不适用：全部针对 modern immediate-consumer 基础设施（SodiumBufferBuilder 等本地不存在），且与本地 #126 自研 vertex writer 体系冲突 |
| `efb63842d` | [modern] 移除钓鱼钩 patch | 不适用（modern 专属） |
| `91c8dff29` | [modern] immediate buffer 几何增长 | 不适用（modern 专属） |
| `26b979c8f` | [modern] vertex format offset 效率 | 不适用（VertexFormatDescriptionImpl 本地不存在） |
| `38ee3f207` | [shaders] 内存泄漏修复 | 部分收编（`2faa57f4`） |
| `8398bc281` | 自适应任务调度器 | 收编（`070e7721`） |
| `bc8373c65` | 重建列表欧氏距离排序 | 收编（`4b756e7f`） |
| `fe57c60fa` | meshing 线程优先级队列 | 收编（`b13523e3`） |

## 关键决策

1. **遮挡剔除达到收编条件**。上轮（`sync-celeritas-7190f87d.md`）观望的理由是"等上游
   后续修复"：#39（清理 -310 行）与 #40（修 occluder bounds 边距、stale-mesh 回退两个
   实际 bug）已落地；`grondag/bitraster` 为 Apache-2.0（geometry 包 Area/AreaFinder/
   BoxFinder 文件头为 Canvas 衍生 LGPL-3.0，OccluderBoxes/RenderableBounds 上游无文件头），
   已登记 `THIRD_PARTY_NOTICES.md` 并存证 `third-party/licenses/bitraster-APACHE-2.0.txt`。
2. **Actinium 启用该特性，不跟随上游 forge122 的硬编码关闭**。上游 forge122（1.12.2）
   `useRasterOcclusionCulling() → false` 的原因是其 forge122 版 meshing task 未改造
   （不产 occluderBoxes）；Actinium 的 meshing task 与上游 modern 结构同源，已完成改造。
   选项默认 `false`，默认路径与旧行为逐位等价（occluder 数组为 null、rasterOccluder
   不创建）。
3. **vpMatrix 只含旋转+投影、不含相机平移**（本特性唯一无法照抄上游的正确性点，错了
   不抛异常、只静默错剔）：`MixinClippingHelperImpl` 在 `updateJoml` 里把 modelview 拷出后
   清零 m30/m31/m32（去掉眼高/第三人称/bobbing 的视空间平移，位置由 Viewport transform
   承担），再左乘 projection，经 `IClippingHelper.celeritas$getVpMatrix()` 暴露给
   `MixinFrustum.sodium$createViewport` 的 3 参构造。阴影 pass / PortalViewportFactory /
   测试保持 2 参构造 → vpMatrix 为 null → rasterActive 静默回退，与上游 <1.20 行为一致。
4. **meshing task 统一 VisGraph → SectionVisibilityBuilder**。两者 visibilityData 编码
   等价（阈值 opaque<256→全可见、opaque==4096→全不可见；flood fill 逃逸面全对全置位，
   位序 `from*8+to` 与 VisibilityEncoding 一致；opaque 判定 `isOpaqueCube` ≡
   `isSolidRender`）。`markRenderable` 插在空气检查之后，保守覆盖 TE/流体/dispatcher
   全部渲染路径；`FACINGS`/`encodeVisibilityData` 死代码随移植删除（全仓仅该一处使用）。
5. **ChunkBuilder 三方合并保留 #126**。上游 8398bc281 重写调度块，不触碰
   `shutdownThreads`；#126 的改动全部在 `shutdownThreads`（释放 worker/main-thread
   context 的 off-heap buffer），原样保留。合并后与上游终态的残余 diff 仅本地注释风格
   与 #126 shutdown 逻辑。
6. **ChunkBuilderSchedulingTest 随被删代码移除**（其验证的 warm-start 常数/饥饿翻倍/
   budget-limited 反馈均被上游整体删除）；`ChunkJobQueueTest` 改用线程状态轮询并在
   commit 3 补优先级出队断言；两个 occlusion 测试跟随构造器/visitor 签名变化，断言语义不变。
7. **bench（上游 common/src/jmh）不移植**：Actinium 无 jmh sourceSet；`common/build.gradle`
   的新增仅为 jmh 任务，main sourceSet 无新依赖。
8. **38ee3f207 不适用明细**：ShaderPackScreen（modern WatchService 自动刷新，1.6.x 手动
   refresh 无可泄漏资源）、colorspace 三文件（组件不存在）、IrisRenderingPipeline（语义已被
   DeferredWorldRenderingPipeline.destroy() 覆盖）、ExtendedShader（体系不存在，1.6.x
   ProgramBuilder 链接后即销毁 shader 对象）。另发现既有疑点（非本轮范围）：
   `DeferredWorldRenderingPipeline.destroy()` 中 `shadowRenderer.destroy()` 疑似从未被
   调用，建议后续单独评估。
9. **88a2bfa8c 的 MixinDirectoryLister 不适用**：其 Mixin 目标是 1.19+ atlas
   DirectoryLister；1.12.2 由模组经 TextureStitchEvent 显式注册 sprite，无目录列举机制。
10. **compatBridge**：仅 `StandardOptions` 镜像加同名 `RASTER_OCCLUSION_CULLING` 常量
    （保持镜像一致），桥 renderer 编排未动，`StandardOptionsFacadeTest` 通过。

## 遮挡剔除使用与验证

- 选项位置：视频设置（RSO 界面）Performance 页 Rendering Culling 组，
  "Rasterized Occlusion Culling" tick box，默认开；切换触发 renderer 重建
  （REQUIRES_RENDERER_RELOAD）。
- 生效三前提：选项开、`renderChunksMany` 开、主地形 pass 的 vpMatrix 非 null；
  任一不满足即回退旧图遍历路径。
- 观测手段：开 "Render Timing Debug" 选项（或 `-Dactinium.perfDebug`）后，每秒
  `GLSM perf` 日志含 `chunk.scheduler`/`chunk.occlusionSearch` 等段；叠加
  `-Dbitraster.stats=true` 后追加 `chunk.raster` 段（每秒 tested/occluded 及占比、
  test/occlude 均耗时、缓冲规模、回溯次数）。

## 遗留验证项

- ~~遮挡剔除实机回归~~：**已验证**（2026-09-07 实机）：开启选项后帧数上涨约 100，
  光影包运行正常，HBM-CE 同场正常，未见误剔除。
- **调度器三连实机回归**（未专项测量）：大量区块更新（飞行/传送/世界加载）下构建
  吞吐无回退——上述验证运行中调度器已在生效且未见异常，但首发建块吞吐未单独计时；
  注意 `REBUILD_LIST_FRAMES=2` 使首发建块列表深度从 10× 降为 2× in-flight target
  （上游有意为之）。
- 上一轮 `sync-celeritas-7190f87d.md` 的遗留项（planar fog 观感、shadow pass、
  GUI 滑块、C 类运行专项）仍未执行，可后续合并验证。

## 下一轮提醒

- Celeritas 同步基准推进至 `fe57c60fa`（2026-09-05）。上游镜像 `D:/Code/celeritas-mirror`
  与 git.taumc.org 上游 HEAD 一致。
- `8d63cb7d4` 的 BufferBuilder 重构若未来上游有后续修复且本地 #126 体系演进出对应
  结构，可再评估。
