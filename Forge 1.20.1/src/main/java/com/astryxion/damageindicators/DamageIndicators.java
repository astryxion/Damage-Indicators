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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;

@Mod.EventBusSubscriber(modid = "damageindicators", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
     * Re-read the client toml from disk so leave-world → edit style → rejoin picks up changes
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
            Config.SPEC.acceptConfig(fresh);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        reloadClientConfigFromDisk();
    }

    @SubscribeEvent
    public static void onPreRenderGuiElement(RenderGuiOverlayEvent.Pre event) {
        if (!Config.INSTANCE.active().hudIndicatorEnabled.get()) {
            return;
        }
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id()) || damageIndicatorEntity == null) {
            return;
        }
        if (Config.INSTANCE.isClassicStyle()) {
            Style2HudRenderer.render(event, damageIndicatorEntity);
        } else {
            Style1HudRenderer.render(event, damageIndicatorEntity, currentMobType, renderModelOnly);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent clientTickEvent) {
        if (clientTickEvent.phase == TickEvent.Phase.START && Minecraft.getInstance().cameraEntity != null) {
            Config.StyleSettings cfg = Config.INSTANCE.active();
            double maxPickDistance = cfg.maxDistance.get();
            double pickDistance = maxPickDistance;
            Vec3 vec3 = Minecraft.getInstance().cameraEntity.getEyePosition(Minecraft.getInstance().getPartialTick());
            HitResult hitResult = Minecraft.getInstance().cameraEntity.pick(pickDistance, Minecraft.getInstance().getPartialTick(), false);
            LivingEntity found = null;
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                pickDistance = hitResult.getLocation().distanceToSqr(vec3);
            }
            Vec3 vec31 = Minecraft.getInstance().cameraEntity.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * maxPickDistance, vec31.y * maxPickDistance, vec31.z * maxPickDistance);
            AABB aabb = Minecraft.getInstance().cameraEntity.getBoundingBox().expandTowards(vec31.scale(maxPickDistance)).inflate(3.0D, 3.0D, 3.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(Minecraft.getInstance().cameraEntity, vec3, vec32, aabb, (lookingAt) -> {
                return !lookingAt.isSpectator() && lookingAt.isPickable();
            }, pickDistance);
            if (entityhitresult != null) {
                Vec3 vec33 = entityhitresult.getLocation();
                Entity entity = entityhitresult.getEntity();
                double d2 = vec3.distanceToSqr(vec33);
                if (d2 < pickDistance) {
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
