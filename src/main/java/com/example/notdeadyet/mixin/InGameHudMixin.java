package com.example.notdeadyet.mixin;

import com.example.notdeadyet.client.TotemTooltip;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 快捷栏上方那行「手持物品名称」。
 *
 * <p>原版实现是：</p>
 *
 * <pre>
 * Text text = Text.empty().append(this.currentStack.getName())
 *         .formatted(this.currentStack.getRarity().getFormatting());
 * if (this.currentStack.contains(DataComponentTypes.CUSTOM_NAME)) {
 *     text.formatted(Formatting.ITALIC);
 * }
 * </pre>
 *
 * <p>所以只要把那一句 {@code getName()} 换掉即可 —— 原版的居中位置、淡出计时、
 * 亮度与阴影全部照旧，不必重写整个方法。</p>
 *
 * <p>同理<b>不能</b>改 {@code getName()} 本身，原因见 {@link ItemStackMixin}。</p>
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Redirect(method = "renderHeldItemTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"))
    private Text notdeadyet$heldItemName(ItemStack stack) {
        // 这里再调 stack.getName() 是安全的：@Redirect 只替换目标方法里那一条指令，
        // 不会影响本方法内部的调用。
        Text replacement = TotemTooltip.displayedName(stack);
        return replacement != null ? replacement : stack.getName();
    }
}
