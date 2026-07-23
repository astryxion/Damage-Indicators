package com.astryxion.damageindicators;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Style 2 HUD — classic Damage Indicators 1.12.2 Clean skin.
 * Ported to Fabric 26.2 GuiGraphicsExtractor (Matrix3x2fStack + RenderPipelines).
 */
public final class Style2HudRenderer {
    private static final String MODID = DamageIndicators.MODID;

    private static final Identifier TEXTURE_BACKGROUND = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/background.png");
    private static final Identifier TEXTURE_FRAME = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/di_frame_skin.png");
    private static final Identifier TEXTURE_HEALTH = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/health.png");
    private static final Identifier TEXTURE_DAMAGE = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/damage.png");
    private static final Identifier TEXTURE_NAMEPLATE = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/name_plate.png");
    private static final Identifier TEXTURE_TYPE_ICONS = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/di_type_icons.png");
    private static final Identifier TEXTURE_POTION_LEFT = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/left_potions.png");
    private static final Identifier TEXTURE_POTION_CENTER = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/center_potions.png");
    private static final Identifier TEXTURE_POTION_RIGHT = Identifier.fromNamespaceAndPath(MODID, "textures/gui/default/right_potions.png");

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

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick, LivingEntity entity) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = Math.max(entity.getMaxHealth(), 0.0F);
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : Math.min(1.0F, entityHealth / entityMaxHealth);
        float scale = cfg.hudIndicatorSize.get().floatValue();

        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : guiGraphics.guiWidth() - Math.round(HUD_EXTENT_RIGHT * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : guiGraphics.guiHeight() - Math.round(HUD_EXTENT_BOTTOM * scale) - cfg.hudIndicatorPositionY.get();

        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(xOffset, yOffset);
        pose.scale(scale, scale);

        float backgroundOpacity = Mth.clamp(cfg.hudIndicatorBackgroundOpacity.get().floatValue(), 0.0F, 1.0F);
        int bgColor = (Mth.clamp((int) (backgroundOpacity * 255.0F), 0, 255) << 24) | 0xFFFFFF;
        blitFull(guiGraphics, TEXTURE_BACKGROUND, BACKGROUND_X, BACKGROUND_Y, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, TEX_BACKGROUND, TEX_BACKGROUND, bgColor);

        if (entity.getHealth() > 0.0F) {
            int scissorMinX = xOffset + Math.round(scale * MOB_PREVIEW_X);
            int scissorMinY = yOffset + Math.round(scale * MOB_PREVIEW_Y);
            int scissorMaxX = xOffset + Math.round(scale * (MOB_PREVIEW_X + BACKGROUND_WIDTH));
            int scissorMaxY = yOffset + Math.round(scale * (MOB_PREVIEW_Y + BACKGROUND_HEIGHT));

            // DI-NeoForge-1.21.11: pop HUD pose before PIP submit so absolute box coords stay correct.
            pose.popMatrix();
            guiGraphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
            renderPortraitEntity(guiGraphics, entity, scissorMinX, scissorMinY, scissorMaxX, scissorMaxY, scale);
            guiGraphics.disableScissor();
            pose.pushMatrix();
            pose.translate(xOffset, yOffset);
            pose.scale(scale, scale);
        }

        drawHealthBar(guiGraphics, healthRatio);
        blitFull(guiGraphics, TEXTURE_FRAME, FRAME_X, FRAME_Y, FRAME_WIDTH, FRAME_HEIGHT, TEX_FRAME_W, TEX_FRAME_H, 0xFFFFFFFF);
        blitFull(guiGraphics, TEXTURE_NAMEPLATE, NAME_PLATE_X, NAME_PLATE_Y, NAME_PLATE_WIDTH, NAME_PLATE_HEIGHT, 1, 1, 0xFFFFFFFF);
        drawMobTypeIcon(guiGraphics, entity);
        if (cfg.hudPotionEffects.get()) {
            drawPotionBoxes(guiGraphics, entity);
        }
        drawHealthText(guiGraphics, entityHealth, entityMaxHealth);
        drawNameText(guiGraphics, entity);

        pose.popMatrix();
    }

    /**
     * Stretch the full source texture (texW×texH) into the on-screen quad (width×height).
     * Must use the uWidth/vHeight overload — the shorter blit() form samples only width×height
     * of the atlas, which shreds the Clean skin frame (350×128 → 178×64).
     */
    private static void blitFull(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int width, int height, int texW, int texH, int color) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, texW, texH, texW, texH, color);
    }

    private static void blitRegion(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int width, int height,
                                   float u, float v, int uWidth, int vHeight, int texW, int texH, int color) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, uWidth, vHeight, texW, texH, color);
    }

    private static void drawHealthBar(GuiGraphicsExtractor guiGraphics, float healthRatio) {
        boolean colorblind = Config.INSTANCE.active().colorblindHealthBar.get();
        if (healthRatio < 1.0F) {
            float uStart = healthRatio * TEX_DAMAGE_W;
            int uWidth = Math.max(1, Math.round((1.0F - healthRatio) * TEX_DAMAGE_W));
            int color = colorblind ? 0xFF1A1A1A : 0xFFFFFFFF;
            blitRegion(guiGraphics, TEXTURE_DAMAGE, HEALTH_BAR_X, HEALTH_BAR_Y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT,
                    uStart, 0.0F, uWidth, TEX_DAMAGE_H, TEX_DAMAGE_W, TEX_DAMAGE_H, color);
        }
        float fillWidth = HEALTH_BAR_WIDTH * healthRatio;
        if (fillWidth > 0.0F) {
            int uWidth = Math.max(1, Math.round(healthRatio * TEX_HEALTH_W));
            int color = colorblind ? 0xFFFFE626 : 0xFFFFFFFF;
            blitRegion(guiGraphics, TEXTURE_HEALTH, HEALTH_BAR_X, HEALTH_BAR_Y, Math.round(fillWidth), HEALTH_BAR_HEIGHT,
                    0.0F, 0.0F, uWidth, TEX_HEALTH_H, TEX_HEALTH_W, TEX_HEALTH_H, color);
        }
    }

    private static void drawMobTypeIcon(GuiGraphicsExtractor guiGraphics, LivingEntity entity) {
        int iconIndex = MobTypes.getCleanSkinIconIndex(entity);
        boolean boss = iconIndex == 4;
        boolean hostile = MobTypes.isHostileForCleanSkin(entity);
        int color;
        if (boss) {
            color = 0x99FFFFFF;
        } else if (hostile) {
            color = 0x99FF0000;
        } else {
            color = 0x9900FF00;
        }
        float uStep = TEX_TYPE_W / 5.0F;
        float uOffset = iconIndex * uStep;
        int uWidth = Math.round(uStep);
        blitRegion(guiGraphics, TEXTURE_TYPE_ICONS, MOB_TYPE_X, MOB_TYPE_Y, MOB_TYPE_WIDTH, MOB_TYPE_HEIGHT,
                uOffset, 0.0F, uWidth, TEX_TYPE_H, TEX_TYPE_W, TEX_TYPE_H, color);
    }

    private static void drawPotionBoxes(GuiGraphicsExtractor guiGraphics, LivingEntity entity) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getDuration() > 10 && effect.showIcon() && effect.isVisible()) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            return;
        }

        blitFull(guiGraphics, TEXTURE_POTION_LEFT, POTION_BOX_X, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_LEFT_W, TEX_POTION_LEFT_H, 0xFFFFFFFF);

        Font font = Minecraft.getInstance().font;
        Matrix3x2fStack pose = guiGraphics.pose();

        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            int cellX = POTION_BOX_X + i * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
            blitFull(guiGraphics, TEXTURE_POTION_CENTER, cellX, POTION_BOX_Y, POTION_CENTER_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_CENTER_W, TEX_POTION_CENTER_H, 0xFFFFFFFF);

            Identifier sprite = Hud.getMobEffectSprite(effect.getEffect());
            int iconSize = POTION_BOX_HEIGHT - 4;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, cellX + 2, POTION_BOX_Y + 2, iconSize, iconSize);

            Component duration = MobEffectUtil.formatDuration(effect, 1.0F, Minecraft.getInstance().level != null ? Minecraft.getInstance().level.tickRateManager().tickrate() : 20.0F);
            String durationText = duration.getString();
            int textWidth = font.width(durationText);
            pose.pushMatrix();
            pose.translate(cellX + 13 - textWidth / 2.0F, POTION_BOX_Y + POTION_BOX_HEIGHT - font.lineHeight * 0.815F);
            pose.scale(0.815F, 0.815F);
            guiGraphics.text(font, durationText, 0, 0, 0xFFFFFF80, true);
            pose.popMatrix();
        }

        int rightX = POTION_BOX_X + effects.size() * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
        blitFull(guiGraphics, TEXTURE_POTION_RIGHT, rightX, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_RIGHT_W, TEX_POTION_RIGHT_H, 0xFFFFFFFF);
    }

    private static void drawHealthText(GuiGraphicsExtractor guiGraphics, float entityHealth, float entityMaxHealth) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        Font font = Minecraft.getInstance().font;
        String divisor = cfg.healthSeperator.get() ? " | " : "/";
        float displayMax = entityHealth > entityMaxHealth ? entityHealth : entityMaxHealth;
        String healthText;
        if (cfg.healthDecimals.get()) {
            healthText = DamageIndicators.roundHealth(entityHealth) + divisor + DamageIndicators.roundHealth(displayMax);
        } else {
            healthText = Mth.ceil(entityHealth) + divisor + Mth.ceil(displayMax);
        }

        Component healthComponent = Component.literal(healthText);
        int healthWidth = font.width(healthComponent);
        Matrix3x2fStack pose = guiGraphics.pose();

        if (font.lineHeight + 2 > HEALTH_BAR_HEIGHT) {
            pose.pushMatrix();
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth * 0.7F) / 2.0F;
            float textY = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT - font.lineHeight * 0.7F - 0.5F;
            pose.translate(textX, textY);
            pose.scale(0.7F, 0.7F);
            drawHudText(guiGraphics, healthComponent, 0, 0, 0xFFFFFF, cfg.hudHealthTextOutline.get());
            pose.popMatrix();
        } else {
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth) / 2.0F;
            float textY = HEALTH_BAR_Y + (HEALTH_BAR_HEIGHT - font.lineHeight) / 2.0F;
            drawHudText(guiGraphics, healthComponent, Math.round(textX), Math.round(textY), 0xFFFFFF, cfg.hudHealthTextOutline.get());
        }
    }

    private static void drawNameText(GuiGraphicsExtractor guiGraphics, LivingEntity entity) {
        Font font = Minecraft.getInstance().font;
        String name = entity.getDisplayName().getString();
        if (entity.isBaby() && !name.toLowerCase().contains("baby")) {
            name = "Baby " + name;
        }
        Component nameComponent = Component.literal(name);
        int nameWidth = font.width(nameComponent);
        int textX = NAME_PLATE_X + (NAME_PLATE_WIDTH - nameWidth) / 2;
        int textY = NAME_PLATE_Y + (NAME_PLATE_HEIGHT - font.lineHeight) / 2;
        drawHudText(guiGraphics, nameComponent, textX, textY, 0xFFFFFF, Config.INSTANCE.active().hudNameTextOutline.get());
    }

    private static void drawHudText(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int color, boolean outline) {
        Font font = Minecraft.getInstance().font;
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        if (outline) {
            guiGraphics.text(font, text, x - 1, y, 0xFF000000, false);
            guiGraphics.text(font, text, x + 1, y, 0xFF000000, false);
            guiGraphics.text(font, text, x, y - 1, 0xFF000000, false);
            guiGraphics.text(font, text, x, y + 1, 0xFF000000, false);
            guiGraphics.text(font, text, x, y, argb, false);
        } else {
            guiGraphics.text(font, text, x, y, argb, true);
        }
    }

    private static void renderPortraitEntity(GuiGraphicsExtractor guiGraphics, LivingEntity entity, int x0, int y0, int x1, int y1, float hudScale) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        // Same BB base as Style 1 (38), then clamp so the model cannot overflow the portrait box.
        int desired = HudPortraitRenderer.style1Scale(entity, 38.0F, hudScale);
        int scale = HudPortraitRenderer.fitToPortraitBox(desired, entity, x1 - x0, y1 - y0);

        // Style 2 facing: slightly stronger right-facing than Style 1, still mild enough for PIP.
        float centerX = (x0 + x1) / 2.0F;
        float centerY = (y0 + y1) / 2.0F;
        HudPortraitRenderer.renderFollowsMouse(
                guiGraphics, x0, y0, x1, y1, scale, 0.0625F,
                centerX + 28.0F, centerY - 10.0F, entity);
    }
}
