package com.astryxion.damageindicators.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept for mixin config compatibility. Popoff spawning is handled by
 * {@link com.astryxion.damageindicators.PopoffRenderer}'s client tick health tracker
 * (the synched-data inject is unreliable alone on NeoForge 1.21.1).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
}
