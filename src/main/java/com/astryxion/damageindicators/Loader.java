package com.astryxion.damageindicators;

import net.fabricmc.api.ClientModInitializer;

public class Loader implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Touch config so defaults are written on first launch.
        Config.INSTANCE.load();
        DamageIndicators.init();
        PopoffRenderer.init();
    }
}
