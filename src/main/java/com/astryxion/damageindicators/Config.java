package com.astryxion.damageindicators;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Client config with a top-level style switch:
 * <ul>
 *   <li>Style 1 - original Retro Damage Indicators (1.20.1) look</li>
 *   <li>Style 2 - Damage Indicators 1.12.2 Clean / classic look</li>
 * </ul>
 * Only the active style's section is read at runtime.
 * <p>
 * Uses the same {@code damageindicators-client.toml} layout as the NeoForge ports.
 */
public class Config {
    public static final Config INSTANCE = new Config();

    private final Path path = FabricLoader.getInstance().getConfigDir().resolve(DamageIndicators.MODID + "-client.toml");
    private CommentedFileConfig file;
    /** Used to hot-reload when the user edits the toml mid-session (NeoForge ModConfigSpec does this automatically). */
    private long lastKnownModified = -1L;

    /** 1 = Retro Damage Indicators, 2 = classic Damage Indicators 1.12.2 */
    public final ConfigValue<Integer> style;
    public final StyleSettings style1;
    public final StyleSettings style2;

    private Config() {
        style = intValue("style", 1, 1, 2,
                "Active visual style. 1 = Retro Damage Indicators (original 1.20.1 look). 2 = Damage Indicators 1.12.2 classic look. Only the matching style section below is used in-game.");
        style1 = new StyleSettings("style-1", true);
        style2 = new StyleSettings("style-2", false);
        load();
    }

    public boolean isClassicStyle() {
        return style.get() == 2;
    }

    /** Settings for the currently selected style. */
    public StyleSettings active() {
        return isClassicStyle() ? style2 : style1;
    }

    public void load() {
        file = CommentedFileConfig.builder(path).sync().autosave().preserveInsertionOrder().build();
        if (Files.exists(path)) {
            file.load();
        }
        writeDefaultsIfMissing();
        file.save();
        rememberModifiedTime();
    }

    public void reload() {
        if (file != null) {
            file.close();
        }
        load();
    }

    /**
     * Hot-reload when the toml changes on disk (same live-update behavior as NeoForge's config watcher).
     * Safe to call every tick; only reloads when the file's mtime changes.
     */
    public void checkForExternalChanges() {
        try {
            if (file == null || !Files.exists(path)) {
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (lastKnownModified < 0L) {
                lastKnownModified = modified;
                return;
            }
            if (modified != lastKnownModified) {
                lastKnownModified = modified;
                file.load();
            }
        } catch (Throwable ignored) {
        }
    }

    private void rememberModifiedTime() {
        try {
            if (Files.exists(path)) {
                lastKnownModified = Files.getLastModifiedTime(path).toMillis();
            }
        } catch (Throwable ignored) {
            lastKnownModified = -1L;
        }
    }

    private void writeDefaultsIfMissing() {
        style.ensureDefault();
        style1.ensureDefaults();
        style2.ensureDefaults();
    }

    private ConfigValue<Integer> intValue(String path, int def, int min, int max, String comment) {
        return new ConfigValue<>() {
            @Override
            public Integer get() {
                int v = file.getOrElse(path, def);
                return Math.max(min, Math.min(max, v));
            }

            @Override
            public void ensureDefault() {
                if (!file.contains(path)) {
                    file.set(path, def);
                    file.setComment(path, comment);
                }
            }
        };
    }

    public interface ConfigValue<T> {
        T get();

        void ensureDefault();
    }

    public final class StyleSettings {
        private final String root;
        private final boolean retroDefaults;
        private final List<ConfigValue<?>> values = new ArrayList<>();

        public final ConfigValue<Boolean> damageParticlesEnabled;
        public final ConfigValue<Double> damageParticleSize;
        public final ConfigValue<Boolean> damageParticleOutline;
        public final ConfigValue<Boolean> hudIndicatorEnabled;
        public final ConfigValue<Double> maxDistance;
        public final ConfigValue<Boolean> colorblindHealthBar;
        public final ConfigValue<Boolean> healthDecimals;
        public final ConfigValue<Boolean> healthSeperator;
        public final ConfigValue<Integer> hudLingerTime;
        public final ConfigValue<Double> hudIndicatorSize;
        public final ConfigValue<Double> hudIndicatorBackgroundOpacity;
        public final ConfigValue<Boolean> hudIndicatorAlignLeft;
        public final ConfigValue<Boolean> hudIndicatorAlignTop;
        public final ConfigValue<Integer> hudIndicatorPositionX;
        public final ConfigValue<Integer> hudIndicatorPositionY;
        public final ConfigValue<Double> hudEntitySize;
        public final ConfigValue<Boolean> hudNameTextOutline;
        public final ConfigValue<Boolean> hudHealthTextOutline;
        public final ConfigValue<Boolean> hudPotionEffects;
        public final ConfigValue<List<? extends String>> oldRenderEntities;

        private StyleSettings(String root, boolean retroDefaults) {
            this.root = root;
            this.retroDefaults = retroDefaults;

            damageParticlesEnabled = bool("damage-particles.damage_particles_enabled", true,
                    "Whether the pop-up particles when a mob is injured or healed are enabled.");
            damageParticleSize = dbl("damage-particles.damage_particle_size", 1.0D, 0.1D, 10.0D,
                    retroDefaults ? "The relative size of damage particles." : "Multiplier for classic popoff size (base size is 3.0).");
            damageParticleOutline = bool("damage-particles.damage_particle_outline", true,
                    retroDefaults
                            ? "Whether the numbers that appear as pop-up particles are outlined in a darker color."
                            : "Whether popoffs use the classic Damage Indicators layered drop-shadow style.");

            hudIndicatorEnabled = bool("hud-indicator.hud_indicator_enabled", true,
                    "Whether the hud damage indicator is enabled.");
            maxDistance = dbl("hud-indicator.max_distance", 100D, 3D, 10000D,
                    "How far away (in blocks) entities can be to appear in the hud health indicator");
            colorblindHealthBar = bool("hud-indicator.colorblind_health_bar", false,
                    "Whether health appears with a more visible yellow/black scheme.");
            healthDecimals = bool("hud-indicator.health_decimals", retroDefaults,
                    "Whether health appears with a decimal point.");
            healthSeperator = bool("hud-indicator.health_separator", retroDefaults,
                    "Whether health appears as a | (true) or / (false).");
            hudLingerTime = integer("hud-indicator.hud_linger_time", 30, 0, 1200,
                    "How long after mousing over an entity the hud damage indicator remains on screen, in game ticks.");
            hudIndicatorSize = dbl("hud-indicator.hud_indicator_size", retroDefaults ? 0.65D : 0.76D, 0.0D, 10.0D,
                    "The relative size of hud indicator.");
            hudIndicatorBackgroundOpacity = dbl("hud-indicator.hud_indicator_background_opacity", retroDefaults ? 0.75D : 1.0D, 0.0D, 1.0D,
                    "How opaque the background of the hud indicator is.");
            hudIndicatorAlignLeft = bool("hud-indicator.hud_indicator_align_left", true,
                    "True if the hud indicator appears on the left side of the screen, false for right.");
            hudIndicatorAlignTop = bool("hud-indicator.hud_indicator_align_top", true,
                    "True if the hud indicator appears on the top of the screen, false for bottom.");
            hudIndicatorPositionX = integer("hud-indicator.hud_indicator_position_x", 15, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    "How many pixels from the left/edge of the screen the hud indicator is.");
            hudIndicatorPositionY = integer("hud-indicator.hud_indicator_position_y", 15, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    "How many pixels from the top/edge of the screen the hud indicator is.");
            hudEntitySize = dbl("hud-indicator.hud_entity_size", retroDefaults ? 38.0D : 22.0D, 0.0D, 2000.0D,
                    retroDefaults
                            ? "The size in pixels a usual entity should render as in the hud indicator."
                            : "Base portrait scale factor for entities (classic Damage Indicators default was 22).");
            hudNameTextOutline = bool("hud-indicator.hud_name_text_outline", false,
                    "Whether the name of the entity in the hud indicator should be outlined.");
            hudHealthTextOutline = bool("hud-indicator.hud_health_text_outline", false,
                    "Whether the health of the entity in the hud indicator should be outlined.");
            hudPotionEffects = bool("hud-indicator.hud_potion_effects", true,
                    "Whether potion effects on the targeted entity are shown (classic style only; ignored by style 1).");
            oldRenderEntities = stringList("hud-indicator.hud_old_render_entities", List.of("alexsmobs:giant_squid"),
                    "List of entity_types to render as a model instead of with full entity context.");
        }

        void ensureDefaults() {
            if (!file.contains(root)) {
                file.setComment(root, retroDefaults
                        ? "Style 1 - Retro Damage Indicators (original 1.20.1). Used only when style = 1."
                        : "Style 2 - Damage Indicators 1.12.2 classic. Used only when style = 2.");
            }
            for (ConfigValue<?> value : values) {
                value.ensureDefault();
            }
        }

        private String key(String relative) {
            return root + "." + relative;
        }

        private ConfigValue<Boolean> bool(String relative, boolean def, String comment) {
            String path = key(relative);
            ConfigValue<Boolean> value = new ConfigValue<>() {
                @Override
                public Boolean get() {
                    return file.getOrElse(path, def);
                }

                @Override
                public void ensureDefault() {
                    if (!file.contains(path)) {
                        file.set(path, def);
                        file.setComment(path, comment);
                    }
                }
            };
            values.add(value);
            return value;
        }

        private ConfigValue<Integer> integer(String relative, int def, int min, int max, String comment) {
            String path = key(relative);
            ConfigValue<Integer> value = new ConfigValue<>() {
                @Override
                public Integer get() {
                    int v = file.getOrElse(path, def);
                    return Math.max(min, Math.min(max, v));
                }

                @Override
                public void ensureDefault() {
                    if (!file.contains(path)) {
                        file.set(path, def);
                        file.setComment(path, comment);
                    }
                }
            };
            values.add(value);
            return value;
        }

        private ConfigValue<Double> dbl(String relative, double def, double min, double max, String comment) {
            String path = key(relative);
            ConfigValue<Double> value = new ConfigValue<>() {
                @Override
                public Double get() {
                    double v = file.getOrElse(path, def);
                    return Math.max(min, Math.min(max, v));
                }

                @Override
                public void ensureDefault() {
                    if (!file.contains(path)) {
                        file.set(path, def);
                        file.setComment(path, comment);
                    }
                }
            };
            values.add(value);
            return value;
        }

        private ConfigValue<List<? extends String>> stringList(String relative, List<String> def, String comment) {
            String path = key(relative);
            ConfigValue<List<? extends String>> value = new ConfigValue<>() {
                @Override
                public List<? extends String> get() {
                    List<String> list = file.getOrElse(path, def);
                    return list != null ? list : def;
                }

                @Override
                public void ensureDefault() {
                    if (!file.contains(path)) {
                        file.set(path, new ArrayList<>(def));
                        file.setComment(path, comment);
                    }
                }
            };
            values.add(value);
            return value;
        }
    }
}
