package com.astryxion.damageindicators;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Optional;

/**
 * Style 2 damage/heal popoff — Damage Indicators 1.12.2 {@code DIWordParticles} (orange).
 */
public class ClassicDamageParticle extends Particle {

    private static final ParticleGroup GROUP = new ParticleGroup(1000);

    // Original DIConfig defaults
    private static final int DAMAGE_COLOR = 0xFFAA00;
    private static final int HEAL_COLOR = 0x00FF00;
    private static final float BASE_SIZE = 3.0F;
    private static final float BASE_GRAVITY = 0.8F;
    private static final int BASE_LIFESPAN = 12;

    private final int damageAmount;
    private final boolean heal;
    private final boolean useDropShadows;
    private float particleSize;
    private boolean growing = true;
    private boolean shouldOnTop;

    protected ClassicDamageParticle(ClientLevel clientLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int damage) {
        super(clientLevel, x, y, z);
        this.damageAmount = Math.abs(damage);
        this.heal = damage < 0;
        this.useDropShadows = Config.INSTANCE.active().damageParticleOutline.get();
        this.particleSize = BASE_SIZE * Config.INSTANCE.active().damageParticleSize.get().floatValue();
        this.lifetime = BASE_LIFESPAN;
        this.gravity = BASE_GRAVITY;
        this.hasPhysics = false;

        // Original: normalize motion to length 0.12 (avoid Particle(xSpeed..) random jitter)
        double len = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
        if (len < 1.0E-8) {
            len = 1.0;
        }
        this.xd = xSpeed / len * 0.12;
        this.yd = ySpeed / len * 0.12;
        this.zd = zSpeed / len * 0.12;
    }

    public void setShouldOnTop(boolean shouldOnTop) {
        this.shouldOnTop = shouldOnTop;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.yd -= 0.04D * (double) this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98F;
        this.yd *= 0.98F;
        this.zd *= 0.98F;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = camera.getPosition();
        double x = Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x;
        double y = Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y;
        double z = Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z;

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        // Original: glScaled(Size * 0.008, ...)
        float renderScale = this.particleSize * 0.008F;
        poseStack.scale(renderScale, renderScale, renderScale);

        int packedColor = this.heal ? HEAL_COLOR : DAMAGE_COLOR;
        int red = packedColor >> 16 & 255;
        int green = packedColor >> 8 & 255;
        int blue = packedColor & 255;
        String text = String.valueOf(this.damageAmount);
        Font font = Minecraft.getInstance().font;
        float posX = font.width(text) / -2.0F;
        float posY = font.lineHeight / -2.0F;

        int depthFuncBackup = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        if (this.shouldOnTop) {
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        } else {
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }

        renderText(poseStack, bufferSource, font, text, posX, posY, red, green, blue);
        bufferSource.endBatch();

        RenderSystem.depthFunc(depthFuncBackup);
        poseStack.popPose();

        // Original grew/shrunk during render (frame-rate dependent)
        float maxSize = BASE_SIZE * Config.INSTANCE.active().damageParticleSize.get().floatValue() * 3.0F;
        if (this.growing) {
            this.particleSize *= 1.08F;
            if (this.particleSize > maxSize) {
                this.growing = false;
            }
        } else {
            this.particleSize *= 0.96F;
        }
    }

    /**
     * Port of DIWordParticles.renderText drop-shadow / layered color passes.
     */
    private void renderText(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Font font, String str, float posX, float posY, int red, int green, int blue) {
        float alpha = 1.0F;
        if (this.useDropShadows) {
            int r = red;
            int g = green;
            int b = blue;
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

            // Shadow offset (nearly black)
            drawString(poseStack, bufferSource, font, str, posX + 1.0F, posY + 1.0F, withAlpha((int) (alpha * 200) & 255, 0, 0, 0));

            // Soft mid layer, slightly larger
            poseStack.pushPose();
            poseStack.translate(-0.2F, -0.2F, 0.0F);
            poseStack.scale(1.075F, 1.075F, 1.0F);
            int midR = (red + r) / 2;
            int midG = (green + g) / 2;
            int midB = (blue + b) / 2;
            drawString(poseStack, bufferSource, font, str, posX, posY, withAlpha((int) (alpha * 64) & 255, midR, midG, midB));
            poseStack.popPose();

            // Brighter mid pass
            int brightR = (red + red + r) / 3;
            int brightG = (green + green + g) / 3;
            int brightB = (blue + blue + b) / 3;
            drawString(poseStack, bufferSource, font, str, posX, posY, withAlpha((int) (alpha * 128) & 255, brightR, brightG, brightB));

            // Final crisp number, slightly smaller
            poseStack.pushPose();
            poseStack.translate(0.15F, 0.15F, 0.0F);
            poseStack.scale(0.95F, 0.95F, 1.0F);
            drawString(poseStack, bufferSource, font, str, posX, posY, withAlpha((int) (alpha * 255) & 255, red, green, blue));
            poseStack.popPose();
        } else {
            drawString(poseStack, bufferSource, font, str, posX, posY, withAlpha(255, red, green, blue));
        }
    }

    private static int withAlpha(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
    }

    private static void drawString(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Font font, String str, float x, float y, int color) {
        font.drawInBatch(str, x, y, color, false, poseStack.last().pose(), bufferSource, false, 0, 15728880);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.of(GROUP);
    }
}
