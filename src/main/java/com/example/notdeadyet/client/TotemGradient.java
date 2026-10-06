package com.example.notdeadyet.client;

import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * 沿指定方向采样的渐变色。
 *
 * <p>颜色不是在 {@code Style} 上按字符设的，而是由 {@link GradientVertexConsumer}
 * 在顶点级别逐顶点调制——同一个字形的四个角拿到不同颜色，中间由 GPU 插值。
 * 对中文名这点很关键：峰值时单个字宽约 67 像素，按字符上色会变成明显的色块。</p>
 */
@FunctionalInterface
public interface TotemGradient {

    /**
     * 采样。
     *
     * @param t     位置，0 = 起点，1 = 终点，已按 {@link Direction} 归一化
     * @param phase 动画相位（圈）；静态渐变忽略它，彩虹用它做流动
     * @return {@code 0xRRGGBB}
     */
    int sample(float t, float phase);

    /** 彩蛋 {@code _jeb} 彩虹的流动速度：每秒转多少圈色相。 */
    float FLOW_SPEED = 0.4F;

    /**
     * 当前动画相位（圈），供 {@code _jeb} 彩蛋做流动彩虹用。
     *
     * <p>动画和物品栏 tooltip 共用它，保证两处流动同步；静态渐变忽略这个值。</p>
     *
     * <p>先对时间取模是为了避免 float 在大数值上丢精度
     * （直接把 1.7e9 毫秒放进去，精度只剩百秒级，彩虹会一顿一顿的）。</p>
     */
    static float currentPhase() {
        return (float) ((System.currentTimeMillis() % 600_000L) / 1000.0) * FLOW_SPEED;
    }

    /** 渐变方向。写在颜色前面：{@code [c:y红,蓝]}。 */
    enum Direction {
        /** 横向，左 → 右。默认，也可以显式写 {@code x}。 */
        HORIZONTAL('x'),
        /** 纵向，上 → 下。 */
        VERTICAL('y'),
        /** 斜向，左上 → 右下。 */
        DIAGONAL('t');

        private final char letter;

        Direction(char letter) {
            this.letter = letter;
        }

        /** 方向字母，大小写不敏感；不是方向字母返回 {@code null}。 */
        public static Direction fromLetter(char c) {
            char lower = Character.toLowerCase(c);
            for (Direction direction : values()) {
                if (direction.letter == lower) {
                    return direction;
                }
            }
            return null;
        }
    }

    /**
     * 多段线性渐变，例如 {@code 红,黄,绿,蓝}。
     *
     * <p>只有一段时退化成纯色；段与段之间在 RGB 空间插值。</p>
     */
    static TotemGradient ofStops(List<Integer> stops) {
        if (stops.isEmpty()) {
            return (t, phase) -> 0xFFFFFF;
        }
        if (stops.size() == 1) {
            int only = stops.get(0);
            return (t, phase) -> only;
        }

        List<Integer> colors = List.copyOf(stops);
        return (t, phase) -> {
            float u = MathHelper.clamp(t, 0.0F, 1.0F) * (colors.size() - 1);
            int index = (int) u;
            if (index >= colors.size() - 1) {
                return colors.get(colors.size() - 1);
            }
            return lerpRgb(colors.get(index), colors.get(index + 1), u - index);
        };
    }

    /**
     * 彩虹：色相沿方向铺满一圈，并随 {@code phase} 流动。
     *
     * <p>这是彩蛋，只由名字里的 {@code _jeb} 触发（致敬原版的 {@code _jeb} 绵羊 ——
     * 它的羊毛颜色也是这样一直循环变色的）。</p>
     */
    static TotemGradient rainbow() {
        return (t, phase) -> hsvToRgb((t + phase) * 360.0F, 1.0F, 1.0F);
    }

    private static int lerpRgb(int from, int to, float delta) {
        int r = (int) MathHelper.lerp(delta, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(delta, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(delta, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }

    /** 标准 HSV→RGB，色相单位为度。彩虹用满饱和满亮度，观感最正。 */
    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * f);
        float t = value * (1.0F - saturation * (1.0F - f));

        float r;
        float g;
        float b;
        switch (sector % 6) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }
}
