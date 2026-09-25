# ZGC 下的 C2 失控编译：精灵透明度扫描导致原生内存耗尽

最后更新：2026-09-21。修复分支：`fix/zgc-sprite-c2-runaway`。

## 症状

客户端进程的原生内存在数分钟内持续上涨（实测 **~200 MB/s**），Windows 提交量被打满后 JVM 以

```text
Native memory allocation (malloc) failed to allocate ... bytes. Error detail: Chunk::new
#  Out of Memory Error (arena.cpp:186)
```

中止（`hs_err_pid*.log`）。在崩溃之前表现为掉帧、`Can't keep up! Running ...ms behind`、
窗口"未响应"——与用户上报的"未响应/卡死"是同一现象。

只在 **ZGC** 下出现：同一环境改 **G1** 后完全正常。
只在安装 **Actinium** 时出现：与之对照的"无 Actinium、同样 9 个加载器基础设施"环境不出现。

## 环境

- Zulu 25.0.3（`-XX:+UseZGC -XX:+UseCompactObjectHeaders -Xms4096m -Xmx8192m`），Windows 11
- Cleanroom 0.6.13-alpha，LWJGL 3.4.1
- 光影：Complementary Reimagined r5.9.3 + Euphoria Patches 1.10.5（该问题与光影无关，见下表 4/5 行）

## 证据链

| # | 条件 | 编译次数 | NMT `Compiler` | 结果 |
| --- | --- | --- | --- | --- |
| 1 | 全包 + ZGC | 70,551 | 21 → 29 GB | 原生 OOM 崩 |
| 2 | 全包 + ZGC（另一轮） | 51,649 | arena peak 24.2 GB | commit 38 GB 崩 |
| 3 | 全包 + **G1** | 68,401 | 不增长 | 稳定 ~6 GB |
| 4 | 仅 Actinium + 基础设施 + ZGC | 43,427 | 1.18 GB（`Arena Chunk` 19.2 GB） | 2 分钟断气 |
| 5 | 同上，**无光影** | 36,885 | 8.75 → 14.8 GB / 30 s | 进世界即涨 |
| 6 | **无 Actinium**（同基础设施）+ ZGC | 31,564 | **1 MB**（`Arena Chunk` 8.7 MB） | 稳定 |

4/5 两行说明光影不是触发源（无光影时涨得更快）；3 行说明与 ZGC 强相关；6 行说明必须有 Actinium。

`-XX:+LogCompilation` 的 XML 里（第 2 行那次），唯一"开始却从未结束"的长任务就是：

```text
compile_id=18490  stamp=38.503s（日志结束于 161s，仍未结束）
net.minecraft.client.renderer.texture.TextureAtlasSprite::
  embeddium$processTransparentImages (SpriteTransparencyLevel;[IZ)SpriteTransparencyLevel;
```

而已完成编译的最长耗时是 12.3 s（OTG `plotStructures`），≥5 s 的只有 6 个。三份 `hs_err` 的崩溃线程
都是 `C2 CompilerThreadN`，其 `Current CompileTask` 即该方法。

**单变量验证**：只加

```text
-XX:CompileCommand=exclude,net.minecraft.client.renderer.texture.TextureAtlasSprite::embeddium$processTransparentImages
```

后，同一环境 `Compiler` 全程 1 MB、总量稳定 5.6 GB（对照：不加时 30 秒涨 6 GB）。

## 根因

`MixinTextureAtlasSprite#processSprite` 在 `generateMipmaps` 阶段逐精灵扫描像素，循环体内
**每个像素调用一次多态方法** `level.chooseNextLevel(...)`（`SpriteTransparencyLevel` 的接收者类型
在迭代间变化）。ZGC 会在 C2 的 IR 中插入 load barrier，这个形状使 C2 进入失控编译：**一次编译
持续向自己的 arena 分配内存且永不结束**，累积到 20+ GB 后 JVM 在 `Chunk::new` 处分配失败中止。
G1 没有该 barrier 故不受影响；没有 Actinium 就不存在这个方法。

## 修复

`embeddium$processTransparentImages` 在循环内只累计透明度等级的 **int 序号最大值**，循环结束后
一次性映射回枚举常量：`chooseNextLevel` 本就只取序号较大者，因此**行为完全等价**，而枚举与多态
调用被彻底移出像素循环。改动文件：

- `src/main/java/com/dhj/actinium/mixin/vintage/features/mipmaps/MixinTextureAtlasSprite.java`

对仍在使用旧版 Actinium 的用户，可用上面的 `-XX:CompileCommand=exclude,...` 临时规避；
该处置同样适用于其它触发同类 C2 病理形状的模组。

## 验证记录（2026-09-21）

- [x] `.\gradlew.bat check --no-daemon` 与 `build --no-daemon` 通过。
- [x] 隔离环境（Actinium + 加载器基础设施 + ZGC + 无光影，**不加** `CompileCommand`）：
      进世界 2 分 43 秒，`Compiler` 1.0 MB、`Arena Chunk` 1–17 MB、
      Java Heap 4.19→5.11 GB、总量 5.6→6.5 GB，无崩溃、无 `hs_err`。
- [ ] 全包 + 光影的回归（贴图/mipmap 视觉、半透明渲染与长时间游玩）待补。

## 上游上报要点

复现要素：Zulu 25.0.3 + ZGC（Windows）。让 C2 编译一个"像素循环内每轮多态调用"的小方法，
编译即进入失控状态并持续分配 arena 内存（NMT `Compiler arena` 线性增长，`Arena Chunk` 同步），
最终在 `Chunk::new` 分配失败中止；G1 不可复现；`-XX:CompileCommand=exclude` 可规避。
可用的独立观测量：`-XX:+LogCompilation` 中出现长期不闭合的 `<task>`，以及
`jcmd <pid> VM.native_memory summary` 里 `Compiler` 分类的 `arena=` 持续增长。
