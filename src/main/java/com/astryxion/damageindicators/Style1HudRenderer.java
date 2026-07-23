package com.astryxion.damageindicators;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

/**
 * Style 1 HUD — original Retro Damage Indicators (208x78 panel).
 * Portrait path based on the proven Forge 1.16.5 Retro DI port.
 */
public final class Style1HudRenderer {
    private static final float TORO_PORT_ENTITY_RENDER_H = 30.0F;
    private static final float TORO_PORT_ENTITY_RENDER_W = 18.0F;
    private static final float TORO_PORT_LAYOUT_SIZE = 40.0F;

    private static final ResourceLocation DAMAGE_INDICATOR_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator.png");
    private static final ResourceLocation DAMAGE_INDICATOR_BACKGROUND_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation DAMAGE_INDICATOR_HEALTH_TEXTURE = new ResourceLocation(DamageIndicators.MODID, "textures/gui/damage_indicator_health.png");
    private static final Quaternion ENTITY_ROTATION;

    static {
        Quaternion q = Vector3f.XP.rotation((float) Math.toRadians(30.0D));
        q.mul(Vector3f.YP.rotation((float) Math.toRadians(130.0D)));
        q.mul(Vector3f.ZP.rotation((float) Math.PI));
        ENTITY_ROTATION = q;
    }

    private Style1HudRenderer() {
    }

    private static void enableHudGuiScissor(MainWindow mw, int guiLeft, int guiTop, int guiRight, int guiBottomInclusive) {
        double sf = mw.getGuiScale();
        int fw = mw.getWidth();
        int fh = mw.getHeight();
        int x1 = Math.min(guiLeft, guiRight);
        int x2 = Math.max(guiLeft, guiRight);
        int y1 = Math.min(guiTop, guiBottomInclusive);
        int y2 = Math.max(guiTop, guiBottomInclusive);
        int vx = MathHelper.clamp((int) Math.round(x1 * sf), 0, fw);
        int vw = MathHelper.clamp((int) Math.round((x2 - x1) * sf), 0, fw - vx);
        int vy = MathHelper.clamp(fh - (int) Math.round(y2 * sf), 0, fh);
        int vh = MathHelper.clamp((int) Math.round((y2 - y1) * sf), 0, fh - vy);
        RenderSystem.enableScissor(vx, vy, vw, vh);
    }

    private static void hudBlit(MatrixStack stack, Minecraft mc, ResourceLocation texture, int x, int y, int blitOffset, float texU, float texV, int width, int height, int textureWidth, int textureHeight) {
        mc.textureManager.bind(texture);
        AbstractGui.blit(stack, x, y, blitOffset, texU, texV, width, height, textureWidth, textureHeight);
    }

    private static int toroHudMiniaturePortraitScale(LivingEntity entity, float hudEntitySizeCfg, float nominalCfg) {
        float ew = Math.max(entity.getBbWidth(), 0.001F);
        float eh = Math.max(entity.getBbHeight(), 0.001F);
        int scaleY = MathHelper.ceil(TORO_PORT_ENTITY_RENDER_H / eh);
        int scaleX = MathHelper.ceil(TORO_PORT_ENTITY_RENDER_W / ew);
        float eff = Math.min(scaleX, scaleY);
        if (entity instanceof ChickenEntity) {
            eff *= 0.7F;
        }
        eff *= hudEntitySizeCfg / Math.max(nominalCfg, 1.0F);
        return MathHelper.clamp(Math.round(eff), 6, 96);
    }

    private static int toroHudPortraitPivotTexY(LivingEntity entity, int portraitClipTopTex, int portraitClipInnerH) {
        float pivotYTor = TORO_PORT_LAYOUT_SIZE * 0.5F + TORO_PORT_ENTITY_RENDER_H * 0.5F;
        if (entity instanceof GhastEntity) {
            pivotYTor -= 10.0F;
        }
        return portraitClipTopTex + Math.round(portraitClipInnerH * (pivotYTor / TORO_PORT_LAYOUT_SIZE));
    }

    private static int toroHudPortraitPivotTexX(int portraitClipLeftTex, int portraitClipInnerW) {
        float pivotXTor = TORO_PORT_LAYOUT_SIZE * 0.5F;
        return portraitClipLeftTex + Math.round(portraitClipInnerW * (pivotXTor / TORO_PORT_LAYOUT_SIZE));
    }

    public static void render(RenderGameOverlayEvent.Post event, LivingEntity entity, MobTypes mobType, boolean renderModelOnly) {
        Minecraft mc = Minecraft.getInstance();
        Config.StyleSettings cfg = Config.INSTANCE.active();
        MatrixStack poseStack = event.getMatrixStack();
        MainWindow mw = event.getWindow();
        float pt = event.getPartialTicks();

        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = entity.getMaxHealth();
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
        float scale = cfg.hudIndicatorSize.get().floatValue();
        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : mw.getGuiScaledWidth() - Math.round(208 * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : mw.getGuiScaledHeight() - Math.round(78 * scale) - cfg.hudIndicatorPositionY.get();

        // No boss-bar push (removed across all ports). Still nudge down for potion icons when top-right.
        if (cfg.hudIndicatorAlignTop.get() && !cfg.hudIndicatorAlignLeft.get()) {
            int potionsActive = 0;
            Collection<EffectInstance> active = mc.player != null ? mc.player.getActiveEffects() : java.util.Collections.<EffectInstance>emptySet();
            for (EffectInstance effectInstance : active) {
                if (effectInstance.showIcon()) {
                    potionsActive++;
                }
            }
            yOffset += Math.min(potionsActive, 2) * 24;
        }

        float backgroundOpacity = cfg.hudIndicatorBackgroundOpacity.get().floatValue();

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset - 0.5F, 0);
        poseStack.scale(scale, scale, scale);

        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int portraitClipBotTex = 61;
        int portraitClipInnerW = scissorBox1MaxX - scissorBox1MinX;
        int portraitClipInnerH = portraitClipBotTex - scissorBox1MinY;
        float hudEntityCfg = cfg.hudEntitySize.get().floatValue();
        float hudModelMul = hudEntityCfg;
        float biggestEntityDimension = Math.max(entity.getBbWidth() * 1.2F + 0.3F, entity.getBbHeight() * 0.9F) * 0.85F;
        if ((double) biggestEntityDimension > 0.5D) {
            hudModelMul /= biggestEntityDimension;
        }
        int entityX = toroHudPortraitPivotTexX(scissorBox1MinX, portraitClipInnerW);
        int entityY = toroHudPortraitPivotTexY(entity, scissorBox1MinY, portraitClipInnerH);
        float yHudBase = yOffset - 0.5F;
        int absPortraitX = Math.round(xOffset + entityX * scale);
        int absPortraitY = Math.round(yHudBase + entityY * scale);
        int portraitScale = toroHudMiniaturePortraitScale(entity, hudEntityCfg, 38.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, backgroundOpacity);
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 50, 0.0F, 0.0F, 208, 78, 256, 256);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_TEXTURE, 0, 0, 50, 0.0F, 0.0F, 208, 78, 256, 256);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();

        int clipLeft = Math.round(scale * scissorBox1MinX + xOffset);
        int clipRight = Math.round(scale * scissorBox1MaxX + xOffset);
        int clipTop = Math.round(scale * scissorBox1MinY + yOffset);
        int clipBot = Math.round(scale * portraitClipBotTex + yOffset);
        enableHudGuiScissor(mw, clipLeft, clipTop, clipRight, clipBot);
        renderHudEntityPortrait(poseStack, absPortraitX, absPortraitY, entityX, entityY, portraitScale, hudModelMul, entity, mc, pt, renderModelOnly);
        RenderSystem.disableScissor();

        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        hudBlit(poseStack, mc, mobType.getTexture(), 5, 55, 50, 0.0F, 0.0F, 18, 18, 18, 18);

        int relativeHealthbarX = 81;
        int relativeHealthbarY = 25;
        int healthbarHeight = 18;
        int healthbarMaxWidth = 124;
        int currentHealthbarWidth = (int) Math.round(healthbarMaxWidth * healthRatio);
        int healthbarVOffset = cfg.colorblindHealthBar.get() ? 36 : 0;
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 50, 0.0F, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 50, 0.0F, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

        poseStack.popPose();

        String healthDivisor = cfg.healthSeperator.get() ? " | " : "/";
        float healthOffsetX = cfg.healthSeperator.get() ? 136 : 140;
        String healthText;
        int firstHalfWidth;
        if (cfg.healthDecimals.get()) {
            healthText = DamageIndicators.roundHealth(entityHealth) + healthDivisor + DamageIndicators.roundHealth(entityMaxHealth);
            firstHalfWidth = mc.font.width("" + DamageIndicators.roundHealth(entityHealth));
        } else {
            healthText = (int) entityHealth + healthDivisor + (int) entityMaxHealth;
            firstHalfWidth = mc.font.width("" + (int) entityHealth);
        }
        ITextComponent healthComponent = new StringTextComponent(healthText);
        int healthWidth = mc.font.width(healthComponent);
        float healthScale = Math.min(88F / (float) healthWidth, 1.35F);

        poseStack.pushPose();
        poseStack.translate(healthOffsetX, 30, 0);
        poseStack.scale(healthScale, healthScale, 1);
        poseStack.translate(-firstHalfWidth, 0, -50);
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        if (cfg.hudHealthTextOutline.get()) {
            DamageIndicators.drawInBatch8xOutline(mc.font, healthComponent.getVisualOrderText(), poseStack.last().pose(), 0.0F, 0.0F, 0xFFFFFF, 0, buffer, false, 15728880);
        } else {
            mc.font.drawInBatch(healthComponent.getVisualOrderText(), 0.0F, 0.0F, 0xFFFFFF, true, poseStack.last().pose(), buffer, false, 0, 15728880);
        }
        buffer.endBatch();
        poseStack.popPose();

        ITextComponent nameComponent = entity.getDisplayName();
        int nameWidth = mc.font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);

        poseStack.pushPose();
        poseStack.translate(138.5F, 6.5F, 0);
        poseStack.scale(nameScale, nameScale, 1);
        poseStack.translate(-nameWidth / 2F, 0, -50);
        IRenderTypeBuffer.Impl nameBuffer = mc.renderBuffers().bufferSource();
        if (cfg.hudNameTextOutline.get()) {
            DamageIndicators.drawInBatch8xOutline(mc.font, nameComponent.getVisualOrderText(), poseStack.last().pose(), 0.0F, 0.0F, 0xFFFFFF, 0, nameBuffer, false, 15728880);
        } else {
            mc.font.drawInBatch(nameComponent.getVisualOrderText(), 0.0F, 0.0F, 0xFFFFFF, true, poseStack.last().pose(), nameBuffer, false, 0, 15728880);
        }
        nameBuffer.endBatch();
        poseStack.popPose();

        poseStack.popPose();
        restoreHudGlState(mc);
    }

    /** Leave GL state safe for Screen/item rendering (creative tabs, etc.). */
    static void restoreHudGlState(Minecraft mc) {
        RenderSystem.disableScissor();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableLighting();
        net.minecraft.client.renderer.RenderHelper.setupForFlatItems();
        mc.getTextureManager().bind(net.minecraft.client.renderer.texture.AtlasTexture.LOCATION_BLOCKS);
    }

    private static void renderHudEntityPortrait(MatrixStack poseStack, int guiAbsX, int guiAbsY, int localPortraitX,
                                                int localPortraitY, int portraitScale, float hudEntityScaleMul, LivingEntity entity, Minecraft mc,
                                                float partialTicks, boolean renderModelOnly) {
        if (renderModelOnly) {
            renderEntityInHudModelOnly(poseStack, mc.renderBuffers().bufferSource(), mc, localPortraitX, localPortraitY,
                    hudEntityScaleMul, ENTITY_ROTATION, entity, partialTicks);
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        try {
            renderHudPortraitDirectGui(mc, guiAbsX, guiAbsY, portraitScale, -40.0F, -20.0F, entity, partialTicks);
        } finally {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            if (mc.level != null) {
                Entity cam = mc.cameraEntity != null ? mc.cameraEntity : mc.player;
                mc.getEntityRenderDispatcher().prepare(mc.level, mc.gameRenderer.getMainCamera(), cam);
            }
        }
    }

    private static void renderHudPortraitDirectGui(Minecraft mc, int guiAbsX, int guiAbsY, int portraitScale,
                                                   float lookYaw, float lookPitch, LivingEntity entity, float partialTicks) {
        float h = (float) Math.atan((double) (lookYaw / 40.0F));
        float lAngle = (float) Math.atan((double) (lookPitch / 40.0F));
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.pushPose();
        matrixStack.translate((float) guiAbsX, (float) guiAbsY, 1050.0F);
        matrixStack.scale(1.0F, 1.0F, -1.0F);
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.0D, 1000.0D);
        matrixStack.scale((float) portraitScale, (float) portraitScale, (float) portraitScale);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(lAngle * 20.0F);
        quaternion.mul(quaternion2);
        matrixStack.mulPose(quaternion);

        float bodyYawGui = 180.0F + h * 20.0F;
        float yawGui = 180.0F + h * 40.0F;
        float pitchGui = -lAngle * 20.0F;

        float saveYBodyRot = entity.yBodyRot;
        float saveYBodyRotO = entity.yBodyRotO;
        float saveYR = entity.yRot;
        float saveYRO = entity.yRotO;
        float saveXR = entity.xRot;
        float saveXRO = entity.xRotO;
        float savePrevHead = entity.yHeadRotO;
        float saveHead = entity.yHeadRot;

        entity.yBodyRot = bodyYawGui;
        entity.yBodyRotO = bodyYawGui;
        entity.yRot = yawGui;
        entity.yRotO = yawGui;
        entity.xRot = pitchGui;
        entity.xRotO = pitchGui;
        entity.yHeadRot = yawGui;
        entity.yHeadRotO = yawGui;

        EntityRendererManager dispatcher = mc.getEntityRenderDispatcher();
        quaternion2.conj();
        dispatcher.overrideCameraOrientation(quaternion2);
        dispatcher.setRenderShadow(false);
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        try {
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, matrixStack, buffer, 15728880);
        } finally {
            buffer.endBatch();
            dispatcher.setRenderShadow(true);
            entity.yBodyRot = saveYBodyRot;
            entity.yBodyRotO = saveYBodyRotO;
            entity.yRot = saveYR;
            entity.yRotO = saveYRO;
            entity.xRot = saveXR;
            entity.xRotO = saveXRO;
            entity.yHeadRotO = savePrevHead;
            entity.yHeadRot = saveHead;
            matrixStack.popPose();
            matrixStack.popPose();
        }
    }

    private static void renderEntityInHudModelOnly(MatrixStack poseStack, IRenderTypeBuffer bufferIn, Minecraft mc, int xPos, int yPos, float scale, Quaternion rotation, LivingEntity entity, float partialTicks) {
        if (!(mc.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingRenderer)) {
            return;
        }
        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        poseStack.translate((double) xPos, (double) yPos, 1050.0D);
        poseStack.scale(scale, scale, -scale);
        poseStack.mulPose(rotation);

        LightTexture lm = mc.gameRenderer.lightTexture();
        try {
            lm.updateLightTexture(partialTicks);
            lm.turnOnLightLayer();
            RenderSystem.disableFog();
            try {
                RenderSystem.runAsFancy(() -> {
                    Vector3f light0 = new Vector3f(1.0F, -1.0F, -1.0F);
                    light0.normalize();
                    Vector3f light1 = new Vector3f(-1.0F, 1.0F, 1.0F);
                    light1.normalize();
                    GlStateManager.setupGui3DDiffuseLighting(light0, light1);
                    RenderSystem.enableLighting();
                    net.minecraft.client.renderer.RenderHelper.setupFor3DItems();

                    EntityRendererManager entityrenderdispatcher = mc.getEntityRenderDispatcher();
                    entityrenderdispatcher.setRenderShadow(false);
                    @SuppressWarnings("unchecked")
                    LivingRenderer<LivingEntity, ?> livingEntityRenderer = (LivingRenderer) entityrenderdispatcher.getRenderer(entity);
                    poseStack.translate(0, 1.5F, 0.0D);
                    poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
                    ResourceLocation tex = livingEntityRenderer.getTextureLocation(entity);
                    RenderType renderType = RenderType.entityCutoutNoCull(tex);
                    livingEntityRenderer.getModel().renderToBuffer(poseStack, bufferIn.getBuffer(renderType), LightTexture.pack(15, 15),
                            LivingRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
                    if (bufferIn instanceof IRenderTypeBuffer.Impl) {
                        ((IRenderTypeBuffer.Impl) bufferIn).endBatch();
                    }
                    entityrenderdispatcher.setRenderShadow(true);
                });
            } finally {
                RenderSystem.enableCull();
                RenderSystem.enableFog();
            }
        } finally {
            lm.turnOffLightLayer();
        }
        poseStack.popPose();
        RenderSystem.disableLighting();
        net.minecraft.client.renderer.RenderHelper.setupFor3DItems();
    }

    /** Shared GL scissor helper for Style2. */
    static void enableScissor(MainWindow mw, int guiLeft, int guiTop, int guiRight, int guiBottomInclusive) {
        enableHudGuiScissor(mw, guiLeft, guiTop, guiRight, guiBottomInclusive);
    }

    /** Shared blit helper for Style2. */
    static void blit(MatrixStack stack, Minecraft mc, ResourceLocation texture, int x, int y, int blitOffset, float texU, float texV, int width, int height, int textureWidth, int textureHeight) {
        hudBlit(stack, mc, texture, x, y, blitOffset, texU, texV, width, height, textureWidth, textureHeight);
    }

    /** Shared portrait renderer for Style2 preview box. */
    static void renderPortraitAt(Minecraft mc, int guiAbsX, int guiAbsY, int portraitScale, LivingEntity entity, float partialTicks) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        try {
            renderHudPortraitDirectGui(mc, guiAbsX, guiAbsY, portraitScale, -30.0F, 0.0F, entity, partialTicks);
        } finally {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            if (mc.level != null) {
                Entity cam = mc.cameraEntity != null ? mc.cameraEntity : mc.player;
                mc.getEntityRenderDispatcher().prepare(mc.level, mc.gameRenderer.getMainCamera(), cam);
            }
        }
    }
}
