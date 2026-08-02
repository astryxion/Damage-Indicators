package com.astryxion.damageindicators;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.forgespi.Environment;

@Mod(DamageIndicators.MODID)
public class Loader {

    public Loader() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));

        if (Environment.get().getDist().isClient()) {
            MinecraftForge.EVENT_BUS.register(DamageIndicators.class);
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
    }
}
