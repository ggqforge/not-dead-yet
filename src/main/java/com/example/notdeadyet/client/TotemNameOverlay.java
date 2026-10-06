package com.example.notdeadyet.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

/**
 * 把图腾名字画在「图腾正中央之前」。
 *
 * <p>本类由 {@code GameRendererMixin} 在原版 {@code renderFloatingItem} 内部调用，
 * 落点恰好是「图腾已经画完、MatrixStack 还没弹出」的那一步。因此：</p>
 *
 * <ul>
 *   <li><b>位置与缩放零误差绑定</b>：中心点与缩放都直接用
 *       {@link TotemAnimationMath} 算出的、和原版同一帧的同一批数值。</li>
 *   <li><b>「前」是确定的</b>：原版 {@code context.draw(Runnable)} 的实现是
 *       {@code draw(); runnable.run(); draw();}（已用字节码确认），
 *       也就是图腾那批顶点在我们插进来之前<b>已经提交并 flush 完毕</b>；
 *       随后画的字必然叠在它上面。再加上 z 取 400（原版 tooltip 的层级，
 *       图腾在 z = -50），无论深度测试开没开都稳压图腾。</li>
 * </ul>
 */
public final class TotemNameOverlay {

    private TotemNameOverlay() {
    }

    /**
     * 文字高度相对图腾高度的比例 —— 这就是「字号预设」。
     *
     * <p>原理：图腾是 16×16 的平面物品模型，占 1 个方块单位；原版把它按
     * {@code scale(o, -o, o)} 画出来，所以它在屏幕上正好约 {@code o} 像素高。
     * 于是 {@code 文字高 = o × 0.30} 意味着 <b>文字高度恒等于图腾高度的 30%</b>，
     * 整个动画过程中视觉比例不变 —— 这正是「完全跟随图腾缩放」的精确含义。</p>
     *
     * <p>数值代入：{@code o ∈ [50, 225]}，所以文字高在 <b>15 ~ 67.5 像素</b>；
     * 原版 GUI 字号是 9 像素，也就是说最小的时候也有 1.7 倍，满足「预设大一点」。</p>
     */
    public static final float TEXT_HEIGHT_OVER_TOTEM = 0.30F;

    /**
     * 屏幕平面内摆动幅度的倍率。
     *
     * <p>图腾自己的 Z 轴倾斜只有 {@code 6·cos(p·8)}（±6°），而且是快摆
     * （动画 40 tick 里 {@code cos(p·8)} 走完 8 个半周期），照搬过来几乎看不出来。
     * 所以这里放大 {@code 2.5} 倍 → <b>±15°</b>，摆动清晰可见但不喧宾夺主。</p>
     *
     * <p>{@code 1.0} = 完全照搬图腾的 ±6°，{@code 0.0} = 直立不摆。
     * 调大不会带来可读性问题 —— 这是<b>屏幕平面内</b>的转动，
     * 文字永远不会被转成一条线，也不会镜像。</p>
     */
    public static final float SWAY_SCALE = 2.5F;

    /**
     * 绘制用 z。GUI 空间里 z 越大越靠近观察者：
     * 原版图腾在 {@code -50}，物品栏图标在 200，tooltip 在 400。
     */
    private static final float TEXT_Z = 400.0F;

    /** 原版 {@code DrawContext} 画文字时用的满亮度光照值，照抄以保持一致。 */
    private static final int MAX_LIGHT = 15728880;

    /**
     * @param context   原版传进来的绘制上下文
     * @param tickDelta 帧内插值
     * @param totem     正在播放动画的物品（原版保证是图腾）
     * @param timeLeft  {@code floatingItemTimeLeft}，&lt;= 0 表示没在播
     * @param itemWidth  {@code floatingItemWidth}，横向摆动幅度，[-1, 1]
     * @param itemHeight {@code floatingItemHeight}，纵向摆动幅度，[-1, 1]
     */
    public static void render(DrawContext context, float tickDelta, ItemStack totem,
                              int timeLeft, float itemWidth, float itemHeight) {
        if (totem == null || totem.isEmpty() || timeLeft <= 0) {
            return;
        }
        if (!totem.isOf(Items.TOTEM_OF_UNDYING)) {
            return;   // 其它模组复用 floating item 机制时不乱显示
        }

        // 只有真被铁砧/命令改过名的图腾才显示，避免每次保命都弹出「不死图腾」四个字
        Text customName = totem.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null) {
            return;
        }

        TotemNameText name = TotemNameText.parse(customName.getString());
        if (name.isEmpty()) {
            return;
        }

        float progress = TotemAnimationMath.progress(timeLeft, tickDelta);
        float phase = TotemAnimationMath.phase(progress);
        float totemScale = TotemAnimationMath.totemScale(phase);

        float centerX = TotemAnimationMath.centerX(context, itemWidth, phase);
        float centerY = TotemAnimationMath.centerY(context, itemHeight, phase);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        OrderedText text = name.text();

        // 字号 = 目标文字高度 / 原版字形高度(9px)，再乘命名里指定的倍率
        float fontScale = totemScale * TEXT_HEIGHT_OVER_TOTEM
                / (float) font.fontHeight * name.sizeMultiplier();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        // 这里可以直接用 context.getMatrices()，是因为 GameRenderer.render() 全程没有
        // 动过 DrawContext 的矩阵栈（整个类里只有 renderNausea 用过 getMatrices），
        // 它在注入点就是单位矩阵 —— 与原版给图腾用的 new MatrixStack() 是同一个坐标系，
        // 所以 centerX / centerY 能原样贴到图腾中心，不需要任何补偿。
        matrices.translate(centerX, centerY, TEXT_Z);

        // 只取缩放的「幅度」，不套用原版的 (o, -o, o)：那个 Y 翻转是给
        // 模型空间朝上的物品用的，照搬到文字上会变成上下镜像。
        matrices.scale(fontScale, fontScale, 1.0F);

        // 绕「垂直于屏幕的那根轴」转动，跟着图腾的 Z 轴倾斜一起摆。
        // 放在 scale 之后有两个好处：scale 的 z 分量是 1，所以旋转不会把文字
        // 推离 TEXT_Z 这个深度（顶点 z 波动只有字体像素量级），
        // 而且旋转轴心就是文字自身中心。
        // 刻意不套用图腾的 Y 轴自转（900°·|sin|），那会让文字周期性侧成一条线、
        // 转过 90° 后还会左右镜像。
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                TotemAnimationMath.textSwayDegrees(progress) * SWAY_SCALE));

        // 以文字自身中心为原点：水平居中 + 垂直居中（字形高 9，取 -4）
        TotemGradient gradient = name.gradient();
        if (gradient != null) {
            drawGradientText(context, font, text, gradient, name.direction());
        } else {
            // 颜色优先级：设置块 [颜色:...] > 模组预设色（图腾亮面金）。
            context.drawTextWithShadow(font, text,
                    -font.getWidth(text) / 2, -font.fontHeight / 2,
                    name.effectiveColor());
        }

        matrices.pop();
    }

    /**
     * 渐变 / 彩虹版本。必须绕过 {@code DrawContext} 直接调 {@link TextRenderer#draw}，
     * 因为要替换掉它内部的 {@code VertexConsumerProvider} —— 只有这样才能在顶点级别刷颜色，
     * 做出真正的平滑渐变（而不是一个字一块颜色）。
     *
     * <p><b>基础色传白是有意的</b>：{@link GradientVertexConsumer} 做的是「乘」调制，
     * 传白等于原样输出渐变；而原版阴影通道会把白乘成 25% 灰，于是阴影自动变成
     * 渐变色的暗版 —— 不用自己画两遍，也不会丢阴影。</p>
     */
    private static void drawGradientText(DrawContext context, TextRenderer font, OrderedText text,
                                         TotemGradient gradient, TotemGradient.Direction direction) {
        int width = font.getWidth(text);
        // 与原版一致：整数除法做居中对齐
        float drawX = -(width / 2);
        float drawY = -(font.fontHeight / 2);

        // 彩蛋 _jeb 的彩虹要流动，所以给一个随时间推进的相位（单位：圈）。
        // 静态渐变会忽略它，不必特判。
        float phase = TotemGradient.currentPhase();

        // 字形的局部坐标：x 以 drawX 为起点铺满 width，y 以 drawY 为起点铺满字形高，
        // 正好当渐变区间（方向由 GradientVertexConsumer 按 direction 选用哪个轴）
        VertexConsumerProvider provider = layer -> new GradientVertexConsumer(
                context.getVertexConsumers().getBuffer(layer), gradient, direction, phase,
                drawX, drawX + width, drawY, drawY + font.fontHeight);

        font.draw(text, drawX, drawY, 0xFFFFFF, true,
                context.getMatrices().peek().getPositionMatrix(), provider,
                TextRenderer.TextLayerType.NORMAL, 0, MAX_LIGHT);
    }
}
