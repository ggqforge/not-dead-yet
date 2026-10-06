package com.example.notdeadyet.client;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * 物品栏 tooltip 里那行名字。
 *
 * <p>把 {@code custom_name} 结尾的设置块剥掉，并按动画用的同一套颜色上色。</p>
 *
 * <p><b>与动画的差别</b>：tooltip 只能拿到 {@link Text}，没法替换
 * {@code VertexConsumerProvider}，所以做不出顶点级插值 —— 渐变只能按<b>逐字</b>采样。
 * 动画那边仍然是顶点级平滑渐变（见 {@link GradientVertexConsumer}）。
 * 颜色取值的规则两边完全一致，只是采样粒度不同。</p>
 */
public final class TotemTooltip {

    private TotemTooltip() {
    }

    /**
     * 求「显示用的名字」：只对<b>改过名的不死图腾</b>生效。
     *
     * <p>这是给「展示名字」的通路用的统一入口 —— 物品栏 tooltip、快捷栏上方的
     * 手持物品名称都调它。<b>绝对不要</b>把它接到 {@link net.minecraft.item.ItemStack#getName()}
     * 上，原因见 {@code ItemStackMixin} 的类注释。</p>
     *
     * @return 该用的文本；不是改过名的图腾、或名字里没有可显示内容时返回 {@code null}
     *         （调用方保持原版行为）
     */
    public static Text displayedName(ItemStack stack) {
        if (!stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return null;
        }
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null) {
            return null;
        }
        return buildNameLine(TotemNameText.parse(customName.getString()));
    }

    /**
     * 构造 tooltip 用的名字行。
     *
     * @param name 已解析的名字
     * @return 带颜色的文本；名字里没有可显示内容时返回 {@code null}（调用方保持原样）
     */
    public static Text buildNameLine(TotemNameText name) {
        if (name.isEmpty()) {
            return null;
        }

        String plain = name.plainText();
        TotemGradient gradient = name.gradient();
        if (gradient == null) {
            return styled(plain, name.effectiveColor());
        }

        // 渐变 / 彩虹：逐字采样。相位与动画共用，_jeb 彩蛋在 tooltip 里也会流动
        // （tooltip 每帧重建，所以这个效果是自动的）。
        float phase = TotemGradient.currentPhase();
        int[] codePoints = plain.codePoints().toArray();
        int count = codePoints.length;
        MutableText result = Text.empty();
        for (int i = 0; i < count; i++) {
            // 只有一个字时 t 取 0，避免除以 0
            float t = count <= 1 ? 0.0F : (float) i / (count - 1);
            result.append(styled(new String(codePoints, i, 1), gradient.sample(t, phase)));
        }
        return result;
    }

    private static MutableText styled(String content, int rgb) {
        // 保留原版的斜体：原版对所有带 custom_name 的物品都会把名字行设成 ITALIC
        return Text.literal(content).setStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(rgb))
                .withItalic(true));
    }
}
