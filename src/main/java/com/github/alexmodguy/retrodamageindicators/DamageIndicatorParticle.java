package com.github.alexmodguy.retrodamageindicators;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class DamageIndicatorParticle extends Particle {

    private final ITextComponent damageString;
    private final boolean heal;
    private float scale;
    private float prevScale;

    protected DamageIndicatorParticle(ClientWorld clientLevel, double x, double y, double z, double damageAmount, boolean heal) {
        super(clientLevel, x, y, z);
        this.lifetime = 15 + clientLevel.random.nextInt(5);
        String text;
        if (Config.INSTANCE.healthDecimals.get()) {
            text = String.valueOf(RetroDamageIndicators.roundHealth((float) damageAmount)).replace(".0", "");
        } else {
            text = "" + (int) damageAmount;
        }
        this.damageString = new StringTextComponent(text);
        this.heal = heal;
        this.scale = 1.0F;
        this.yd = 0.2F + Math.random() * 0.2F;
        this.gravity = 1.3F;
    }

    @Override
    public void tick() {
        super.tick();
        float ageScaled = this.age / (float) this.lifetime;
        this.prevScale = this.scale;
        this.scale = 1.0F - ageScaled;
    }

    @Override
    public void render(IVertexBuilder vertexConsumer, ActiveRenderInfo camera, float partialTicks) {
        net.minecraft.client.renderer.IRenderTypeBuffer.Impl multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vector3d cameraPos = camera.getPosition();
        double x = (float) MathHelper.lerp((double) partialTicks, this.xo, this.x);
        double y = (float) MathHelper.lerp((double) partialTicks, this.yo, this.y);
        double z = (float) MathHelper.lerp((double) partialTicks, this.zo, this.z);
        int color = heal ? 0X00FF00 : 0XFF0000;
        int colorOutline = heal ? 0X003300 : 0X330000;
        float scaled = this.prevScale + (this.scale - this.prevScale) * partialTicks;
        float particleScale = scaled * 0.035F;
        MatrixStack posestack = new MatrixStack();
        posestack.pushPose();
        posestack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
        posestack.mulPose(camera.rotation());
        posestack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        float f = (float)(-Minecraft.getInstance().font.width(damageString.getVisualOrderText()) / 2);
        posestack.scale(particleScale, particleScale, particleScale);
        posestack.translate(0.0F, -2.0F, 0.0F);
        if(Config.INSTANCE.damageParticleOutline.get()){
            RetroDamageIndicators.drawInBatch8xOutline(Minecraft.getInstance().font, damageString.getVisualOrderText(),
                    posestack.last().pose(), f, 0.0F, color, colorOutline, multibuffersource$buffersource, false, 15728880);
        }else{
            Minecraft.getInstance().font.drawInBatch(damageString.getVisualOrderText(),
                    f, 0.0F, color, false, posestack.last().pose(), multibuffersource$buffersource,
                    false, 0, 15728880);
        }
        multibuffersource$buffersource.endBatch();
        posestack.popPose();
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.CUSTOM;
    }

}
