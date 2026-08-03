# Eclipse 体积云远平面截断问题

最后更新：2026-08-03

## 背景

Eclipse shader pack 在天空方向出现“准心周围圆盘内只有蓝天、体积云缺失”的问题。
该问题不是 `centerDepthSmooth`、深度采样或后处理叠加导致，而是 `GetVolumetricClouds`
的云 ray march 在进入云层前被距离边界截断。

## 根因

`Eclipse-Shader-Unstable/shaders/lib/volumetricClouds.glsl` 中的关键逻辑：

```glsl
float maxdist = far + 16.0 * 5.0;
float lViewPosM = length(viewPos) < maxdist ? length(viewPos) - 1.0 : 100000000.0;
```

`raymarchCloud` 会在 `length(newPos) > referenceDistance` 时提前退出。

Actinium 的 `far` uniform 为 `renderDistanceChunks * 16`。1.12.2 的实际投影远平面也处于
同一量级，因此天空像素重建出的 `length(viewPos)` 小于 `maxdist`，Eclipse 使用
`length(viewPos) - 1.0` 作为 ray 截断距离。Eclipse 云层默认在 500-680 高度，地面视角下
云 ray 起点距离通常已经超过该截断值，采样点还没到云层就被 `break`。

圆盘跟随准心是因为远平面 `viewPos` 长度在屏幕中心最小、离中心越远越大，中心区域最容易被截断。

## 为什么上游 Iris / Sodium 不出现

- Sodium 不执行 Iris shader pack 的 GLSL，不参与体积云 ray march。
- 现代 Iris 的 `far` uniform 同样使用 `renderDistanceChunks * 16`，但实际投影远平面和
  shader 重建出的 `viewPos` 长度通常更大，Eclipse 会直接走 `100000000.0` 分支。
- Iris 曾提供 `MixinTweakFarPlane` 来对齐 OptiFine 的投影远平面行为。
- Actinium 的 1.12.2 管线下没有这一差异，因此触发 Eclipse 自己的 early-out。

## 可选方案：同步投影远平面

可以在 `EntityRenderer.setupCameraTransform` 的 `gluPerspective` far 参数上按 OptiFine
风格调整实际投影远平面，使 `length(viewPos) > maxdist`。这样 Eclipse 会自然进入无截断分支。

该方案会影响：

- `gbufferProjection` / `gbufferProjectionInverse`
- 地形 frustum 和远平面深度精度
- fog、Distant Horizons、手部投影
- 其他 shader pack 的远平面相关行为

影响面较大，需要独立回归，不应作为 Eclipse 单一问题的首选修复。

## 已采用方案

在 `CompatibilityTransformer` 中重写 `lViewPosM`：

```glsl
float lViewPosM = length(viewPos) >= far - 1.0 ? 100000000.0
    : length(viewPos) < maxdist ? length(viewPos) - 1.0 : 100000000.0;
```

该补丁只对接近远平面的天空 ray 放开截断，仍保留普通地形像素的原距离裁剪语义。

## 验证

- `gradlew :test --no-daemon` 通过。
- 客户端实测中，补丁前 `colortex10` 中心 alpha 约为 `0.98`；补丁后约为 `0`，中心空洞消失。
- Celeritas 的 vertex/geometry 地形修复及测试保留。
