package com.astryxion.damageindicators;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.entity.PartEntity;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;


@Mod(DamageIndicators.MODID)
public class DamageIndicators {
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

    public DamageIndicators(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(DamageIndicators.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.getDist().isClient() && ModList.get().isLoaded("cloth_config")) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> ConfigScreen.create(parent));
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
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
        if (Config.INSTANCE.healthDecimals.get()) {
            textStr = String.valueOf(roundHealth((float) Math.abs(damage))).replace(".0", "");
        } else {
            textStr = "" + (int) Math.abs(damage);
        }
        boolean heal = damage > 0;
        int color = heal ? 0x00FF00 : 0xFF0000;
        int colorOutline = heal ? 0x003300 : 0x330000;
        activeDamageTexts.add(new DamageText(x, y, z, Component.literal(textStr), color, colorOutline));
    }

    @SubscribeEvent
    public static void onPreRenderGuiElement(RenderGuiLayerEvent.Pre event) {
        if (Config.INSTANCE.hudIndicatorEnabled.get() && Minecraft.getInstance().screen == null) {

            if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY) && damageIndicatorEntity != null && damageIndicatorEntity.isAlive()) {
                float entityMaxHealth = damageIndicatorEntity.getMaxHealth();
                float entityHealth = Math.min(Config.INSTANCE.hpBarAnimated.get() ? displayedHealth : damageIndicatorEntity.getHealth(), entityMaxHealth);
                float healthRatio = entityMaxHealth <= 0.0F ? 0.0F : entityHealth / entityMaxHealth;
                float scale = Config.INSTANCE.hudIndicatorSize.get().floatValue();
                int xOffset = Config.INSTANCE.hudIndicatorAlignLeft.get() ? Config.INSTANCE.hudIndicatorPositionX.get() : event.getGuiGraphics().guiWidth() - (int) (208 * scale) - Config.INSTANCE.hudIndicatorPositionX.get();
                int yOffset = Config.INSTANCE.hudIndicatorAlignTop.get() ? Config.INSTANCE.hudIndicatorPositionY.get() : event.getGuiGraphics().guiHeight() - (int) (78 * scale) - Config.INSTANCE.hudIndicatorPositionY.get();
                {
                    int bossBars = 0;
                    if (Config.INSTANCE.hudIndicatorAlignTop.get()) {
                        if (bossBars > 0) {
                            yOffset += Math.min(event.getGuiGraphics().guiHeight() / 3, 12 + 19 * bossBars);
                        }
                        if (!Config.INSTANCE.hudIndicatorAlignLeft.get()) {
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
                float backgroundOpacity = Config.INSTANCE.hudIndicatorBackgroundOpacity.get().floatValue();
                int relativeHealthbarX = 81;
                int relativeHealthbarY = 25;
                int healthbarHeight = 18;
                int healthbarMaxWidth = 124;
                int currentHealthbarWidth = (int) Math.round(healthbarMaxWidth * healthRatio);
                Matrix3x2fStack pose = event.getGuiGraphics().pose();
                pose.pushMatrix();
                pose.translate(xOffset, yOffset - 0.5F);
                pose.scale(scale, scale);

                // Background first (matches inventory screen), then portrait entity, then frame — avoids washing out the model and keeps layers correct.
                int bgColor = ((int)(backgroundOpacity * 255) << 24) | 0xFFFFFF;
                event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, bgColor);

                int scissorBox1MinX = 16;
                int scissorBox1MinY = 4;
                int scissorBox1MaxX = 73;
                int scissorBox2MaxY = 61;

                if (damageIndicatorEntity != null) {
                    float biggestEntityDimension = Math.max(damageIndicatorEntity.getBbWidth() * 1.2F + 0.3F, damageIndicatorEntity.getBbHeight() * 0.9F) * 0.85F;
                    int renderScale = (int) Config.INSTANCE.hudEntitySize.get().floatValue();
                    if ((double) biggestEntityDimension > 0.5D) {
                        renderScale = (int)(renderScale / biggestEntityDimension);
                    }
                    renderScale = (int)(renderScale * scale);

                    int absBoxX1 = xOffset + Math.round(scale * scissorBox1MinX);
                    int absBoxY1 = (int)(yOffset + scale * scissorBox1MinY);
                    int absBoxX2 = xOffset + Math.round(scale * scissorBox1MaxX);
                    int absBoxY2 = (int)(yOffset + scale * scissorBox2MaxY);

                    float centerX = (absBoxX1 + absBoxX2) / 2.0F;
                    float centerY = (absBoxY1 + absBoxY2) / 2.0F;
                    float mouseX = centerX + 17;
                    float mouseY = centerY - 12;

                    pose.popMatrix();
                    event.getGuiGraphics().enableScissor(absBoxX1, absBoxY1, absBoxX2, absBoxY2);
                    extractHudEntityInInventoryFollowsMouse(
                            event.getGuiGraphics(),
                            absBoxX1, absBoxY1, absBoxX2, absBoxY2,
                            renderScale, 0.0625F,
                            mouseX, mouseY,
                            damageIndicatorEntity);
                    event.getGuiGraphics().disableScissor();
                    pose.pushMatrix();
                    pose.translate(xOffset, yOffset - 0.5F);
                    pose.scale(scale, scale);
                }

                event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256);

                // mob type render
                int relativeMobTypeX = 5;
                int relativeMobTypeY = 55;
                event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, currentMobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18);

                // health render
                int healthbarVOffset = Config.INSTANCE.colorblindHealthBar.get() ? 36 : 0;
                event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256);
                event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256);

                // health text
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
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, healthComponent, 0, 0, healthColor, Config.INSTANCE.hudHealthTextOutline.get());
                pose.popMatrix();

                // name text
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
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, nameComponent, 0, 0, nameColor, Config.INSTANCE.hudNameTextOutline.get());
                pose.popMatrix();

                // mod source text
                if (Config.INSTANCE.showModSource.get() && !currentModSource.isEmpty()) {
                    Component modSourceComponent = Component.literal("[" + currentModSource + "]");
                    int modSourceWidth = Minecraft.getInstance().font.width(modSourceComponent);
                    float maxScale = Config.INSTANCE.modSourceSize.get().floatValue();
                    float modSourceScale = Math.min(110F / modSourceWidth, maxScale);
                    float modSourceX = 143F + Config.INSTANCE.modSourceOffsetX.get();
                    float modSourceY = 46F + Config.INSTANCE.modSourceOffsetY.get();
                    int modSourceColor = 0xFF000000 | Config.INSTANCE.modSourceColor.get();

                    pose.pushMatrix();
                    pose.translate(modSourceX, modSourceY);
                    pose.scale(modSourceScale, modSourceScale);
                    pose.translate(-modSourceWidth / 2F, 0);
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, modSourceComponent, 0, 0, modSourceColor, false);
                    pose.popMatrix();
                }

                // damage flash: tint the same sprites as the HUD so alpha follows the art (no solid rectangle over the whole widget)
                if (Config.INSTANCE.damageFlash.get() && damageFlashTicks > 0) {
                    float flashAlpha = (damageFlashTicks / (float) Config.INSTANCE.damageFlashDuration.get()) * 0.45f;
                    int flashColor = ((int) (flashAlpha * 255) << 24) | 0x00FF2200;
                    var g = event.getGuiGraphics();
                    g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, flashColor);
                    g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_TEXTURE, 0, 0, 0, 0, 208, 78, 256, 256, flashColor);
                    g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset + 18, healthbarMaxWidth, healthbarHeight, 256, 256, flashColor);
                    g.blit(RenderPipelines.GUI_TEXTURED, DAMAGE_INDICATOR_HEALTH_TEXTURE, relativeHealthbarX, relativeHealthbarY, 0, healthbarVOffset, currentHealthbarWidth, healthbarHeight, 256, 256, flashColor);
                    g.blit(RenderPipelines.GUI_TEXTURED, currentMobType.getTexture(), relativeMobTypeX, relativeMobTypeY, 0, 0, 18, 18, 18, 18, flashColor);
                }

                pose.popMatrix();
            }

        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post clientTickEvent) {
        if (Minecraft.getInstance().getCameraEntity() != null) {
            Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
            double maxPickDistance = Config.INSTANCE.maxDistance.get();
            double pickDistance = maxPickDistance;
            Vec3 vec3 = cameraEntity.getEyePosition(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
            HitResult hitResult = cameraEntity.pick(pickDistance, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
            LivingEntity found = null;
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                pickDistance = hitResult.getLocation().distanceToSqr(vec3);
            }
            Vec3 vec31 = cameraEntity.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * maxPickDistance, vec31.y * maxPickDistance, vec31.z * maxPickDistance);
            AABB aabb = cameraEntity.getBoundingBox().expandTowards(vec31.scale(maxPickDistance)).inflate(3.0D, 3.0D, 3.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(cameraEntity, vec3, vec32, aabb, (lookingAt) -> {
                return !lookingAt.isSpectator() && lookingAt.isPickable();
            }, pickDistance);
            if (entityhitresult != null) {
                Vec3 vec33 = entityhitresult.getLocation();
                Entity entity = entityhitresult.getEntity();
                double d2 = vec3.distanceToSqr(vec33);
                if (d2 < pickDistance) {
                    if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof ArmorStand)) {
                        found = (LivingEntity) entity;
                    } else if (entity instanceof PartEntity<?> partEntity && partEntity.getParent() instanceof LivingEntity living
                            && living.isAlive() && !(living instanceof ArmorStand)) {
                        found = living;
                    }
                }
            }
            if (found != null) {
                float currentHealth = found.getHealth();

                // reset animation state when switching target
                if (found != damageIndicatorEntity) {
                    displayedHealth = currentHealth;
                    lastKnownHealth = currentHealth;
                    damageFlashTicks = 0;
                }

                // detect damage for flash
                if (Config.INSTANCE.damageFlash.get() && currentHealth < lastKnownHealth - 0.01f) {
                    damageFlashTicks = Config.INSTANCE.damageFlashDuration.get();
                }
                lastKnownHealth = currentHealth;

                damageIndicatorEntity = found;
                currentMobType = MobTypes.getTypeFor(found);
                resetDamageIndicatorEntityIn = Config.INSTANCE.hudLingerTime.get();
                String modId = BuiltInRegistries.ENTITY_TYPE.getKey(found.getType()).getNamespace();
                currentModSource = ModList.get().getModContainerById(modId)
                        .map(c -> c.getModInfo().getDisplayName())
                        .orElse(modId);
            } else if (resetDamageIndicatorEntityIn-- < 0) {
                damageIndicatorEntity = null;
                currentModSource = "";
                displayedHealth = 0f;
                lastKnownHealth = -1f;
                resetDamageIndicatorEntityIn = 0;
            }

            // Keep displayed HP in sync with the entity even while lingering (e.g. lethal hit: actual HP hits 0
            // before we stop targeting, otherwise animated HP can stick at ~1.0 over a dead mob).
            if (damageIndicatorEntity != null) {
                if (!damageIndicatorEntity.isAlive()) {
                    damageIndicatorEntity = null;
                    currentModSource = "";
                    displayedHealth = 0f;
                    lastKnownHealth = -1f;
                    resetDamageIndicatorEntityIn = 0;
                    damageFlashTicks = 0;
                } else {
                    float targetHealth = damageIndicatorEntity.getHealth();
                    if (Config.INSTANCE.hpBarAnimated.get()) {
                        float speed = Config.INSTANCE.hpBarAnimationSpeed.get().floatValue();
                        displayedHealth += (targetHealth - displayedHealth) * speed;
                        if (Math.abs(displayedHealth - targetHealth) < 0.05f) {
                            displayedHealth = targetHealth;
                        }
                    } else {
                        displayedHealth = targetHealth;
                    }
                }
            }

            if (damageFlashTicks > 0) damageFlashTicks--;

            if (!Minecraft.getInstance().isPaused()) {
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

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        if (activeDamageTexts.isEmpty() || !Config.INSTANCE.damageParticlesEnabled.get()) return;

        CameraRenderState cameraState = event.getLevelRenderState().cameraRenderState;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = cameraState.pos;
        Quaternionf cameraRotation = cameraState.orientation;

        for (DamageText dt : activeDamageTexts) {
            float lifeRatio = 1.0f - (float) dt.age / dt.maxAge;
            float dtScale = 0.025f * lifeRatio * Config.INSTANCE.damageParticleSize.get().floatValue();
            if (dtScale <= 0) continue;

            poseStack.pushPose();
            poseStack.translate(dt.x - cameraPos.x, dt.y - cameraPos.y, dt.z - cameraPos.z);
            poseStack.mulPose(cameraRotation);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(dtScale, dtScale, dtScale);

            float textX = -Minecraft.getInstance().font.width(dt.text) / 2f;
            if (Config.INSTANCE.damageParticleOutline.get()) {
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

    /**
     * Same idea as {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#extractEntityInInventoryFollowsMouse},
     * but strips name tags, leashes, and fire so the HUD portrait does not show extra quads (e.g. a gray nameplate slab) on the model.
     */
    private static void extractHudEntityInInventoryFollowsMouse(
            GuiGraphics graphics,
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
            GuiGraphics graphics,
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
        graphics.submitEntityRenderState(renderState, size, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    private static EntityRenderState createHudPortraitRenderState(LivingEntity entity) {
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = entityRenderDispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.lightCoords = 15728880;
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        renderState.displayFireAnimation = false;
        renderState.nameTag = null;
        renderState.nameTagAttachment = null;
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
