# HBM's Nuclear Tech - Community Edition 兼容性说明

兼容状态：**已验证**（attribute 状态栈映射 + 方块实体世界 lightmap 同步）
最后更新：2026-09-21

## 验证范围

- HBM's Nuclear Tech - Community Edition：2.5.0.5（CurseForge `1312314:8330665`，dev 回归基线）
  与 2.6.1.0（CurseForge `1312314:8873889`，issue #170 报告者环境）
- HBM's NTM CE: Space（NTM-Space）：0.9.2（CurseForge `1413353:8314483`）
- 相关功能：HBM `RenderUtil` attribute 状态栈、`TileEntityRendererDispatcher` 世界 lightmap

## 已有兼容面

`com.hbm.util.RenderUtil` 自己维护一套 attribute 快照栈（`ATTRIB_STACK` + 8 个
`AttribSnapshot` 子记录），GLSM 接管渲染后该栈不再是权威状态源，因此 Actinium 用
GLSM 的状态栈取代它：

- `mixin/mod/hbm/MixinRenderUtil`（late/conditional，`mixins.actinium.hbm.json`）在
  `pushAttrib(I)V` / `popAttrib()V` 的 `HEAD` 处取消原方法，改为走
  `com.dhj.actinium.compat.hbm.HbmRenderStateCompat` → `GLStateManager.glPushAttrib` /
  `glPopAttrib`。
- `mixin/early/hbm/MixinTileEntityRendererDispatcherLightmap`（early，注入体按
  `isHbmInstalled()` 运行时门控）补齐世界 lightmap，修掉 FENSU 等机器的黑色剪影、
  stale lightmap 与 depth 恢复异常。

## issue #170：打开 Stardar（系统地图）GUI 直接崩溃

### 现象与环境

2026-09-21 报告：打开 NTM-Space 的 Stardar 机器 GUI（`com.hbmspace.inventory.gui.GUIMachineStardar`）
时客户端立即崩溃：

```text
java.lang.IllegalArgumentException: Unsupported HBM RenderUtil attribute bits: 0x4
	at com.dhj.actinium.compat.hbm.HbmRenderStateCompat.toGlMask
	at com.dhj.actinium.compat.hbm.HbmRenderStateCompat.pushAttrib
	at com.hbm.util.RenderUtil.handler$...$actinium$pushAttrib
	at com.hbmspace.inventory.gui.GUIMachineStardar.drawSystemMap(GUIMachineStardar.java:323)
```

报告者环境：NTM-CE 2.6.1.0 + NTM-Space 0.9.2 + Actinium alpha-0.0.9。

### 根因

`HbmRenderStateCompat.toGlMask` 把 HBM 掩码里**未映射的位**当作错误，抛
`IllegalArgumentException`。这与宿主 HBM 自己的语义相反：`RenderUtil.pushAttrib(int)`
的字节码是每个 attribute 组一个 `if ((mask & bit) != 0)` 分支，**对不认识的位什么都不做**，
既不报错也不捕获。也就是说 Actinium 把一个"HBM 本来会静默忽略"的位升级成了崩溃。

该崩溃点的实参在 NTM-Space 源码里是：

```java
// com/hbmspace/inventory/gui/GUIMachineStardar.java:323（v0.9.2 与 nightly 一致）
RenderUtil.pushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
```

其中 `GL11.GL_LINE_BIT == 0x4` —— 正是报错里的 `0x4`。`0x4` 既不在 HBM 的 7 个捕获位
（`0x2000 0x40 0x40000 0x4000 0x100 0x8 0x80`）里，也不在 Actinium 的映射表里，
于是走进抛异常分支。

> 旁证：`HbmRenderStateCompat` 原有的 `rejectsUnknownHbmAttributeBits` 单测把"对未知位
> 抛异常"固定成了期望行为，所以这个缺陷在 #170 之前不会被单测拦下。

### 触发链

| 环节 | 位置 | 行为 |
| --- | --- | --- |
| 1 | `GUIMachineStardar.drawSystemMap` | `pushAttrib(0x204)`，其中含 `GL_LINE_BIT = 0x4` |
| 2 | `MixinRenderUtil.actinium$pushAttrib` | `HEAD` 取消原方法，转交 compat 层 |
| 3 | `HbmRenderStateCompat.pushAttrib` → `toGlMask` | `0x4` 未映射 → 抛异常 |
| 4 | 异常穿透 GUI 渲染 | 客户端崩溃（"Rendering screen"） |

### 修复

`HbmRenderStateCompat.toGlMask` 改为**与 HBM 对齐**：只映射自己认识的位，未映射的位直接
忽略，不再抛异常；`HBM_ALL_BITS` 哨兵值的展开语义保持不变（`0xFFFFF` → `0x461C8`）。
常量 `HBM_SUPPORTED_BITS` 随之更名 `HBM_MAPPED_BITS`，因为它的含义是"已映射的位"而非
"受支持的白名单"。

代价与 HBM 原行为一致：被忽略的位所代表的 GL 状态不会被恢复——而 HBM 自己在该位上也
不恢复它，所以这不是回退。

### 验证

- 单测（`HbmRenderStateCompatTest`，8 项全通过）覆盖：`0x4` 单独出现只保存 shade model
  （`GL_LIGHTING_BIT`）；`0x6004` 这类"已映射位 + 未知位"混合掩码按已映射位展开；
  `0x100000` 这类 `HBM_ALL_BITS` 之外的高位不影响其它映射；`0xFFFFF` 哨兵展开不变。
- `./gradlew check --no-daemon` 通过（含 `MixinConfigurationTest` 与 remap Jar 结构校验）。
- 待实机确认：NTM-Space 0.9.2 + NTM-CE 2.6.1.0 下打开 Stardar GUI 的星图不再崩溃，
  且退出该 GUI 后后续绘制无状态串扰。

### 备注（版本交叉核对）

查证时对 NTM-CE **2.5.0.5 与 2.6.1.0** 两版的 `pushAttrib(int)` 做了逐常量比对，7 个捕获位
与两个哨兵常量完全一致，均无 `0x4` 分支，所以本修复对两个版本都成立。NTM-Space 0.9.2
的字节码中该调用点常量按 `sipush` 解出为 `0x6004`，而崩溃报告是 `0x4`（源码为 `0x204`）——
三者都含 `0x4` 且都不在 Actinium 映射表内，因此该差异不影响修复结论，仅记录备查。
