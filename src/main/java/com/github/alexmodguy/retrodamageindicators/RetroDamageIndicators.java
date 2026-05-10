package com.github.alexmodguy.retrodamageindicators;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.overlay.BossOverlayGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.potion.EffectInstance;
import net.minecraft.client.renderer.LightTexture;

import java.util.Collection;
import java.util.Map;

import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = RetroDamageIndicators.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RetroDamageIndicators {
    public static final String MODID = "retrodamageindicators";
    // ToroHealth EntityDisplay sizing (Forge 1.16.4) — match 1:1 in our hud texture clip.
    private static final float TORO_PORT_ENTITY_RENDER_H = 30.0F;
    private static final float TORO_PORT_ENTITY_RENDER_W = 18.0F;
    private static final float TORO_PORT_LAYOUT_SIZE = 40.0F;
    private static final ResourceLocation DAMAGE_INDICATOR_TEXTURE = new ResourceLocation(MODID, "textures/gui/damage_indicator.png");
    private static final ResourceLocation DAMAGE_INDICATOR_BACKGROUND_TEXTURE = new ResourceLocation(MODID, "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation DAMAGE_INDICATOR_HEALTH_TEXTURE = new ResourceLocation(MODID, "textures/gui/damage_indicator_health.png");
    private static final Quaternion ENTITY_ROTATION;

    static {
        Quaternion q = Vector3f.XP.rotation((float)Math.toRadians(30.0D));
        q.mul(Vector3f.YP.rotation((float)Math.toRadians(130.0D)));
        q.mul(Vector3f.ZP.rotation((float)Math.PI));
        ENTITY_ROTATION = q;
    }

    private static LivingEntity damageIndicatorEntity;
    private static MobTypes currentMobType = MobTypes.UNKNOWN;
    private static int resetDamageIndicatorEntityIn = 0;
    private static boolean renderModelOnly;

    /**
     * Boss bar count for layout offset. Field is {@code events} under official mappings, {@code field_184060_g} under
     * SRG/production (CurseForge). Reflection must use the runtime bytecode name or the mod crashes at class init.
     */
    private static final java.lang.reflect.Field BOSS_OVERLAY_EVENTS_FIELD = bossOverlayResolveEventsField();

    private static java.lang.reflect.Field bossOverlayResolveEventsField() {
        String[] bytecodeNames = new String[] {"field_184060_g", "events"};
        for (String name : bytecodeNames) {
            try {
                java.lang.reflect.Field f = BossOverlayGui.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static int bossOverlayEventsSize(BossOverlayGui gui) {
        java.lang.reflect.Field f = BOSS_OVERLAY_EVENTS_FIELD;
        if (f == null) {
            return 0;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<?, ?> events = (Map<?, ?>) f.get(gui);
            return events == null ? 0 : events.size();
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private static float clientPartialTicks() {
        return Minecraft.getInstance().getFrameTime();
    }

    public static void drawInBatch8xOutline(FontRenderer font, IReorderingProcessor text, Matrix4f mat, float x, float y, int textColor, int outlineColor, IRenderTypeBuffer bufferSource, boolean seeThroughText, int light) {
        int[][] dirs = new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for(int[] dir : dirs) {
            font.drawInBatch(text, x + dir[0], y + dir[1], outlineColor, false, mat, bufferSource, seeThroughText, 0, light);
        }
        font.drawInBatch(text, x, y, textColor, false, mat, bufferSource, seeThroughText, 0, light);
    }

    /** GL scissor: bottom-left origin, pixels. {@code yTop}/{@code yBottom} are Mojang gui Y (small at top). */
    private static void enableHudGuiScissor(MainWindow mw, int guiLeft, int guiTop, int guiRight, int guiBottomInclusive) {
        double sf = mw.getGuiScale();
        int fw = mw.getWidth();
        int fh = mw.getHeight();
        int x1 = Math.min(guiLeft, guiRight);
        int x2 = Math.max(guiLeft, guiRight);
        int y1 = Math.min(guiTop, guiBottomInclusive);
        int y2 = Math.max(guiTop, guiBottomInclusive);
        int vx = MathHelper.clamp((int)Math.round(x1 * sf), 0, fw);
        int vw = MathHelper.clamp((int)Math.round((x2 - x1) * sf), 0, fw - vx);
        int vy = MathHelper.clamp(fh - (int)Math.round(y2 * sf), 0, fh);
        int vh = MathHelper.clamp((int)Math.round((y2 - y1) * sf), 0, fh - vy);
        RenderSystem.enableScissor(vx, vy, vw, vh);
    }

    private static void hudBlit(MatrixStack stack, Minecraft mc, ResourceLocation texture, int x, int y, int blitOffset, float texU, float texV, int width, int height, int textureWidth, int textureHeight) {
        mc.textureManager.bind(texture);
        AbstractGui.blit(stack, x, y, blitOffset, texU, texV, width, height, textureWidth, textureHeight);
    }

    /** Mirrors net.torocraft.torohealth.display.EntityDisplay#updateScale; cfgDefault38 scales around Toro's look at default hud_entity_size 38. */
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

    /** Pivot like Toro's WIDTH/2 and HEIGHT/2+RENDER_HEIGHT/2, mapped onto our hud texture clip. */
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

    @SubscribeEvent
    public static void onPostRenderHud(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.INSTANCE.hudIndicatorEnabled.get()) {
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            return;
        }
        if (damageIndicatorEntity == null) {
            return;
        }
        MatrixStack poseStack = event.getMatrixStack();
        MainWindow mw = event.getWindow();
        float pt = event.getPartialTicks();

        float entityHealth = Math.min(damageIndicatorEntity.getHealth(), damageIndicatorEntity.getMaxHealth());
        float entityMaxHealth = damageIndicatorEntity.getMaxHealth();
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
        float scale = Config.INSTANCE.hudIndicatorSize.get().floatValue();
        int xOffset = Config.INSTANCE.hudIndicatorAlignLeft.get() ? Config.INSTANCE.hudIndicatorPositionX.get()
                : mw.getGuiScaledWidth() - Math.round((int) (208 * scale)) - Config.INSTANCE.hudIndicatorPositionX.get();
        int yOffset = Config.INSTANCE.hudIndicatorAlignTop.get() ? Config.INSTANCE.hudIndicatorPositionY.get()
                : mw.getGuiScaledHeight() - Math.round((int) (78 * scale)) - Config.INSTANCE.hudIndicatorPositionY.get();
        int bossBars = 0;
        if (mc.gui instanceof ForgeIngameGui) {
            ForgeIngameGui forgeGui = (ForgeIngameGui) mc.gui;
            BossOverlayGui bossOverlayGui = forgeGui.getBossOverlay();
            bossBars = bossOverlayEventsSize(bossOverlayGui);
            if (Config.INSTANCE.hudIndicatorAlignTop.get()) {
                if (bossBars > 0) {
                    yOffset += Math.min(mw.getGuiScaledHeight() / 3, 12 + 19 * bossBars);
                }
                if (!Config.INSTANCE.hudIndicatorAlignLeft.get()) {
                    int potionsActive = 0;
                    Collection<EffectInstance> active = mc.player != null ? mc.player.getActiveEffects() : java.util.Collections.emptySet();
                    for (EffectInstance effectInstance : active) {
                        if (effectInstance.showIcon()) {
                            potionsActive++;
                        }
                    }
                    yOffset += Math.min(potionsActive, 2) * 24;
                }
            }
        }
        float backgroundOpacity = Config.INSTANCE.hudIndicatorBackgroundOpacity.get().floatValue();

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset - 0.5F, 0);
        poseStack.scale(scale, scale, scale);

        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int portraitClipBotTex = 61;
        int portraitClipInnerW = scissorBox1MaxX - scissorBox1MinX;
        int portraitClipInnerH = portraitClipBotTex - scissorBox1MinY;
        float hudEntityCfg = Config.INSTANCE.hudEntitySize.get().floatValue();
        float hudModelMul = hudEntityCfg;
        if (damageIndicatorEntity != null) {
            float biggestEntityDimension = Math.max(damageIndicatorEntity.getBbWidth() * 1.2F + 0.3F,
                    damageIndicatorEntity.getBbHeight() * 0.9F) * 0.85F;
            if ((double) biggestEntityDimension > 0.5D) {
                hudModelMul /= biggestEntityDimension;
            }
        }
        int entityX = toroHudPortraitPivotTexX(scissorBox1MinX, portraitClipInnerW);
        int entityY = damageIndicatorEntity == null ? portraitClipInnerH / 2 + scissorBox1MinY
                : toroHudPortraitPivotTexY(damageIndicatorEntity, scissorBox1MinY, portraitClipInnerH);
        float yHudBase = yOffset - 0.5F;
        int absPortraitX = Math.round(xOffset + entityX * scale);
        int absPortraitY = Math.round(yHudBase + entityY * scale);
        int portraitScale = damageIndicatorEntity == null ? 1
                : toroHudMiniaturePortraitScale(damageIndicatorEntity, hudEntityCfg, 38.0F);

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

        // Draw portrait after background/frame so opaque pixels in those textures do not cover the entity.
        int clipLeft = Math.round(scale * scissorBox1MinX + xOffset);
        int clipRight = Math.round(scale * scissorBox1MaxX + xOffset);
        int clipTop = Math.round(scale * scissorBox1MinY + yOffset);
        int clipBot = Math.round(scale * portraitClipBotTex + yOffset);
        enableHudGuiScissor(mw, clipLeft, clipTop, clipRight, clipBot);
        if (damageIndicatorEntity != null) {
            renderHudEntityPortrait(poseStack, absPortraitX, absPortraitY, entityX, entityY, portraitScale, hudModelMul, damageIndicatorEntity, mc, pt);
        }
        RenderSystem.disableScissor();

        poseStack.pushPose();
        poseStack.translate(0, 0, -200);

        int relativeMobTypeX = 5;
        int relativeMobTypeY = 55;
        hudBlit(poseStack, mc, currentMobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 50, 0.0F, 0.0F, 18, 18, 18, 18);

        int relativeHealthbarX = 81;
        int relativeHealthbarY = 25;
        int healthbarHeight = 18;
        int healthbarMaxWidth = 124;
        int currentHealthbarWidth = (int) Math.round(healthbarMaxWidth * healthRatio);
        int healthbarVOffset = Config.INSTANCE.colorblindHealthBar.get() ? 36 : 0;
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 50, 0.0F, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
        hudBlit(poseStack, mc, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 50, 0.0F, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

        poseStack.popPose();

        String healthText;
        float healthOffsetX = 136;
        float healthOffsetY = 30;
        String healthDivisor;
        int firstHalfWidth;
        if (Config.INSTANCE.healthSeperator.get()) {
            healthDivisor = " | ";
        } else {
            healthDivisor = "/";
            healthOffsetX += 4;
        }
        if (Config.INSTANCE.healthDecimals.get()) {
            healthText = roundHealth(entityHealth) + healthDivisor + roundHealth(entityMaxHealth);
            firstHalfWidth = mc.font.width("" + roundHealth(entityHealth));
        } else {
            healthText = (int) entityHealth + healthDivisor + (int) entityMaxHealth;
            firstHalfWidth = mc.font.width("" + (int) entityHealth);
        }
        ITextComponent healthComponent = new StringTextComponent(healthText);
        int healthWidth = mc.font.width(healthComponent);
        float healthScale = Math.min(88F / (float) healthWidth, 1.35F);
        int healthColor = 0XFFFFFF;
        int healthOutlineColor = 0;

        poseStack.pushPose();
        poseStack.translate(healthOffsetX, healthOffsetY, 0);
        poseStack.scale(healthScale, healthScale, 1);
        poseStack.translate(-firstHalfWidth, 0, -50);
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        if (Config.INSTANCE.hudHealthTextOutline.get()) {
            drawInBatch8xOutline(mc.font, healthComponent.getVisualOrderText(), poseStack.last().pose(), 0.0F, 0.0F, healthColor, healthOutlineColor, buffer, false, 15728880);
        } else {
            mc.font.drawInBatch(healthComponent.getVisualOrderText(), 0.0F, 0.0F, healthColor, true, poseStack.last().pose(), buffer, false, 0, 15728880);
        }
        buffer.endBatch();
        poseStack.popPose();

        ITextComponent nameComponent = damageIndicatorEntity.getDisplayName();
        int nameWidth = mc.font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);
        float nameOffsetX = 138.5F;
        float nameOffsetY = 6.5F;
        int nameColor = 0XFFFFFF;
        int nameOutlineColor = 0;

        poseStack.pushPose();
        poseStack.translate(nameOffsetX, nameOffsetY, 0);
        poseStack.scale(nameScale, nameScale, 1);
        poseStack.translate(-nameWidth / 2F, 0, -50);
        IRenderTypeBuffer.Impl nameBuffer = mc.renderBuffers().bufferSource();
        if (Config.INSTANCE.hudNameTextOutline.get()) {
            drawInBatch8xOutline(mc.font, nameComponent.getVisualOrderText(), poseStack.last().pose(), 0.0F, 0.0F, nameColor, nameOutlineColor, nameBuffer, false, 15728880);
        } else {
            mc.font.drawInBatch(nameComponent.getVisualOrderText(), 0.0F, 0.0F, nameColor, true, poseStack.last().pose(), nameBuffer, false, 0, 15728880);
        }
        nameBuffer.endBatch();
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * Portrait path aligned with Damage Indicators 1.12.2 ({@code DIGuiTools#renderEntity}): full entity renderer at
     * the origin after a gui matrix stack, depth on, flush buffers, clear depth strip so later HUD blends stay clean —
     * and with Toro's 1.16 {@code EntityDisplay} approach: {@link EntityRendererManager#overrideCameraOrientation} +
     * {@link EntityRendererManager#render} for correct layering (inventory helper alone was glitchy here).
     */
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

        // LivingRenderer lerps yaw/pitch from *O fields each frame — if only current values match the gui pose while
        // previous values still reflect AI movement, interpolation jitters wildly while the mob walks or turns.
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

    private static void renderHudEntityPortrait(MatrixStack poseStack, int guiAbsX, int guiAbsY, int localPortraitX,
            int localPortraitY, int portraitScale, float hudEntityScaleMul, LivingEntity entity, Minecraft mc,
            float partialTicks) {
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

    /** Legacy model-only path for entity ids in config; default HUD portrait uses full static entity render like Toro/1.12.2 DI. */
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

    public static float roundHealth(float entityHealth) {
        return (float) (Math.round(entityHealth * 5) / 5D);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent clientTickEvent) {
        if (clientTickEvent.phase != TickEvent.Phase.START || Minecraft.getInstance().cameraEntity == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.cameraEntity;
        double maxPickDistance = Config.INSTANCE.maxDistance.get();
        double pickDistance = maxPickDistance;
        float pt = clientPartialTicks();
        Vector3d vec3 = cameraEntity.getEyePosition(pt);
        RayTraceResult hitResult = cameraEntity.pick(maxPickDistance, pt, false);
        LivingEntity found = null;
        if (hitResult != null && hitResult.getType() != RayTraceResult.Type.MISS) {
            pickDistance = hitResult.getLocation().distanceToSqr(vec3);
        }
        Vector3d vec31 = cameraEntity.getViewVector(1.0F);
        Vector3d vec32 = vec3.add(vec31.x * maxPickDistance, vec31.y * maxPickDistance, vec31.z * maxPickDistance);
        AxisAlignedBB aabb = cameraEntity.getBoundingBox().expandTowards(vec31.scale(maxPickDistance)).inflate(3.0D, 3.0D, 3.0D);
        EntityRayTraceResult entityhitresult = ProjectileHelper.getEntityHitResult(cameraEntity, vec3, vec32, aabb,
                entity -> (!entity.isSpectator() && entity.isPickable()), pickDistance);
        if (entityhitresult != null) {
            Vector3d vec33 = entityhitresult.getLocation();
            Entity entity = entityhitresult.getEntity();
            double d2 = vec3.distanceToSqr(vec33);
            if (d2 < pickDistance) {
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    if (living.isAlive() && !(living instanceof ArmorStandEntity)) {
                        found = living;
                    }
                } else if (entity instanceof PartEntity) {
                    PartEntity<?> partEntity = (PartEntity<?>) entity;
                    Entity parentEntity = partEntity.getParent();
                    if (parentEntity instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) parentEntity;
                        found = living;
                    }
                }
            }
        }
        if (found != null) {
            damageIndicatorEntity = found;
            currentMobType = MobTypes.getTypeFor(found);
            resetDamageIndicatorEntityIn = Config.INSTANCE.hudLingerTime.get();
            ResourceLocation key = ForgeRegistries.ENTITIES.getKey(found.getType());
            String keyStr = key == null ? "" : key.toString();
            renderModelOnly = Config.INSTANCE.oldRenderEntities.get().contains(keyStr);
        } else if (--resetDamageIndicatorEntityIn < 0) {
            damageIndicatorEntity = null;
            resetDamageIndicatorEntityIn = 0;
        }
    }

    public static void spawnHurtParticles(Entity entity, float damage) {
        double x = entity.getRandomX(1.0D);
        double y = entity.getEyeY();
        double z = entity.getRandomZ(1.0D);
        Minecraft.getInstance().particleEngine.add(new DamageIndicatorParticle(Minecraft.getInstance().level,
                x, y, z, Math.abs(damage), damage > 0));
    }
}
