package com.astryxion.damageindicators;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Style 1 HUD — original Retro Damage Indicators (208x78 panel, dual scissor).
 */
public final class Style1HudRenderer {
    private static final ResourceLocation DAMAGE_INDICATOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator.png");
    private static final ResourceLocation DAMAGE_INDICATOR_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation DAMAGE_INDICATOR_HEALTH_TEXTURE = ResourceLocation.fromNamespaceAndPath(DamageIndicators.MODID, "textures/gui/damage_indicator_health.png");
    private static final Quaternionf ENTITY_ROTATION = (new Quaternionf()).rotationXYZ((float) Math.toRadians(30), (float) Math.toRadians(130), (float) Math.PI);

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
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset - 0.5F, 0);
        poseStack.scale(scale, scale, scale);

        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int scissorBox1MaxY = 49;
        int scissorBox2MinX = 28;
        int scissorBox2MinY = 49;
        int scissorBox2MaxX = 73;
        int scissorBox2MaxY = 61;
        int entityX = 45;
        int entityY = 56;

        guiGraphics.enableScissor(xOffset + Math.round(scale * scissorBox1MinX), yOffset + Math.round(scale * scissorBox1MinY), xOffset + Math.round(scale * scissorBox1MaxX), yOffset + Math.round(scale * scissorBox1MaxY));
        float biggestEntityDimension = Math.max(entity.getBbWidth() * 1.2F + 0.3F, entity.getBbHeight() * 0.9F) * 0.85F;
        float renderScale = cfg.hudEntitySize.get().floatValue();
        if ((double) biggestEntityDimension > 0.5D) {
            renderScale /= biggestEntityDimension;
        }
        renderEntityInGui(guiGraphics, entityX, entityY, renderScale, ENTITY_ROTATION, entity, partialTick, renderModelOnly);
        guiGraphics.disableScissor();
        guiGraphics.enableScissor(xOffset + Math.round(scale * scissorBox2MinX), yOffset + Math.round(scale * scissorBox2MinY), xOffset + Math.round(scale * scissorBox2MaxX), yOffset + Math.round(scale * scissorBox2MaxY));
        renderEntityInGui(guiGraphics, entityX, entityY, renderScale, ENTITY_ROTATION, entity, partialTick, renderModelOnly);
        guiGraphics.disableScissor();

        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundOpacity);
        guiGraphics.blit(DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256);

        int relativeMobTypeX = 5;
        int relativeMobTypeY = 55;
        guiGraphics.blit(mobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18);

        int healthbarVOffset = cfg.colorblindHealthBar.get() ? 36 : 0;
        guiGraphics.blit(DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
        guiGraphics.blit(DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

        poseStack.popPose();

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

        poseStack.pushPose();
        poseStack.translate(healthOffsetX, healthOffsetY, 0);
        poseStack.scale(healthScale, healthScale, 1);
        poseStack.translate(-firstHalfWidth, 0, -50);
        drawHudText(guiGraphics, font, healthComponent, 0, 0, 0xFFFFFF, cfg.hudHealthTextOutline.get());
        poseStack.popPose();

        Component nameComponent = entity.getDisplayName();
        int nameWidth = font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);
        float nameOffsetX = 138.5F;
        float nameOffsetY = 6.5F;

        poseStack.pushPose();
        poseStack.translate(nameOffsetX, nameOffsetY, 0);
        poseStack.scale(nameScale, nameScale, 1);
        poseStack.translate(-nameWidth / 2F, 0, -50);
        drawHudText(guiGraphics, font, nameComponent, 0, 0, 0xFFFFFF, cfg.hudNameTextOutline.get());
        poseStack.popPose();

        poseStack.popPose();
        guiGraphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
    }

    private static void drawHudText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color, boolean outline) {
        if (outline) {
            font.drawInBatch8xOutline(text.getVisualOrderText(), x, y, 0xFF000000 | (color & 0xFFFFFF), 0xFF000000,
                    guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), 15728880);
            guiGraphics.flush();
        } else {
            guiGraphics.drawString(font, text, x, y, 0xFF000000 | (color & 0xFFFFFF), true);
        }
    }

    public static void renderEntityInGui(GuiGraphics guiGraphics, int xPos, int yPos, float scale, Quaternionf rotation, Entity entity, float partialTicks, boolean renderModelOnly) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xPos, yPos, -60.0D);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(rotation);

        Vector3f light0 = new Vector3f(1, -1.0F, -1.0F).normalize();
        Vector3f light1 = new Vector3f(-1, 1.0F, 1.0F).normalize();
        RenderSystem.setShaderLights(light0, light1);
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);
        if (renderModelOnly && entityrenderdispatcher.getRenderer(entity) instanceof LivingEntityRenderer<?, ?> livingEntityRenderer) {
            guiGraphics.pose().translate(0, 1.5F, 0.0D);
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
            @SuppressWarnings({"unchecked", "rawtypes"})
            LivingEntityRenderer renderer = livingEntityRenderer;
            RenderType renderType = renderer.getModel().renderType(renderer.getTextureLocation(entity));
            int overlay = LivingEntityRenderer.getOverlayCoords((LivingEntity) entity, 0.0F);
            renderer.getModel().renderToBuffer(guiGraphics.pose(), guiGraphics.bufferSource().getBuffer(renderType), 15728880, overlay);
        } else {
            float f = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
            if (entity instanceof LivingEntity living) {
                float f1 = living.yBodyRotO + (living.yBodyRot - living.yBodyRotO) * partialTicks;
                guiGraphics.pose().mulPose(Axis.YN.rotationDegrees(-f1));
            } else {
                guiGraphics.pose().mulPose(Axis.YN.rotationDegrees(-f));
            }
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
        }
        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
}
