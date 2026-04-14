package com.astryxion.damageindicators;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class DamageIndicators {
    public static final String MODID = "damageindicators";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier DAMAGE_INDICATOR_TEXTURE = Identifier.fromNamespaceAndPath(MODID, "textures/gui/damage_indicator.png");
    private static final Identifier DAMAGE_INDICATOR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(MODID, "textures/gui/damage_indicator_background.png");
    private static final Identifier DAMAGE_INDICATOR_HEALTH_TEXTURE = Identifier.fromNamespaceAndPath(MODID, "textures/gui/damage_indicator_health.png");
    private static LivingEntity damageIndicatorEntity;
    private static MobTypes currentMobType = MobTypes.UNKNOWN;
    private static String currentModSource = "";
    private static int resetDamageIndicatorEntityIn = 0;
    private static float displayedHealth = 0f;
    private static float lastKnownHealth = -1f;
    private static int damageFlashTicks = 0;
    private static final List<DamageText> activeDamageTexts = new ArrayList<>();

    private DamageIndicators() {
    }

    public static float roundHealth(float entityHealth) {
        return (float) (Math.round(entityHealth * 5) / 5D);
    }

    public static void spawnHurtParticles(Entity entity, float damage) {
        if (Minecraft.getInstance().level == null) return;
        if (activeDamageTexts.size() >= 100) return;
        double x = entity.getRandomX(1.0D);
        double y = entity.getEyeY();
        double z = entity.getRandomZ(1.0D);
        String textStr;
        if (Config.INSTANCE.healthDecimals) {
            textStr = String.valueOf(roundHealth((float) Math.abs(damage))).replace(".0", "");
        } else {
            textStr = "" + (int) Math.abs(damage);
        }
        boolean heal = damage > 0;
        int color = heal ? 0x00FF00 : 0xFF0000;
        int colorOutline = heal ? 0x003300 : 0x330000;
        activeDamageTexts.add(new DamageText(x, y, z, Component.literal(textStr), color, colorOutline));
    }

    public static void renderHudBeforeBossBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!Config.INSTANCE.hudIndicatorEnabled || Minecraft.getInstance().screen != null) {
            return;
        }
        if (damageIndicatorEntity == null) {
            return;
        }

        float entityMaxHealth = damageIndicatorEntity.getMaxHealth();
        float rawShown = damageIndicatorEntity.isAlive()
                ? (Config.INSTANCE.hpBarAnimated ? displayedHealth : damageIndicatorEntity.getHealth())
                : 0f;
        float entityHealth = Math.min(rawShown, entityMaxHealth);
        float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
        float scale = (float) Config.INSTANCE.hudIndicatorSize;
        int xOffset = Config.INSTANCE.hudIndicatorAlignLeft ? Config.INSTANCE.hudIndicatorPositionX : graphics.guiWidth() - (int) (208 * scale) - Config.INSTANCE.hudIndicatorPositionX;
        int yOffset = Config.INSTANCE.hudIndicatorAlignTop ? Config.INSTANCE.hudIndicatorPositionY : graphics.guiHeight() - (int) (78 * scale) - Config.INSTANCE.hudIndicatorPositionY;
        if (Minecraft.getInstance().gui instanceof Gui) {
            int bossBars = 0;
            if (Config.INSTANCE.hudIndicatorAlignTop) {
                if (bossBars > 0) {
                    yOffset += Math.min(graphics.guiHeight() / 3, 12 + 19 * bossBars);
                }
                if (!Config.INSTANCE.hudIndicatorAlignLeft) {
                    int potionsActive = 0;
                    for (MobEffectInstance mobEffectInstance : Minecraft.getInstance().player.getActiveEffects()) {
                        if (mobEffectInstance.showIcon()) {
                            potionsActive++;
                        }
                    }
                    yOffset += Math.min(potionsActive, 2) * 24;
                }
            }
        }
        float backgroundOpacity = (float) Config.INSTANCE.hudIndicatorBackgroundOpacity;
        int relativeHealthbarX = 81;
        int relativeHealthbarY = 25;
        int healthbarHeight = 18;
        int healthbarMaxWidth = 124;
        int currentHealthbarWidth = (int) Math.round(healthbarMaxWidth * healthRatio);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(xOffset, yOffset - 0.5F);
        pose.scale(scale, scale);

        int bgColor = ((int) (backgroundOpacity * 255) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, bgColor);

        int scissorBox1MinX = 16;
        int scissorBox1MinY = 4;
        int scissorBox1MaxX = 73;
        int scissorBox2MaxY = 61;

        if (damageIndicatorEntity != null) {
            float biggestEntityDimension = Math.max(damageIndicatorEntity.getBbWidth() * 1.2F + 0.3F, damageIndicatorEntity.getBbHeight() * 0.9F) * 0.85F;
            int renderScale = (int) Config.INSTANCE.hudEntitySize;
            if ((double) biggestEntityDimension > 0.5D) {
                renderScale = (int) (renderScale / biggestEntityDimension);
            }
            renderScale = (int) (renderScale * scale);

            int absBoxX1 = xOffset + Math.round(scale * scissorBox1MinX);
            int absBoxY1 = (int) (yOffset + scale * scissorBox1MinY);
            int absBoxX2 = xOffset + Math.round(scale * scissorBox1MaxX);
            int absBoxY2 = (int) (yOffset + scale * scissorBox2MaxY);

            float centerX = (absBoxX1 + absBoxX2) / 2.0F;
            float centerY = (absBoxY1 + absBoxY2) / 2.0F;
            float mouseX = centerX + 17;
            float mouseY = centerY - 12;

            pose.popMatrix();
            graphics.enableScissor(absBoxX1, absBoxY1, absBoxX2, absBoxY2);
            extractHudEntityInInventoryFollowsMouse(
                    graphics,
                    absBoxX1, absBoxY1, absBoxX2, absBoxY2,
                    renderScale, 0.0625F,
                    mouseX, mouseY,
                    damageIndicatorEntity);
            graphics.disableScissor();
            pose.pushMatrix();
            pose.translate(xOffset, yOffset - 0.5F);
            pose.scale(scale, scale);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256);

        int relativeMobTypeX = 5;
        int relativeMobTypeY = 55;
        graphics.blit(RenderPipelines.GUI_TEXTURED, currentMobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18);

        int healthbarVOffset = Config.INSTANCE.colorblindHealthBar ? 36 : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

        String healthText;
        float healthOffsetX = 136;
        float healthOffsetY = 30;
        String healthDivisor;
        int firstHalfWidth;
        if (Config.INSTANCE.healthSeperator) {
            healthDivisor = " | ";
        } else {
            healthDivisor = "/";
            healthOffsetX += 4;
        }
        if (Config.INSTANCE.healthDecimals) {
            healthText = roundHealth(entityHealth) + healthDivisor + roundHealth(entityMaxHealth);
            firstHalfWidth = Minecraft.getInstance().font.width("" + roundHealth(entityHealth));
        } else {
            healthText = (int) entityHealth + healthDivisor + (int) entityMaxHealth;
            firstHalfWidth = Minecraft.getInstance().font.width("" + (int) entityHealth);
        }
        Component healthComponent = Component.literal(healthText);
        int healthWidth = Minecraft.getInstance().font.width(healthComponent);
        float healthScale = Math.min(88F / (float) healthWidth, 1.35F);
        int healthColor = 0xFFFFFFFF;

        pose.pushMatrix();
        pose.translate(healthOffsetX, healthOffsetY);
        pose.scale(healthScale, healthScale);
        pose.translate(-firstHalfWidth, 0);
        graphics.text(Minecraft.getInstance().font, healthComponent, 0, 0, healthColor, Config.INSTANCE.hudHealthTextOutline);
        pose.popMatrix();

        Component nameComponent = damageIndicatorEntity.getDisplayName();
        int nameWidth = Minecraft.getInstance().font.width(nameComponent);
        float nameScale = Math.min(113F / (float) nameWidth, 1.25F);
        float nameOffsetX = 138.5F;
        float nameOffsetY = 6.5F;
        int nameColor = 0xFFFFFFFF;

        pose.pushMatrix();
        pose.translate(nameOffsetX, nameOffsetY);
        pose.scale(nameScale, nameScale);
        pose.translate(-nameWidth / 2F, 0);
        graphics.text(Minecraft.getInstance().font, nameComponent, 0, 0, nameColor, Config.INSTANCE.hudNameTextOutline);
        pose.popMatrix();

        if (Config.INSTANCE.showModSource && !currentModSource.isEmpty()) {
            Component modSourceComponent = Component.literal("[" + currentModSource + "]");
            int modSourceWidth = Minecraft.getInstance().font.width(modSourceComponent);
            float maxScale = (float) Config.INSTANCE.modSourceSize;
            float modSourceScale = Math.min(110F / modSourceWidth, maxScale);
            float modSourceX = 143F + Config.INSTANCE.modSourceOffsetX;
            float modSourceY = 46F + Config.INSTANCE.modSourceOffsetY;
            int modSourceColor = 0xFF000000 | Config.INSTANCE.modSourceColor;

            pose.pushMatrix();
            pose.translate(modSourceX, modSourceY);
            pose.scale(modSourceScale, modSourceScale);
            pose.translate(-modSourceWidth / 2F, 0);
            graphics.text(Minecraft.getInstance().font, modSourceComponent, 0, 0, modSourceColor, false);
            pose.popMatrix();
        }

        if (Config.INSTANCE.damageFlash && damageFlashTicks > 0) {
            float flashAlpha = (damageFlashTicks / (float) Config.INSTANCE.damageFlashDuration) * 0.45f;
            int flashColor = ((int) (flashAlpha * 255) << 24) | 0x00FF2200;
            var g = graphics;
            g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, flashColor);
            g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, flashColor);
            g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256, flashColor);
            g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256, flashColor);
            g.blit(RenderPipelines.GUI_TEXTURED, currentMobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18, flashColor);
        }

        pose.popMatrix();
    }

    public static void onClientTick(Minecraft client) {
        if (client.getCameraEntity() != null) {
            Entity cameraEntity = client.getCameraEntity();
            double maxPickDistance = Config.INSTANCE.maxDistance;
            double pickDistance = maxPickDistance;
            Vec3 vec3 = cameraEntity.getEyePosition(client.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            HitResult hitResult = cameraEntity.pick(pickDistance, client.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
            LivingEntity found = null;
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                pickDistance = hitResult.getLocation().distanceToSqr(vec3);
            }
            Vec3 vec31 = cameraEntity.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * maxPickDistance, vec31.y * maxPickDistance, vec31.z * maxPickDistance);
            AABB aabb = cameraEntity.getBoundingBox().expandTowards(vec31.scale(maxPickDistance)).inflate(3.0D, 3.0D, 3.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(cameraEntity, vec3, vec32, aabb, lookingAt -> !lookingAt.isSpectator() && lookingAt.isPickable(), pickDistance);
            if (entityhitresult != null) {
                Vec3 vec33 = entityhitresult.getLocation();
                Entity entity = entityhitresult.getEntity();
                double d2 = vec3.distanceToSqr(vec33);
                if (d2 < pickDistance) {
                    if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof ArmorStand)) {
                        found = living;
                    } else if (entity instanceof EnderDragonPart dragonPart
                            && dragonPart.parentMob instanceof LivingEntity living
                            && living.isAlive()) {
                        found = living;
                    }
                }
            }
            if (found != null) {
                float currentHealth = found.getHealth();

                if (found != damageIndicatorEntity) {
                    displayedHealth = currentHealth;
                    lastKnownHealth = currentHealth;
                    damageFlashTicks = 0;
                }

                if (Config.INSTANCE.damageFlash && currentHealth < lastKnownHealth - 0.01f) {
                    damageFlashTicks = Config.INSTANCE.damageFlashDuration;
                }
                lastKnownHealth = currentHealth;

                float speed = (float) Config.INSTANCE.hpBarAnimationSpeed;
                displayedHealth += (currentHealth - displayedHealth) * speed;
                if (Math.abs(displayedHealth - currentHealth) < 0.05f) displayedHealth = currentHealth;

                damageIndicatorEntity = found;
                currentMobType = MobTypes.getTypeFor(found);
                resetDamageIndicatorEntityIn = Config.INSTANCE.hudLingerTime;
                String modId = BuiltInRegistries.ENTITY_TYPE.getKey(found.getType()).getNamespace();
                currentModSource = FabricLoader.getInstance().getModContainer(modId)
                        .map(c -> c.getMetadata().getName())
                        .orElse(modId);
            } else {
                // Crosshair no longer resolves to this mob (e.g. it died — isAlive() fails on ray trace).
                // Keep linger HUD but sync health so the bar can hit 0 instead of freezing at the last value.
                if (damageIndicatorEntity != null) {
                    LivingEntity target = damageIndicatorEntity;
                    if (!target.isAlive() || target.isRemoved()) {
                        displayedHealth = 0f;
                        lastKnownHealth = 0f;
                    } else {
                        float currentHealth = target.getHealth();
                        if (Config.INSTANCE.damageFlash && currentHealth < lastKnownHealth - 0.01f) {
                            damageFlashTicks = Config.INSTANCE.damageFlashDuration;
                        }
                        lastKnownHealth = currentHealth;
                        float speed = (float) Config.INSTANCE.hpBarAnimationSpeed;
                        displayedHealth += (currentHealth - displayedHealth) * speed;
                        if (Math.abs(displayedHealth - currentHealth) < 0.05f) displayedHealth = currentHealth;
                    }
                }
                if (resetDamageIndicatorEntityIn-- < 0) {
                    damageIndicatorEntity = null;
                    currentModSource = "";
                    displayedHealth = 0f;
                    lastKnownHealth = -1f;
                    resetDamageIndicatorEntityIn = 0;
                }
            }

            if (damageFlashTicks > 0) damageFlashTicks--;

            if (!client.isPaused()) {
                for (DamageText dt : activeDamageTexts) {
                    dt.age++;
                    dt.vy -= 0.04;
                    dt.y += dt.vy;
                }
                activeDamageTexts.removeIf(dt -> dt.age >= dt.maxAge);
            }
        } else {
            activeDamageTexts.clear();
        }
    }

    public static void onRenderLevelAfterSolidFeatures(LevelRenderContext context) {
        if (activeDamageTexts.isEmpty() || !Config.INSTANCE.damageParticlesEnabled) return;

        CameraRenderState cameraState = context.levelState().cameraRenderState;
        if (cameraState == null) return;
        PoseStack poseStack = context.poseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = cameraState.pos;
        Quaternionf cameraRotation = cameraState.orientation;

        for (DamageText dt : activeDamageTexts) {
            float lifeRatio = 1.0f - (float) dt.age / dt.maxAge;
            float dtScale = 0.025f * lifeRatio * (float) Config.INSTANCE.damageParticleSize;
            if (dtScale <= 0) continue;

            poseStack.pushPose();
            poseStack.translate(dt.x - cameraPos.x, dt.y - cameraPos.y, dt.z - cameraPos.z);
            poseStack.mulPose(cameraRotation);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(dtScale, dtScale, dtScale);

            float textX = -Minecraft.getInstance().font.width(dt.text) / 2f;
            if (Config.INSTANCE.damageParticleOutline) {
                Minecraft.getInstance().font.drawInBatch8xOutline(
                        dt.text.getVisualOrderText(), textX, 0f,
                        dt.color, dt.colorOutline,
                        poseStack.last().pose(), bufferSource, 15728880);
            } else {
                Minecraft.getInstance().font.drawInBatch(
                        dt.text.getVisualOrderText(), textX, 0f,
                        dt.color, false, poseStack.last().pose(), bufferSource,
                        Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            }
            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    private static void extractHudEntityInInventoryFollowsMouse(
            GuiGraphicsExtractor graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int size,
            float offsetY,
            float mouseX,
            float mouseY,
            LivingEntity entity
    ) {
        float centerX = (x0 + x1) / 2.0F;
        float centerY = (y0 + y1) / 2.0F;
        float xAngle = (float) Math.atan((centerX - mouseX) / 40.0F);
        float yAngle = (float) Math.atan((centerY - mouseY) / 40.0F);
        renderHudEntityInInventoryFollowsAngle(graphics, x0, y0, x1, y1, size, offsetY, xAngle, yAngle, entity);
    }

    private static void renderHudEntityInInventoryFollowsAngle(
            GuiGraphicsExtractor graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int size,
            float offsetY,
            float xAngle,
            float yAngle,
            LivingEntity entity
    ) {
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);
        EntityRenderState renderState = createHudPortraitRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingRenderState) {
            livingRenderState.bodyRot = 180.0F + xAngle * 20.0F;
            livingRenderState.yRot = xAngle * 20.0F;
            if (livingRenderState.pose != Pose.FALL_FLYING) {
                livingRenderState.xRot = -yAngle * 20.0F;
            } else {
                livingRenderState.xRot = 0.0F;
            }

            livingRenderState.boundingBoxWidth = livingRenderState.boundingBoxWidth / livingRenderState.scale;
            livingRenderState.boundingBoxHeight = livingRenderState.boundingBoxHeight / livingRenderState.scale;
            livingRenderState.scale = 1.0F;
        }

        Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + offsetY, 0.0F);
        graphics.entity(renderState, size, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    private static EntityRenderState createHudPortraitRenderState(LivingEntity entity) {
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = entityRenderDispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        renderState.displayFireAnimation = false;
        renderState.nameTag = null;
        renderState.nameTagAttachment = null;
        renderState.scoreText = null;
        if (renderState.leashStates != null) {
            renderState.leashStates.clear();
        }
        return renderState;
    }

    static class DamageText {
        double x, y, z;
        final Component text;
        final int color, colorOutline;
        int age;
        final int maxAge;
        double vy;

        DamageText(double x, double y, double z, Component text, int color, int colorOutline) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.color = color;
            this.colorOutline = colorOutline;
            this.age = 0;
            this.maxAge = 15 + (int) (Math.random() * 6);
            this.vy = 0.15 + Math.random() * 0.15;
        }
    }
}
