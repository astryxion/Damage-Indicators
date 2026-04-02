package com.github.alexmodguy.retrodamageindicators;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Config {
    public static final Config INSTANCE;

    static {
        INSTANCE = new Config();
        INSTANCE.load();
    }

    private boolean damageParticlesEnabledVal = true;
    private double damageParticleSizeVal = 1.0D;
    private boolean damageParticleOutlineVal = true;
    private boolean hudIndicatorEnabledVal = true;
    private double maxDistanceVal = 100D;
    private boolean colorblindHealthBarVal = false;
    private boolean healthDecimalsVal = true;
    private boolean healthSeperatorVal = true;
    private int hudLingerTimeVal = 30;
    private double hudIndicatorSizeVal = 0.75D;
    private double hudIndicatorBackgroundOpacityVal = 0.75D;
    private boolean hudIndicatorAlignLeftVal = true;
    private boolean hudIndicatorAlignTopVal = true;
    private int hudIndicatorPositionXVal = 10;
    private int hudIndicatorPositionYVal = 10;
    private double hudEntitySizeVal = 38.0D;
    private boolean hudNameTextOutlineVal = false;
    private boolean hudHealthTextOutlineVal = false;
    private List<String> oldRenderEntitiesVal = new ArrayList<>(List.of("alexsmobs:giant_squid"));

    public final BooleanValue damageParticlesEnabled = () -> damageParticlesEnabledVal;
    public final DoubleValue damageParticleSize = () -> damageParticleSizeVal;
    public final BooleanValue damageParticleOutline = () -> damageParticleOutlineVal;
    public final BooleanValue hudIndicatorEnabled = () -> hudIndicatorEnabledVal;
    public final DoubleValue maxDistance = () -> maxDistanceVal;
    public final BooleanValue colorblindHealthBar = () -> colorblindHealthBarVal;
    public final BooleanValue healthDecimals = () -> healthDecimalsVal;
    public final BooleanValue healthSeperator = () -> healthSeperatorVal;
    public final IntValue hudLingerTime = () -> hudLingerTimeVal;
    public final DoubleValue hudIndicatorSize = () -> hudIndicatorSizeVal;
    public final DoubleValue hudIndicatorBackgroundOpacity = () -> hudIndicatorBackgroundOpacityVal;
    public final BooleanValue hudIndicatorAlignLeft = () -> hudIndicatorAlignLeftVal;
    public final BooleanValue hudIndicatorAlignTop = () -> hudIndicatorAlignTopVal;
    public final IntValue hudIndicatorPositionX = () -> hudIndicatorPositionXVal;
    public final IntValue hudIndicatorPositionY = () -> hudIndicatorPositionYVal;
    public final DoubleValue hudEntitySize = () -> hudEntitySizeVal;
    public final BooleanValue hudNameTextOutline = () -> hudNameTextOutlineVal;
    public final BooleanValue hudHealthTextOutline = () -> hudHealthTextOutlineVal;
    public final ConfigValue<List<? extends String>> oldRenderEntities = () -> oldRenderEntitiesVal;

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("retrodamageindicators.properties");
    }

    public void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return;
        }
        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(path));
            damageParticlesEnabledVal = Boolean.parseBoolean(p.getProperty("damage_particles_enabled", "true"));
            damageParticleSizeVal = Double.parseDouble(p.getProperty("damage_particle_size", "1.0"));
            damageParticleOutlineVal = Boolean.parseBoolean(p.getProperty("damage_particle_outline", "true"));
            hudIndicatorEnabledVal = Boolean.parseBoolean(p.getProperty("hud_indicator_enabled", "true"));
            maxDistanceVal = Double.parseDouble(p.getProperty("max_distance", "100"));
            colorblindHealthBarVal = Boolean.parseBoolean(p.getProperty("colorblind_health_bar", "false"));
            healthDecimalsVal = Boolean.parseBoolean(p.getProperty("health_decimals", "true"));
            healthSeperatorVal = Boolean.parseBoolean(p.getProperty("health_separator", "true"));
            hudLingerTimeVal = Integer.parseInt(p.getProperty("hud_linger_time", "30"));
            hudIndicatorSizeVal = Double.parseDouble(p.getProperty("hud_indicator_size", "0.75"));
            hudIndicatorBackgroundOpacityVal = Double.parseDouble(p.getProperty("hud_indicator_background_opacity", "0.75"));
            hudIndicatorAlignLeftVal = Boolean.parseBoolean(p.getProperty("hud_indicator_align_left", "true"));
            hudIndicatorAlignTopVal = Boolean.parseBoolean(p.getProperty("hud_indicator_align_top", "true"));
            hudIndicatorPositionXVal = Integer.parseInt(p.getProperty("hud_indicator_position_x", "10"));
            hudIndicatorPositionYVal = Integer.parseInt(p.getProperty("hud_indicator_position_y", "10"));
            hudEntitySizeVal = Double.parseDouble(p.getProperty("hud_entity_size", "38.0"));
            hudNameTextOutlineVal = Boolean.parseBoolean(p.getProperty("hud_name_text_outline", "false"));
            hudHealthTextOutlineVal = Boolean.parseBoolean(p.getProperty("hud_health_text_outline", "false"));
            String listVal = p.getProperty("hud_old_render_entities", "alexsmobs:giant_squid");
            oldRenderEntitiesVal = new ArrayList<>();
            for (String s : listVal.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) oldRenderEntitiesVal.add(t);
            }
            if (oldRenderEntitiesVal.isEmpty()) oldRenderEntitiesVal.add("alexsmobs:giant_squid");
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("damage_particles_enabled", String.valueOf(damageParticlesEnabledVal));
        p.setProperty("damage_particle_size", String.valueOf(damageParticleSizeVal));
        p.setProperty("damage_particle_outline", String.valueOf(damageParticleOutlineVal));
        p.setProperty("hud_indicator_enabled", String.valueOf(hudIndicatorEnabledVal));
        p.setProperty("max_distance", String.valueOf(maxDistanceVal));
        p.setProperty("colorblind_health_bar", String.valueOf(colorblindHealthBarVal));
        p.setProperty("health_decimals", String.valueOf(healthDecimalsVal));
        p.setProperty("health_separator", String.valueOf(healthSeperatorVal));
        p.setProperty("hud_linger_time", String.valueOf(hudLingerTimeVal));
        p.setProperty("hud_indicator_size", String.valueOf(hudIndicatorSizeVal));
        p.setProperty("hud_indicator_background_opacity", String.valueOf(hudIndicatorBackgroundOpacityVal));
        p.setProperty("hud_indicator_align_left", String.valueOf(hudIndicatorAlignLeftVal));
        p.setProperty("hud_indicator_align_top", String.valueOf(hudIndicatorAlignTopVal));
        p.setProperty("hud_indicator_position_x", String.valueOf(hudIndicatorPositionXVal));
        p.setProperty("hud_indicator_position_y", String.valueOf(hudIndicatorPositionYVal));
        p.setProperty("hud_entity_size", String.valueOf(hudEntitySizeVal));
        p.setProperty("hud_name_text_outline", String.valueOf(hudNameTextOutlineVal));
        p.setProperty("hud_health_text_outline", String.valueOf(hudHealthTextOutlineVal));
        p.setProperty("hud_old_render_entities", String.join(",", oldRenderEntitiesVal));
        try {
            Files.createDirectories(getConfigPath().getParent());
            p.store(Files.newOutputStream(getConfigPath()), "Retro Damage Indicators config");
        } catch (IOException ignored) {
        }
    }

    public interface BooleanValue { boolean get(); }
    public interface DoubleValue { double get(); }
    public interface IntValue { int get(); }
    public interface ConfigValue<T> { T get(); }
}
