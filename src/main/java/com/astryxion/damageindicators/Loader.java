package com.astryxion.damageindicators;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = DamageIndicators.MODID, dist = Dist.CLIENT)
public class Loader {

    public Loader(IEventBus modEventBus, ModContainer container) {
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
    }
}
