# Chocolate Quest Repoured (CQR) 兼容性说明

兼容状态：**已验证**
最后更新：2026-09-05

## 模组信息

- Chocolate Quest Repoured 2.8.0B（modid `cqrepoured`）
- 触发 shader：jar 内 `assets/cqrepoured/shaders/sphere/`（CRLF 换行，`#version 110`），
  fragment 用 `textureCube(cubemap, gl_TexCoord[0].stp)` 采样 cubemap。

## 现象（修复前）

- 与 Actinium 共存时启动崩溃于 preinit，CQR 抛
  `RuntimeException: Failed to compile shader: 0`（issue #123）。

## 根因机制

glsm 的 `CompatShaderTransformer` 把模组 compat GLSL 转换为 core profile GLSL，底层使用
`org.taumc.glsl.ShaderParser`（glsl-transformation-lib）。该库文法把以下 6 个旧式采样函数
词法化为**关键字 token**，但 parser 不接受它们出现在函数调用位置：

```
texture1D, textureCube, texture2DRect, texture1DArray, texture2DArray, textureCubeArray
```

后果链：ANTLR 对语法错误**静默恢复**（只在 error listener 计数，不抛异常）→ Transformer
在破碎 AST 上序列化出畸形 GLSL（含 `<missing ';'>` 合成 token、uniform 声明错位进 `main`
函数体）→ 驱动（NVIDIA 实测）拒绝编译 → CQR 的 shader 编译检查失败，抛出上述崩溃。

其余旧式函数（texture2D/3D、texture*Proj/Lod/Grad 全系、shadow1D/shadow2D 全系、
texelFetch2D、textureSize2D）解析均正常，此前已走 post-parse 的
`renameFunctionCall`（`GlslTransformUtils.TEXTURE_RENAMES`）改名。

## 修复机制（glsm，`CompatShaderTransformer` / `GlslTransformUtils`）

1. **pre-parse 重命名**：`GlslTransformUtils.renameParseBreakingTextureFunctions` 用
   `\b<name>\b` 词边界正则把上述 6 个函数统一改为核心 `texture`，在
   `replaceTexture` 之后、`renameReservedWords` 之前执行（顺序约束：replaceTexture 先把
   裸 `texture` 标识符改名，本步新产生的 `texture(...)` 调用保持原名，parser 可正常接受）。
2. **`TEXTURE_RENAMES` 补全**：能解析但会漏进输出的旧式变体加入 post-parse 改名表——
   `texture1DProj`/`texture2DRectProj` → `textureProj`，`texture1DLod`/`textureCubeLod`/
   `texture1DArrayLod`/`texture2DArrayLod` → `textureLod`。
3. **Fail Fast 语法检查**：parse 返回后检查 `preParser()`/`parser()` 的
   `getNumberOfSyntaxErrors()`，任一非零即抛 `IllegalStateException`，由 `transform()`
   既有的 `catch (Exception)` 落入 `fixupVersion` 兜底（只修版本号、保持原文结构），
   杜绝"静默恢复 → 畸形输出 → 驱动拒编译"的崩坏链。
4. **shadow 包装扩展**：`renameAndWrapShadow` 的语义经字节码确认为纯「整体调用包
   `vec4(...)` + 改名」，无维度绑定，故从 shadow2D/shadow2DLod 扩展到
   `shadow1D` → `texture`、`shadow1DProj`/`shadow2DProj` → `textureProj`、
   `shadow1DLod` → `textureLod`（旧式 shadow 全系返回 vec4，现代 shadow sampler 的
   texture 系返回 float，vec4 包装语义一致）。

## 回归测试

`CompatShaderTransformerTest`（src/test）：

- `chocolateQuestRepouredSphereFragmentShaderTransformsToCoreProfile` /
  `...VertexShader...`：CQR 原版 shader 源码，Java 内显式构造 CRLF（`.gitattributes`
  全仓 LF 规范化，资源文件存不住 CRLF）；断言转换产物 ANTLR preParser/parser 语法错误
  均为 0、无 `<missing`、uniform 声明在 `main` 之前、`texture(cubemap,` 调用形态正确、
  FFP 内建完成 `actinium_*` 改名。
- `parseBreakingLegacyTextureFunctionsAreRenamedBeforeParsing`（参数化）：6 个
  parse-breaking 函数逐一走完整 transform。
- `syntacticallyInvalidShaderFallsBackToVersionFixup`：缺分号的必坏输入验证 Fail Fast
  落入 `fixupVersion` 兜底（版本号修正、结构保持原文）。
- `legacyTextureFunctionAloneTriggersTransformation`：shader 只含 `textureCube` 而无其他
  compat 内建时，转换仍被触发（`COMPAT_BUILTINS` 已补 `texture1D`/`textureCube`/`shadow1D`
  三个触发词，前缀匹配同时覆盖其 Array/Lod/Proj/Rect 变体）。
- `shadow1DFamilyIsWrappedAndRenamedLikeShadow2D`：shadow1D/shadow2DProj 的 vec4 包装与
  改名锁定。

## 验证记录

- 单元测试层：上述用例红→绿（修复前 8 个用例失败，CQR fragment 语法错误计数 9；
  修复后全部通过），`./gradlew check` 全量回归通过。
- dev 实机验证（2026-09-05）：`run/client` 以 unimined 受管依赖加载 CQR 2.8.0B +
  geckolib 3.0.31 + ReachFix 1.1.3（CQR 的 requiredMods），共存启动直达标题界面并持续
  渲染；`latest.log` 无 `Failed to compile shader`、无 CompatShaderTransformer 语法错误
  告警，SphereRenderer 着色器编译初始化成功（issue 原崩溃点）。注意 CQR 必须以受管依赖
  方式加载：直接往 mods/ 丢生产 jar 会因 dev 命名空间（MCP 名）与其 mixin refmap
  （SRG 名）不匹配而在 WorldServerMixin 阶段失败，与本修复无关。

## 注意事项

- 无已知遗留缺口：转换触发条件（`needsTransformation`/`COMPAT_BUILTINS`）已随修复补上
  `texture1D`/`textureCube`/`shadow1D`，纯函数场景（shader 只含旧式采样函数而无其他
  compat 内建）同样进入转换路径。
