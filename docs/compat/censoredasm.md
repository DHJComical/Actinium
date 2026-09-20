# CensoredASM / Chibi（LoliASM）兼容性说明

兼容状态:**已验证**（dev 运行；生产整合包实机回归待用户确认）
最后更新:2026-09-19

## 验证范围

- 版本:CensoredASM 5.33,CurseForge `chibi-460609` 文件 `8225778`
  （dev 侧 `modRuntimeOnly 'curse.maven:chibi-460609:8225778'`）
- 环境:Cleanroom + Java 25,`./gradlew runClient`（dev 依赖集，含光影相关模组）
- 上游问题:issue #159 *[Crash] Incompatibility with CensoredASM (now "Chibi")*
- 用户报告日志:[crash `Ucfr1xx`](https://mclo.gs/Ucfr1xx)、
  [debug.log `Oy97zvu`](https://mclo.gs/Oy97zvu)

## 症状:装有 CensoredASM 时启动崩溃（issue #159）

### 现象

Actinium 与 CensoredASM 5.33 共存时,游戏在初始化阶段崩溃:

```
net.minecraftforge.fml.common.LoaderExceptionModCrash: Caught exception from LoliASM (loliasm)
Caused by: java.lang.NoClassDefFoundError: net/minecraft/client/renderer/texture/TextureMap
    at java.lang.Class.getDeclaredMethods0(Native Method)
    at net.minecraftforge.fml.common.eventhandler.EventBus.register(EventBus.java:106)
    at zone.rong.loliasm.proxy.ClientProxy.preInit(ClientProxy.java:36)
```

`NoClassDefFoundError` 只是次生症状;`debug.log` 同一时刻的更早一行才是首因:

```
[FATAL] [CleanMix]: Mixin apply for mod actinium failed mixins.actinium.vintage.json:
features.textures.MixinTextureAtlas from mod actinium -> net.minecraft.client.renderer.texture.TextureMap:
org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException
@At("INVOKE") on net/minecraft/client/renderer/texture/TextureMap::getFilteredIterator with priority 1000
cannot inject into net/minecraft/client/renderer/texture/TextureMap::func_94248_c()V
merged by zone.rong.loliasm.client.sprite.ondemand.mixins.TextureMapMixin with priority 1000
```

### 根因:两组渲染 mixin 落在同一批 vanilla 成员上

1. **`TextureMap.updateAnimations`（`func_94248_c`）**——Actinium 的
   `MixinTextureAtlas.getFilteredIterator`（`@ModifyExpressionValue`,默认
   priority 1000）向该方法注入;CensoredASM 的 `ondemand.mixins.TextureMapMixin`
   以**同优先级 `@Overwrite`** 合并了同一方法。Mixin 的
   `Injector.findTargetNodes` 要求注入点优先级高于合并者
   （`injectionPoint.checkPriority(mergedPriority, mixinPriority)`),1000 对 1000
   不满足 → 抛 `InvalidInjectionException` → `MixinApplyError` → `TextureMap`
   类加载失败 → LoliASM 的 `ClientProxy.preInit` 因 `NoClassDefFoundError` 崩游戏。
2. **`BufferBuilder.tex(double,double)`**——Actinium 的 `BufferBuilderMixin`
   与 LoliASM 的 `ondemand.mixins.BufferBuilderMixin` 都对其 `@Overwrite`。这是
   同一类故障的下一个触发点,只是上面那处先崩、还没轮到它。
3. **`BakedQuad` 字段**——LoliASM 的 `squashBakedQuads`(默认开启)会为
   `net.minecraft.client.renderer.block.model.BakedQuad` 注册字节码变换,并生成
   `NewBakedQuadCallsRedirector` 把 `new BakedQuad(...)` 改道到去掉实例变量的子类;
   Actinium 的 `MixinBakedQuad` 以 `@Shadow` 读取
   `face`/`applyDiffuseLighting`/`sprite`/`tintIndex`,在该变换下无法成立。

LoliASM 这两个特性由 `mixins.ondemand_sprites.json`(14 个 mixin)与
`mixins.bakedquadsquasher.json` 承载,冲突面覆盖 `TextureMap`、`BufferBuilder`、
`TextureAtlasSprite`、`TextureManager`、`RenderItem`、`ItemRenderer`、
`BlockModelRenderer` 等 Actinium 同样会触及的类,因此不能只逐点让位。

### 触发条件与回归来源

LoliASM **自带上游为 Celeritas 准备的让位逻辑**:`zone.rong.loliasm.core.LoliTransformer`
在构造时用 `Class.forName` 探测

```java
isCeleritasInstalled = doesClassExist("org.taumc.celeritas.core.CeleritasLoadingPlugin");
```

命中后 `LoliSpriteMixinPlugin` 让整组 on-demand animated textures 不应用、`squashBakedQuads`
被强制关闭,并打印:

```
Celeritas is installed. onDemandAnimatedTextures won't be activated as Celeritas has similar optimizations.
A sodium port is installed. BakedQuads won't be squashed as it is incompatible with Sodium.
```

Actinium 在移除 Celeritas 兼容桥（`celeritas` mod id 与 `org.taumc.celeritas` API 镜像）后
不再提供该 marker,`isCeleritasInstalled` 恒为 `false`,这条上游让位路径随之失效——这就是
#159 的回归点。注意 `squashBakedQuads` 的判定发生在 coremod 构造期,运行期（mixin /
late 配置）已经没有干预窗口。

## 修复:提供探测标记类（2026-09-19）

`src/main/java/org/taumc/celeritas/core/CeleritasLoadingPlugin.java`:一个**无成员**的
`final` 类,不实现任何接口、不挂任何行为,唯一作用是让上述 `Class.forName` 命中,从而
让 LoliASM 走它自己文档化的 Celeritas 路径（两组冲突特性一起关闭）。

- 为什么不是 Actinium 侧逐个让位:让 `BufferBuilder.tex` 的 `@Overwrite` 让位会丢失
  Actinium 的顶点快速写入路径;而 `squashBakedQuads` 对 `BakedQuad` 的变换发生在
  LoliASM coremod 构造期,Actinium 侧无从规避——两者都只有"让 LoliASM 不启用"这一条路。
- 为什么不是 launchwrapper transformer 改写 `LoliTransformer`:那依赖 coremod
  transformer 的注册顺序（Actinium 若晚于 LoliASM 注册,`LoliTransformer` 已经加载完毕,
  改写静默失效且难察觉),标记类则与时序无关。
- 边界:该类**不是** API 镜像,`AGENTS.md` 的第三方 addon 绑定契约已就此写明例外;
  任何 addon 不得引用它,也不得往 `org.taumc.celeritas` 包添加其它成员。

功能上没有损失:Actinium 自身的 `performance.animateOnlyVisibleTextures` 提供同义的
"只更新可见动画纹理"优化,而 LoliASM 的让位逻辑正是以"现代渲染管线自带同类优化"为前提的。

## 验证

dev 实机（`./gradlew runClient`,CensoredASM 5.33 在依赖集中）:

- LoliASM 两条让位日志均出现（见上文引用）→ marker 在 coremod 阶段对
  `Class.forName` 可见,两组特性已关闭。
- `mixins.ondemand_sprites.json` 仍被注册（`[CleanMix]: Adding
  [mixins.ondemand_sprites.json] mixin configuration.`）,但其 mixin 全部经
  `LoliSpriteMixinPlugin` 判定为不应用。
- 日志中无 `Mixin apply for mod actinium failed` / `InvalidInjectionException`,无新增
  crash-report;客户端正常启动到标题界面并进入集成服务器（地形、GUI、GLSM 重定向均正常）。
- `./gradlew check --no-daemon` 通过（含 `verifyDistributedJar`,已把该 marker 类列入
  必需条目,防止打包/裁剪环节丢失）。

生产整合包（用户实例、手动放置 jar）实机回归:**待用户确认**。

## 残余限制

- 本说明只覆盖"CensoredASM 让位两组冲突特性"这一条路径。LoliASM 其余 mixin 组
  （`internal`、`registries`、`rendering`、`crashes`、`forgefixes` 等）未逐组审计;
  目前 dev 共存启动与进世界无异常,但不等价于全量验证。
- 该 marker 只影响用 `org.taumc.celeritas.core.CeleritasLoadingPlugin` 这一具体类名做
  探测的模组（已知为 LoliASM）。若上游改名,探测自然失效,需要重新适配。
- 用户侧规避（修复不可用时）:`config/loliasm.json` 中把 `onDemandAnimatedTextures` 与
  `squashBakedQuads` 置为 `false`,即 LoliASM 的 TextureMap/BufferBuilder/BakedQuad 路径
  不再与 Actinium 竞争。
