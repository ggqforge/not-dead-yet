# Not Dead Yet · 不死图腾名称显示

> 「还不死」—— 不死图腾被铁砧改名后，触发保命动画时把名字**直接贴在动画中的图腾正中央之前**，
> 用原版像素字体与平滑渐变绘制。

| 项目 | 值 |
| --- | --- |
| 模组名 | **Not Dead Yet**（取「还不死」之意，也是英语里那句 "I'm not dead yet!"） |
| 模组 id | `notdeadyet` |
| Minecraft | **1.21.1** |
| 加载器 | Fabric（`fabric-loader >= 0.16`） |
| 依赖 | **无**（纯 Mixin，不需要 Fabric API） |
| Java | 21 |
| 环境 | **纯客户端**，服务器不用装 |

> 面向玩家的纯文本用法说明在 [`使用说明.txt`](使用说明.txt)（UTF-8 with BOM + CRLF，
> 双击即可用记事本正常打开，可直接随 jar 一起分发）。本 README 偏实现细节。

---

## 1. 效果

1. 用铁砧把不死图腾改名，例如 `第二条命`、`应急[尺寸:2]`、`应急[颜色:红色;尺寸:2]`。
2. 带着它被致命伤害打死，图腾触发保命动画。
3. 动画播放期间，名字就画在**图腾的正中央、图层在它前面**，并且**字号全程跟着图腾缩放走**。

未改名的普通图腾**不会**显示任何文字，避免每次保命都冒出「不死图腾」四个字。

4. 凡是**显示名字**的地方都**不显示方括号设置块**，而且颜色和动画完全一致
   （没指定颜色就是预设的图腾金）：

   - 物品栏里鼠标悬停时的 tooltip
   - 快捷栏上方切换物品时弹出的名称

> 也就是说 `应急[颜色:红色;尺寸:2]` 在 tooltip 和快捷栏名称里都是红色的「应急」两个字。
> 实现方式见 §5.6 —— 那里有个**很容易踩坏铁砧的陷阱**。

| 名字 | 显示效果 |
| --- | --- |
| `第二条命` | 图腾金 `#EADB84` + 阴影（预设色） |
| `应急[尺寸:2]` | 预设金，字号 2 倍 |
| `应急[颜色:红色;尺寸:2]` | 原版红，字号 2 倍 |
| `凤凰[color:red;size:2]` | 同上，键值写英文也行 |
| `神像[c:#FF8800]` | 十六进制颜色 |
| `朝霞[c:红,橙,黄]` | 平滑渐变 |
| `垂帘[c:y红,蓝]` | 纵向渐变 |
| `斜辉[c:t红,黄,绿]` | 斜向渐变 |
| `虹光[c:_jeb]` | 彩蛋：流动彩虹 |

> 全部都能在**铁砧里直接打字**。注意 `§` 不行——`AnvilScreenHandler` 会把 `§` 滤掉，
> 所以本模组用方括号设置块代替它，详见 §5.4.1 与 §5.5.1。

---

## 2. 安装

**前置：必须先装 Fabric Loader 1.21.1。** 只装原版 1.21.1 直接丢 jar 进 `mods` 是**不会加载**的。

二选一：

- **PCL2**：版本设置 → 安装新版本 → 选 `Fabric 1.21.1`，装好后启动一次；
- **官方安装器**：https://fabricmc.net/use/installer/ ，选 1.21.1 → Install。

然后：

1. 新建/进入 `.minecraft\mods`；
2. 把 `build/libs/not-dead-yet-1.0.0+mc1.21.1.jar` 放进去；
3. 用 Fabric 1.21.1 配置启动游戏。

```
.minecraft/mods/
└── not-dead-yet-1.0.0+mc1.21.1.jar     ← 只需要这一个，没有前置
```

> 本模组是纯客户端模组，服务器不需要装，也不会因为服务器没装而失效。

---

## 3. 自己构建

需要 **JDK 21**（不是 JRE）。工程自带的 `gradlew.bat` 会自动探测 JDK
（优先 `JAVA_HOME`，其次常见安装路径，并确认 `bin\javac.exe` 存在），
并把 `GRADLE_USER_HOME` 指到工作区的 `.gradle-home` 以避开 `%USERPROFILE%\.gradle` 不可写的问题。

```powershell
cd totem-name-display
.\gradlew.bat              # 构建 jar
.\gradlew.bat runClient    # 启动带模组的客户端调试
.\gradlew.bat clean build  # 清理后重建
```

产物在 `build/libs/`：

```
build/libs/not-dead-yet-1.0.0+mc1.21.1.jar          ← 丢进 mods
build/libs/not-dead-yet-1.0.0+mc1.21.1-sources.jar  ← 源码，不用管
```

> 首次构建要下载 Minecraft、Yarn 映射、Loom 与反编译工具，需联网，约 0.4 GB 缓存，
> 耗时可达十几分钟；之后增量构建很快。

> **在 DSH 沙箱里构建时要放到「完全权限」。** Windows 沙箱会让 Java 的
> `Files.isWritable` 对任何文件都返回 false（实测：对已存在且写入成功的文件也返回 false），
> 而 Loom 的 `remapJar` / `remapSourcesJar` 通过 Java 的 zipfs 写 jar，
> zipfs 构造时执行 `readOnly = !Files.isWritable(zfpath)`，于是必然判定只读并报
> `ReadOnlyFileSystemException` / `can't be written`。这不是代码问题，
> 在普通命令行或 IDE 里直接构建不会遇到。

`gradle/wrapper/` 下保留了 Gradle 官方 wrapper（含 SHA-256 校验和），IDE 导入工程时会自动识别。

---

## 4. 代码结构

```
src/main/
├── java/com/example/notdeadyet/
│   ├── TotemNameDisplay.java            客户端入口，只打一行加载日志
│   ├── client/
│   │   ├── TotemAnimationMath.java      原版动画运动学（§5.1 那段代码的逐行对照）
│   │   ├── TotemNameText.java           解析名字：末尾 [设置块]，含颜色/渐变/方向/字号
│   │   ├── TotemGradient.java           渐变 / 彩虹的采样函数 + 共用相位
│   │   ├── GradientVertexConsumer.java  顶点级刷色，做出平滑渐变（§5.5.2）
│   │   ├── TotemTooltip.java           名字显示的统一入口：剥设置块 + 同色上色（§5.6）
│   │   └── TotemNameOverlay.java        把文字画到图腾正中央之前
│   └── mixin/
│       ├── GameRendererMixin.java       @Inject 进 renderFloatingItem
│       ├── InGameHudMixin.java          @Redirect 快捷栏上方的物品名称（§5.6）
│       └── ItemStackMixin.java          @Inject 进 ItemStack.getTooltip（§5.6）
└── resources/
    ├── fabric.mod.json                  元数据，版本号由 gradle.properties 注入
    └── notdeadyet.mixins.json            Mixin 配置（refmap 由 Loom 自动生成）
```

六个类。整个模组只有**三处** Mixin 注入，都不含 `@Overwrite`，冲突面很小。

---

## 5. 实现要点

### 5.1 原版图腾动画的真实代码

常见误解是「图腾保命会在世界里生成粒子/实体」。**1.21.1 里它不是世界渲染，而是纯 HUD 绘制。**
把 Loom 缓存里 yarn 命名后的 Minecraft jar 用 `javap -p -c` 反汇编，
`GameRenderer` 里那条通路还原成 Java 是这样的：

```java
// 客户端收到 EntityStatusS2CPacket(status = 35) 时：
public void showFloatingItem(ItemStack stack) {
    this.floatingItem = stack;
    this.floatingItemTimeLeft = 40;                                  // 计时器 40 tick
    this.floatingItemWidth  = this.random.nextFloat() * 2.0F - 1.0F; // [-1, 1]
    this.floatingItemHeight = this.random.nextFloat() * 2.0F - 1.0F; // [-1, 1]
}

// 此后每帧（在 if (!options.hudHidden) 之内、InGameHud.render 之前）：
private void renderFloatingItem(DrawContext context, float tickDelta) {
    if (this.floatingItem == null || this.floatingItemTimeLeft <= 0) return;

    int i = 40 - this.floatingItemTimeLeft;
    float p  = ((float) i + tickDelta) / 40.0F;
    float p2 = p * p;
    float p3 = p * p2;
    float k  = 10.25F * p3 * p2 - 24.95F * p2 * p2 + 25.5F * p3 - 13.8F * p2 + 4.0F * p;
    float phase = k * (float) Math.PI;                                // ← 相位

    float w = this.floatingItemWidth  * (float) (context.getScaledWindowWidth()  / 4);
    float h = this.floatingItemHeight * (float) (context.getScaledWindowHeight() / 4);

    MatrixStack ms = new MatrixStack();
    ms.push();
    ms.translate(
        (float) (context.getScaledWindowWidth()  / 2) + w * MathHelper.abs(MathHelper.sin(phase * 2.0F)),
        (float) (context.getScaledWindowHeight() / 2) + h * MathHelper.abs(MathHelper.sin(phase * 2.0F)),
        -50.0F);

    float s = 50.0F + 175.0F * MathHelper.sin(phase);                 // ← 缩放，恒在 [50, 225]
    ms.scale(s, -s, s);
    ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(900.0F * MathHelper.abs(MathHelper.sin(phase))));
    ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F * MathHelper.cos(p * 8.0F)));
    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.0F * MathHelper.cos(p * 8.0F)));

    context.draw(() -> this.client.getItemRenderer().renderItem(
            this.floatingItem, ModelTransformationMode.FIXED, 15728880,
            OverlayTexture.DEFAULT_UV, ms, context.getVertexConsumers(), this.client.world, 0));
    ms.pop();
}
```

关键点有三个，全部由字节码确认：

1. **相位是 `π·k(p)`，不是 `3π·p`。** `k` 是五次缓动多项式，满足 `k(0)=0`、`k(1)=1`。
   早期版本把相位写成 `3π·p` 是错的。
2. **`k` 单调落在 [0,1]，所以 `phase ∈ [0, π]`，于是 `sin(phase) ≥ 0` 恒成立。**
   这直接决定了缩放 `s` **永远为正**、范围恰好 **[50, 225]**——
   这也是「字号可以完全跟随图腾缩放」而不会出现镜像或消失的原因（§5.4）。
3. **`scale(s, -s, s)` 里的负号是给模型空间朝上的 3D 物品用的。**
   照搬到文字上会得到上下镜像的字，所以本模组只取缩放的**幅度**。

反汇编命令（仓库里不含 Minecraft，需先从 Loom 缓存取 jar）：

```powershell
$javap = "$env:JAVA_HOME\bin\javap.exe"
$jar = ".\.gradle-home\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2\minecraft-merged-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar"
& $javap -p -c -constants -classpath $jar net.minecraft.client.render.GameRenderer |
    Select-String -Pattern "renderFloatingItem" -Context 0,120
```

### 5.2 怎么做到「直接绑定动画」

早期版本的做法是：自己在 HUD 回调里**另算一套**位置和缩放，试图模仿图腾的轨迹。
只要公式有任何偏差，文字和图腾就会漂移——而且它是两套独立的时间线，天然存在对不齐的可能。

现在改成**结构性绑定**：直接注入原版动画方法内部。

```java
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow private ItemStack floatingItem;
    @Shadow private int floatingItemTimeLeft;
    @Shadow private float floatingItemWidth;
    @Shadow private float floatingItemHeight;

    @Inject(
        method = "renderFloatingItem(Lnet/minecraft/client/gui/DrawContext;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void notdeadyet$drawTotemName(DrawContext context, float tickDelta, CallbackInfo ci) {
        TotemNameOverlay.render(context, tickDelta,
                this.floatingItem, this.floatingItemTimeLeft,
                this.floatingItemWidth, this.floatingItemHeight);
    }
}
```

**为什么注入点是 `MatrixStack.pop()` 之前**：`renderFloatingItem` 的字节码里只有一处
`MatrixStack.pop()`，它紧跟在 `context.draw(...)` 之后。所以这个位置正好是
「图腾已经画完、矩阵还没弹出」的瞬间——既拿到了同一帧的动画状态，
绘制顺序又天然排在图腾之后。

由此带来的好处是**不需要维护任何自己的状态机**：

- 图腾什么时候开始播、什么时候结束，文字完全一致；
- 按 `F1` 隐藏 HUD 时一起隐藏（`renderFloatingItem` 本身就在 `if (!options.hudHidden)` 之内）；
- 图腾在副手、被其它模组改写、动画被延长——都会自动跟随，因为没有独立的判定逻辑。

### 5.3 「在图腾正中央之前」是怎么保证的

**位置**：`TotemAnimationMath.centerX/centerY` 复刻原版 `translate` 的那两个表达式，
连 `屏宽 / 4` 都保持**整数除法后再转 float**，保证奇数分辨率下也不差那 0.5 像素。
文字以自身中心对齐这个点：`-font.getWidth(text)/2` 与 `-font.fontHeight/2`。

**「前」有两重保障**：

1. **绘制顺序**：`DrawContext.draw(Runnable)` 的字节码是
   `draw(); runningDrawCallback = true; runnable.run(); runningDrawCallback = false; draw();`
   ——图腾那批顶点在注入点之前**已经提交并 flush 完毕**，随后画的字必然叠在它上面。
2. **图层深度**：GUI 空间里 z 越大越靠近观察者（原版物品栏图标用 200、tooltip 用 400，
   而图腾在 `-50`）。文字取 **z = 400**，因此无论深度测试是否开启、无论图腾转到什么角度，
   文字都稳压在图腾前面。

**朝向**：文字会跟着图腾做**屏幕平面内**的转动 —— 也就是绕「垂直于屏幕的那根轴」摆，
幅度直接取自原版图腾的 Z 轴倾斜 `6·cos(p·8)`（±6°，动画 40 tick 里走 8 个半周期）。

选这个方向是因为它对文字是**安全**的：屏幕平面内的转动永远不会把文字转成一条线，
也不会镜像。图腾那两段会造成可读性问题的旋转被刻意排除：

| 排除的旋转 | 原因 |
| --- | --- |
| Y 轴自转 `900·\|sin(phase)\|` | 最大 2.5 圈；转到 90° 文字侧成一条线，超过 90° 还会左右镜像 |
| X 轴倾斜 `6·cos(p·8)` | 把文字往屏幕外翻，`cos 6° ≈ 0.995` 肉眼几乎看不出，却平白引入深度抖动 |

**符号必须翻一次**：原版矩阵按顶点顺序展开是

```
T · o · Ry(θy) · Rx(θx) · Rz(θz) · diag(1, -1, 1)
  = T · o · Ry(θy) · Rx(-θx) · Rz(-θz) · diag(1, -1, 1)
```

末尾那个 `diag(1, -1, 1)` 来自 `scale(o, -o, o)`，负责把「模型空间 y 朝上」翻成 GUI 的
y 向下。反射会把绕 X、Z 的旋转取反（`F·Rz(θ)·F = Rz(-θ)`；Y 轴是对合的，
所以不受影响）。文字的坐标本来就在 GUI 空间、不需要这个翻转，于是要把它翻回来——
直接照抄原版的 `+6·cos(p·8)` 会让文字和图腾**朝相反方向**倾斜。

**摆动幅度**：图腾自己的 Z 轴倾斜只有 ±6°，而且是快摆、照搬过来几乎看不出来，
所以预设用 `SWAY_SCALE = 2.5` 把它放大到 **±15°**，摆动清晰可见。
（`1.0` = 完全照搬图腾的 ±6°，`0.0` = 直立不摆；调大不会有可读性问题，见上表。）

### 5.4 字号：预设 + 命名时更改

**预设**（`TotemNameOverlay.TEXT_HEIGHT_OVER_TOTEM = 0.30F`）：

图腾是 16×16 的平面物品模型，占 1 个方块单位；原版按 `scale(s, -s, s)` 画出来，
所以它在屏幕上正好约 **`s` 像素高**。于是

```java
文字高度 = s × 0.30          // 即「文字高度恒等于图腾高度的 30%」
字号     = 文字高度 / 9      // 9 是原版字形高度 font.fontHeight
```

动画全程保持同一视觉比例——这就是「完全跟随图腾缩放」的精确含义。
代入 `s ∈ [50, 225]`，文字高在 **15 ~ 67.5 像素**；原版 GUI 字号是 9 像素，
所以最小的时候也有 1.7 倍，满足「预设大一点」。

实测数值（`p` = 动画进度，由 `TotemAnimationMath` 计算）：

| p | k(p) | phase = π·k | sin(phase) | 图腾缩放 s | 文字高 = 0.30s |
| --- | --- | --- | --- | --- | --- |
| 0.00 | 0.000 | 0.000 | 0.000 | 50.0 | 15.0 px |
| 0.10 | 0.285 | 0.896 | 0.781 | 186.6 | 56.0 px |
| 0.25 | 0.448 | 1.409 | 0.987 | 222.7 | 66.8 px |
| 0.50 | 0.498 | 1.566 | 1.000 | 225.0 | 67.5 px |
| 0.75 | 0.533 | 1.676 | 0.995 | 224.0 | 67.2 px |
| 0.90 | 0.694 | 2.181 | 0.819 | 193.4 | 58.0 px |
| 1.00 | 1.000 | 3.142 | 0.000 | 50.0 | 15.0 px |

**命名时更改**：在名字末尾写一个方括号**设置块**，语法见 §5.4.1。

| 写法 | 效果 |
| --- | --- |
| `应急[尺寸:2]` | 字号 2 倍 |
| `应急[s:2]` | 同上，`s` 是短键 |
| `应急[size:2.5]` | 字号 2.5 倍 |
| `应急[大小:1.5]` | 键也可以写中文 |
| `应急[s:2x]` | 尾巴带 `x` 也行 |

倍率被夹在 **0.25 ~ 6** 之间，防止写出让文字糊满屏幕的值。

#### 5.4.1 设置块语法（颜色 + 尺寸写在同一个方括号里）

```
应急[颜色:红色;尺寸:2]
应急[c:红,蓝]             c 是「颜色」的短键，逗号分隔即为渐变
应急[c:红,蓝;s:1.5]       s 是「尺寸」的短键
应急[c:y红,蓝]            方向字母：x 横向（默认）、y 纵向、t 斜向
应急[c:t红,黄,绿]         斜向多段渐变
凤凰[c:#FF8800]
备用[颜色:金][尺寸:1.5]    也可以拆成多个方括号连写
```

**键名可以省略**。判定规则是「先试字号、再试颜色」：能把整段当数字读的
（`2`、`1.5`、`2x`）算字号，其余一律当颜色值。

```
应急[yred,blue;2]        等价于 [c:yred,blue;s:2]
应急[red,blue]           等价于 [c:red,blue]
应急[2]                  等价于 [s:2]
应急[红色]               等价于 [c:红色]
备用[_jeb]               等价于 [c:_jeb]
```

> **为什么定成字号优先**：原版单字符颜色代码里 `0`~`9` 都有效，所以 `[2]` 天然有歧义
> （字号 2 还是颜色 `§2` 深绿）。定成「数字优先」之后规则是确定的、可预期的；
> 要写数字颜色代码就显式带上键，例如 `[c:2]`。
>
> ⚠️ **省略键名的代价**：名字**以 `[颜色]` 这种形式结尾就会被当成设置块吃掉**。
> 比如图腾叫 `药水[红]`，显示出来会只剩 `药水` 而且颜色变红。要保留这种结尾，
> 写成 `[备注:红]` 之类**带键且键不认识**的形式即可（不认识就不会被吞）。
> 要求每段都带冒号，就能完全避免这种误吞 —— 这是这个简写唯一的取舍。

| 键 | 可用值 |
| --- | --- |
| `c` / `颜色` / `色` / `color` / `colour` / `渐变` / `gradient` | 见下表 |
| `s` / `尺寸` / `大小` / `size` | 数字，`2` / `2.5` / `2x` 都行 |
| （省略键名） | 数字 → 字号；其余 → 颜色 |

`颜色` 的值有三种形态，前面还可以加一个**方向字母**：

| 形态 | 写法 | 说明 |
| --- | --- | --- |
| **纯色** | `[c:红色]`、`[c:c]`、`[c:#FF8800]` | 中文名 / 英文名 / 单个代码字符 / 十六进制 |
| **渐变** | `[c:红,蓝]`、`[c:红,黄,绿,蓝]` | 逗号分隔若干色标（`#FF0000,#0000FF` 混写也行），段间线性插值 |
| **彩蛋** | `[c:_jeb]` | **动态**彩虹，见 §5.4.2 |

| 方向字母 | 含义 |
| --- | --- |
| 不写，或 `x` | 横向，左 → 右（默认） |
| `y` | 纵向，上 → 下 |
| `t` | 斜向，左上 → 右下 |

**颜色名**：中文名（红、金色、淡紫…）、英文名（`red`、`gold`、`light_purple`…）、
单个颜色码字符（`c`、`f`…，即原版 `§c` 里的那个字母；只认 16 个颜色码 `0-9a-f`，`k`/`l`/`m`/`n`/`o`/`r` 这些修饰符不算）、十六进制（`#RRGGBB` 或 `#RGB`）。
几个「原版没有对应色」的近似映射：橙/橙色 → `GOLD`，粉/粉色 → `LIGHT_PURPLE`，天蓝 → `AQUA`。

**中英文全识别**：键、值、标点都认两种写法——`:` 与 `：`、`;` 与 `；`、`,` 与 `，`，
英文键值大小写不敏感（`GOLD` = `gold`）。

**方向字母的歧义怎么解决**：`y` 既是方向字母，也是颜色名 `yellow` 的首字母
（`t` 与 `teal` 同理）。所以解析时**先试「方向字母 + 颜色」，失败再退回把整串当颜色**：
`yellow` 会被切成方向 `y` + `ellow`，而 `ellow` 不是合法颜色，于是回退，
`[c:yellow]` 正常得到黄色。实测两种写法都对。

几条刻意的设计：

- **只在末尾生效**，并且**至少有一段（键值对或省略键名的值）解析成功时才吃掉方括号**。所以
  `应急[备用]`（既不是字号也不是颜色）、`应急[备注:无]`（键不认识）、`应急[尺寸:大]`（值不合法）、
  `应急[颜色:大红]`（颜色不认识）、`应急[颜色:红,大蓝]`（渐变里有坏色标）
  都会**原样显示**，不会被静默吞掉——写错了看得见。
- **渐变里任意一段坏掉就整体失败**，不静默丢弃坏的那段。
- **重复写以最靠右的为准**：`[颜色:红][颜色:蓝]` 取蓝。
- 解析失败的原因会写进日志（`latest.log`），带上原始方括号内容，方便排查。
- 解析结果**按名字缓存**，动画期间每帧调用也只解析一次，日志同一个名字只打一次。

#### 5.4.2 彩蛋：`[c:_jeb]` 动态彩虹

`[c:_jeb]`（或 `[颜色:_jeb]`、`[c:y_jeb]` 配方向）是唯一的彩虹入口，
故意做成**彩蛋**而不是常规功能——致敬原版 `_jeb` 绵羊那身一直循环变色的羊毛。

它和静态渐变的区别是**会流动**：色相除了沿文字铺开，还额外叠加一个随时间推进的相位
（`TotemGradient.FLOW_SPEED = 0.4F`，即每秒转 0.4 圈）。

实测采样（`t` 为位置，`phase` 为相位）：

| | `t=0` | `t=0.5` | `t=1` |
| --- | --- | --- | --- |
| `phase=0` | `#FF0000` | `#00FFFF` | `#FF0000` |
| `phase=0.25` | `#7FFF00` | `#7F00FF` | `#7FFF00` |

相位由 `TotemGradient.currentPhase()` 统一提供，动画和 tooltip 共用同一个值。
**必须先取模**：直接把 1.7e9 的毫秒数放进 `float`，有效精度只剩百秒级，动画会一顿一顿的。

### 5.6 名字的所有显示位置：剥掉设置块 + 同色

设置块是给模组读的，不算名字的一部分，所以凡是「显示名字」的地方都要处理：

| 显示位置 | 原版从哪取值 | 本模组的注入点 |
| --- | --- | --- |
| 物品栏 tooltip | `ItemStack.getTooltip(...)` 的第一行 | `ItemStackMixin`（`@Inject` 到 RETURN） |
| 快捷栏上方的物品名称 | `InGameHud.renderHeldItemTooltip` 里的 `getName()` | `InGameHudMixin`（`@Redirect`） |
| 掉落物在地上的名字 | `ItemEntity.getName()` | **未处理**，见 §7 |

两个入口最终都走同一个 `TotemTooltip.displayedName(ItemStack)`；
颜色规则与动画共用 `TotemGradient` 和 `TotemNameText.DEFAULT_COLOR`。

快捷栏那处用 `@Redirect` 而不是重写整个方法，是因为原版只把
`Text.empty().append(getName()).formatted(rarity)` 当作文字来源 ——
换掉那一句，居中对齐、淡出计时、阴影全都保持原样。

#### 5.6.1 关键陷阱：绝对不能改 `ItemStack.getName()`

最直觉的做法是 Mixin `ItemStack.getName()`，把设置块从「名字」里抹掉。
**这样做会毁掉铁砧功能。** 反汇编确认有两处依赖它：

| 位置 | 用途 |
| --- | --- |
| `AnvilScreen.onSlotUpdate(...)` | 客户端用它**回填铁砧输入框** |
| `AnvilScreenHandler.updateResult()` | 服务端用它判断「名字有没有被改过」 |

一旦 `getName()` 返回剥掉设置块的文本：

1. 输入框会回填成 `应急`（设置块没了）；
2. 服务端发现输入框内容与 `getName()` 不一致，就用输入框内容覆盖 `custom_name`；
3. 结果：**在铁砧里重新命名一次，颜色和尺寸设置就被抹掉了。**

所以只注入 `ItemStack.getTooltip(...)` 的返回值，`custom_name` 本体分毫不动：

```java
@Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
```

识别方式是比对第一行：原版把 `getName()` 的结果放在 `lines.get(0)`，
所以先确认 `lines.get(0).getString()` 等于 `custom_name` 的纯文本，一致才替换，
避免误伤附魔、耐久那些行。

#### 5.6.2 tooltip 里的渐变只能逐字采样

动画是**顶点级**调制（§5.5.2）：一个字形四个角拿到不同颜色，中间由 GPU 插值。
tooltip 拿不到 `VertexConsumerProvider`，只能构造 `Text`，所以渐变降级为**逐字**上色：

```java
float t = count <= 1 ? 0.0F : (float) i / (count - 1);   // 按字符位置归一化
result.append(styled(new String(codePoints, i, 1), gradient.sample(t, phase)));
```

首尾字符取到的正是两个色标本身，所以 `[c:红,蓝]` 在 tooltip 里就是首字红、末字蓝。

**已知代价**：彩虹 `_jeb` 在文本上正好绕满一圈色相，因此只有 2 个字的名字会首尾同色
（整体仍随时间变色，只是两个字颜色一样）；字数多了就有明显过渡。

颜色仍走同一个 `TotemGradient`，相位共用 `TotemGradient.currentPhase()`，
所以 tooltip 里的彩虹同样在流动——tooltip 每帧重建，这个效果是自动的。
斜体保留原版行为（原版对所有带 `custom_name` 的物品都会把名字行设成 `ITALIC`）。

### 5.5 颜色：预设图腾金，可用命令改成任意原版颜色

预设字色取自**原版图腾贴图本身**，不是估的。把
`assets/minecraft/textures/item/totem_of_undying.png`（16×16，126 个不透明像素）
逐像素统计，得到它的完整调色板：

| 颜色 | 占比 | 部位 |
| --- | --- | --- |
| `#85400F` | 23.8% | 深棕描边 |
| `#A05B23` | 20.6% | 棕色暗部 |
| `#D1A75D` | 15.9% | 中间调金 |
| `#EADB84` | 13.5% | 亮面金 |
| `#C58742` | 11.1% | 金色 |
| `#F8EEA5` | 5.6% | 高光金 |

选出 `#EADB84`（亮面金）作为预设——它是图腾金属面的本色，比中间调金更有金属感，
分离度也够。

**关于对比度**：文字画在图腾**正中央**，也就是压在躯干上（该区域平均亮度 155/255）。
按 WCAG 对比度实测各候选对躯干的效果：

| 候选 | 平均对比度 |
| --- | --- |
| `#C58742` | 1.90:1 |
| `#D1A75D` | 1.94:1（最沉稳，但最容易融进背景） |
| **`#EADB84`（本预设）** | **2.53:1** |
| `#F8EEA5` | 2.94:1 |
| `#FFD966`（旧版，录屏取色） | 2.59:1 |
| `#FFFFFF`（原版白字，对照） | 3.46:1 |

中间调金 `#D1A75D` 是几个候选里对比度**最低**的（1.94:1），最沉稳但最容易融进背景；
亮面金 `#EADB84` 兼顾了金属感和分离度，所以最终选它。想更醒目还可以用 `#F8EEA5`。
上面的调色板与对比度是这样得出的：从 Loom 缓存的 `minecraft-client.jar` 里取出
`assets/minecraft/textures/item/totem_of_undying.png`，逐像素统计不透明像素的 RGB
分布，再按 WCAG 相对亮度公式算各候选色对躯干区域的平均对比度。
（统计脚本是作者本地的开发工具，未随仓库分发；结论就是上面两张表，可自行核对。）

绘制仍走原版 `DrawContext.drawTextWithShadow`，所以阴影由原版
`TextRenderer` 自己推导（`(color & 0xFCFCFC) >> 2`，对 `#EADB84` 即 `#3A3621`），
与物品栏提示的行为完全一致。

#### 5.5.1 为什么用方括号而不是 `§`

**因为铁砧会把 `§` 滤掉。** 这是开发中实测确认的：

| 位置 | 行为 |
| --- | --- |
| `StringHelper.isValidChar(char)` | 对 `167`（即 `§`）**直接返回 false**；`[` `]` `:` `;` `,` 和中文全部放行 |
| `StringHelper.stripInvalidChars(String)` | 把 `isValidChar` 为 false 的字符全部丢弃 |
| `AnvilScreenHandler.setNewItemName` | **第一条指令**就是 `sanitize(...)`，即上面的过滤 + 截断到 50 字符 |

所以往铁砧里粘贴 `§c` 是没用的——`§` 在写入 `custom_name` 之前就被删掉了。
而设置块用的全是合法字符，因此能在铁砧里正常存下来。这就是本模组只认方括号的原因。

**模组本身不再解析 `§`，也不沿用组件自带的样式**：名字只取 `custom_name` 的纯文本，
剥掉设置块后直接交给原版渲染。命令想设颜色也一样写设置块：

```
/give @s minecraft:totem_of_undying[minecraft:custom_name='应急[c:红,蓝;size:1.5]']
```

> 顺带一个发现：**原版自己就会解析 `§`**。`Text.literal("A§cB§lC").asOrderedText()`
> 得到的依次是 `A`（无样式）、`B`（红）、`C`（红 + 粗体）——`§` 序列被吃掉并转成了样式。
> 所以命令写的 `§c` 其实仍然有效，但那是原版行为、不是本模组提供的功能，
> 而且铁砧里根本用不了，因此不作为推荐写法。

#### 5.5.2 渐变为什么是平滑的：顶点级调制

原版文字的着色单位是**字形**：一个 `Style` 里的 `TextColor` 管一个字符。
如果按字符上色，中文名会很难看——峰值时单个字宽约 67 像素，四个字就是四条纯色竖条，
是「色块拼接」而不是渐变。

所以要真正做到平滑，就得在**顶点级别**动手。原版画一个字形时的调用序列是
（`GlyphRenderer.draw` / `drawRectangle`，已用字节码确认）：

```
vertex.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v).light(light).next();
```

两个可以利用的点：

| 观察 | 用途 |
| --- | --- |
| `vertex(Matrix4f, float, float, float)` 是接口上的 **default 方法** | 拿到的是**变换前**的局部坐标，正是文字排版坐标，可以直接当渐变位置 |
| `color(FFFF)`、`color(I)`、`colorRgb(I)` 三个 default 方法最终都汇流到 `color(IIII)` | 只要覆盖那**一个**方法就全覆盖了 |

于是 `GradientVertexConsumer` 只做两件事：拦下 `vertex(Matrix4f,...)` 记下局部 x 与 y，
在 `color(IIII)` 里按方向采样渐变（`x` 用横轴、`y` 用纵轴、`t` 取两者平均得到斜向等值线）。
同一个字形的四个角因此拿到不同颜色，中间由 GPU 插值——**真正的平滑渐变**。

**为什么是「乘」而不是「覆盖」**：传进来的颜色里已经带着原版算好的信息——
阴影通道会给它乘 0.25 的变暗系数。如果直接用渐变色的 RGB 覆盖，阴影就丢了，
文字会糊在图腾上。所以这里做调制：

```java
输出 = 渐变采样色 × 传入色 / 255
```

主通道传入白色（模组故意传 `0xFFFFFF`），乘法等于原样输出渐变；
阴影通道传入 25% 灰，于是自动得到**渐变色的暗版阴影**——不用自己画两遍，阴影是白送的。

代价是这条路径必须绕过 `DrawContext.drawTextWithShadow`（它内部固定用自己的
`VertexConsumerProvider`），改为直接调 `TextRenderer.draw(...)` 并传入包装过的 provider。
光照值与图层类型照抄原版：`15728880` 与 `TextLayerType.NORMAL`。

**支持三个方向**：`x` 横向（默认）、`y` 纵向、`t` 斜向（左上→右下）。
斜向的实现是取归一化后的 x 与 y 的平均，等值线因此是 45° 斜线。

---

## 6. 调参

想改默认字号，只动一个常量：`TotemNameOverlay.TEXT_HEIGHT_OVER_TOTEM`。

| 常量 | 位置 | 默认 | 含义 |
| --- | --- | --- | --- |
| `TEXT_HEIGHT_OVER_TOTEM` | `TotemNameOverlay` | `0.30F` | **文字高 / 图腾高**。改成 `1.0` 就是和图腾一样高；`0.0` 级别会小到看不见 |
| `RAINBOW_FLOW_SPEED` → `FLOW_SPEED` | `TotemGradient` | `0.4F` | 彩蛋 `_jeb` 彩虹的流动速度，每秒转多少圈色相（动画与 tooltip 共用） |
| `SWAY_SCALE` | `TotemNameOverlay` | `2.5F` | 屏幕平面内摆动倍率。`2.5` = ±15°，`1.0` = 图腾原本的 ±6°，`0.0` = 不摆 |
| `TEXT_Z` | `TotemNameOverlay` | `400.0F` | 绘制层深度，越大越靠前 |
| `DEFAULT_TEXT_COLOR` → `DEFAULT_COLOR` | `TotemNameText` | `0xEADB84` | 没写颜色时的字色，取自图腾贴图亮面金（动画与 tooltip 共用） |
| `MIN_SIZE_MULTIPLIER` / `MAX_SIZE_MULTIPLIER` | `TotemNameText` | `0.25F` / `6.0F` | 命名后缀的允许范围 |
| `TOTAL_TICKS` / `TOTEM_Z` | `TotemAnimationMath` | `40` / `-50.0F` | 原版常量，**不要改** |

---

## 7. 已知边界

- 只处理**玩家自己**触发的图腾动画（屏幕上那个 HUD 漂浮物品）。其它实体的保命动画不上 HUD，
  因为它们根本不走这条渲染通路。
- 名字不换行，也不做宽度钳制——按规则「完全跟随图腾缩放」，动画中段（`s≈225`）时
  很长的名字可能横向超出屏幕。铁砧名字建议控制在 20 字符以内，或用 `[尺寸:0.5]` 压小。
- 因为完全跟随缩放，动画首尾（`p→0` 或 `p→1`）时文字只有 15 像素高，是全程最小的时刻。
  这是绑定图腾缩放的自然结果；若不想要，把 `TEXT_HEIGHT_OVER_TOTEM` 调大或改为固定字号。
- 按 `F1` 隐藏 HUD 时文字一起隐藏，与原版行为一致。
- 名字的显示位置里，**掉落物躺在地上的那个名字没处理**（`ItemEntity.getName()`）。
  它和快捷栏名称是同一类问题，但那条通路会同时影响死亡消息、`/data` 之类的输出，
  改动面比前两处大，所以先留着。要处理的话加一个 `ItemEntityMixin` 即可。
- tooltip 与快捷栏名称里的渐变只能**逐字**上色（拿不到 `VertexConsumerProvider`），
  所以彩虹 `_jeb` 对只有 2 个字的名字会首尾同色，见 §5.6.2。
- 纯客户端模组，服务器不需要装。

---

## 8. 换到别的 Minecraft 版本

本模组现在**只依赖一个原版方法**，所以移植成本很低，但要改的地方是版本相关的：

| 目标版本 | 要改什么 |
| --- | --- |
| 1.20.4 及更早 | 名字不在组件里，改读 NBT：`stack.getSubNbt(ItemStack.DISPLAY_KEY).getString(ItemStack.CUSTOM_NAME_KEY)`；`AnvilScreenHandler` 的 § 处理**可能不同**，需要复核 |
| **1.21.1** | 当前版本 |
| 1.21.2 ~ 1.21.4 | `renderFloatingItem` 的**签名可能变**（比如参数里多了 `RenderTickCounter`），公式本身一般稳定；用 `javap` 复核方法描述符与 `MatrixStack.pop()` 的调用点数量 |
| 1.21.5 及以上 | HUD 改成图层系统，但本模组**不依赖 HUD 回调**，只要 `renderFloatingItem` 还在就仍可用 |

**跨版本必做的一步**（用 §5.1 的反汇编命令）：

1. 确认 `GameRenderer` 里仍有 `renderFloatingItem`，并记下它的**准确描述符**，
   同步修改 `GameRendererMixin` 的 `method = "..."`；
2. 确认该方法内 `MatrixStack.pop()` 只出现**一次**，否则注入点要用
   `ordinal` 或换成 `@At("TAIL")`；
3. 确认四个 `@Shadow` 字段名（`floatingItem` / `floatingItemTimeLeft` /
   `floatingItemWidth` / `floatingItemHeight`）没被改名。

这三步任何一步不符，创建世界时 Fabric 会在日志里**明确报 Mixin 应用失败**
（`defaultRequire: 1` 保证不会静默失效），不会出现「装上了但没效果」。
另外记得同步 `gradle.properties` 与 `fabric.mod.json` 里的版本号。
