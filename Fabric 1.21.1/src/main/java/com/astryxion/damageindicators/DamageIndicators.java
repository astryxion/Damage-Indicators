package com.astryxion.damageindicators;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DamageIndicators {
    public static final String MODID = "damageindicators";

    private static LivingEntity damageIndicatorEntity;
    private static MobTypes currentMobType = MobTypes.UNKNOWN;
    private static int resetDamageIndicatorEntityIn = 0;
    private static boolean renderModelOnly;

    public static LivingEntity getTargetEntity() {
        return damageIndicatorEntity;
    }

    public static MobTypes getCurrentMobType() {
        return currentMobType;
    }

    public static boolean isRenderModelOnly() {
        return renderModelOnly;
    }

    public static float roundHealth(float entityHealth) {
        return (float) (Math.round(entityHealth * 5) / 5D);
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reloadClientConfigFromDisk());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            Config.INSTANCE.checkForExternalChanges();
            onClientTick();
        });

        // Mirror NeoForge RenderGuiLayerEvent (boss overlay timing) as best-effort on Fabric 1.21.1:
        // HudRenderCallback runs once per HUD pass (no per-layer API yet).
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            if (Minecraft.getInstance().screen != null) {
                return;
            }
            renderHudIfNeeded(graphics, deltaTracker.getGameTimeDeltaPartialTick(false), true);
        });

        // When a menu is open, Gui layers can be skipped — ScreenEvent path draws instead.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!isInGameMenuScreen(screen)) {
                return;
            }
            ScreenEvents.beforeRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    renderHudIfNeeded(graphics, tickDelta, true));
        });
    }

    /**
     * Re-read the client toml from disk so leave-world -> edit style -> rejoin picks up changes
     * without restarting Minecraft.
     */
    public static void reloadClientConfigFromDisk() {
        try {
            Config.INSTANCE.reload();
        } catch (Throwable ignored) {
        }
    }

    private static void clearClientState() {
        damageIndicatorEntity = null;
        currentMobType = MobTypes.UNKNOWN;
        resetDamageIndicatorEntityIn = 0;
        renderModelOnly = false;
        PopoffRenderer.clear();
    }

    private static boolean isInGameMenuScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof net.minecraft.client.gui.screens.PauseScreen
                || screen instanceof net.minecraft.client.gui.screens.ChatScreen
                || screen instanceof net.minecraft.client.gui.screens.DeathScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>;
    }

    private static void renderHudIfNeeded(GuiGraphics guiGraphics, float partialTick, boolean allowedLayer) {
        if (!allowedLayer || !Config.INSTANCE.active().hudIndicatorEnabled.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Leaving world / title screens: level or camera are torn down; rendering a cached entity NPEs.
        if (mc.options.hideGui || mc.level == null || mc.player == null || mc.cameraEntity == null
                || damageIndicatorEntity == null) {
            return;
        }
        var camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return;
        }
        if (damageIndicatorEntity.isRemoved() || damageIndicatorEntity.level() != mc.level) {
            damageIndicatorEntity = null;
            return;
        }
        if (Config.INSTANCE.isClassicStyle()) {
            Style2HudRenderer.render(guiGraphics, partialTick, damageIndicatorEntity);
        } else {
            Style1HudRenderer.render(guiGraphics, partialTick, damageIndicatorEntity, currentMobType, renderModelOnly);
        }
    }

    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.cameraEntity == null) {
            if (damageIndicatorEntity != null) {
                clearClientState();
            }
            return;
        }
        // Freeze linger while a menu is open so the HUD does not expire mid-inventory/pause.
        if (mc.screen != null) {
            return;
        }
        Config.StyleSettings cfg = Config.INSTANCE.active();
        double maxPickDistance = cfg.maxDistance.get();
        double pickDistance = maxPickDistance;
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 vec3 = mc.cameraEntity.getEyePosition(partialTick);
        HitResult hitResult = mc.cameraEntity.pick(pickDistance, partialTick, false);
        LivingEntity found = null;
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            pickDistance = hitResult.getLocation().distanceToSqr(vec3);
        }
        Vec3 vec31 = mc.cameraEntity.getViewVector(1.0F);
        Vec3 vec32 = vec3.add(vec31.x * maxPickDistance, vec31.y * maxPickDistance, vec31.z * maxPickDistance);
        AABB aabb = mc.cameraEntity.getBoundingBox().expandTowards(vec31.scale(maxPickDistance)).inflate(3.0D, 3.0D, 3.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(mc.cameraEntity, vec3, vec32, aabb, (lookingAt) -> {
            return !lookingAt.isSpectator() && lookingAt.isPickable();
        }, pickDistance);
        if (entityhitresult != null) {
            Vec3 vec33 = entityhitresult.getLocation();
            Entity entity = entityhitresult.getEntity();
            double d2 = vec3.distanceToSqr(vec33);
            if (d2 < pickDistance || pickDistance == maxPickDistance) {
                if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof ArmorStand)) {
                    found = living;
                } else if (entity instanceof EnderDragonPart part && part.parentMob instanceof LivingEntity living) {
                    found = living;
                }
            }
        }
        if (found != null) {
            damageIndicatorEntity = found;
            currentMobType = MobTypes.getTypeFor(found);
            resetDamageIndicatorEntityIn = cfg.hudLingerTime.get();
            renderModelOnly = cfg.oldRenderEntities.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(found.getType()).toString());
        } else if (resetDamageIndicatorEntityIn-- < 0) {
            damageIndicatorEntity = null;
            resetDamageIndicatorEntityIn = 0;
        }
    }

    public static void spawnHurtParticles(Entity entity, float damage) {
        if (!Config.INSTANCE.active().damageParticlesEnabled.get()) {
            return;
        }
        if (Config.INSTANCE.isClassicStyle()) {
            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight();
            double z = entity.getZ();
            double bounce = 0.05D * 1.5D;
            int amount = damage > 0 ? -Math.round(damage) : Math.round(Math.abs(damage));
            boolean onTop = Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasLineOfSight(entity);
            PopoffRenderer.spawnClassic(x, y, z, 0.001D, bounce, 0.001D, amount, onTop);
        } else {
            double x = entity.getRandomX(1.0D);
            double y = entity.getEyeY();
            double z = entity.getRandomZ(1.0D);
            PopoffRenderer.spawnRetro(x, y, z, Math.abs(damage), damage > 0);
        }
    }
}
