package com.example.notdeadyet.mixin;

import com.example.notdeadyet.client.TotemTooltip;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 把物品栏 tooltip 里的名字行换成「剥掉设置块 + 动画同款颜色」的版本。
 *
 * <p><b>为什么不动 {@link ItemStack#getName()}</b>：这是本模组最容易踩的坑。
 * 反编译确认过，{@code AnvilScreenHandler.updateResult()} 和
 * {@code AnvilScreen.onSlotUpdate(...)} 都是靠 {@code getName()} 读名字的：</p>
 *
 * <ul>
 *   <li>客户端 {@code onSlotUpdate} 用它回填铁砧输入框；</li>
 *   <li>服务端 {@code updateResult} 用它判断「名字有没有被改过」。</li>
 * </ul>
 *
 * <p>一旦在 {@code getName()} 里把设置块剥掉，输入框会回填成没有设置块的文本，
 * 服务端也会认为名字变了、拿输入框内容去覆盖 {@code custom_name} ——
 * 结果就是<b>重新命名一次就把颜色和尺寸设置弄丢</b>。
 * 所以这里只改 tooltip，{@code custom_name} 本体保持原样。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void notdeadyet$rewriteTotemName(Item.TooltipContext context, PlayerEntity player,
                                             TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        Text replacement = TotemTooltip.displayedName(stack);
        if (replacement == null) {
            return;
        }

        List<Text> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) {
            return;
        }

        // 原版把 getName() 的结果放在第一行。比对字符串确认，避免改动其它行
        // （比如附魔、耐久、提示之类）。
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null || !lines.get(0).getString().equals(customName.getString())) {
            return;
        }

        List<Text> result = new ArrayList<>(lines);
        result.set(0, replacement);
        cir.setReturnValue(result);
    }
}
