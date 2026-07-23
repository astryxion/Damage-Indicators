package com.astryxion.damageindicators;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2fStack;

/**
 * Style 1 HUD — original Retro Damage Indicators (208x78 panel, dual scissor).
 * Ported to Fabric 1.21.11 GuiGraphics (Matrix3x2fStack + RenderPipelines).
 */
public final class Style1HudRenderer {
    private static final Identifier DAMAGE_INDICATOR_TEXTURE = Identifier.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator.png");
    private static final Identifier DAMAGE_INDICATOR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator_background.png");
    private static final Identifier DAMAGE_INDICATOR_HEALTH_TEXTURE = Identifier.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator_health.png");

    private Style1HudRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, float partialTick, LivingEntity entity, MobTypes mobType, boolean renderModelOnly) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = entity.getMaxHealth();
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
        float scale = cfg.hudIndicatorSize.get().floatValue();
        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : guiGraphics.guiWidth() - (int) (208 * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : guiGraphics.guiHeight() - (int) (78 * scale) - cfg.hudIndicatorPositionY.get();
        if (cfg.hudIndicatorAlignTop.get() && !cfg.hudIndicatorAlignLeft.get()) {
            int potionsActive = 0;
            if (Minecraft.getInstance().player != null) {
                for (MobEffectInstance mobEffectInstance : Minecraft.getInstance().player.getActiveEffects()) {
                    if (mobEffectInstance.showIcon()) {
                        potionsActive++;
                    }
                }
            }
            yOffset += Math.min(potionsActive, 2) * 24;
        }
        float backgroundOpacity = cfg.hudIndicatorBackgroundOpacity.get().floatValue();
        int relativeHealthbarX = 81;
        int relativeHealthbarY = 25;
        int healthbarHeight = 18;
        int healthbarMaxWidth = 124;
        int currentHealthbarWidth = (int) Math.round(healthbarMaxWidth * healthRatio);
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(xOffset, yOffset - 0.5F);
        pose.scale(scale, scale);

        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int scissorBox2MaxY = 61;

        // Background first, then portrait (DI-NeoForge-1.21.11 order — avoids washing out the model).
        int bgColor = ((Mth.clamp((int) (backgroundOpacity * 255.0F), 0, 255)) << 24) | 0xFFFFFF;
        blit(guiGraphics, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, bgColor);

        int p1x0 = xOffset + Math.round(scale * scissorBox1MinX);
        int p1y0 = yOffset + Math.round(scale * scissorBox1MinY);
        int p1x1 = xOffset + Math.round(scale * scissorBox1MaxX);
        // Single combined portrait box (upper+lower scissor) like DI-NeoForge-1.21.11.
        int p2y1 = yOffset + Math.round(scale * scissorBox2MaxY);
        int desired = HudPortraitRenderer.style1Scale(entity, cfg.hudEntitySize.get().floatValue(), scale);
        int entityScale = HudPortraitRenderer.fitToPortraitBox(desired, entity, p1x1 - p1x0, p2y1 - p1y0);

        // DI-NeoForge-1.21.11: pop HUD pose before PIP submit.
        pose.popMatrix();
        guiGraphics.enableScissor(p1x0, p1y0, p1x1, p2y1);
        float centerX = (p1x0 + p1x1) / 2.0F;
        float centerY = (p1y0 + p2y1) / 2.0F;
        HudPortraitRenderer.renderFollowsMouse(
                guiGraphics, p1x0, p1y0, p1x1, p2y1, entityScale, 0.0625F,
                centerX + 17.0F, centerY - 12.0F, entity);
        guiGraphics.disableScissor();
        pose.pushMatrix();
        pose.translate(xOffset, yOffset - 0.5F);
        pose.scale(scale, scale);

        blit(guiGraphics, DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, 0xFFFFFFFF);

        int relativeMobTypeX = 5;
        int relativeMobTypeY = 55;
        blit(guiGraphics, mobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18, 0xFFFFFFFF);

        int healthbarVOffset = cfg.colorblindHealthBar.get() ? 36 : 0;
        blit(guiGraphics, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256, 0xFFFFFFFF);
        blit(guiGraphics, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256, 0xFFFFFFFF);

        String healthText;
        float healthOffsetX = 136;
        float healthOffsetY = 30;
        String healthDivisor;
        int firstHalfWidth;
        if (cfg.healthSeperator.get()) {
            healthDivisor = " | ";
        } else {
            healthDivisor = "/";
            healthOffsetX += 4;
        }
        if (cfg.healthDecimals.get()) {
            healthText = DamageIndicators.roundHealth(entityHealth) + healthDivisor + DamageIndicators.roundHealth(entityMaxHealth);
            firstHalfWidth = Minecraft.getInstance().font.width("" + DamageIndicators.roundHealth(entityHealth));
        } else {
            healthText = (int) entityHealth + healthDivisor + (int) entityMaxHealth;
            firstHalfWidth = Minecraft.getInstance().font.width("" + (int) entityHealth);
        }
        Component healthComponent = Component.literal(healthText);
        int healthWidth = Minecraft.getInstance().font.width(healthComponent);
        float healthScale = Math.min(88F / (float) healthWidth, 1.35F);
        Font font = Minecraft.getInstance().font;

        pose.pushMatrix();
        pose.translate(healthOffsetX, healthOffsetY);
        pose.scale(healthScale, healthScale);
        pose.translate(-firstHalfWidth, 0);
        drawHudText(guiGraphics, font, healthComponent, 0, 0, 0xFFFFFF, cfg.hudHealthTextOutline.get());
        pose.popMatrix();

        Component nameComponent = entity.getDisplayName();
        int nameWidth = font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);
        float nameOffsetX = 138.5F;
        float nameOffsetY = 6.5F;

        pose.pushMatrix();
        pose.translate(nameOffsetX, nameOffsetY);
        pose.scale(nameScale, nameScale);
        pose.translate(-nameWidth / 2F, 0);
        drawHudText(guiGraphics, font, nameComponent, 0, 0, 0xFFFFFF, cfg.hudNameTextOutline.get());
        pose.popMatrix();

        pose.popMatrix();
    }

    private static void blit(GuiGraphics guiGraphics, Identifier texture, int x, int y, float u, float v, int width, int height, int texW, int texH, int color) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, texW, texH, color);
    }

    private static void drawHudText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color, boolean outline) {
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        if (outline) {
            guiGraphics.drawString(font, text, x - 1, y, 0xFF000000, false);
            guiGraphics.drawString(font, text, x + 1, y, 0xFF000000, false);
            guiGraphics.drawString(font, text, x, y - 1, 0xFF000000, false);
            guiGraphics.drawString(font, text, x, y + 1, 0xFF000000, false);
        }
        guiGraphics.drawString(font, text, x, y, argb, !outline);
    }
}
