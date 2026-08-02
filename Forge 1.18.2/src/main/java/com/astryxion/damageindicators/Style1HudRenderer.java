package com.astryxion.damageindicators;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * Style 1 HUD — original Retro Damage Indicators (208x78 panel, dual scissor).
 */
public final class Style1HudRenderer {
    private static final ResourceLocation DAMAGE_INDICATOR_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator.png");
    private static final ResourceLocation DAMAGE_INDICATOR_BACKGROUND_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation DAMAGE_INDICATOR_HEALTH_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator_health.png");
    private static final Quaternion ENTITY_ROTATION;

    static {
        Quaternion rotation = Vector3f.XP.rotationDegrees(30.0F);
        rotation.mul(Vector3f.YP.rotationDegrees(130.0F));
        rotation.mul(Vector3f.ZP.rotationDegrees(180.0F));
        ENTITY_ROTATION = rotation;
    }

    private Style1HudRenderer() {
    }

    public static void render(RenderGameOverlayEvent.Pre event, LivingEntity entity, MobTypes mobType, boolean renderModelOnly) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = entity.getMaxHealth();
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
        float scale = cfg.hudIndicatorSize.get().floatValue();
        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : event.getWindow().getGuiScaledWidth() - (int) (208 * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : event.getWindow().getGuiScaledHeight() - (int) (78 * scale) - cfg.hudIndicatorPositionY.get();
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
        PoseStack poseStack = event.getMatrixStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset - 0.5F, 0);
        poseStack.scale(scale, scale, scale);

        // upper half entity render box scissor coords.
        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int scissorBox1MaxY = 49;
        // lower half entity render box scissor coords.
        int scissorBox2MinX = 28;
        int scissorBox2MinY = 49;
        int scissorBox2MaxX = 73;
        int scissorBox2MaxY = 61;
        int entityX = 45;
        int entityY = 56;

        float biggestEntityDimension = Math.max(entity.getBbWidth() * 1.2F + 0.3F, entity.getBbHeight() * 0.9F) * 0.85F;
        float renderScale = cfg.hudEntitySize.get().floatValue();
        if ((double) biggestEntityDimension > 0.5D) {
            renderScale /= biggestEntityDimension;
        }

        // Original 1.20.1 order was entity → panel@z=-200 (depth keeps portrait in front).
        // On 1.18.2, translucent mobs (e.g. slime) do not write depth, so the opaque
        // portrait fill in the background texture grayed them out. Same layering intent
        // via draw order: background, then entity, then frame/chrome on top.
        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        //background render
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ZERO, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundOpacity);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, DAMAGE_INDICATOR_BACKGROUND_TEXTURE);
        GuiComponent.blit(poseStack, 0, 0, 50, 0, 0, 208, 78, 256, 256);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();

        //render the first half of the entity (above y = 49)
        enableScissor(xOffset + Math.round(scale * scissorBox1MinX), yOffset + Math.round(scale * scissorBox1MinY), xOffset + Math.round(scale * scissorBox1MaxX), yOffset + Math.round(scale * scissorBox1MaxY));
        renderEntityInGui(poseStack, bufferSource, entityX, entityY, renderScale, ENTITY_ROTATION, entity, event.getPartialTicks(), renderModelOnly);
        disableScissor();
        //render the second half of the entity (below y = 49)
        enableScissor(xOffset + Math.round(scale * scissorBox2MinX), yOffset + Math.round(scale * scissorBox2MinY), xOffset + Math.round(scale * scissorBox2MaxX), yOffset + Math.round(scale * scissorBox2MaxY));
        renderEntityInGui(poseStack, bufferSource, entityX, entityY, renderScale, ENTITY_ROTATION, entity, event.getPartialTicks(), renderModelOnly);
        disableScissor();

        // Clear depth so frame/chrome drawn after the portrait is not occluded by the entity.
        RenderSystem.clear(256, Minecraft.ON_OSX);

        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ZERO, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        //foreground render
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, DAMAGE_INDICATOR_TEXTURE);
        GuiComponent.blit(poseStack, 0, 0, 50, 0, 0, 208, 78, 256, 256);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // mob type render
        int relativeMobTypeX = 5;
        int relativeMobTypeY = 55;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, mobType.getTexture());
        GuiComponent.blit(poseStack, relativeMobTypeX, relativeMobTypeY, 50, 0, 0, 18, 18, 18, 18);

        //health render
        int healthbarVOffset = cfg.colorblindHealthBar.get() ? 36 : 0;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, DAMAGE_INDICATOR_HEALTH_TEXTURE);
        GuiComponent.blit(poseStack, relativeHealthbarX, relativeHealthbarY, 50, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
        GuiComponent.blit(poseStack, relativeHealthbarX, relativeHealthbarY, 50, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

        poseStack.popPose();

        //health text
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
        Component healthComponent = new TextComponent(healthText);
        int healthWidth = Minecraft.getInstance().font.width(healthComponent);
        float healthScale = Math.min(88F / (float) healthWidth, 1.35F);
        int healthColor = 0XFFFFFF;
        int healthOutlineColor = 0;

        poseStack.pushPose();
        poseStack.translate(healthOffsetX, healthOffsetY, 0);
        poseStack.scale(healthScale, healthScale, 1);
        poseStack.translate(-firstHalfWidth, 0, -50);
        if (cfg.hudHealthTextOutline.get()) {
            Minecraft.getInstance().font.drawInBatch8xOutline(healthComponent.getVisualOrderText(), 0.0F, 0.0F, healthColor, healthOutlineColor, poseStack.last().pose(), bufferSource, 15728880);
        } else {
            Minecraft.getInstance().font.drawInBatch(healthComponent.getVisualOrderText(), 0.0F, 0.0F, healthColor, true, poseStack.last().pose(), bufferSource, false, 0, 15728880);
        }
        poseStack.popPose();

        //name text
        Component nameComponent = entity.getDisplayName();
        int nameWidth = Minecraft.getInstance().font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);
        float nameOffsetX = 138.5F;
        float nameOffsetY = 6.5F;
        int nameColor = 0XFFFFFF;
        int nameOutlineColor = 0;

        poseStack.pushPose();
        poseStack.translate(nameOffsetX, nameOffsetY, 0);
        poseStack.scale(nameScale, nameScale, 1);
        poseStack.translate(-nameWidth / 2F, 0, -50);
        if (cfg.hudNameTextOutline.get()) {
            Minecraft.getInstance().font.drawInBatch8xOutline(nameComponent.getVisualOrderText(), 0.0F, 0.0F, nameColor, nameOutlineColor, poseStack.last().pose(), bufferSource, 15728880);
        } else {
            Minecraft.getInstance().font.drawInBatch(nameComponent.getVisualOrderText(), 0.0F, 0.0F, nameColor, true, poseStack.last().pose(), bufferSource, false, 0, 15728880);
        }
        poseStack.popPose();

        bufferSource.endBatch();
        poseStack.popPose();
    }

    public static void renderEntityInGui(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int xPos, int yPos, float scale, Quaternion rotation, Entity entity, float partialTicks, boolean renderModelOnly) {
        poseStack.pushPose();
        poseStack.translate((double) xPos, (double) yPos, -60.0D);
        poseStack.mulPoseMatrix(Matrix4f.createScaleMatrix(scale, scale, (-scale)));
        poseStack.mulPose(rotation);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        Vector3f light0 = new Vector3f(1, -1.0F, -1.0F);
        light0.normalize();
        Vector3f light1 = new Vector3f(-1, 1.0F, 1.0F);
        light1.normalize();
        RenderSystem.setShaderLights(light0, light1);
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);
        if (renderModelOnly && entityrenderdispatcher.getRenderer(entity) instanceof LivingEntityRenderer livingEntityRenderer) {
            poseStack.translate(0, 1.5F, 0.0D);
            poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
            RenderType renderType = livingEntityRenderer.getModel().renderType(livingEntityRenderer.getTextureLocation(entity));
            livingEntityRenderer.getModel().renderToBuffer(poseStack, bufferSource.getBuffer(renderType), 15728880, LivingEntityRenderer.getOverlayCoords((LivingEntity) entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            float f = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
            if (entity instanceof LivingEntity living) {
                float f1 = living.yBodyRotO + (living.yBodyRot - living.yBodyRotO) * partialTicks;
                poseStack.mulPose(Vector3f.YN.rotationDegrees(-f1));
            } else {
                poseStack.mulPose(Vector3f.YN.rotationDegrees(-f));
            }
            RenderSystem.runAsFancy(() -> {
                entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, bufferSource, 15728880);
            });
        }
        bufferSource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);
        poseStack.popPose();
        Lighting.setupFor3DItems();
    }

    /** 1.18.2 equivalent of GuiGraphics.enableScissor(minX, minY, maxX, maxY). */
    private static void enableScissor(int minX, int minY, int maxX, int maxY) {
        Window window = Minecraft.getInstance().getWindow();
        int width = Math.max(0, maxX - minX);
        int height = Math.max(0, maxY - minY);
        double guiScale = window.getGuiScale();
        int scissorX = (int) (minX * guiScale);
        int scissorY = (int) ((window.getGuiScaledHeight() - maxY) * guiScale);
        int scissorW = (int) (width * guiScale);
        int scissorH = (int) (height * guiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
    }

    private static void disableScissor() {
        RenderSystem.disableScissor();
    }
}
