package com.astryxion.damageindicators;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class DamageIndicatorsClient implements ClientModInitializer {

    private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(DamageIndicators.MODID, "damage_indicator_hud");

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.BOSS_BAR,
                HUD_ELEMENT_ID,
                DamageIndicators::renderHudBeforeBossBar
        );
        ClientTickEvents.END_CLIENT_TICK.register(DamageIndicators::onClientTick);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(DamageIndicators::onRenderLevelAfterSolidFeatures);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("damageindicators")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config")
                                .executes(ctx -> {
                                    Minecraft.getInstance().setScreen(new DamageIndicatorsConfigScreen(Minecraft.getInstance().screen));
                                    return 1;
                                }))));
    }
}
