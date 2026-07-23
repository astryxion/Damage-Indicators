package com.astryxion.damageindicators;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.entity.PartEntity;

import java.nio.file.Files;

@EventBusSubscriber(modid = DamageIndicators.MODID, value = Dist.CLIENT)
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

    /**
     * Re-read the client toml from disk so leave-world -> edit style -> rejoin picks up changes
     * without restarting Minecraft.
     */
    public static void reloadClientConfigFromDisk() {
        try {
            var path = FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml");
            if (!Files.exists(path)) {
                return;
            }
            CommentedFileConfig fresh = CommentedFileConfig.builder(path).sync().autosave().preserveInsertionOrder().build();
            fresh.load();
            // NeoForge ModConfigSpec.acceptConfig expects ILoadedConfig; apply via correction against disk values.
            Config.SPEC.correct(fresh);
            fresh.save();
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        reloadClientConfigFromDisk();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }

    private static void clearClientState() {
        damageIndicatorEntity = null;
        currentMobType = MobTypes.UNKNOWN;
        resetDamageIndicatorEntityIn = 0;
        renderModelOnly = false;
        PopoffRenderer.clear();
    }

    @SubscribeEvent
    public static void onPreRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        // When a menu is open, Gui layers can be skipped on 1.21.1 — ScreenEvent path draws instead.
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        renderHudIfNeeded(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY));
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        // Only draw on in-world menus (pause/inventory). Skip loading/title/disconnect screens —
        // those tear down EntityRenderDispatcher.camera and crash portrait rendering.
        if (!isInGameMenuScreen(event.getScreen())) {
            return;
        }
        renderHudIfNeeded(event.getGuiGraphics(), event.getPartialTick(), true);
    }

    private static boolean isInGameMenuScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof net.minecraft.client.gui.screens.PauseScreen
                || screen instanceof net.minecraft.client.gui.screens.ChatScreen
                || screen instanceof net.minecraft.client.gui.screens.DeathScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>;
    }

    private static void renderHudIfNeeded(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, boolean allowedLayer) {
        if (!allowedLayer || !Config.INSTANCE.active().hudIndicatorEnabled.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Leaving world / title screens: level or camera are torn down; rendering a cached entity NPEs.
        if (mc.options.hideGui || mc.level == null || mc.player == null || mc.getCameraEntity() == null
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getCameraEntity() == null) {
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
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Entity cameraEntity = mc.getCameraEntity();
        Vec3 vec3 = cameraEntity.getEyePosition(partialTick);
        HitResult hitResult = cameraEntity.pick(pickDistance, partialTick, false);
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
            if (d2 < pickDistance || pickDistance == maxPickDistance) {
                if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof ArmorStand)) {
                    found = living;
                } else if (entity instanceof PartEntity<?> partEntity && partEntity.getParent() instanceof LivingEntity living) {
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
