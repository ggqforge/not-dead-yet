package com.example.notdeadyet;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 不死图腾名称显示 —— 客户端模组入口。
 *
 * <p>功能：被铁砧改名过的不死图腾触发保命动画时，用原版像素字体把名字画在
 * 动画中的图腾正中央之前，颜色与字效遵循原版设定（默认白字+阴影，支持 § 颜色代码）。</p>
 *
 * <p>全部逻辑都在 Mixin（{@code GameRendererMixin}）里完成，这里只负责打一行加载日志。
 * 本模组是纯客户端模组，只需要装在客户端，且不依赖 Fabric API。</p>
 */
public class TotemNameDisplay implements ClientModInitializer {
    public static final String MOD_ID = "notdeadyet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[{}] 不死图腾名称显示已加载", MOD_ID);
    }
}
