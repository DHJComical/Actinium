# PBR 兼容性评估

最后更新：2026-07-31。

本文记录 Actinium 与 Iris、Sodium、Angelica 之间的 PBR 兼容性关系。评估以 Iris + Sodium 的完整 PBR 行为作为基准，不把 Sodium 单独视为 PBR 实现。

## 范围与基准

- `D:/Code/Iris`：PBR 资源加载、LabPBR 格式、sampler、shader transform，以及 Sodium 渲染后端的顶点扩展。
- `D:/Code/sodium`：高性能区块渲染和顶点缓冲后端；Sodium 本身不负责 `_n`、`_s`、LabPBR 或 PBR sampler。
- `D:/Code/Angelica`：Minecraft 1.12.2 运行时兼容层，提供 GLSM、Celeritas 和 Iris 相关 Mixin，包括 `ISpriteExt` 的实现。
- `D:/Code/Actinium`：将 Iris 风格 PBR 管线、Celeritas、GLSM 和 Angelica 兼容类组合到同一个模组中。

“源码覆盖率”表示实现了多少基准能力；“运行时可用性”还必须通过资源重载、动画 atlas、实体和方块实体场景验证。两者不能等同。

## 总结

按源码结构估计，Actinium 对 Iris + Sodium PBR 基准的整体覆盖约为 **75%–85%**：

- Terrain PBR 顶点路径约为 **85%–90%**，已经包含 tangent、normal、mid UV、`mc_Entity` 和 `at_midBlock` 等扩展。
- SimpleTexture、`normals` / `specular` sampler、MRT 和 LabPBR 基础逻辑基本齐全。
- Entity 与 Block Entity 缺少现代 Iris 中完整的实体顶点 serializer，复杂实体 PBR 的覆盖明显低于 terrain。
- Atlas PBR 的动画和生命周期存在阻断级风险，因此当前日志对应的端到端结果不能判定为可用。

与 Angelica 的关系不是普通的外部模组兼容：Actinium 直接复用了 Angelica 包名下的接口和底层类。当前问题是 Actinium 引用了 `ISpriteExt`，却没有在自己的运行时配置中应用 Angelica 提供的 `TextureAtlasSprite` Mixin。

## 能力矩阵

| 能力 | Iris + Sodium 基准 | Actinium 当前状态 | Angelica 关系 |
|---|---|---|---|
| SimpleTexture `_n` / `_s` | 完整支持 | 已有 `SimplePBRLoader`，风险较低 | 使用同一条 1.12 PBR 资源路径 |
| Atlas 静态 `_n` / `_s` | 完整支持 | 已实现，但部分 sprite 会因 `ISpriteExt` 转换失败而丢失 | Angelica 的条件 Mixin 能补上类型契约 |
| Atlas 动画 PBR | 完整支持 | 当前会触发客户端崩溃 | Angelica 提供动画状态接口，但旧版实现仍需验证空 metadata 场景 |
| LabPBR 通道与 mipmap | 完整支持 | 有离散 specular mipmap 生成器；采样过滤判断存在 bug | 同源代码需要同步检查 |
| `normals` / `specular` sampler | 完整支持 | 已接入动态 sampler | 由 Iris/GLSM bridge 提供运行时纹理 ID |
| MRT / `colortex8+` | 完整支持 | 日志已确认 `colortex8`–`colortex15` 创建成功 | 与 Angelica GLSM 状态管理耦合 |
| Terrain tangent / normal / mid UV | 完整支持 | 动态扩展顶点格式并按 shader 需求写入 | Angelica 使用固定扩展格式，Actinium 的动态选择更灵活 |
| Entity / Block Entity PBR 顶点 | 有完整 serializer | 未发现等价的现代实体 serializer，属于中低兼容 | 旧版 Angelica/Iris 路径也需要单独场景验证 |
| 资源重载与 atlas 生命周期 | 有明确清理路径 | PBR atlas 的 holder 清理与 GL 删除路径分离，存在残留风险 | 依赖 `AutoClosableAbstractTexture` 与 GLSM 删除回调 |

## 日志证据

分析日志：

`E:/Minecraft/Minecraft_Mod_Pack/externalgames/斯卡纳岛Future-Debug/logs/latest.log`

### Shader 管线已启动

日志启用了 `MC_NORMAL_MAP` 和 `MC_SPECULAR_MAP`，创建了 `colortex8` 到 `colortex15`，并记录了 shaderpack 加载完成：

```text
Total shaderpack load time ... 10852.9 ms
```

因此当前主要故障不是 shaderpack 无法编译，也不是 MRT 创建失败。

### 9 个 specular atlas sprite 加载失败

日志 `11925`–`11933` 共出现 9 条错误，全部来自 `_s.png`：

```text
TextureAtlasSprite cannot be cast to
com.gtnewhorizons.angelica.mixins.interfaces.ISpriteExt
```

Actinium 的 [AtlasPBRLoader.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/loader/AtlasPBRLoader.java#L176) 在 `syncAnimation` 中直接转换 `ISpriteExt`。Actinium 的 [mixins.actinium.iris.json](../src/main/resources/mixins.actinium.iris.json#L45) 只注册自己的 Iris sprite 和 texture map Mixin，没有注册 Angelica 的 `ISpriteExt` 实现。

Angelica 对应的实现位于 [MixinTextureAtlasSprite.java](D:/Code/Angelica/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/textures/MixinTextureAtlasSprite.java#L10)，并由 [Mixins.java](D:/Code/Angelica/src/main/java/com/gtnewhorizons/angelica/mixins/Mixins.java#L368) 在 Iris 或 Celeritas 启用时条件注册。

异常被 loader 捕获后会丢弃对应 PBR sprite，所以这 9 个资源不会进入 specular atlas。

### PBR 动画触发客户端崩溃

日志 `11953` 开始出现：

```text
IndexOutOfBoundsException: Index 0 out of bounds for length 0
```

调用链是：

```text
AnimationMetadataSection
  -> TextureAtlasSprite.updateAnimation
  -> PBRAtlasTexture.cycleAnimationFrames
  -> PBRAtlasHolder.cycleAnimationFrames
  -> TextureMap.updateAnimations
```

Actinium 在没有动画 metadata 时创建空 frame 列表，见 [AtlasPBRLoader.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/loader/AtlasPBRLoader.java#L109)。随后 [PBRAtlasTexture.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/PBRAtlasTexture.java#L47) 仅判断 metadata 是否为非空，空 frame 列表仍会被加入 `animatedSprites`，最终在 [PBRAtlasTexture.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/PBRAtlasTexture.java#L130) 更新第 0 帧时崩溃。

### 其他日志 warning

GLSL lexer warning、未初始化 fragment 输出和 Distant Horizons 重复 uniform 目前没有证据是 PBR 崩溃的直接原因。它们应在 atlas 问题修复后再单独回归。

## 已确认的源码风险

### 1. `ISpriteExt` 契约未闭合（修复前）

Actinium 只复用了接口引用，没有保证所有 `TextureAtlasSprite` 实例实现该接口。应当二选一：

1. 在 Actinium 自己的条件 Mixin 中提供同等实现；
2. 去除对 Angelica 私有接口的硬转换，改用 Minecraft 原生 animation metadata 或 Actinium 自己的接口。

### 2. 空动画 metadata 被当成有效动画（修复前）

静态 PBR 资源应保持 `null` animation metadata，或者在加入 `animatedSprites` 前确认 frame count 大于 1。只判断 `hasAnimationMetadata()` 不足以保证 `updateAnimation()` 安全。

### 3. PBR atlas 生命周期可能残留（修复前）

[PBRTextureManager.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/PBRTextureManager.java#L129) 释放纹理时调用 `deleteGlTexture()`，而 [PBRAtlasTexture.java](../shader/src/main/java/net/coderbot/iris/texture/pbr/PBRAtlasTexture.java#L139) 的 `close()` 才会清除 `TextureMap` 中的 atlas holder。重载或删除纹理后，旧 atlas 可能仍被 `TextureMapIrisMixin` 更新。

### 4. LabPBR mipmap 判断错误（修复前）

[TextureFormat.java](../shader/src/main/java/net/coderbot/iris/texture/format/TextureFormat.java#L52) 使用：

```java
boolean mipmap = (minFilter & 1 << 8) == 1;
```

按位与结果是 `0` 或 `256`，不会等于 `1`。因此 LabPBR specular texture 即使有 mipmap，也可能退化为 `GL_NEAREST`，影响远距离采样质量。

## PBR 补齐实现状态

当前代码阶段已完成以下修复：

- Atlas loader 不再依赖 Angelica 的 `ISpriteExt` 硬转换，改用原生 animation metadata。
- 静态 PBR 资源不再创建空 animation metadata；动画 metadata 会归一化为明确的 frame 列表。
- PBR atlas 按动画帧切分并分别生成 mipmap，避免整张动画图被当成单帧上传。
- PBR atlas 关闭时先清理 `TextureMap` holder，再删除 GL texture，避免资源重载后继续更新旧 atlas。
- LabPBR mipmap 过滤判断已改为正确的非零位掩码判断。
- 视频设置 Debug 页面新增 `PBR Texture Debug`，配置持久化到 `actinium-options.json`，并支持 `-Dactinium.pbrDebug` 覆盖。
- 新增限量 PBR DEBUG，覆盖资源发现、frame 数量、atlas 上传和关闭事件；atlas 导出仍受同一开关控制。

上述内容已通过 IDE 静态检查，尚未启动 `runClient`。实际客户端验证需要由用户从 IDEA 启动；`run` 下不兼容 1.12.2 的材质包日志不纳入判断。

## 兼容性判定

### 源码覆盖率

| 范围 | 判定 |
|---|---|
| PBR 资源 loader、sampler、MRT、基础格式 | 高，约 80%–90% |
| Terrain PBR 顶点数据 | 高，约 85%–90% |
| Entity / Block Entity PBR | 中低，约 60%–70% |
| 整体源码能力 | 中高，约 75%–85% |

### 当前日志对应的运行时

| 场景 | 判定 |
|---|---|
| Shaderpack 启动与 MRT | 通过 |
| SimpleTexture PBR | 代码路径存在，需单独回归 |
| 静态 atlas PBR | 部分可用，至少 9 个 `_s` sprite 丢失 |
| 动画 atlas PBR | 失败，会崩溃 |
| 资源重载后 atlas 状态 | 未通过验证，存在生命周期风险 |

因此日志所对应的旧版本不应宣称“完整兼容 Iris + Sodium PBR”。当前代码已补齐 atlas 动画、metadata、生命周期和 LabPBR 过滤修复，但仍需由 IDEA `runClient` 完成实际材质包回归后，才能确认端到端兼容性。

## 修复与验证优先级

1. **已完成：** 修复空动画 metadata 导致的 `IndexOutOfBoundsException`，确保静态 PBR sprite 不进入动画更新集合。
2. **已完成：** 移除 `ISpriteExt` 硬转换，直接使用原生 animation metadata。
3. **已完成：** 统一 PBR atlas 的 `close()`、GL 删除和 `TextureMap` holder 清理路径。
4. **P1：** 增加现代 Iris 类似的 Entity/Block Entity 顶点 serializer，验证 tangent、mid UV 和实体 ID。
5. **P2：** 修复 LabPBR mipmap 位掩码，并做远距离 specular 采样回归。

建议的回归场景至少包括：静态 `_n`、静态 `_s`、带和不带 `.mcmeta` 的 atlas sprite、普通动画 atlas、PBR 与基础纹理帧数不一致、资源重载、terrain、entity、block entity，以及删除纹理后的下一帧动画更新。
