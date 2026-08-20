# TC4 Research Port: Reborn 渲染兼容

最后更新：2026-08-20。相关 issue：[Actinium #74](https://github.com/DHJComical/Actinium/issues/74)。
验证分支：`research/issue74-tc4-research-port-fps`。

## 模组与源码入口

目标模组为 TC4 Research Port: Reborn，CurseForge 坐标为
`curse.maven:oldresearchreborn-1632015:8642028`，运行时 mod id 为 `oldresearch`，版本为
`1.0.1-release`。它依赖 Thaumcraft 和 Baubles，因此开发运行环境同时加入：

- `curse.maven:thaumcraft-223628:2629023`
- `curse.maven:baubles-227083:2518667`

Old Research 自带的旧版 Tessellator 位于
`com.wonginnovations.oldresearch.tc4legacy.client.Tessellator`。它保留了自己的
client-array 绘制路径，不能直接使用 Actinium 注入到 vanilla Tessellator 的路径。

## 问题与证据

Issue #74 的复现结果是：正常 GUI 约 850 FPS，打开包含 Old Research 绘制内容的 GUI 后下降到
约 16 FPS；随后客户端线程停止响应。

DEBUG 计时器确认调用链已进入兼容层，并在容量准备阶段停止：

```text
[DEBUG-OR74] enter drawing=true vertices=4 rawInts=32 rawSize=65536 mode=7
[DEBUG-OR74] ... after-vertex-size value=24
[DEBUG-OR74] before-capacity requiredBytes=96
```

`requiredBytes=96` 对应 4 个顶点和 24-byte vertex format，说明问题发生在上传前的 repack buffer
容量准备，而不是 Old Research 的顶点数据解析。

## 根因

`MixinSplashProgress.finish()` 会调用 `TessellatorStreamingDrawer.destroy()`。原实现释放
repack buffer 后将 `repackCapacity` 设为 `0`。Old Research GUI 在 splash 结束后第一次绘制时
调用 `ensureRepackCapacity(96)`，原扩容逻辑从当前容量开始翻倍：

```java
int newCapacity = repackCapacity;
while (newCapacity < requiredBytes) {
    newCapacity *= 2;
}
```

当 `newCapacity` 为 `0` 时，循环永远保持为 `0`，Client thread 因此无限循环，表现为 GUI 卡死。

## 修复

- `TessellatorStreamingDrawer` 使用原有的 64 KiB 初始容量作为 destroy 后的懒重建下限；
- `ensureRepackCapacity` 在 buffer 已被 destroy 后重新分配 buffer，并避免对 null buffer 重复释放；
- 将容量计算提取为 `nextRepackCapacity`，覆盖 destroy 后从 0 开始和正常的 64 KiB 到 128 KiB 扩容；
- Old Research 的 `Tessellator.draw()` 由条件 Mixin 转发到
  `OldResearchTessellatorCompat`，再复用 Actinium streaming drawer；
- `[DEBUG-OR74]` 临时探针已移除，Old Research Mixin 仅在 `oldresearch` 已加载时启用。

## 性能优化

- 常见的 `POSITION_COLOR` 和 `POSITION_TEXTURE_COLOR` legacy raw layout 直接批量打包到
  native `IntBuffer`，避免每个属性分别调用通用 writer；`POSITION` 和
  `POSITION_TEXTURE` 也使用同一条快路径。
- 单个矩形 quad 在位置/UV 构成平行四边形且颜色、法线、亮度等平坦属性保持不变时，使用
  `GL_TRIANGLE_FAN` 绘制，绕过共享 quad EBO；不满足条件的 quad 继续使用原有
  `QuadConverter`，保留原始 provoking-vertex 语义。
- 相关条件和 packed 输出由 `TessellatorStreamingDrawerTest` 直接调用实现验证。

## 验证状态

已完成：

- 定向测试：`TessellatorStreamingDrawerTest` 验证 destroy 后 `0 -> 64 KiB` 不会进入死循环，
  以及正常容量扩容；
- `.\gradlew.bat :test --tests com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawerTest --no-daemon`；
- `.\gradlew.bat check --no-daemon`；
- `.\gradlew.bat build --no-daemon`；
- Mixin 配置、late loader、remap Jar 和模块边界检查均通过；
- 构建产物包含 Old Research 兼容类和 `mixins.actinium.oldresearch.json`。
- 性能优化后的研究笔记 GUI FPS 尚待在同一客户端场景复测；当前已知基线为普通 GUI 约
  800 FPS、放入研究笔记后约 400 FPS。

待完成：

- 使用性能优化后的客户端再次打开 Old Research GUI，确认不再卡死；
- 记录优化后的 GUI FPS，并与当前约 400 FPS 的基线对比；
- 在实际运行环境确认 Old Research GUI 内的研究树、节点图标和 Thaumcraft 相关绘制无视觉回归。

## 来源

- [Actinium issue #74](https://github.com/DHJComical/Actinium/issues/74)：FPS 下降和客户端无响应的复现描述；
- [dependencies.gradle](../../gradle/scripts/dependencies.gradle)：Old Research、Thaumcraft 和 Baubles 的
  CurseForge Maven 坐标；
- [MixinOldResearchTessellator.java](../../src/main/java/com/dhj/actinium/mixin/mod/oldresearch/MixinOldResearchTessellator.java)：
  Old Research Tessellator 的目标类、字段映射和 draw 注入；
- [TessellatorStreamingDrawer.java](../../glsm/src/main/java/com/gtnewhorizons/angelica/glsm/streaming/TessellatorStreamingDrawer.java)：
  repack capacity 生命周期和修复后的懒重建逻辑；
- 本地运行证据：`run/client/logs/debug.log` 中的 FML mod 元数据、Mixin 应用记录以及
  `[DEBUG-OR74]` 计时器输出（当前运行目录属于本地验证产物，不纳入 Git）。
