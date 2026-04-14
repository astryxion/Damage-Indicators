package com.astryxion.damageindicators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Fabric config: {@code config/damageindicators.json} (Gson, same defaults as the NeoForge spec).
 */
public final class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Path configPath;

    public static Data INSTANCE = Data.defaults();

    private Config() {
    }

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("damageindicators.json");
        load();
    }

    public static Path getConfigPath() {
        return configPath;
    }

    public static void load() {
        if (!Files.isRegularFile(configPath)) {
            INSTANCE = Data.defaults();
            save();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Data parsed = GSON.fromJson(reader, Data.class);
            INSTANCE = parsed == null ? Data.defaults() : Data.clamp(parsed);
        } catch (IOException e) {
            LOGGER.error("Failed to read config {}, using defaults", configPath, e);
            INSTANCE = Data.defaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(Data.clamp(INSTANCE), writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write config {}", configPath, e);
        }
    }

    public static final class Data {
        public boolean damageParticlesEnabled = true;
        public double damageParticleSize = 1.0;
        public boolean damageParticleOutline = true;
        public boolean hudIndicatorEnabled = true;
        public double maxDistance = 100.0;
        public boolean colorblindHealthBar = false;
        public boolean healthDecimals = true;
        public boolean healthSeperator = true;
        public int hudLingerTime = 30;
        public double hudIndicatorSize = 0.75;
        public double hudIndicatorBackgroundOpacity = 0.75;
        public boolean hudIndicatorAlignLeft = true;
        public boolean hudIndicatorAlignTop = true;
        public int hudIndicatorPositionX = 10;
        public int hudIndicatorPositionY = 10;
        public double hudEntitySize = 38.0;
        public boolean hudNameTextOutline = false;
        public boolean hudHealthTextOutline = false;
        public boolean hpBarAnimated = true;
        public double hpBarAnimationSpeed = 0.15;
        public boolean damageFlash = true;
        public int damageFlashDuration = 8;
        public boolean showModSource = false;
        public double modSourceSize = 1.0;
        public int modSourceOffsetX = 0;
        public int modSourceOffsetY = 5;
        public int modSourceColor = 0xAAAAAA;
        public List<String> hudOldRenderEntities = new ArrayList<>(List.of("alexsmobs:giant_squid"));

        public static Data defaults() {
            return new Data();
        }

        public static Data clamp(Data d) {
            if (d.hudOldRenderEntities == null) {
                d.hudOldRenderEntities = new ArrayList<>(List.of("alexsmobs:giant_squid"));
            }
            d.damageParticleSize = clamp(d.damageParticleSize, 0.1, 10.0);
            d.maxDistance = clamp(d.maxDistance, 3.0, 10000.0);
            d.hudLingerTime = (int) clamp(d.hudLingerTime, 0, 1200);
            d.hudIndicatorSize = clamp(d.hudIndicatorSize, 0.0, 10.0);
            d.hudIndicatorBackgroundOpacity = clamp(d.hudIndicatorBackgroundOpacity, 0.0, 10.0);
            d.hudEntitySize = clamp(d.hudEntitySize, 0.0, 2000.0);
            d.hpBarAnimationSpeed = clamp(d.hpBarAnimationSpeed, 0.01, 1.0);
            d.damageFlashDuration = (int) clamp(d.damageFlashDuration, 2, 30);
            d.modSourceSize = clamp(d.modSourceSize, 0.1, 5.0);
            d.modSourceOffsetX = (int) clamp(d.modSourceOffsetX, -500, 500);
            d.modSourceOffsetY = (int) clamp(d.modSourceOffsetY, -500, 500);
            d.modSourceColor = (int) clamp(d.modSourceColor, 0x000000, 0xFFFFFF);
            return d;
        }

        private static double clamp(double v, double min, double max) {
            return Math.min(max, Math.max(min, v));
        }

        private static long clamp(long v, long min, long max) {
            return Math.min(max, Math.max(min, v));
        }
    }
}
