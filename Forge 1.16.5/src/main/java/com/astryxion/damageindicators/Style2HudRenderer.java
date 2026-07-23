package com.astryxion.damageindicators;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Style 2 HUD — classic Damage Indicators 1.12.2 Clean skin (Forge 1.16.5).
 * Uses stretched blit (dest size != source region) matching GuiGraphics.blit on 1.20+.
 */
public final class Style2HudRenderer {
    private static final String MODID = DamageIndicators.MODID;

    private static final ResourceLocation TEXTURE_BACKGROUND = new ResourceLocation(MODID, "textures/gui/default/background.png");
    private static final ResourceLocation TEXTURE_FRAME = new ResourceLocation(MODID, "textures/gui/default/di_frame_skin.png");
    private static final ResourceLocation TEXTURE_HEALTH = new ResourceLocation(MODID, "textures/gui/default/health.png");
    private static final ResourceLocation TEXTURE_DAMAGE = new ResourceLocation(MODID, "textures/gui/default/damage.png");
    private static final ResourceLocation TEXTURE_NAMEPLATE = new ResourceLocation(MODID, "textures/gui/default/name_plate.png");
    private static final ResourceLocation TEXTURE_TYPE_ICONS = new ResourceLocation(MODID, "textures/gui/default/di_type_icons.png");
    private static final ResourceLocation TEXTURE_POTION_LEFT = new ResourceLocation(MODID, "textures/gui/default/left_potions.png");
    private static final ResourceLocation TEXTURE_POTION_CENTER = new ResourceLocation(MODID, "textures/gui/default/center_potions.png");
    private static final ResourceLocation TEXTURE_POTION_RIGHT = new ResourceLocation(MODID, "textures/gui/default/right_potions.png");

    private static final int BACKGROUND_WIDTH = 49;
    private static final int BACKGROUND_HEIGHT = 51;
    private static final int BACKGROUND_X = -4;
    private static final int BACKGROUND_Y = -4;
    private static final int FRAME_WIDTH = 178;
    private static final int FRAME_HEIGHT = 64;
    private static final int FRAME_X = -15;
    private static final int FRAME_Y = -5;
    private static final int HEALTH_BAR_WIDTH = 112;
    private static final int HEALTH_BAR_HEIGHT = 17;
    private static final int HEALTH_BAR_X = 49;
    private static final int HEALTH_BAR_Y = 13;
    private static final int NAME_PLATE_WIDTH = 112;
    private static final int NAME_PLATE_HEIGHT = 12;
    private static final int NAME_PLATE_X = 49;
    private static final int NAME_PLATE_Y = 0;
    private static final int MOB_PREVIEW_X = -4;
    private static final int MOB_PREVIEW_Y = -3;
    private static final int MOB_TYPE_WIDTH = 18;
    private static final int MOB_TYPE_HEIGHT = 18;
    private static final int MOB_TYPE_X = -13;
    private static final int MOB_TYPE_Y = 39;
    private static final int POTION_BOX_SIDE_WIDTH = 4;
    private static final int POTION_BOX_HEIGHT = 22;
    private static final int POTION_BOX_X = 48;
    private static final int POTION_BOX_Y = 31;
    private static final int POTION_CENTER_WIDTH = 20;

    private static final int TEX_BACKGROUND = 64;
    private static final int TEX_FRAME_W = 350;
    private static final int TEX_FRAME_H = 128;
    private static final int TEX_HEALTH_W = 64;
    private static final int TEX_HEALTH_H = 32;
    private static final int TEX_DAMAGE_W = 2;
    private static final int TEX_DAMAGE_H = 32;
    private static final int TEX_TYPE_W = 256;
    private static final int TEX_TYPE_H = 64;
    private static final int TEX_POTION_LEFT_W = 8;
    private static final int TEX_POTION_LEFT_H = 32;
    private static final int TEX_POTION_CENTER_W = 2;
    private static final int TEX_POTION_CENTER_H = 64;
    private static final int TEX_POTION_RIGHT_W = 8;
    private static final int TEX_POTION_RIGHT_H = 32;

    private static final int HUD_EXTENT_RIGHT = FRAME_X + FRAME_WIDTH;
    private static final int HUD_EXTENT_BOTTOM = FRAME_Y + FRAME_HEIGHT;

    private Style2HudRenderer() {
    }

    public static void render(RenderGameOverlayEvent.Post event, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = Math.max(entity.getMaxHealth(), 0.0F);
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : Math.min(1.0F, entityHealth / entityMaxHealth);
        float scale = cfg.hudIndicatorSize.get().floatValue();
        MainWindow mw = event.getWindow();
        MatrixStack poseStack = event.getMatrixStack();
        float partialTicks = event.getPartialTicks();

        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : mw.getGuiScaledWidth() - Math.round(HUD_EXTENT_RIGHT * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : mw.getGuiScaledHeight() - Math.round(HUD_EXTENT_BOTTOM * scale) - cfg.hudIndicatorPositionY.get();

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 0);
        poseStack.scale(scale, scale, scale);

        float backgroundOpacity = MathHelper.clamp(cfg.hudIndicatorBackgroundOpacity.get().floatValue(), 0.0F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, backgroundOpacity);
        blitStretched(poseStack, mc, TEXTURE_BACKGROUND, BACKGROUND_X, BACKGROUND_Y, BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                0.0F, 0.0F, TEX_BACKGROUND, TEX_BACKGROUND, TEX_BACKGROUND, TEX_BACKGROUND);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (entity.getHealth() > 0.0F) {
            int scissorMinX = xOffset + Math.round(scale * MOB_PREVIEW_X);
            int scissorMinY = yOffset + Math.round(scale * MOB_PREVIEW_Y);
            int scissorMaxX = xOffset + Math.round(scale * (MOB_PREVIEW_X + BACKGROUND_WIDTH));
            int scissorMaxY = yOffset + Math.round(scale * (MOB_PREVIEW_Y + BACKGROUND_HEIGHT));
            Style1HudRenderer.enableScissor(mw, scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
            renderPortraitEntity(mc, xOffset, yOffset, scale, entity, partialTicks);
            RenderSystem.disableScissor();
        }

        drawHealthBar(poseStack, mc, healthRatio);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blitStretched(poseStack, mc, TEXTURE_FRAME, FRAME_X, FRAME_Y, FRAME_WIDTH, FRAME_HEIGHT,
                0.0F, 0.0F, TEX_FRAME_W, TEX_FRAME_H, TEX_FRAME_W, TEX_FRAME_H);
        blitStretched(poseStack, mc, TEXTURE_NAMEPLATE, NAME_PLATE_X, NAME_PLATE_Y, NAME_PLATE_WIDTH, NAME_PLATE_HEIGHT,
                0.0F, 0.0F, 1, 1, 1, 1);

        drawMobTypeIcon(poseStack, mc, entity);

        if (cfg.hudPotionEffects.get()) {
            drawPotionBoxes(poseStack, mc, entity);
        }

        drawHealthText(poseStack, mc, entityHealth, entityMaxHealth);
        drawNameText(poseStack, mc, entity);

        poseStack.popPose();
        Style1HudRenderer.restoreHudGlState(mc);
    }

    /**
     * Stretch a source UV region into a destination rectangle (1.20 GuiGraphics.blit semantics).
     */
    private static void blitStretched(MatrixStack poseStack, Minecraft mc, ResourceLocation texture,
                                      int x, int y, int width, int height,
                                      float u, float v, int regionW, int regionH, int texW, int texH) {
        mc.textureManager.bind(texture);
        AbstractGui.blit(poseStack, x, y, width, height, u, v, regionW, regionH, texW, texH);
    }

    private static void drawHealthBar(MatrixStack poseStack, Minecraft mc, float healthRatio) {
        boolean colorblind = Config.INSTANCE.active().colorblindHealthBar.get();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (healthRatio < 1.0F) {
            float uStart = healthRatio * TEX_DAMAGE_W;
            int uWidth = Math.max(1, Math.round((1.0F - healthRatio) * TEX_DAMAGE_W));
            if (colorblind) {
                RenderSystem.color4f(0.1F, 0.1F, 0.1F, 1.0F);
            } else {
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            blitStretched(poseStack, mc, TEXTURE_DAMAGE, HEALTH_BAR_X, HEALTH_BAR_Y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT,
                    uStart, 0.0F, uWidth, TEX_DAMAGE_H, TEX_DAMAGE_W, TEX_DAMAGE_H);
        }

        float fillWidth = HEALTH_BAR_WIDTH * healthRatio;
        if (fillWidth > 0.0F) {
            int uWidth = Math.max(1, Math.round(healthRatio * TEX_HEALTH_W));
            if (colorblind) {
                RenderSystem.color4f(1.0F, 0.9F, 0.15F, 1.0F);
            } else {
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            blitStretched(poseStack, mc, TEXTURE_HEALTH, HEALTH_BAR_X, HEALTH_BAR_Y, Math.round(fillWidth), HEALTH_BAR_HEIGHT,
                    0.0F, 0.0F, uWidth, TEX_HEALTH_H, TEX_HEALTH_W, TEX_HEALTH_H);
        }

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawMobTypeIcon(MatrixStack poseStack, Minecraft mc, LivingEntity entity) {
        int iconIndex = MobTypes.getCleanSkinIconIndex(entity);
        boolean boss = iconIndex == 4;
        boolean hostile = MobTypes.isHostileForCleanSkin(entity);

        if (boss) {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.6F);
        } else if (hostile) {
            RenderSystem.color4f(1.0F, 0.0F, 0.0F, 0.6F);
        } else {
            RenderSystem.color4f(0.0F, 1.0F, 0.0F, 0.6F);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float uStep = TEX_TYPE_W / 5.0F;
        float uOffset = iconIndex * uStep;
        int uWidth = Math.round(uStep);
        blitStretched(poseStack, mc, TEXTURE_TYPE_ICONS, MOB_TYPE_X, MOB_TYPE_Y, MOB_TYPE_WIDTH, MOB_TYPE_HEIGHT,
                uOffset, 0.0F, uWidth, TEX_TYPE_H, TEX_TYPE_W, TEX_TYPE_H);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawPotionBoxes(MatrixStack poseStack, Minecraft mc, LivingEntity entity) {
        List<EffectInstance> effects = new ArrayList<EffectInstance>();
        Collection<EffectInstance> active = entity.getActiveEffects();
        for (EffectInstance effect : active) {
            if (effect.getDuration() > 10 && effect.showIcon()) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        blitStretched(poseStack, mc, TEXTURE_POTION_LEFT, POTION_BOX_X, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT,
                0.0F, 0.0F, TEX_POTION_LEFT_W, TEX_POTION_LEFT_H, TEX_POTION_LEFT_W, TEX_POTION_LEFT_H);

        PotionSpriteUploader effectTextures = mc.getMobEffectTextures();
        FontRenderer font = mc.font;

        for (int i = 0; i < effects.size(); i++) {
            EffectInstance effect = effects.get(i);
            int cellX = POTION_BOX_X + i * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
            blitStretched(poseStack, mc, TEXTURE_POTION_CENTER, cellX, POTION_BOX_Y, POTION_CENTER_WIDTH, POTION_BOX_HEIGHT,
                    0.0F, 0.0F, TEX_POTION_CENTER_W, TEX_POTION_CENTER_H, TEX_POTION_CENTER_W, TEX_POTION_CENTER_H);

            TextureAtlasSprite sprite = effectTextures.get(effect.getEffect());
            int iconSize = POTION_BOX_HEIGHT - 4;
            AbstractGui.blit(poseStack, cellX + 2, POTION_BOX_Y + 2, 0, iconSize, iconSize, sprite);

            ITextComponent duration = new StringTextComponent(EffectUtils.formatDuration(effect, 1.0F));
            String durationText = duration.getString();
            int textWidth = font.width(durationText);
            poseStack.pushPose();
            poseStack.translate(cellX + 13 - textWidth / 2.0F, POTION_BOX_Y + POTION_BOX_HEIGHT - font.lineHeight * 0.815F, 0.1F);
            poseStack.scale(0.815F, 0.815F, 0.815F);
            IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
            font.drawInBatch(new StringTextComponent(durationText).getVisualOrderText(), 0.0F, 0.0F, 0xFFFF80, true, poseStack.last().pose(), buffer, false, 0, 15728880);
            buffer.endBatch();
            poseStack.popPose();
        }

        int rightX = POTION_BOX_X + effects.size() * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
        blitStretched(poseStack, mc, TEXTURE_POTION_RIGHT, rightX, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT,
                0.0F, 0.0F, TEX_POTION_RIGHT_W, TEX_POTION_RIGHT_H, TEX_POTION_RIGHT_W, TEX_POTION_RIGHT_H);
    }

    private static void drawHealthText(MatrixStack poseStack, Minecraft mc, float entityHealth, float entityMaxHealth) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        FontRenderer font = mc.font;
        String divisor = cfg.healthSeperator.get() ? " | " : "/";
        float displayMax = entityHealth > entityMaxHealth ? entityHealth : entityMaxHealth;
        String healthText;
        if (cfg.healthDecimals.get()) {
            healthText = DamageIndicators.roundHealth(entityHealth) + divisor + DamageIndicators.roundHealth(displayMax);
        } else {
            healthText = MathHelper.ceil(entityHealth) + divisor + MathHelper.ceil(displayMax);
        }

        ITextComponent healthComponent = new StringTextComponent(healthText);
        int healthWidth = font.width(healthComponent);

        if (font.lineHeight + 2 > HEALTH_BAR_HEIGHT) {
            poseStack.pushPose();
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth * 0.7F) / 2.0F;
            float textY = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT - font.lineHeight * 0.7F - 0.5F;
            poseStack.translate(textX, textY, 0);
            poseStack.scale(0.7F, 0.7F, 1.0F);
            drawHudText(poseStack, mc, healthComponent, 0.0F, 0.0F, 0xFFFFFF, cfg.hudHealthTextOutline.get());
            poseStack.popPose();
        } else {
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth) / 2.0F;
            float textY = HEALTH_BAR_Y + (HEALTH_BAR_HEIGHT - font.lineHeight) / 2.0F;
            drawHudText(poseStack, mc, healthComponent, textX, textY, 0xFFFFFF, cfg.hudHealthTextOutline.get());
        }
    }

    private static void drawNameText(MatrixStack poseStack, Minecraft mc, LivingEntity entity) {
        FontRenderer font = mc.font;
        String name = entity.getDisplayName().getString();
        if (entity.isBaby() && !name.toLowerCase().contains("baby")) {
            name = "Baby " + name;
        }
        ITextComponent nameComponent = new StringTextComponent(name);
        int nameWidth = font.width(nameComponent);
        float textX = NAME_PLATE_X + (NAME_PLATE_WIDTH - nameWidth) / 2.0F;
        float textY = NAME_PLATE_Y + (NAME_PLATE_HEIGHT - font.lineHeight) / 2.0F;
        drawHudText(poseStack, mc, nameComponent, textX, textY, 0xFFFFFF, Config.INSTANCE.active().hudNameTextOutline.get());
    }

    private static void drawHudText(MatrixStack poseStack, Minecraft mc, ITextComponent text, float x, float y, int color, boolean outline) {
        FontRenderer font = mc.font;
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        if (outline) {
            DamageIndicators.drawInBatch8xOutline(font, text.getVisualOrderText(), poseStack.last().pose(), x, y, color, 0, buffer, false, 15728880);
        } else {
            font.drawInBatch(text.getVisualOrderText(), x, y, color, true, poseStack.last().pose(), buffer, false, 0, 15728880);
        }
        buffer.endBatch();
    }

    private static void renderPortraitEntity(Minecraft mc, int hudXOffset, int hudYOffset, float hudScale, LivingEntity entity, float partialTicks) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float scaleFactor = cfg.hudEntitySize.get().floatValue();
        float yOffset = -5.0F;
        float xOffset = 0.0F;
        float babyScaleFactor = 2.0F;
        float entitySizeScaling = 0.0F;

        if (entity instanceof PlayerEntity) {
            yOffset = 20.0F;
        } else if (entity instanceof SlimeEntity) {
            scaleFactor = 5.0F;
            entitySizeScaling = 2.0F;
            yOffset = -5.0F;
        } else if (entity instanceof WitherEntity) {
            scaleFactor = 15.0F;
            yOffset = 5.0F;
        }

        float eyeAdj = (3.0F - entity.getEyeHeight()) * entitySizeScaling;
        float finalScale = scaleFactor + scaleFactor * eyeAdj;
        if (entity.isBaby()) {
            finalScale *= babyScaleFactor;
        }
        finalScale *= 0.85F;

        float entityXLocal = MOB_PREVIEW_X + 25.0F + xOffset;
        float entityYLocal = MOB_PREVIEW_Y + 52.0F + yOffset;
        if (entity == mc.player) {
            entityYLocal -= 30.0F;
        }

        int absX = Math.round(hudXOffset + entityXLocal * hudScale);
        int absY = Math.round(hudYOffset + entityYLocal * hudScale);
        int portraitScale = Math.max(6, Math.round(finalScale * hudScale));
        Style1HudRenderer.renderPortraitAt(mc, absX, absY, portraitScale, entity, partialTicks);
    }
}
