package com.github.alexmodguy.retrodamageindicators;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class Loader implements ModInitializer {

    public static final SimpleParticleType DAMAGE_INDICATOR_PARTICLE = FabricParticleTypes.simple(false);

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath(RetroDamageIndicatorsCommon.MODID, "damage_indicator"), DAMAGE_INDICATOR_PARTICLE);
    }
}
