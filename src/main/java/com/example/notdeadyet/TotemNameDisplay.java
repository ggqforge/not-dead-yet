package com.example.notdeadyet;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 不死图腾名称动画 —— 客户端模组入口。
 *
 * <p>功能：被铁砧改名过的不死图腾触发保命动画时，用原版像素字体把名字画在
 * 动画中的图腾正中央之前。颜色默认取图腾贴图上的亮面金 {@code #EADB84}，
 * 也可以由名字末尾的设置块指定（见 {@link com.example.notdeadyet.client.TotemNameText}）。</p>
 *
 * <p>动画绘制在 {@code GameRendererMixin}；名字在物品栏 tooltip 与快捷栏上方的
 * 显示分别是 {@code ItemStackMixin} 与 {@code InGameHudMixin}。
 * 这里只负责打一行加载日志。本模组是纯客户端模组，只需要装在客户端，且不依赖 Fabric API。</p>
 */
public class TotemNameDisplay implements ClientModInitializer {
    public static final String MOD_ID = "notdeadyet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[{}] 不死图腾名称动画已加载", MOD_ID);
    }
}
