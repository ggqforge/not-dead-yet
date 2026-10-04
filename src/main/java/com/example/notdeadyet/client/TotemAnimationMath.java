package com.example.notdeadyet.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * 原版不死图腾触发动画的运动学。
 *
 * <p>本类是 1.21.1 {@code GameRenderer.renderFloatingItem(DrawContext, float)}
 * 的逐行对照实现，公式全部来自字节码，不是拟合出来的。原版那段反汇编还原成 Java 是：</p>
 *
 * <pre>
 * int i = 40 - floatingItemTimeLeft;
 * float f = ((float)i + tickDelta) / 40.0F;                 // p
 * float g = f * f;                                          // p²
 * float h = f * g;                                          // p³
 * float k = 10.25F*h*g - 24.95F*g*g + 25.5F*h - 13.8F*g + 4.0F*f;
 * float l = k * (float)Math.PI;                             // 相位
 * float m = floatingItemWidth  * (float)(scaledWidth  / 4);
 * float n = floatingItemHeight * (float)(scaledHeight / 4);
 * matrixStack.translate(scaledWidth / 2  + m * |sin(l * 2)|,
 *                       scaledHeight / 2 + n * |sin(l * 2)|, -50.0F);
 * float o = 50.0F + 175.0F * sin(l);                        // 缩放
 * matrixStack.scale(o, -o, o);
 * </pre>
 *
 * <p><b>关键点</b>：相位是 {@code π·k(p)}，其中 k 是五次缓动多项式（k(0)=0、k(1)=1），
 * <b>不是</b> {@code 3π·p}。因为 k 单调落在 [0,1]，所以 {@code phase ∈ [0, π]}，
 * 于是 {@code sin(phase) ≥ 0} 恒成立 —— 缩放 {@code o = 50 + 175·sin(phase)}
 * 永远在 <b>[50, 225]</b> 之间且绝不为负，这正是「字号可以安全地完全跟随图腾缩放」的前提。</p>
 */
public final class TotemAnimationMath {

    private TotemAnimationMath() {
    }

    /** 原版 {@code showFloatingItem} 写入 {@code floatingItemTimeLeft} 的初值。 */
    public static final int TOTAL_TICKS = 40;

    /** 原版 {@code translate} 用的 z。 */
    public static final float TOTEM_Z = -50.0F;

    /**
     * 动画进度 {@code p = (已过 tick + 帧内插值) / 40}，范围 [0, 1]。
     *
     * <p>带 {@code tickDelta} 才能得到与帧率无关的平滑动画。</p>
     */
    public static float progress(int timeLeft, float tickDelta) {
        int elapsed = TOTAL_TICKS - timeLeft;
        return (elapsed + tickDelta) / (float) TOTAL_TICKS;
    }

    /**
     * 原版相位 {@code phase = π · k(p)}。
     *
     * <p>k 是五次缓动多项式 {@code 10.25p⁵ - 24.95p⁴ + 25.5p³ - 13.8p² + 4p}，
     * 满足 k(0)=0、k(1)=1。</p>
     */
    public static float phase(float p) {
        float p2 = p * p;
        float p3 = p * p2;
        float k = 10.25F * p3 * p2 - 24.95F * p2 * p2 + 25.5F * p3 - 13.8F * p2 + 4.0F * p;
        return k * (float) Math.PI;
    }

    /** 图腾缩放 {@code o = 50 + 175·sin(phase)}，取值恒在 [50, 225]。 */
    public static float totemScale(float phase) {
        return 50.0F + 175.0F * MathHelper.sin(phase);
    }

    /**
     * 图腾中心的屏幕 X：{@code 屏宽/2 + itemWidth · (屏宽/4) · |sin(2·phase)|}。
     *
     * <p>注意 {@code 屏宽/4} 与原版一样是<b>整数除法</b>后再转 float，
     * 否则奇数宽度下会和原版差出 0.5 像素。</p>
     */
    public static float centerX(DrawContext context, float itemWidth, float phase) {
        int screenWidth = context.getScaledWindowWidth();
        return (float) (screenWidth / 2) + itemWidth * (float) (screenWidth / 4) * swing(phase);
    }

    /** 图腾中心的屏幕 Y，与原版 {@code translate} 的 Y 完全同源。 */
    public static float centerY(DrawContext context, float itemHeight, float phase) {
        int screenHeight = context.getScaledWindowHeight();
        return (float) (screenHeight / 2) + itemHeight * (float) (screenHeight / 4) * swing(phase);
    }

    /**
     * 文字在<b>屏幕平面内</b>应该倾斜的角度（度），也就是绕「垂直于屏幕的那根轴」转动。
     *
     * <p>取值直接来自原版图腾的 Z 轴旋转 {@code Rz(θz)}，其中
     * {@code θz = 6 · cos(p · 8)}，范围 ±6°，在动画里来回摆动 8 个半周期。</p>
     *
     * <h2>为什么这里要带负号</h2>
     *
     * <p>原版那串变换按顶点顺序展开是</p>
     *
     * <pre>
     * T · diag(o, -o, o) · Ry(θy) · Rx(θx) · Rz(θz)
     *   = T · o · Ry(θy) · Rx(-θx) · Rz(-θz) · diag(1, -1, 1)
     * </pre>
     *
     * <p>最后那个 {@code diag(1, -1, 1)} 是 {@code scale(o, -o, o)} 带进来的，
     * 作用是把「模型空间 y 朝上」翻成 GUI 的 y 向下。反射矩阵会把绕 X、Z 的旋转
     * 取反（{@code F · Rz(θ) · F = Rz(-θ)}，而 Y 轴是对合的
     * {@code F · Ry(θ) · F = Ry(θ)}）。</p>
     *
     * <p>文字的坐标本来就在 GUI 的 y 向下空间里，不需要那个翻转，
     * 所以要把符号翻回来，否则倾斜方向会和图腾相反。也就是说：
     * <b>模型空间的 {@code +6·cos(p·8)}，在屏幕上等价于 {@code -6·cos(p·8)}。</b></p>
     */
    public static float textSwayDegrees(float p) {
        return -6.0F * MathHelper.cos(p * 8.0F);
    }

    /** 原版横向与纵向摆动共用的那一项 {@code |sin(phase * 2)|}。 */
    private static float swing(float phase) {
        return MathHelper.abs(MathHelper.sin(phase * 2.0F));
    }
}
