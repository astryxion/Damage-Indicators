package com.astryxion.damageindicators;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
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

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            Config.INSTANCE.checkForExternalChanges();
            onClientTick();
        });

        // Mirror Forge RenderGuiOverlayEvent on boss event progress overlay timing as best-effort
        // (Fabric 1.20.1 HudRenderCallback runs once per HUD pass).
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!Config.INSTANCE.active().hudIndicatorEnabled.get()) {
                return;
            }
            if (damageIndicatorEntity == null) {
                return;
            }
            if (Config.INSTANCE.isClassicStyle()) {
                Style2HudRenderer.render(graphics, tickDelta, damageIndicatorEntity);
            } else {
                Style1HudRenderer.render(graphics, tickDelta, damageIndicatorEntity, currentMobType, renderModelOnly);
            }
        });
    }

    /**
     * Re-read the client toml from disk so leave-world → edit style → rejoin picks up changes
     * without restarting Minecraft.
     */
    public static void reloadClientConfigFromDisk() {
        try {
            Config.INSTANCE.reload();
        } catch (Throwable ignored) {
        }
    }

    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.cameraEntity == null) {
            return;
        }
        Config.StyleSettings cfg = Config.INSTANCE.active();
        double maxPickDistance = cfg.maxDistance.get();
        double pickDistance = maxPickDistance;
        Vec3 vec3 = mc.cameraEntity.getEyePosition(mc.getFrameTime());
        HitResult hitResult = mc.cameraEntity.pick(pickDistance, mc.getFrameTime(), false);
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
            if (d2 < pickDistance) {
                if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof ArmorStand)) {
                    found = living;
                } else if (entity instanceof EnderDragonPart part) {
                    found = part.parentMob;
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
            ClassicDamageParticle particle = new ClassicDamageParticle(
                    Minecraft.getInstance().level, x, y, z, 0.001D, bounce, 0.001D, amount);
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasLineOfSight(entity)) {
                particle.setShouldOnTop(true);
            }
            Minecraft.getInstance().particleEngine.add(particle);
        } else {
            double x = entity.getRandomX(1.0D);
            double y = entity.getEyeY();
            double z = entity.getRandomZ(1.0D);
            Minecraft.getInstance().particleEngine.add(new DamageIndicatorParticle(
                    Minecraft.getInstance().level, x, y, z, Math.abs(damage), damage > 0));
        }
    }
}
