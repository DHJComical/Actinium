# LagGoggles（TickCentral）兼容性说明

兼容状态：**部分**（根因、修复与字节码锚点契约测试已完成；实机验证待用户确认）
最后更新：2026-09-23

## 验证范围

- 版本：LagGoggles 5.9（CurseForge 283525，tag `1.12.2-5.9-140`）+ TickCentral 3.2（独立分发的 coremod，
  `RenderManagerTransformer` 由 LagGoggles jar 提供）
- 相关功能：实体渲染的 `entityId` uniform 与 entities gbuffer phase（`RenderManagerIrisMixin`）
- 相关 issue：#166
- 复现日志：issue #166 中 LagGoggles 组的 `latest.log` / `debug.log`

## 症状：共存时启动崩溃（issue #166）

### 现象

只安装 Actinium 时启动正常；装上 LagGoggles（连带 TickCentral）后**启动阶段必崩**，日志顺序固定：

1. `InjectionError: Critical injection failure: Redirector actinium$renderEntityWithIrisId(...) in
   mixins.actinium.iris.json:features.iris.RenderManagerIrisMixin from mod actinium failed injection check,
   (0/1) succeeded. Scanned 0 target(s).`
2. `MixinTransformerError` → `ClassNotFoundException` / `NoClassDefFoundError:
   net/minecraft/client/renderer/entity/RenderManager`
3. `LoaderException: contenttweaker Failed to load new mod instance`（ContentTweaker 的
   `ResourceLoader.setup` 反射读取 RenderManager 字段时踩到第 2 步）

没有崩溃报告，JVM 直接被结束。第 3 步只是下游症状，不是 ContentTweaker 缺陷。

### 机制

1. `mixins.actinium.iris.json` 的 `injectors.defaultRequire = 1`。修复前的
   `RenderManagerIrisMixin` 在 `RenderManager.renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V`
   **方法体内**下了两个 `@Redirect`，锚点为 `Render.doRender` 与 `Render.doRenderShadowAndFire`
   两个调用点。
2. LagGoggles 作为 coremod 注册 `com.github.terminatornl.laggoggles.tickcentral.RenderManagerTransformer`
   （日志 `Loaded coremods (and transformers)` 段可见），其 `transform()` 对
   `net.minecraft.client.renderer.entity.RenderManager` 做**方法体搬迁**：
   按 `desc.endsWith(";DDDFFZ)V")` 找到渲染方法，用 `CopyMethodAppearance` 复制出同签名的新方法，
   新方法体是转发到 `RenderManagerAdapter.redirectRenderEntity` 的一行调用；
   随后把**原方法改名为 `laggoggles_trueRender`**（`Main.MODID_LOWER + "_trueRender"`）并把复制出的
   转发方法挂回原名；`Initializer.renameTargetInstruction` 再把 adapter 内部的调用改写成
   `laggoggles_trueRender`。
3. 于是变换后的类里：`renderEntity` 只剩转发桩，`Render.doRender` 调用点被搬进
   `laggoggles_trueRender`。TickCentral 的变换先于 Mixin 应用（由失败结果反推：若 Mixin 先应用，
   注入会落在原方法体上并随方法体一起被改名，不会失败）。
4. Mixin 仍然**找得到**目标方法（否则报的是 `could not find any targets matching ...`），
   但在名为 `renderEntity` 的方法里再也找不到那个调用点，`require = 1` 校验失败 →
   `InjectionError`。日志里的 `Scanned 0 target(s)` 是 Mixin 0.8.7 的报数缺陷：
   `InjectionInfo.targetCount` 字段在源码里从未被赋值，恒为 0，**不代表"没有匹配到方法"**
   （该语义由 `TargetSelectors.validate` 的另一条报错承担）。
5. `InjectionError` 让 Mixin 放弃整个 `RenderManager` 类的变换（`MixinTransformerError`），
   类加载失败，依赖它的 ContentTweaker 构造随即抛 `NoClassDefFoundError`，启动崩溃。

## 修复：把锚点从调用点移到方法入口

`RenderManagerIrisMixin` 的两个 `@Redirect` 改为 MixinExtras `@WrapMethod`：包裹整个
`renderEntity` / `renderMultipass` 入口方法，用 `try/finally` 复原实体 id 与 gbuffer phase。

- **入口方法在两种布局下都存在**：LagGoggles 保留同名同描述符的转发方法，所以 `@WrapMethod`
  的锚点始终命中；被搬走的只是方法体，而包裹入口天然覆盖任意布局的方法体（无论 Mixin 与
  TickCentral 谁先应用都成立）。
- 同时**删除**了对 `doRenderShadowAndFire` 的透传 `@Redirect`：它只调用原方法、不做事，却和
  `doRender` 一样依赖被搬走的调用点，是同一颗定时炸弹。
- `renderMultipass` 一并改为 `@WrapMethod`。TickCentral 只改描述符以 `;DDDFFZ)V` 结尾的方法，
  该方法是同类脆弱性，顺手消除后两个 hook 的语义也保持一致。
- **唯一的行为差异**：实体 id 与 entities phase 现在覆盖整个 `renderEntity`，即 `doRender` 之外还
  包含 `doRenderShadowAndFire`（阴影／着火特效）与 debug 包围盒；修复前只包住 `doRender` 调用点。
  这与上游 Iris 的做法一致（上游在实体渲染调用的 push/pop 之间设置 `currentEntity`，同样覆盖整个
  实体渲染），但属于需要实机确认的观感变化。
- `renderMultipass` 的相位进入点同样从内部调用点提前到方法入口（早于 vanilla 的 lightmap/color
  设置）；该方法的绘制调用仍是最后一步，状态设置与实际绘制的前后关系不变。
- `ShaderRegressionDebug.logEntityPhase` 接受 renderer 为空：包裹入口后，日志可能在
  "实体没有已注册 Render"时触发（原调用点重定向不可能遇到这种情况）。

与既有兼容层的组合顺序无关：BetterPortals 的 `@Inject(HEAD, cancellable)` 若取消渲染，取消发生在
被包裹的方法体内部，`finally` 仍会复原实体状态，不会泄漏 phase。

## 验证

- `./gradlew check --no-daemon` 通过（含 remap jar 结构校验）。
- 新增 `RenderManagerIrisAnchorTest`（`src/test/java/com/dhj/actinium/mixin/features/iris/`），
  在真实 dev `RenderManager` 字节码上复刻 TickCentral 的搬迁变换（同
  `SomniaEntityRendererAnchorTest` 复刻 Somnia 改写的做法）：
  - 断言变换后名为 `renderEntity` 的方法里**不再**含 `Render.doRender` 调用点——即旧 `@Redirect`
    的 0 命中失败可复现；
  - 断言变换后同签名同名的入口方法**仍存在**且转发到被搬走的方法体——即 `@WrapMethod` 锚点仍命中；
  - 另锁定 `RenderManagerIrisMixin` 不得在 `renderEntity` 内使用调用点注入（`@Redirect` /
    `@WrapOperation`）。变异校验：临时把 `@Redirect` 加回该 mixin 时，该测试如期失败。
- **实机验证待用户确认**（dev 环境无法装载该 coremod 组合）：
  - LagGoggles 5.9 + TickCentral 3.2 共存，能启动到主菜单并进入世界（修复前必崩）；
  - 启用光影包后，实体 `entityId` 相关表现正常（实体高亮／染色、名称牌、着火火焰材质）；
  - 实体阴影与着火特效观感与修复前一致。

## 残余限制

- 本仓库只锁定了字节码层面的锚点契约，最终判据是用户实机；未审计 LagGoggles 对 RenderManager
  之外类的影响（其 `RenderManagerTransformer` 只改这一个类，其余是 TickCentral 的方块／实体
  tick 重定向）。
- 同 issue 中的 **Curvy Pipes** 崩溃不属于本类问题：日志无任何 Java 异常，JVM 在
  `FMLConstructionEvent to mod curvy_pipes` 期间被直接结束，属该模组自身（闭源 Rust 部分）的
  原生崩溃，维护者已声明不会修复，本仓库不处理。
