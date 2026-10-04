package com.example.notdeadyet.client;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/**
 * 在顶点级别给文字刷渐变的 {@link VertexConsumer} 包装器。
 *
 * <h2>原理</h2>
 *
 * <p>原版画一个字形时（{@code GlyphRenderer.draw} / {@code drawRectangle}）的调用序列是：</p>
 *
 * <pre>
 * vertex.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v).light(light).next();
 * </pre>
 *
 * <p>{@code vertex(Matrix4f, ...)} 是接口上的 <b>default</b> 方法，拿到的是<b>变换前</b>的
 * 局部坐标，也就是文字排版坐标 —— 正好可以直接当渐变位置用。所以这里拦下它记下 x/y，
 * 再让 default 实现照常做矩阵变换。</p>
 *
 * <p>颜色方面：{@code color(FFFF)}、{@code color(I)}、{@code colorRgb(I)} 三个 default 方法
 * 最终都会汇流到 {@code color(IIII)}（已用字节码确认），所以只要覆盖那一个就全覆盖了。</p>
 *
 * <h2>为什么要「乘」而不是「覆盖」</h2>
 *
 * <p>传进来的颜色里已经带着原版算好的信息：阴影通道会给它乘 0.25 的变暗系数。
 * 如果直接覆盖成渐变色的 RGB，阴影就丢了，文字会糊在图腾上。</p>
 *
 * <p>所以这里做的是<b>调制</b>：{@code 输出 = 渐变采样色 × 传入色 / 255}。
 * 主通道传入的是白色（{@link TotemNameOverlay} 会传 {@code 0xFFFFFF}），
 * 乘法等于原样输出渐变；阴影通道传入的是 25% 灰，于是自动得到<b>渐变色的暗版阴影</b>，
 * 不需要自己画两遍。</p>
 *
 * <h2>⚠️ 每个方法都必须 {@code return this}</h2>
 *
 * <p>原版画字形时是一路链式调用的：</p>
 *
 * <pre>
 * consumer.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v).light(light)
 * </pre>
 *
 * <p>也就是说 <b>{@code color()} 是在 {@code vertex()} 的返回值上调用的</b>。
 * 如果这里写成 {@code return delegate.vertex(...)}，链子从第二个调用开始就跑到
 * 被包装的裸 consumer 上了，{@code color()} 会完全绕过本类 —— 表现就是
 * <b>渐变失效、文字变成传入的基础色（白）</b>，而且不报任何错。</p>
 *
 * <p>这个 bug 真的发生过，而且静态看代码完全看不出来（{@code color()} 本身写得没问题）。
 * 是靠 {@code tools/probe/GradientProbe.java} 用一个假 consumer 记录颜色才定位到的：
 * 直接调 {@code color(IIII)} 得到正确渐变，走 {@code color(FFFF)} 默认方法却原样透传，
 * 且探针渐变一次都没被采样。</p>
 */
final class GradientVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final TotemGradient gradient;
    private final TotemGradient.Direction direction;
    private final float phase;
    private final float minX;
    private final float spanX;
    private final float minY;
    private final float spanY;

    /** 最近一次 {@code vertex} 的局部坐标，用来决定这个顶点该取渐变的哪一段。 */
    private float localX;
    private float localY;

    GradientVertexConsumer(VertexConsumer delegate, TotemGradient gradient,
                           TotemGradient.Direction direction, float phase,
                           float minX, float maxX, float minY, float maxY) {
        this.delegate = delegate;
        this.gradient = gradient;
        this.direction = direction;
        this.phase = phase;
        this.minX = minX;
        this.spanX = maxX - minX;
        this.minY = minY;
        this.spanY = maxY - minY;
    }

    @Override
    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        this.localX = x;
        this.localY = y;
        // 交给 default 实现做矩阵变换，它会转调下面的 vertex(FFF)
        VertexConsumer.super.vertex(matrix, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        float t;
        switch (direction) {
            case VERTICAL -> t = normalize(localY, minY, spanY);
            case DIAGONAL -> t = (normalize(localX, minX, spanX) + normalize(localY, minY, spanY)) * 0.5F;
            default -> t = normalize(localX, minX, spanX);
        }

        int rgb = gradient.sample(t, phase);
        delegate.color(
                ((rgb >> 16) & 0xFF) * red / 255,
                ((rgb >> 8) & 0xFF) * green / 255,
                (rgb & 0xFF) * blue / 255,
                alpha);
        return this;
    }

    private static float normalize(float value, float min, float span) {
        return span > 0.0F ? (value - min) / span : 0.0F;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }
}
