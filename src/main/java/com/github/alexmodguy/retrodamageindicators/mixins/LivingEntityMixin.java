package com.github.alexmodguy.retrodamageindicators.mixins;

import com.github.alexmodguy.retrodamageindicators.Config;
import com.github.alexmodguy.retrodamageindicators.RetroDamageIndicators;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    @Final
    private static DataParameter<Float> DATA_HEALTH_ID;

    @Shadow
    public abstract float getHealth();

    private float lastTrackedHealth = 0;

    @Inject(method = "onSyncedDataUpdated", at = @At("HEAD"))
    public void retroDamageIndicators_onSyncedDataUpdated(DataParameter<?> key, CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (key == DATA_HEALTH_ID) {
            if (living.level != null && living.level.isClientSide()
                    && Config.INSTANCE.damageParticlesEnabled.get()
                    && lastTrackedHealth != living.getHealth()) {
                float difference = living.getHealth() - lastTrackedHealth;
                if (!living.removed && living.isAddedToWorld()) {
                    RetroDamageIndicators.spawnHurtParticles(living, difference);
                }
                lastTrackedHealth = living.getHealth();
            }
        }
    }
}
