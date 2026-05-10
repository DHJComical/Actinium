# Actinium → Angelica iris 全量重构指南

基于代码阅读，2026-05-05

---

## 总体策略

把 Angelica 的 `net/coderbot/iris/` 370 个文件整体复制进 Actinium，
然后逐层替换所有 Angelica 专属依赖为 1.12.2 原生等价物。
保留 Celeritas 的 32 个 chunk 渲染 Mixin 不动，只替换 9 个 shader Mixin。

全程分五个阶段，每个阶段结束时应该能编译，不一定能运行。

---

## 阶段一：复制代码，建立编译基线（预计 2~3 天）

### 1.1 复制 iris 主体

```bash
cp -r Angelica/src/main/java/net/coderbot/iris \
       Actinium/src/main/java/net/coderbot/iris

cp -r Angelica/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/shaders \
       Actinium/src/main/java/com/dhj/actinium/mixin/angelica/shaders
```

### 1.2 复制 Angelica 的垫片层（只取 iris 真正用到的）

需要复制的文件：

```
Angelica/src/main/java/com/gtnewhorizons/angelica/
  compat/mojang/Camera.java
  compat/mojang/GameModeUtil.java
  compat/mojang/InteractionHand.java
  compat/mojang/NativeImage.java
  compat/mojang/Constants.java
  compat/mojang/AutoClosableAbstractTexture.java
  compat/toremove/MatrixStack.java
  compat/toremove/RenderLayer.java
  compat/iris/BiomeCategoryCache.java
  compat/iris/ModdedBiomeDetector.java
  client/rendering/TextureTracker.java
  rendering/RenderingState.java
  rendering/StateAwareTessellator.java
  rendering/celeritas/BlockRenderLayer.java        ← 直接删，用 MC 原生
  rendering/celeritas/CeleritasWorldRenderer.java  ← 对应 Actinium 的等价类
  rendering/celeritas/api/IrisShaderProvider.java
  rendering/celeritas/api/IrisShaderProviderHolder.java
  rendering/celeritas/iris/IrisExtendedChunkVertexType.java
```

放置路径统一改为 `com/dhj/actinium/angelica/`，全局替换包名。

### 1.3 建立 stub 类（让编译通过但不实现）

以下类暂时用空 stub，后续阶段逐步实现：

```java
// com/dhj/actinium/angelica/glsm/GLStateManager.java
// 空 stub，方法全部抛 UnsupportedOperationException
// 阶段二会逐个替换调用点，这个类最终会被删掉

// com/dhj/actinium/angelica/glsm/RenderSystem.java
// 同上

// com/dhj/actinium/angelica/config/AngelicaConfig.java
public class AngelicaConfig {
    public static boolean enableIris = true;
    public static boolean enableSodium = true;
}
```

### 1.4 目标

`./gradlew compileJava` 无报错（允许警告）。

---

## 阶段二：替换 GLSM 依赖（预计 3~4 天）

这是工作量最大的阶段。iris 有 120 处调用 `GLStateManager` 和 `RenderSystem`，
全部替换为 1.12.2 原生调用或直接 LWJGL3 调用。

### 替换规则表

**GLStateManager → net.minecraft.client.renderer.GlStateManager**

| Angelica GLStateManager                    | 1.12.2 等价                                  |
|--------------------------------------------|--------------------------------------------|
| `GLStateManager.enableBlend()`             | `GlStateManager.enableBlend()`             |
| `GLStateManager.disableBlend()`            | `GlStateManager.disableBlend()`            |
| `GLStateManager.enableDepth()`             | `GlStateManager.enableDepth()`             |
| `GLStateManager.disableDepth()`            | `GlStateManager.disableDepth()`            |
| `GLStateManager.enableTexture2D()`         | `GlStateManager.enableTexture2D()`         |
| `GLStateManager.disableTexture2D()`        | `GlStateManager.disableTexture2D()`        |
| `GLStateManager.enableCull()`              | `GlStateManager.enableCull()`              |
| `GLStateManager.disableCull()`             | `GlStateManager.disableCull()`             |
| `GLStateManager.enableAlpha()`             | `GlStateManager.enableAlpha()`             |
| `GLStateManager.disableAlpha()`            | `GlStateManager.disableAlpha()`            |
| `GLStateManager.colorMask(r,g,b,a)`        | `GlStateManager.colorMask(r,g,b,a)`        |
| `GLStateManager.depthMask(f)`              | `GlStateManager.depthMask(f)`              |
| `GLStateManager.depthFunc(f)`              | `GlStateManager.depthFunc(f)`              |
| `GLStateManager.glBlendFunc(s,d)`          | `GlStateManager.blendFunc(s,d)`            |
| `GLStateManager.tryBlendFuncSeparate(...)` | `GlStateManager.tryBlendFuncSeparate(...)` |
| `GLStateManager.glActiveTexture(u)`        | `GlStateManager.setActiveTexture(u)`       |
| `GLStateManager.glBindTexture(t,id)`       | `GlStateManager.bindTexture(id)`           |
| `GLStateManager.viewport(x,y,w,h)`         | `GL11.glViewport(x,y,w,h)`                 |
| `GLStateManager.color(r,g,b,a)`            | `GlStateManager.color(r,g,b,a)`            |

**RenderSystem → 直接 LWJGL3 调用**

| Angelica RenderSystem                      | 1.12.2 等价                            |
|--------------------------------------------|--------------------------------------|
| `RenderSystem.glUniform1i(loc,v)`          | `GL20.glUniform1i(loc,v)`            |
| `RenderSystem.glUniform1f(loc,v)`          | `GL20.glUniform1f(loc,v)`            |
| `RenderSystem.glUniform2f(loc,v0,v1)`      | `GL20.glUniform2f(loc,v0,v1)`        |
| `RenderSystem.glUniform3f(...)`            | `GL20.glUniform3f(...)`              |
| `RenderSystem.glUniform4f(...)`            | `GL20.glUniform4f(...)`              |
| `RenderSystem.uniformMatrix4fv(loc,t,buf)` | `GL20.glUniformMatrix4fv(loc,t,buf)` |
| `RenderSystem.glUseProgram(p)`             | `GL20.glUseProgram(p)`               |
| `RenderSystem.glBindFramebuffer(t,id)`     | `GL30.glBindFramebuffer(t,id)`       |
| `RenderSystem.glFramebufferTexture2D(...)` | `GL30.glFramebufferTexture2D(...)`   |
| `RenderSystem.glDrawBuffers(fb,buf)`       | `GL30.glDrawBuffers(buf)`            |
| `RenderSystem.glGenTextures()`             | `GL11.glGenTextures()`               |
| `RenderSystem.glDeleteTextures(id)`        | `GL11.glDeleteTextures(id)`          |
| `RenderSystem.texImage2D(...)`             | `GL11.glTexImage2D(...)`             |
| `RenderSystem.texParameteri(...)`          | `GL11.glTexParameteri(...)`          |
| `RenderSystem.generateMipmaps(t,target)`   | `GL30.glGenerateMipmap(target)`      |
| `RenderSystem.glGetAttribLocation(p,n)`    | `GL20.glGetAttribLocation(p,n)`      |
| `RenderSystem.glGetUniformLocation(p,n)`   | `GL20.glGetUniformLocation(p,n)`     |
| `RenderSystem.getProgramInfoLog(p)`        | `GL20.glGetProgramInfoLog(p)`        |
| `RenderSystem.getShaderInfoLog(s)`         | `GL20.glGetShaderInfoLog(s)`         |
| `RenderSystem.bindImageTexture(...)`       | `GL42.glBindImageTexture(...)`       |
| `RenderSystem.bufferData(t,d,u)`           | `GL15.glBufferData(t,d,u)`           |

**状态对象替换**

| Angelica 类型  | 替换方案                                         |
|--------------|----------------------------------------------|
| `BlendState` | 直接读 `GlStateManager.blendState`（已有 accessor） |
| `AlphaState` | 直接读 `GlStateManager.alphaState`              |
| `DepthState` | 直接读 `GlStateManager.depthState`              |
| `ColorMask`  | 内联为四个 boolean 字段                             |

### 操作方式

不要手工逐个改，用 IDE 的批量替换：

```
查找：import com.gtnewhorizons.angelica.glsm.GLStateManager;
全部删除，改为：
  import net.minecraft.client.renderer.GlStateManager;
  import org.lwjgl.opengl.GL11;
  import org.lwjgl.opengl.GL20;
  import org.lwjgl.opengl.GL30;
```

然后让编译器报错，逐个修方法名。方法名差异很小，大多数只需要改大小写。

### 阶段二目标

删掉 stub `GLStateManager.java` 和 `RenderSystem.java`，`compileJava` 仍无报错。

---

## 阶段三：替换垫片层（预计 2~3 天）

### 3.1 Camera → 直接从 EntityRenderer 取

Angelica 的 `Camera` 是一个单例，追踪摄像机位置。1.12.2 的等价数据直接在渲染时可以取到：

```java
// 替换 Camera.INSTANCE.getPos() →
Entity renderView = Minecraft.getMinecraft().getRenderViewEntity();
double camX = renderView.lastTickPosX + (renderView.posX - renderView.lastTickPosX) * partialTicks;
// 等等

// 或者直接复用 Actinium 已有的 ActiniumCapturedRenderingState
```

### 3.2 RenderingState → ActiniumCapturedRenderingState

`RenderingState` 只有相机位置、投影矩阵、modelView 矩阵、FOV 五个字段，
直接映射到 `ActiniumCapturedRenderingState` 的对应字段：

```java
// 全局替换
RenderingState.INSTANCE.getCameraPosition()
→ ActiniumCapturedRenderingState.getCameraPosition()

RenderingState.INSTANCE.getProjectionMatrix()
→ ActiniumRenderPipeline.INSTANCE.getGbufferProjectionMatrix()
```

### 3.3 NativeImage → BufferedImage + LWJGL STBImage

`NativeImage` 只用于读取 PNG 和像素操作，用途集中在 `CustomTextureManager` 和噪声纹理生成：

```java
// NativeImage.read(InputStream) →
BufferedImage img = ImageIO.read(stream);

// NativeImage.getR/G/B/A(pixel) →
int argb = img.getRGB(x, y);
int r = (argb >> 16) & 0xFF;
// 等等
```

### 3.4 BlockRenderLayer → net.minecraft.util.BlockRenderLayer

1.12.2 原生就有这个类，**直接删掉** Angelica 的 `BlockRenderLayer` 垫片，
全局替换 import 即可：

```
com.gtnewhorizons.angelica.rendering.celeritas.BlockRenderLayer
→ net.minecraft.util.BlockRenderLayer
```

### 3.5 MatrixStack

Angelica 的 `MatrixStack` 在 iris 里只用于传递矩阵参数，不做实际变换。
最简单的方案是用 `float[]` 或 JOML `Matrix4f` 替代所有用到它的地方。

### 3.6 TextureInfoCache → GL 直接查询

`TextureInfoCache` 只用于查询纹理的 `internalFormat`，共 3 处调用：

```java
// TextureInfoCache.INSTANCE.getInfo(textureId).getInternalFormat() →
int[] result = new int[1];
GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT, result);
// 或者用 GL30 DSA 版本
```

### 3.7 AngelicaConfig → ActiniumConfig

```java
// AngelicaConfig.enableIris → 直接 true（Actinium 默认开启）
// AngelicaConfig.enableSodium → 直接 true
// 全局删掉这两个条件判断
```

---

## 阶段四：替换 Mixin（预计 3~5 天）

Angelica 的 shader Mixin 有 **28 个文件**，分布在 `shaders/` 和 `shaders/startup/`。

### 4.1 Mixin 差异对照

1.7.10 和 1.12.2 的渲染类名高度一致，差异主要在方法签名：

| 差异点                  | 1.7.10 (Angelica)                                                     | 1.12.2                                |
|----------------------|-----------------------------------------------------------------------|---------------------------------------|
| EntityRenderer 主渲染方法 | `renderWorld(float, long)`                                            | `renderWorldPass(int, float, long)`   |
| 粒子渲染                 | `renderParticles(Entity, float)`                                      | `renderParticles(Entity, float)` ✅ 一致 |
| 阴影渲染                 | `doRenderShadowAndFire(Entity, double, double, double, float, float)` | 同名 ✅                                  |
| TileEntity 渲染        | `renderTileEntity(TileEntity, float, int)`                            | `render(TileEntity, float, int)`      |
| Framebuffer          | `net.minecraft.client.shader.Framebuffer`                             | 同 ✅                                   |
| RenderGlobal 地形渲染    | `renderChunkLayer(BlockRenderLayer, double, double, double)`          | 同 ✅（Celeritas 已 hook）                 |

### 4.2 每个 Mixin 的迁移策略

**直接可用（方法签名一致）：**
- `MixinRender.java` — 实体渲染基类
- `MixinRenderBiped.java` — 双足实体
- `MixinRenderManager.java` — 实体管理器
- `MixinTileEntityRendererDispatcher.java` — TileEntity 渲染
- `MixinFramebuffer.java` — framebuffer 操作
- `MixinRenderItem.java` — 物品渲染
- `MixinGuiIngameForge.java` — GUI 渲染

**需要调整方法签名（1~5行改动）：**
- `MixinEntityRenderer.java` — `renderWorld` → `renderWorldPass` 多一个 int 参数
- `MixinRenderGlobal.java` — 检查 `renderSky`、`renderClouds`、`renderEntities` 签名
- `MixinItemRenderer.java` — 确认第一人称手臂渲染方法名

**1.12.2 不存在，需要删掉或重写：**
- `MixinRenderDragon.java` — 1.12.2 末影龙渲染类名不同（`RenderEnderDragon`）
- `MixinRenderHorse.java` — 1.12.2 马渲染已合并进通用实体渲染器
- `AccessorEntityHorse.java` — 同上
- `MixinRenderManagerDAPI.java` — 针对 DragonAPI，非必须

**1.12.2 独有，需要新增：**
- Llama、Parrot、ShulkerBox 等 1.9+ 新实体的渲染 Mixin（如果要支持其光照效果）

### 4.3 操作顺序

先只保留最关键的三个，让游戏能启动：

1. `MixinEntityRenderer` — 管线入口
2. `MixinRenderGlobal` — 地形和天空
3. `MixinFramebuffer` — FBO 接管

确认能启动后，再逐个加入实体、物品、TileEntity 的 Mixin。

---

## 阶段五：Celeritas 接口对接（预计 2~3 天）

### 5.1 IrisShaderProvider / CeleritasTerrainPipeline

Angelica 通过 `IrisShaderProvider` 接口把 shader 程序注入 Celeritas 的 chunk 渲染器。
Actinium 已经有类似机制（`ActiniumChunkShaderInterface`），需要把两边的接口对齐：

```
Angelica 的接口：
  IrisShaderProvider.getTerrainShader(BlockRenderLayer) → GlProgram

Actinium 已有的：
  ActiniumChunkShaderInterface（实现了 Celeritas 的 ChunkShaderInterface）
```

把 `IrisShaderProvider` 的实现指向 Actinium 的 `ActiniumChunkShaderInterface`，
或者直接让 Angelica 的 iris pipeline 通过 `ActiniumChunkShaderInterface` 注入 program。

### 5.2 IrisExtendedChunkVertexType

Angelica 定义了扩展顶点格式（包含 `mc_midTexCoord`、`at_midBlock`、`at_tangent`）。
Actinium 已经有 `ActiniumExtendedChunkVertexEncoder`，做同样的事。
把 Angelica iris pipeline 里引用 `IrisExtendedChunkVertexType` 的地方改为 Actinium 的实现。

### 5.3 RenderingState 的更新时机

Angelica 在 `MixinEntityRenderer` 里把投影矩阵和 modelView 矩阵写进 `RenderingState`。
替换为 `ActiniumCapturedRenderingState` 后，确保写入时机相同（在 `renderWorldPass` 的 HEAD）。

---

## 可以直接删掉的部分

Angelica iris 里以下功能在 Actinium 上不需要，直接删掉对应文件：

| 文件/目录                            | 原因                          |
|----------------------------------|-----------------------------|
| `compat/dh/`                     | VintageHorizons 后续单独实现，协议不同 |
| `texture/pbr/`                   | PBR 支持是长期目标，先不移植            |
| `gl/buffer/ShaderStorageBuffer*` | Compute shader 暂不支持         |
| `gl/image/`                      | Image load/store 暂不支持       |
| `gl/program/ComputeProgram`      | 同上                          |
| `fantastic/`                     | 粒子分阶段渲染，非核心功能               |
| `gui/`                           | GUI 可以先用 Actinium 现有的简单实现   |

删掉这些后，要移植的文件从 370 个降到约 **220 个**。

---

## 每天结束的检查点

| 阶段    | 检查点                                        |
|-------|--------------------------------------------|
| 阶段一结束 | `./gradlew compileJava` 通过（允许 stub 类存在）    |
| 阶段二结束 | 删掉所有 GLStateManager/RenderSystem stub，仍能编译 |
| 阶段三结束 | 删掉所有垫片类，仍能编译                               |
| 阶段四结束 | 游戏能启动，进入世界不崩溃（不要求光影正确）                     |
| 阶段五结束 | MakeUp 能加载，地形有基础着色                         |

---

## 风险点

**最高风险：阶段四的 Mixin 时序**

Angelica 的 `MixinEntityRenderer` 里有精确的 `@At(value="INVOKE", target=...)` hook 点，
1.7.10 和 1.12.2 的 `EntityRenderer` 字节码结构有差异，
这些 `ordinal` 和具体的调用目标需要一个个对照 1.12.2 反编译代码确认。
这是整个重构里最需要手工调试的部分，预计会有 2~3 个 hook 点需要反复测试。

**中等风险：BlendState / AlphaState 的读取**

Angelica iris 在若干地方保存和恢复混合状态（用于 shadow pass 前后），
替换为直接读 `GlStateManager` 内部状态后，需要确认 1.12.2 的 `GlStateManager`
的状态字段能通过 Actinium 已有的 accessor Mixin 正确读到。

**低风险：shaderpack 解析层**

`net/coderbot/iris/shaderpack/` 的 86 个文件几乎不依赖 MC API，
复制后改个包名基本就能编译，是整个重构里最稳定的部分。