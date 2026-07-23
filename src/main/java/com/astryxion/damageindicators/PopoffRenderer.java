package com.astryxion.damageindicators;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * World-space popoffs for NeoForge 1.21.1.
 * <p>
 * Spawn: client tick health delta (mixin alone does not fire reliably here).
 * Render: FloatingDamageIndicators hook ({@code AFTER_ENTITIES} + event PoseStack +
 * {@code scale(s,-s,s)} billboard). Do NOT apply particle-style Z-180 on the event stack —
 * that combination makes text invisible.
 */
@EventBusSubscriber(modid = DamageIndicators.MODID, value = Dist.CLIENT)
public final class PopoffRenderer {
    private static final int MAX_ENTRIES = 64;
    /** Same packed light as Forge 1.20.1 particles. */
    private static final int FULL_BRIGHT = 15728880;
    private static final CopyOnWriteArrayList<Popoff> ENTRIES = new CopyOnWriteArrayList<>();
    private static final Map<Integer, Float> LAST_HEALTH = new ConcurrentHashMap<>();

    private PopoffRenderer() {
    }

    public static void spawnRetro(double x, double y, double z, double damageAmount, boolean heal) {
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.removeFirst();
        }
        ENTRIES.add(Popoff.retro(x, y, z, damageAmount, heal));
    }

    public static void spawnClassic(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int amount, boolean onTop) {
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.removeFirst();
        }
        ENTRIES.add(Popoff.classic(x, y, z, xSpeed, ySpeed, zSpeed, amount, onTop));
    }

    public static void clear() {
        ENTRIES.clear();
        LAST_HEALTH.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && Config.INSTANCE.active().damageParticlesEnabled.get()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living) || living.isRemoved()) {
                    continue;
                }
                float health = living.getHealth();
                Float prev = LAST_HEALTH.put(living.getId(), health);
                if (prev != null && Float.compare(prev, health) != 0) {
                    DamageIndicators.spawnHurtParticles(living, health - prev);
                }
            }
            LAST_HEALTH.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        } else {
            LAST_HEALTH.clear();
        }

        Iterator<Popoff> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Popoff popoff = it.next();
            popoff.tick();
            if (popoff.removed) {
                ENTRIES.remove(popoff);
            }
        }
    }

    @SubscribeEvent
    public static void onAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null || mc.gameRenderer == null) {
            return;
        }
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Quaternionf cameraRotation = new Quaternionf(camera.rotation());

        for (Popoff popoff : ENTRIES) {
            if (popoff.classic) {
                renderClassic(poseStack, buffer, font, cameraPos, cameraRotation, partialTick, popoff);
            } else {
                renderRetro(poseStack, buffer, font, cameraPos, cameraRotation, partialTick, popoff);
            }
            buffer.endBatch();
        }
    }

    private static void renderRetro(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Font font,
                                    Vec3 cameraPos, Quaternionf cameraRotation, float partialTick, Popoff p) {
        double x = Mth.lerp(partialTick, p.xo, p.x) - cameraPos.x;
        double y = Mth.lerp(partialTick, p.yo, p.y) - cameraPos.y;
        double z = Mth.lerp(partialTick, p.zo, p.z) - cameraPos.z;
        float scale = Mth.lerp(partialTick, p.prevScale, p.scale) * 0.035F;
        int color = opaque(p.heal ? 0x00FF00 : 0xFF0000);
        int colorOutline = opaque(p.heal ? 0x003300 : 0x330000);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        poseStack.scale(scale, -scale, scale);
        poseStack.translate(0.0F, 2.0F, 0.0F);
        float f = -font.width(p.sequence) / 2.0F;

        if (Config.INSTANCE.active().damageParticleOutline.get()) {
            font.drawInBatch8xOutline(p.sequence, f, 0.0F, color, colorOutline, poseStack.last().pose(), buffer, FULL_BRIGHT);
        } else {
            font.drawInBatch(p.sequence, f, 0.0F, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    private static void renderClassic(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Font font,
                                      Vec3 cameraPos, Quaternionf cameraRotation, float partialTick, Popoff p) {
        double x = Mth.lerp(partialTick, p.xo, p.x) - cameraPos.x;
        double y = Mth.lerp(partialTick, p.yo, p.y) - cameraPos.y;
        double z = Mth.lerp(partialTick, p.zo, p.z) - cameraPos.z;
        float renderScale = p.particleSize * 0.008F;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);
        poseStack.scale(renderScale, -renderScale, renderScale);

        int packedColor = p.heal ? 0x00FF00 : 0xFFAA00;
        int red = packedColor >> 16 & 255;
        int green = packedColor >> 8 & 255;
        int blue = packedColor & 255;
        String text = String.valueOf(p.damageAmount);
        float posX = font.width(text) / -2.0F;
        float posY = font.lineHeight / -2.0F;

        renderClassicText(poseStack, buffer, font, text, posX, posY, red, green, blue, p.useDropShadows);
        poseStack.popPose();

        float maxSize = 3.0F * Config.INSTANCE.active().damageParticleSize.get().floatValue() * 3.0F;
        if (p.growing) {
            p.particleSize *= 1.08F;
            if (p.particleSize > maxSize) {
                p.growing = false;
            }
        } else {
            p.particleSize *= 0.96F;
        }
    }

    /** Full Forge ClassicDamageParticle drop-shadow / glow passes. */
    private static void renderClassicText(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Font font,
                                          String str, float posX, float posY, int red, int green, int blue, boolean dropShadows) {
        if (dropShadows) {
            int r = red, g = green, b = blue;
            if (red > green && red > blue) {
                r = 255;
                g = 0;
                b = 0;
            } else if (green > red && green > blue) {
                r = 0;
                g = 255;
                b = 0;
            } else if (blue > red && blue > green) {
                r = 0;
                g = 0;
                b = 255;
            }
            drawString(poseStack, buffer, font, str, posX + 1.0F, posY + 1.0F, withAlpha(200, 0, 0, 0));
            poseStack.pushPose();
            poseStack.translate(-0.2F, -0.2F, 0.0F);
            poseStack.scale(1.075F, 1.075F, 1.0F);
            drawString(poseStack, buffer, font, str, posX, posY, withAlpha(64, (red + r) / 2, (green + g) / 2, (blue + b) / 2));
            poseStack.popPose();
            drawString(poseStack, buffer, font, str, posX, posY, withAlpha(128, (red + red + r) / 3, (green + green + g) / 3, (blue + blue + b) / 3));
            poseStack.pushPose();
            poseStack.translate(0.15F, 0.15F, 0.0F);
            poseStack.scale(0.95F, 0.95F, 1.0F);
            drawString(poseStack, buffer, font, str, posX, posY, withAlpha(255, red, green, blue));
            poseStack.popPose();
        } else {
            drawString(poseStack, buffer, font, str, posX, posY, withAlpha(255, red, green, blue));
        }
    }

    private static void drawString(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Font font, String str, float x, float y, int color) {
        font.drawInBatch(str, x, y, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
    }

    /** Ensure RGB colors from Forge have opaque alpha for 1.21.1 font rendering. */
    private static int opaque(int rgb) {
        return (rgb & 0xFC000000) == 0 ? rgb | 0xFF000000 : rgb;
    }

    private static int withAlpha(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
    }

    private static final class Popoff {
        final boolean classic;
        final boolean heal;
        final boolean useDropShadows;
        final boolean shouldOnTop;
        final int damageAmount;
        final FormattedCharSequence sequence;
        final int lifetime;
        double x, y, z, xo, yo, zo, xd, yd, zd;
        float gravity;
        float scale = 1.0F;
        float prevScale = 1.0F;
        float particleSize;
        boolean growing = true;
        int age;
        boolean removed;

        private Popoff(boolean classic, boolean heal, boolean useDropShadows, boolean shouldOnTop, int damageAmount, FormattedCharSequence sequence, int lifetime) {
            this.classic = classic;
            this.heal = heal;
            this.useDropShadows = useDropShadows;
            this.shouldOnTop = shouldOnTop;
            this.damageAmount = damageAmount;
            this.sequence = sequence;
            this.lifetime = lifetime;
        }

        static Popoff retro(double x, double y, double z, double damageAmount, boolean heal) {
            String text;
            if (Config.INSTANCE.active().healthDecimals.get()) {
                text = String.valueOf(DamageIndicators.roundHealth((float) damageAmount)).replace(".0", "");
            } else {
                text = "" + (int) damageAmount;
            }
            Popoff p = new Popoff(false, heal, false, false, 0, Component.literal(text).getVisualOrderText(), 15 + (int) (Math.random() * 5));
            p.x = p.xo = x;
            p.y = p.yo = y;
            p.z = p.zo = z;
            p.yd = 0.2F + Math.random() * 0.2F;
            p.gravity = 1.3F;
            return p;
        }

        static Popoff classic(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int amount, boolean onTop) {
            Popoff p = new Popoff(true, amount < 0, Config.INSTANCE.active().damageParticleOutline.get(), onTop, Math.abs(amount), Component.literal(String.valueOf(Math.abs(amount))).getVisualOrderText(), 12);
            p.x = p.xo = x;
            p.y = p.yo = y;
            p.z = p.zo = z;
            p.particleSize = 3.0F * Config.INSTANCE.active().damageParticleSize.get().floatValue();
            p.gravity = 0.8F;
            double len = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
            if (len < 1.0E-8) {
                len = 1.0;
            }
            p.xd = xSpeed / len * 0.12;
            p.yd = ySpeed / len * 0.12;
            p.zd = zSpeed / len * 0.12;
            return p;
        }

        void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                removed = true;
                return;
            }
            yd -= 0.04D * gravity;
            x += xd;
            y += yd;
            z += zd;
            if (classic) {
                xd *= 0.98F;
                yd *= 0.98F;
                zd *= 0.98F;
            } else {
                prevScale = scale;
                scale = 1.0F - age / (float) lifetime;
            }
        }
    }
}
