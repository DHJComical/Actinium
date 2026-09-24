# Mobends / DragonCore 皮肤全白兼容性说明

兼容状态：**已验证**（根因、修复与 `./gradlew check`；实机确认玩家皮肤恢复彩色）
最后更新：2026-09-24

## 验证范围

- 版本：Mobends 1.0（元素之诗整合包，DragonCore 2.0.1 内置分发）、DragonCore 2.0.1
- 相关功能：玩家模型渲染的 display list 缓存路径、`ModelPart` 皮肤覆盖层
- 触发环境：元素之诗整合包（Cleanroom 0.6.13 + OptiFine 禁用），无光影
- 复现日志：玩家皮肤纯白无细节，`logs/latest.log`

## 症状：所有玩家皮肤渲染为纯白

### 现象

无光影下进服，所有玩家（含自己）皮肤渲染为**纯白无细节**。实体轮廓正常（几何、动作在），
只是贴图整体发白——典型的"采样到无纹理/纹理坐标丢失"表现。

### 机制（逐层定位）

1. dragoncore 的 `vfa`（玩家 Renderer，extends `RenderLivingBase`）接管玩家渲染；Mobends 的
   `ModelPart` 参与玩家模型（`ModelPlayer → ModelBiped → ModelPart`）绘制，用**原生 display list
   缓存**部件几何：`glNewList` 编译一次，之后 `glCallList` 回放。
2. Mobends `ModelPart.func_78785_a` 渲染逻辑：
   `renderPart(scale)`（正常模型）→ 读当前绑定纹理 → `renderPartSkin(scale)`（覆盖层）→
   `bindTexture(读到的值)` 恢复。恢复用的纹理 id 来自**反射读取 vanilla
   `GlStateManager.TEXTURES[activeTextureUnit].textureName`**（SRG `field_179174_p[i].field_179059_b`）。
3. Actinium 的 `GLSMRedirector` 把所有 `GlStateManager.bindTexture` **调用点**改写为
   `GLStateManager.glBindTexture`（GLSM 自己的实现），vanilla `bindTexture` 方法体在正常流程
   **从不执行** → vanilla 的 `TEXTURES[].textureName` 镜像**永不更新（恒 0）**。
4. Mobends 读到 0 → 覆盖层画完后 `bindTexture(0)` 把 unit0 **真实解绑** → 后续所有 player
   display list 回放时 unit0 无纹理 → FFP 无纹理变体 → **纯白**。
5. 不使用 Actinium 正常：vanilla `bindTexture` 正常更新镜像，Mobends 读回的是皮肤纹理 id。

### 关键证据（运行时探针）

- `DLIST-REPLAY`：玩家模型 display list 回放时 `unit0Binding=0`（38/40 次），且
  `cachedBind == actualBind == 0`（不是缓存失步，是真实解绑）。
- `ENDLIST`：首个 list 编译时 `unit0Binding=89`（doRender 已绑定皮肤），后续 list 全为 0——
  第一个 `renderPartSkin` 覆盖层执行 `bindTexture(0)` 后全局 unit0 被污染。

## 修复：同步 vanilla 纹理镜像

`GLSMRedirector` 只改调用点、不动 vanilla 方法体，因此 vanilla 镜像必须以另一路径被写入。

- **glsm** `GLSMHooks` 新增 `textureBindSyncCallback`（`TextureBindSyncCallback` 接口）：宿主
  在每次真实绑定时被回调，写入 vanilla 镜像。回调挂在 `GLStateManager.glBindTexture` 的
  **每次 GL_TEXTURE_2D 绑定**后（而非仅在 GLSM 缓存变化时——玩家每帧绑定同一张皮肤是缓存
  命中，只在变化时同步会让镜像永远停在 0，这正是早期版本修复失败的原因）。
- **根项目兼容层** `VanillaTextureMirrorCompat` 把
  `GlStateManager.TEXTURES` 数组解析一次（字段元素类型为包私有，`@Accessor` 与
  `findStaticGetter(Object[].class)` 均因类型失配不可用，改为 `Field` 解析 + `MethodHandle`
  句柄取值，热路径无反射开销）；`Actinium.onConstruct` 注册其创建的回调，元素经 mixin accessor
  `AccessorGlStateManagerTextureState`
  （`@Mixin(targets = "net/minecraft/client/renderer/GlStateManager$TextureState")`）写入
  `textureName`。
- 不能调用 vanilla `bindTexture` 本身：其方法体内的绑定调用同样被 `GLSMRedirector` 重定向回
  GLSM，形成无限递归（曾导致 `StackOverflowError`，见崩溃报告）。

## 后续维护

- 该机制与 CPU 上任何"反射读取 vanilla 纹理镜像"的第三方 mod 通用，不止 Mobends。
- 若未来 GLSM 提供镜像字段的统一维护（如直接在绑定层持有 vanilla 字段），此回调可拆除。
