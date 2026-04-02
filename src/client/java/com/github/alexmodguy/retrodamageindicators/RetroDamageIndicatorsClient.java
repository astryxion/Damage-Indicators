package com.github.alexmodguy.retrodamageindicators;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class RetroDamageIndicatorsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(Loader.DAMAGE_INDICATOR_PARTICLE, (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new DamageIndicatorParticle(level, x, y, z, 0, false));
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().screen == null) {
                GuiGraphics guiGraphics = (GuiGraphics) drawContext;
                int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                float partialTick = tickDelta.getGameTimeDeltaPartialTick(false);
                RetroDamageIndicators.onRenderHud(guiGraphics, partialTick, width, height);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> RetroDamageIndicators.onClientTick());
    }
}
