package com.astryxion.damageindicators;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Style 2 HUD — classic Damage Indicators 1.12.2 Clean skin.
 */
public final class Style2HudRenderer {
    private static final String MODID = DamageIndicators.MODID;

    // Original Damage Indicators 1.12.2 default ("Clean") skin assets
    private static final ResourceLocation TEXTURE_BACKGROUND = new ResourceLocation(MODID, "textures/gui/default/background.png");
    private static final ResourceLocation TEXTURE_FRAME = new ResourceLocation(MODID, "textures/gui/default/di_frame_skin.png");
    private static final ResourceLocation TEXTURE_HEALTH = new ResourceLocation(MODID, "textures/gui/default/health.png");
    private static final ResourceLocation TEXTURE_DAMAGE = new ResourceLocation(MODID, "textures/gui/default/damage.png");
    private static final ResourceLocation TEXTURE_NAMEPLATE = new ResourceLocation(MODID, "textures/gui/default/name_plate.png");
    private static final ResourceLocation TEXTURE_TYPE_ICONS = new ResourceLocation(MODID, "textures/gui/default/di_type_icons.png");
    private static final ResourceLocation TEXTURE_POTION_LEFT = new ResourceLocation(MODID, "textures/gui/default/left_potions.png");
    private static final ResourceLocation TEXTURE_POTION_CENTER = new ResourceLocation(MODID, "textures/gui/default/center_potions.png");
    private static final ResourceLocation TEXTURE_POTION_RIGHT = new ResourceLocation(MODID, "textures/gui/default/right_potions.png");

    // skin.cfg sizes / positions for the default Clean skin
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

    // Source texture pixel sizes
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

    // Frame extents relative to loc (used for screen alignment)
    private static final int HUD_EXTENT_RIGHT = FRAME_X + FRAME_WIDTH; // 163
    private static final int HUD_EXTENT_BOTTOM = FRAME_Y + FRAME_HEIGHT; // 59

    private Style2HudRenderer() {
    }

    public static void render(RenderGuiOverlayEvent.Pre event, LivingEntity entity) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        float entityHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
        float entityMaxHealth = Math.max(entity.getMaxHealth(), 0.0F);
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : Math.min(1.0F, entityHealth / entityMaxHealth);
        float scale = cfg.hudIndicatorSize.get().floatValue();

        int xOffset = cfg.hudIndicatorAlignLeft.get()
                ? cfg.hudIndicatorPositionX.get()
                : event.getWindow().getGuiScaledWidth() - Math.round(HUD_EXTENT_RIGHT * scale) - cfg.hudIndicatorPositionX.get();
        int yOffset = cfg.hudIndicatorAlignTop.get()
                ? cfg.hudIndicatorPositionY.get()
                : event.getWindow().getGuiScaledHeight() - Math.round(HUD_EXTENT_BOTTOM * scale) - cfg.hudIndicatorPositionY.get();

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 0);
        poseStack.scale(scale, scale, scale);

        float backgroundOpacity = Mth.clamp(cfg.hudIndicatorBackgroundOpacity.get().floatValue(), 0.0F, 1.0F);

        // 1. Background
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundOpacity);
        blitFull(guiGraphics, TEXTURE_BACKGROUND, BACKGROUND_X, BACKGROUND_Y, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, TEX_BACKGROUND, TEX_BACKGROUND);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 2. Mob preview (scissored to portrait box)
        if (entity.getHealth() > 0.0F) {
            int scissorMinX = xOffset + Math.round(scale * MOB_PREVIEW_X);
            int scissorMinY = yOffset + Math.round(scale * MOB_PREVIEW_Y);
            int scissorMaxX = xOffset + Math.round(scale * (MOB_PREVIEW_X + BACKGROUND_WIDTH));
            int scissorMaxY = yOffset + Math.round(scale * (MOB_PREVIEW_Y + BACKGROUND_HEIGHT));
            guiGraphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
            renderPortraitEntity(guiGraphics, entity, event.getPartialTick());
            guiGraphics.flush();
            guiGraphics.disableScissor();
            // Original DI cleared the depth buffer after the portrait so later GUI
            // layers (frame/nameplate) draw on top of the high-Z entity preview.
            RenderSystem.clear(256, Minecraft.ON_OSX);
        }

        // 3. Health bar (damage strip + health fill)
        drawHealthBar(guiGraphics, healthRatio);

        // 4. Frame
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blitFull(guiGraphics, TEXTURE_FRAME, FRAME_X, FRAME_Y, FRAME_WIDTH, FRAME_HEIGHT, TEX_FRAME_W, TEX_FRAME_H);

        // 5. Name plate
        blitFull(guiGraphics, TEXTURE_NAMEPLATE, NAME_PLATE_X, NAME_PLATE_Y, NAME_PLATE_WIDTH, NAME_PLATE_HEIGHT, 1, 1);

        // 6. Mob type icon
        drawMobTypeIcon(guiGraphics, entity);

        // 7. Potion boxes
        if (cfg.hudPotionEffects.get()) {
            drawPotionBoxes(guiGraphics, entity);
        }

        // 8. Health text
        drawHealthText(guiGraphics, entityHealth, entityMaxHealth);

        // 9. Name text
        drawNameText(guiGraphics, entity);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        poseStack.popPose();
    }

    private static void blitFull(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int texW, int texH) {
        guiGraphics.blit(texture, x, y, width, height, 0.0F, 0.0F, texW, texH, texW, texH);
    }

    private static void drawHealthBar(GuiGraphics guiGraphics, float healthRatio) {
        boolean colorblind = Config.INSTANCE.active().colorblindHealthBar.get();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Missing HP background: full bar width, UV from healthRatio → 1 (original DI behavior)
        if (healthRatio < 1.0F) {
            float uStart = healthRatio * TEX_DAMAGE_W;
            int uWidth = Math.max(1, Math.round((1.0F - healthRatio) * TEX_DAMAGE_W));
            if (colorblind) {
                RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            guiGraphics.blit(TEXTURE_DAMAGE, HEALTH_BAR_X, HEALTH_BAR_Y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT,
                    uStart, 0.0F, uWidth, TEX_DAMAGE_H, TEX_DAMAGE_W, TEX_DAMAGE_H);
        }

        // Current HP fill: left-aligned, UV 0 → healthRatio
        float fillWidth = HEALTH_BAR_WIDTH * healthRatio;
        if (fillWidth > 0.0F) {
            int uWidth = Math.max(1, Math.round(healthRatio * TEX_HEALTH_W));
            if (colorblind) {
                RenderSystem.setShaderColor(1.0F, 0.9F, 0.15F, 1.0F);
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            guiGraphics.blit(TEXTURE_HEALTH, HEALTH_BAR_X, HEALTH_BAR_Y, Math.round(fillWidth), HEALTH_BAR_HEIGHT,
                    0.0F, 0.0F, uWidth, TEX_HEALTH_H, TEX_HEALTH_W, TEX_HEALTH_H);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawMobTypeIcon(GuiGraphics guiGraphics, LivingEntity entity) {
        int iconIndex = MobTypes.getCleanSkinIconIndex(entity);
        boolean boss = iconIndex == 4;
        boolean hostile = MobTypes.isHostileForCleanSkin(entity);

        if (boss) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.6F);
        } else if (hostile) {
            RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 0.6F);
        } else {
            RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 0.6F);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float uStep = TEX_TYPE_W / 5.0F;
        float uOffset = iconIndex * uStep;
        int uWidth = Math.round(uStep);
        guiGraphics.blit(TEXTURE_TYPE_ICONS, MOB_TYPE_X, MOB_TYPE_Y, MOB_TYPE_WIDTH, MOB_TYPE_HEIGHT,
                uOffset, 0.0F, uWidth, TEX_TYPE_H, TEX_TYPE_W, TEX_TYPE_H);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawPotionBoxes(GuiGraphics guiGraphics, LivingEntity entity) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getDuration() > 10 && effect.showIcon() && effect.isVisible()) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        blitFull(guiGraphics, TEXTURE_POTION_LEFT, POTION_BOX_X, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_LEFT_W, TEX_POTION_LEFT_H);

        MobEffectTextureManager effectTextures = Minecraft.getInstance().getMobEffectTextures();
        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = guiGraphics.pose();

        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            int cellX = POTION_BOX_X + i * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
            blitFull(guiGraphics, TEXTURE_POTION_CENTER, cellX, POTION_BOX_Y, POTION_CENTER_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_CENTER_W, TEX_POTION_CENTER_H);

            TextureAtlasSprite sprite = effectTextures.get(effect.getEffect());
            int iconSize = POTION_BOX_HEIGHT - 4;
            guiGraphics.blit(cellX + 2, POTION_BOX_Y + 2, 0, iconSize, iconSize, sprite);

            Component duration = MobEffectUtil.formatDuration(effect, 1.0F);
            String durationText = duration.getString();
            int textWidth = font.width(durationText);
            poseStack.pushPose();
            // Original: translate(cellX + 13 - textWidth/2, ...) then scale 0.815
            poseStack.translate(cellX + 13 - textWidth / 2.0F, POTION_BOX_Y + POTION_BOX_HEIGHT - font.lineHeight * 0.815F, 0.1F);
            poseStack.scale(0.815F, 0.815F, 0.815F);
            int durationColor = 0xFFFF80;
            font.drawInBatch(Component.literal(durationText).getVisualOrderText(), 0.0F, 0.0F, durationColor, true,
                    poseStack.last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
            poseStack.popPose();
        }

        int rightX = POTION_BOX_X + effects.size() * POTION_CENTER_WIDTH + POTION_BOX_SIDE_WIDTH;
        blitFull(guiGraphics, TEXTURE_POTION_RIGHT, rightX, POTION_BOX_Y, POTION_BOX_SIDE_WIDTH, POTION_BOX_HEIGHT, TEX_POTION_RIGHT_W, TEX_POTION_RIGHT_H);
        guiGraphics.flush();
    }

    private static void drawHealthText(GuiGraphics guiGraphics, float entityHealth, float entityMaxHealth) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        Font font = Minecraft.getInstance().font;
        String divisor = cfg.healthSeperator.get() ? " | " : "/";
        float displayMax = entityHealth > entityMaxHealth ? entityHealth : entityMaxHealth;
        String healthText;
        if (cfg.healthDecimals.get()) {
            healthText = DamageIndicators.roundHealth(entityHealth) + divisor + DamageIndicators.roundHealth(displayMax);
        } else {
            // Original DI used MathHelper.ceil for both values
            healthText = Mth.ceil(entityHealth) + divisor + Mth.ceil(displayMax);
        }

        Component healthComponent = Component.literal(healthText);
        int healthWidth = font.width(healthComponent);
        int healthColor = 0xFFFFFF;
        PoseStack poseStack = guiGraphics.pose();

        // Original: if font height + 2 > bar height, draw at 0.7 scale bottom-aligned; else centered
        if (font.lineHeight + 2 > HEALTH_BAR_HEIGHT) {
            poseStack.pushPose();
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth * 0.7F) / 2.0F;
            float textY = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT - font.lineHeight * 0.7F - 0.5F;
            poseStack.translate(textX, textY, 0);
            poseStack.scale(0.7F, 0.7F, 1.0F);
            drawHudText(guiGraphics, healthComponent, 0.0F, 0.0F, healthColor, cfg.hudHealthTextOutline.get());
            poseStack.popPose();
        } else {
            float textX = HEALTH_BAR_X + (HEALTH_BAR_WIDTH - healthWidth) / 2.0F;
            float textY = HEALTH_BAR_Y + (HEALTH_BAR_HEIGHT - font.lineHeight) / 2.0F;
            drawHudText(guiGraphics, healthComponent, textX, textY, healthColor, cfg.hudHealthTextOutline.get());
        }
    }

    private static void drawNameText(GuiGraphics guiGraphics, LivingEntity entity) {
        Font font = Minecraft.getInstance().font;
        String name = entity.getDisplayName().getString();
        // Match original DI default path: prefix "Baby " for babies (no forced italic)
        if (entity.isBaby() && !name.toLowerCase().contains("baby")) {
            name = "Baby " + name;
        }
        Component nameComponent = Component.literal(name);
        int nameWidth = font.width(nameComponent);
        float textX = NAME_PLATE_X + (NAME_PLATE_WIDTH - nameWidth) / 2.0F;
        float textY = NAME_PLATE_Y + (NAME_PLATE_HEIGHT - font.lineHeight) / 2.0F;
        drawHudText(guiGraphics, nameComponent, textX, textY, 0xFFFFFF, Config.INSTANCE.active().hudNameTextOutline.get());
    }

    private static void drawHudText(GuiGraphics guiGraphics, Component text, float x, float y, int color, boolean outline) {
        PoseStack poseStack = guiGraphics.pose();
        Font font = Minecraft.getInstance().font;
        if (outline) {
            font.drawInBatch8xOutline(text.getVisualOrderText(), x, y, color, 0, poseStack.last().pose(), guiGraphics.bufferSource(), 15728880);
        } else {
            font.drawInBatch(text.getVisualOrderText(), x, y, color, true, poseStack.last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        }
        guiGraphics.flush();
    }

    private static void renderPortraitEntity(GuiGraphics guiGraphics, LivingEntity entity, float partialTicks) {
        Config.StyleSettings cfg = Config.INSTANCE.active();
        // Defaults match original Damage Indicators EntityConfigurationEntry
        float scaleFactor = cfg.hudEntitySize.get().floatValue();
        float yOffset = -5.0F;
        float xOffset = 0.0F;
        float babyScaleFactor = 2.0F;
        float entitySizeScaling = 0.0F;

        if (entity instanceof Player) {
            yOffset = 20.0F;
        } else if (entity instanceof Slime) {
            // Original DI EntityConfigurationEntry defaults for Slime/MagmaCube
            scaleFactor = 5.0F;
            entitySizeScaling = 2.0F;
            yOffset = -5.0F;
        } else if (entity instanceof WitherBoss) {
            // Original DI: ScaleFactor=15, YOffset=5
            scaleFactor = 15.0F;
            yOffset = 5.0F;
        }

        float eyeAdj = (3.0F - entity.getEyeHeight()) * entitySizeScaling;
        float finalScale = scaleFactor + scaleFactor * eyeAdj;
        if (entity.isBaby()) {
            finalScale *= babyScaleFactor;
        }
        finalScale *= 0.85F;

        // Original: translate to (previewX + 25 + XOffset, previewY + 52 + YOffset), self gets -30 Y
        float entityX = MOB_PREVIEW_X + 25.0F + xOffset;
        float entityY = MOB_PREVIEW_Y + 52.0F + yOffset;
        if (entity == Minecraft.getInstance().player) {
            entityY -= 30.0F;
        }

        // Original DI lockPosition: -30° after GUI Z-flip.
        float lockedYaw = 180.0F - 30.0F;
        Quaternionf poseRotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf();
        renderEntityInGui(guiGraphics, entityX, entityY, finalScale, poseRotation, cameraOrientation, entity, partialTicks, lockedYaw);
    }

    public static void renderEntityInGui(GuiGraphics guiGraphics, float xPos, float yPos, float scale, Quaternionf rotation, Entity entity, float partialTicks) {
        renderEntityInGui(guiGraphics, xPos, yPos, scale, rotation, null, entity, partialTicks, Float.NaN);
    }

    public static void renderEntityInGui(GuiGraphics guiGraphics, float xPos, float yPos, float scale, Quaternionf poseRotation, @Nullable Quaternionf cameraOrientation, Entity entity, float partialTicks, float lockedYawDegrees) {
        boolean renderModelOnly = DamageIndicators.isRenderModelOnly();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xPos, yPos, 50.0D);
        // Original DI flattened Z to 0.1 so large models stay inside the portrait better
        guiGraphics.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -0.1F));
        guiGraphics.pose().mulPose(poseRotation);
        Lighting.setupForEntityInInventory();

        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (cameraOrientation != null) {
            cameraOrientation.conjugate();
            entityrenderdispatcher.overrideCameraOrientation(cameraOrientation);
        }
        entityrenderdispatcher.setRenderShadow(false);

        int hurtTimeBackup = 0;
        float yBodyRot = 0, yBodyRotO = 0, yRot = 0, yRotO = 0, xRot = 0, xRotO = 0, yHeadRot = 0, yHeadRotO = 0;
        boolean locked = !Float.isNaN(lockedYawDegrees) && entity instanceof LivingEntity;
        if (entity instanceof LivingEntity living) {
            hurtTimeBackup = living.hurtTime;
            living.hurtTime = 0;
            if (locked) {
                yBodyRot = living.yBodyRot;
                yBodyRotO = living.yBodyRotO;
                yRot = living.getYRot();
                yRotO = living.yRotO;
                xRot = living.getXRot();
                xRotO = living.xRotO;
                yHeadRot = living.yHeadRot;
                yHeadRotO = living.yHeadRotO;
                living.yBodyRot = lockedYawDegrees;
                living.yBodyRotO = lockedYawDegrees;
                living.setYRot(lockedYawDegrees);
                living.yRotO = lockedYawDegrees;
                living.setXRot(0.0F);
                living.xRotO = 0.0F;
                living.yHeadRot = lockedYawDegrees;
                living.yHeadRotO = lockedYawDegrees;
            }
        }

        if (renderModelOnly && entity instanceof LivingEntity livingForModel && entityrenderdispatcher.getRenderer(entity) instanceof LivingEntityRenderer livingEntityRenderer) {
            guiGraphics.pose().translate(0.0D, 1.5D, 0.0D);
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
            var renderType = livingEntityRenderer.getModel().renderType(livingEntityRenderer.getTextureLocation(livingForModel));
            livingEntityRenderer.getModel().renderToBuffer(guiGraphics.pose(), guiGraphics.bufferSource().getBuffer(renderType), 15728880, LivingEntityRenderer.getOverlayCoords(livingForModel, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            if (!locked) {
                float entityYRot = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
                if (entity instanceof LivingEntity living) {
                    float bodyRot = living.yBodyRotO + (living.yBodyRot - living.yBodyRotO) * partialTicks;
                    guiGraphics.pose().mulPose(Axis.YN.rotationDegrees(-bodyRot));
                } else {
                    guiGraphics.pose().mulPose(Axis.YN.rotationDegrees(-entityYRot));
                }
            }
            RenderSystem.runAsFancy(() ->
                    entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880)
            );
        }

        if (entity instanceof LivingEntity living) {
            living.hurtTime = hurtTimeBackup;
            if (locked) {
                living.yBodyRot = yBodyRot;
                living.yBodyRotO = yBodyRotO;
                living.setYRot(yRot);
                living.yRotO = yRotO;
                living.setXRot(xRot);
                living.xRotO = xRotO;
                living.yHeadRot = yHeadRot;
                living.yHeadRotO = yHeadRotO;
            }
        }

        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        entityrenderdispatcher.overrideCameraOrientation(null);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
}
