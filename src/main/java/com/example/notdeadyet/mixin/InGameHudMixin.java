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

    // require = 0 是刻意加的：@Redirect 对同一条指令是「独占」的，万一有别的 HUD /
    // 提示框类模组也重定向了这一句 getName()，Mixin 就无法应用。
    // 本模组的 mixin 配置是 required: true / defaultRequire: 1，
    // 不写 require = 0 的话会让玩家**游戏直接启动崩溃**；
    // 写了之后最坏情况只是「快捷栏名称」这一小功能失效，并在日志里留下警告。
    @Redirect(method = "renderHeldItemTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"),
            require = 0)
    private Text notdeadyet$heldItemName(ItemStack stack) {
        // 这里再调 stack.getName() 是安全的：@Redirect 只替换目标方法里那一条指令，
        // 不会影响本方法内部的调用。
        Text replacement = TotemTooltip.displayedName(stack);
        return replacement != null ? replacement : stack.getName();
    }
}
