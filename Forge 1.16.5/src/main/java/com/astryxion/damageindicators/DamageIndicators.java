package com.astryxion.damageindicators;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = DamageIndicators.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DamageIndicators {
    public static final String MODID = "damageindicators";

    private static LivingEntity damageIndicatorEntity;
    private static MobTypes currentMobType = MobTypes.UNKNOWN;
    private static int resetDamageIndicatorEntityIn = 0;
    private static boolean renderModelOnly;
    private static long lastKnownConfigModified = -1L;
    /** Same FileConfig Forge loaded — never open a second handle on the same toml. */
    private static CommentedFileConfig boundClientConfig;

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

    public static void drawInBatch8xOutline(FontRenderer font, IReorderingProcessor text, Matrix4f mat, float x, float y, int textColor, int outlineColor, IRenderTypeBuffer bufferSource, boolean seeThroughText, int light) {
        int[][] dirs = new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int[] dir : dirs) {
            font.drawInBatch(text, x + dir[0], y + dir[1], outlineColor, false, mat, bufferSource, seeThroughText, 0, light);
        }
        font.drawInBatch(text, x, y, textColor, false, mat, bufferSource, seeThroughText, 0, light);
    }

    /** Capture Forge's loaded client FileConfig so we reload the same handle. */
    public static void bindForgeClientConfig(CommentedConfig data) {
        if (data instanceof CommentedFileConfig) {
            boundClientConfig = (CommentedFileConfig) data;
            rememberConfigModifiedTime(clientConfigPath());
        }
    }

    private static Path clientConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml");
    }

    /**
     * Re-read the client toml and clear ForgeConfigSpec caches.
     * {@code FileConfig.load()} alone is not enough — ConfigValue keeps a cached snapshot until
     * {@link net.minecraftforge.common.ForgeConfigSpec#afterReload()}.
     */
    public static void reloadClientConfigFromDisk() {
        try {
            Path path = clientConfigPath();
            if (!Files.exists(path)) {
                return;
            }
            if (boundClientConfig == null) {
                boundClientConfig = CommentedFileConfig.builder(path).sync().autosave().preserveInsertionOrder().build();
            }
            boundClientConfig.load();
            // Re-bind so SPEC.childConfig is this handle, then clear all ConfigValue caches.
            Config.SPEC.setConfig(boundClientConfig);
            rememberConfigModifiedTime(path);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hot-reload when the toml changes on disk mid-session (Forge's watcher often misses in-world edits).
     */
    public static void checkForExternalConfigChanges() {
        try {
            Path path = clientConfigPath();
            if (!Files.exists(path)) {
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (lastKnownConfigModified < 0L) {
                lastKnownConfigModified = modified;
                return;
            }
            if (modified != lastKnownConfigModified) {
                lastKnownConfigModified = modified;
                reloadClientConfigFromDisk();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void rememberConfigModifiedTime(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                lastKnownConfigModified = Files.getLastModifiedTime(path).toMillis();
            }
        } catch (Throwable ignored) {
            lastKnownConfigModified = -1L;
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
        reloadClientConfigFromDisk();
    }

    @SubscribeEvent
    public static void onPostRenderHud(RenderGameOverlayEvent.Post event) {
        if (!Config.INSTANCE.active().hudIndicatorEnabled.get()) {
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.BOSSHEALTH || damageIndicatorEntity == null) {
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
        if (clientTickEvent.phase != TickEvent.Phase.START) {
            return;
        }
        checkForExternalConfigChanges();
        if (Minecraft.getInstance().cameraEntity == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Config.StyleSettings cfg = Config.INSTANCE.active();
        double maxPickDistance = cfg.maxDistance.get();
        double pickDistance = maxPickDistance;
        float pt = mc.getFrameTime();
        Entity cameraEntity = mc.cameraEntity;
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
                        found = (LivingEntity) parentEntity;
                    }
                }
            }
        }
        if (found != null) {
            damageIndicatorEntity = found;
            currentMobType = MobTypes.getTypeFor(found);
            resetDamageIndicatorEntityIn = cfg.hudLingerTime.get();
            ResourceLocation key = ForgeRegistries.ENTITIES.getKey(found.getType());
            String keyStr = key == null ? "" : key.toString();
            renderModelOnly = cfg.oldRenderEntities.get().contains(keyStr);
        } else if (--resetDamageIndicatorEntityIn < 0) {
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
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.canSee(entity)) {
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

/**
 * Bind Forge's client FileConfig when it loads / reloads (MOD bus).
 */
@Mod.EventBusSubscriber(modid = DamageIndicators.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
class DamageIndicatorsConfigEvents {
    @SubscribeEvent
    public static void onConfigLoading(ModConfig.Loading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            DamageIndicators.bindForgeClientConfig(event.getConfig().getConfigData());
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfig.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            DamageIndicators.bindForgeClientConfig(event.getConfig().getConfigData());
        }
    }
}
