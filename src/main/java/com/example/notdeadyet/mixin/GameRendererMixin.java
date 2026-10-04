package com.example.notdeadyet.mixin;

import com.example.notdeadyet.client.TotemNameOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把名字<b>直接挂到</b>原版不死图腾触发动画上。
 *
 * <h2>动画到底在哪</h2>
 *
 * <p>1.21.1 里图腾保命动画<b>不是</b> 3D 世界里的粒子或实体，而是纯 HUD 绘制：</p>
 *
 * <pre>
 * 服务端  LivingEntity.tryUseDeathProtector()
 *   └─ 发出 EntityStatusS2CPacket(status = 35)
 * 客户端  ClientPlayNetworkHandler.onEntityStatus()
 *   └─ GameRenderer.showFloatingItem(getActiveTotemOfUndying(player))   // 计时器置 40
 * 每帧    GameRenderer.render()
 *   └─ if (!options.hudHidden) renderFloatingItem(DrawContext, tickDelta)
 * </pre>
 *
 * <p>所以「把文字贴到动画的图腾前面」最正确的做法，就是钻进
 * {@code renderFloatingItem} 里面、而且是在它画完图腾之后。</p>
 *
 * <h2>为什么注入在 MatrixStack.pop() 之前</h2>
 *
 * <p>{@code renderFloatingItem} 的字节码里只有一处 {@code MatrixStack.pop()}，
 * 它紧跟在</p>
 *
 * <pre>
 * context.draw(() -> client.getItemRenderer().renderItem(floatingItem,
 *         ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV,
 *         matrixStack, context.getVertexConsumers(), client.world, 0));
 * </pre>
 *
 * <p>之后。注入到这里，正好是「图腾已画完、矩阵还没弹」的瞬间：
 * 既拿到了同一帧的动画状态，又保证了绘制顺序排在图腾后面（也就是画面上更靠前）。</p>
 *
 * <p>借助这个注入点，文字与动画的绑定是<b>结构性</b>的：图腾什么时候播、什么时候被
 * F1 隐藏，文字都一样，不存在自己另算一套导致对不齐的可能。</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /** 原版字段 {@code private ItemStack floatingItem;} —— 就是正在播动画的那个图腾。 */
    @Shadow
    private ItemStack floatingItem;

    /** 原版字段 {@code private int floatingItemTimeLeft;} —— 40 起递减，&lt;= 0 时不绘制。 */
    @Shadow
    private int floatingItemTimeLeft;

    /** 原版字段 {@code private float floatingItemWidth;} —— 横向摆动幅度。 */
    @Shadow
    private float floatingItemWidth;

    /** 原版字段 {@code private float floatingItemHeight;} —— 纵向摆动幅度。 */
    @Shadow
    private float floatingItemHeight;

    @Inject(
            method = "renderFloatingItem(Lnet/minecraft/client/gui/DrawContext;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V")
    )
    private void notdeadyet$drawTotemName(DrawContext context, float tickDelta, CallbackInfo ci) {
        TotemNameOverlay.render(context, tickDelta,
                this.floatingItem, this.floatingItemTimeLeft,
                this.floatingItemWidth, this.floatingItemHeight);
    }
}
