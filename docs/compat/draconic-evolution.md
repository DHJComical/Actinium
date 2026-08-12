# Draconic Evolution 兼容性说明

兼容状态：**已验证**（dev 环境回归通过；生产整合包全量回归待做）
最后更新：2026-08-12

## 模组信息

- Draconic-Evolution-1.12.2-2.3.28.354-universal.jar（modid `draconicevolution`）
- 相关依赖：BrandonsCore-1.12.2-2.4.20.162-universal.jar（硬依赖）、
  RedstoneFlux-1.12-2.1.1.1-universal.jar（BC 硬依赖）、
  Draconic-Additions-1.12.2-1.16.4.44-universal.jar（DE 硬依赖）、
  CodeChickenLib 3.3.8（DE 渲染依赖，生产整合包为 3.2.3.358）

## 现象（NovaEngineering-World，无光影路径）

DE 在场时出现并发性渲染异常簇：

1. 云异常：云渲染成天空底色、直接遮挡其后所有物体；
2. 草方块侧面偏绿；
3. 退回主菜单后主菜单消失（生产环境亦出现过 Esc 菜单消失/背包与 JEI 闪烁/
   左上角 HUD 异常，部分与 StellarCore HUD 缓存开关组合相关，见 stellarcore.md）。

## 归因验证记录（2026-08-11 ~ 08-12）

环境：Actinium 2.4.0-dev（工作区构建，含 CloudRenderStateBoundary /
StellarCoreHudCacheState）、Cleanroom 0.5.17-alpha、Java 25.0.3、Win11、
NovaEngineering-World 整合包（265 mods）。

二分轨迹（均为「几乎每次启动」判定）：

- 全量 265：异常 → R4a 内容家族（96）异常 → A 半（50）异常 → A1（33）异常 →
  A1a（12）异常 → A1a1（EnderIO 系 + Draconic 系，7）异常 → 移出 Draconic 系后
  **75 jar 完全正常** → Draconic 四件套锁定。
- 隔离最小环境（versions/1.12.2-Cleanroom：Actinium + Fugue + CodeChickenLib +
  Baubles + WI-Zoom，配置与主实例对齐，`renderClouds:true`）：
  - 基础 5 个：正常；
  - +RedstoneFlux：正常；
  - +BrandonsCore：正常（首次启动出现一次 SplashProgress 竞态崩溃，重试通过，
    与 DE 无关的瞬态问题）；
  - **+Draconic-Evolution：①② 复现 + 主菜单消失**；
  - 移除 DE：正常。
- 主实例 75 + BC + DE + RF（78）：①② 异常 → 生产环境确认。
- **全量 250（265 − DE/DA/PackagedDraconic/tconevo/novaeng）：完全正常**。

结论：**Draconic-Evolution 为云异常/草方块侧面偏绿/主菜单消失的直接触发源**
（与其硬依赖 BC/RF 无关；DA 非必需——隔离环境无 DA 仍复现）。

## 移除 DE 的连带影响

- `tconevo`（Tinkers' Evolution）声明 `required-after:draconicevolution`，FML
  启动检查无法绕过，需一并移除或先禁用其 Draconic 集成（tconevo 的
  `disabledModHooks` 配置不能消除 @Mod 级硬依赖）。
- `NovaEngineering-Core` 的 `ItemMachineAssemblyTool` **实现 DE API 接口**
  `com.brandon3055.draconicevolution.api.itemconfig.IConfigurableItem`
  （编译期耦合），缺 DE 时该类加载失败 → 崩溃，需一并移除。
- `PackagedDraconic` 依赖 DE，需一并移除。

## 根因机制（已定位，2026-08-12）

### 机制链

1. **原版 GlStateManager 字段缓存冻结**：`GLSMRedirector` 字节码级改写所有对
   `net.minecraft.client.renderer.GlStateManager` 的方法调用为
   `GLStateManager.xxx`，原版方法体从不执行，其内部字段
   （`blendState`/`alphaState`/`depthState`/`cullState`/`lightingState` 等）
   从加载起保持 Java 初始值。dev 实例中 GlStateManager 在进世界时首次加载
   （late transform），症状恰好从进世界开始出现。
2. **DE 每帧执行 `HudHandler.drawHUD`**（`RenderGameOverlayEvent$Post`），
   无条件调用 CCL `GlStateTracker.pushState()/popState()`。该 tracker 直接读
   原版冻结字段保存状态，`popState` 把真实 GL 状态（blend/alpha/depth/cull/
   lighting）重置为「初始默认值」（全 off）。这些状态在世界渲染中会被重新
   设置，但**帧末（GUI 之后）的恢复把状态留在错误集合**，直接破坏依赖进入
   状态的环节——退出世界后的主菜单（GUI 绘制依赖进入时 blend 开启）因此
   消失。
3. **CCL GlStateTracker 只覆盖 6 项状态**，DE 绘制引入的纹理绑定/矩阵改动
   成为残留；DE 条件渲染（能量水晶/反应堆/光束等 TESR、物品渲染）同样使用
   GlStateTracker——生产整合包场景必然触发。

### 验证结论（dev 环境，Cleanroom 0.6.7，DE 2.3.28.354 + CCL 3.3.8）

- 基线（DE 在场）：三症状复现；云绘制前 dump 显示 GLSM tracked 与真实 GPU
  状态稳定（程序 id 为 FFP 模拟 program，alpha 为 FFP-only 模拟，均为正常
  设计行为——排除内容层纹理污染假设）。
- **跳过 DE drawHUD（`skipHud`）：三症状全部消失** → 触发源锁定 drawHUD。
- **F1 修复生效（drawHUD 恢复执行 + GlStateTracker 兼容桥）：三症状全部消失**。
- 附带排除：主菜单消失时 PanoramaRenderer 纹理/FBO/VAO 全部存活
  （`glIsTexture` 等），**排除资源生命周期假设**——主菜单消失为帧末状态
  残留所致，随 F1 一并修复。

## 修复（2026-08-12，dev 回归通过）

### F1：CCL GlStateTracker 兼容桥（正式修复）

`com.dhj.actinium.mixin.mod.ccl.MixinGlStateTracker`（`mixins.actinium.ccl.json`，
late/conditional，CCL 存在性保护）`@Overwrite` CCL 的
`GlStateTracker.pushState()/popState()`，实现委托给
`com.dhj.actinium.compat.ccl.GlStateTrackerSnapshot`（**放在 mixin 包之外**——
mixin 包内类被外部直接引用会触发 `IllegalClassLoadError`，且该检查只在
非 dev 环境强制，dev 验证无法覆盖）：读写 **GLSM 真实 tracked 状态**
（blend/alpha/depth/cull/lighting/rescaleNormal 的 enabled + 参数），
depthMask/cullMode 经 `RENDER_BACKEND` 从真实 GPU 查询（GLSM 无跟踪，
且 GL11 查询会被重定向回 tracked 近似值）；恢复时调用 GLSM 更新 tracked
与真实 GPU——帧末状态回到「GUI 渲染后的合理状态」，不再被重置为初始默认值。

覆盖 DE 全部 GlStateTracker 使用点：`HudHandler.drawHUD`（每帧）、能量水晶/
反应堆/物品渲染器（TESR/物品阶段）。

### 附带修复（独立 bug）

- `MixinEntityRendererFovFallback`（vintage）：`getFOVModifier` 返回 ≤0 时以
  `fovSetting` 兜底，消除启动时序导致的 ∞ 投影帧（`updateFovModifierHand`
  未初始化）。
- `AngelicaClassDump.dumpBytes`：`Launch.minecraftHome == null` 时跳过 dump，
  消除启动早期类转储 NPE。

### 待回归

- [ ] 生产整合包（NovaEngineering-World 全量 265）回归：云/草/主菜单 +
  Esc 菜单/背包/JEI/HUD
- [ ] 光影包场景回归（DE 在场时）
- [ ] 全量 250 状态下回归 ⑥ 手部症状（Botania 系六选一，见 bisect-log.md 归档）
- [ ] CCL 3.2.3.358（生产整合包版本）验证 F1 mixin 兼容性
