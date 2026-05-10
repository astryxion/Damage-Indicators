package com.github.alexmodguy.retrodamageindicators;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(RetroDamageIndicators.MODID)
public class Loader {

    public Loader() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(RetroDamageIndicators.class);
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
    }

}
