package com.astryxion.damageindicators;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.21.11 HUD portrait rendering — cleaned EntityRenderState (no nametag/leash/fire)
 * like Damage-Indicators-DI-NeoForge-1.21.11.
 */
public final class HudPortraitRenderer {
    private HudPortraitRenderer() {
    }

    /**
     * Style 1 / shared BB formula from NeoForge 1.21.1 Style1HudRenderer.
     */
    public static int style1Scale(LivingEntity entity, float baseEntitySize, float hudScale) {
        float biggest = Math.max(entity.getBbWidth() * 1.2F + 0.3F, entity.getBbHeight() * 0.9F) * 0.85F;
        float renderScale = baseEntitySize;
        if (biggest > 0.5F) {
            renderScale /= biggest;
        }
        return Math.max(1, Math.round(renderScale * hudScale));
    }

    /**
     * Clamp PIP size so the entity's BB fits inside the portrait with margin.
     * Needed for Style 2 (higher hud scale) and wide bosses like Wither under rotation.
     */
    public static int fitToPortraitBox(int desired, LivingEntity entity, int boxW, int boxH) {
        float dim = Math.max(entity.getBbWidth() * 1.15F, entity.getBbHeight()) * 1.1F;
        int box = Math.max(1, Math.min(boxW, boxH));
        int maxFit = Math.max(1, Math.round(box * 0.70F / Math.max(0.1F, dim)));
        return Math.min(Math.max(1, desired), maxFit);
    }

    public static void renderFollowsMouse(
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
        renderFollowsAngle(graphics, x0, y0, x1, y1, size, offsetY, xAngle, yAngle, entity);
    }

    public static void renderFollowsAngle(
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

    /**
     * Same idea as InventoryScreen entity extract, but strips name tags, leashes, and fire
     * so the HUD portrait does not show extra quads (e.g. a gray nametag slab) on the model.
     */
    public static EntityRenderState createHudPortraitRenderState(LivingEntity entity) {
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
}
