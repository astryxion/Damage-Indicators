package com.github.alexmodguy.retrodamageindicators;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RetroDamageIndicators.MODID)
public class Loader {

    public Loader(IEventBus modEventBus, ModContainer container) {
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(RetroDamageIndicators.class);
            container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
    }
}
