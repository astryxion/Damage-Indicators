package com.astryxion.damageindicators;

import net.fabricmc.api.ModInitializer;

public class DamageIndicatorsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        Config.init();
    }
}
