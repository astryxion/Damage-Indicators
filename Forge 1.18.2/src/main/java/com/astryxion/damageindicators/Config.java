package com.astryxion.damageindicators;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Client config with a top-level style switch:
 * <ul>
 *   <li>Style 1 - original Retro Damage Indicators (1.20.1) look</li>
 *   <li>Style 2 - Damage Indicators 1.12.2 Clean / classic look</li>
 * </ul>
 * Only the active style's section is read at runtime.
 */
public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final Config INSTANCE;

    static {
        final Pair<Config, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SPEC = clientPair.getRight();
        INSTANCE = clientPair.getLeft();
    }

    /** 1 = Retro Damage Indicators, 2 = classic Damage Indicators 1.12.2 */
    public final ForgeConfigSpec.IntValue style;
    public final StyleSettings style1;
    public final StyleSettings style2;

    public Config(final ForgeConfigSpec.Builder builder) {
        style = builder
                .comment("Active visual style. 1 = Retro Damage Indicators (original 1.20.1 look). 2 = Damage Indicators 1.12.2 classic look. Only the matching style section below is used in-game.")
                .translation("rdi_style")
                .defineInRange("style", 1, 1, 2);

        builder.comment("Style 1 - Retro Damage Indicators (original 1.20.1). Used only when style = 1.").push("style-1");
        style1 = new StyleSettings(builder, true);
        builder.pop();

        builder.comment("Style 2 - Damage Indicators 1.12.2 classic. Used only when style = 2.").push("style-2");
        style2 = new StyleSettings(builder, false);
        builder.pop();
    }

    public boolean isClassicStyle() {
        return style.get() == 2;
    }

    /** Settings for the currently selected style. */
    public StyleSettings active() {
        return isClassicStyle() ? style2 : style1;
    }

    public static final class StyleSettings {
        public final ForgeConfigSpec.BooleanValue damageParticlesEnabled;
        public final ForgeConfigSpec.DoubleValue damageParticleSize;
        public final ForgeConfigSpec.BooleanValue damageParticleOutline;
        public final ForgeConfigSpec.BooleanValue hudIndicatorEnabled;
        public final ForgeConfigSpec.DoubleValue maxDistance;
        public final ForgeConfigSpec.BooleanValue colorblindHealthBar;
        public final ForgeConfigSpec.BooleanValue healthDecimals;
        public final ForgeConfigSpec.BooleanValue healthSeperator;
        public final ForgeConfigSpec.IntValue hudLingerTime;
        public final ForgeConfigSpec.DoubleValue hudIndicatorSize;
        public final ForgeConfigSpec.DoubleValue hudIndicatorBackgroundOpacity;
        public final ForgeConfigSpec.BooleanValue hudIndicatorAlignLeft;
        public final ForgeConfigSpec.BooleanValue hudIndicatorAlignTop;
        public final ForgeConfigSpec.IntValue hudIndicatorPositionX;
        public final ForgeConfigSpec.IntValue hudIndicatorPositionY;
        public final ForgeConfigSpec.DoubleValue hudEntitySize;
        public final ForgeConfigSpec.BooleanValue hudNameTextOutline;
        public final ForgeConfigSpec.BooleanValue hudHealthTextOutline;
        public final ForgeConfigSpec.BooleanValue hudPotionEffects;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> oldRenderEntities;

        private StyleSettings(ForgeConfigSpec.Builder builder, boolean retroDefaults) {
            builder.push("damage-particles");
            damageParticlesEnabled = builder.comment("Whether the pop-up particles when a mob is injured or healed are enabled.")
                    .define("damage_particles_enabled", true);
            damageParticleSize = builder.comment(retroDefaults
                            ? "The relative size of damage particles."
                            : "Multiplier for classic popoff size (base size is 3.0).")
                    .defineInRange("damage_particle_size", 1.0D, 0.1D, 10.0D);
            damageParticleOutline = builder.comment(retroDefaults
                            ? "Whether the numbers that appear as pop-up particles are outlined in a darker color."
                            : "Whether popoffs use the classic Damage Indicators layered drop-shadow style.")
                    .define("damage_particle_outline", true);
            builder.pop();

            builder.push("hud-indicator");
            hudIndicatorEnabled = builder.comment("Whether the hud damage indicator is enabled.")
                    .define("hud_indicator_enabled", true);
            maxDistance = builder.comment("How far away (in blocks) entities can be to appear in the hud health indicator")
                    .defineInRange("max_distance", 100D, 3D, 10000D);
            colorblindHealthBar = builder.comment("Whether health appears with a more visible yellow/black scheme.")
                    .define("colorblind_health_bar", false);
            healthDecimals = builder.comment("Whether health appears with a decimal point.")
                    .define("health_decimals", retroDefaults);
            healthSeperator = builder.comment("Whether health appears as a | (true) or / (false).")
                    .define("health_separator", retroDefaults);
            hudLingerTime = builder.comment("How long after mousing over an entity the hud damage indicator remains on screen, in game ticks.")
                    .defineInRange("hud_linger_time", 30, 0, 1200);
            hudIndicatorSize = builder.comment("The relative size of hud indicator.")
                    // Style 1 panel is larger (208x78 vs ~178x64); default 0.65 matches style 2's on-screen footprint at 0.76.
                    .defineInRange("hud_indicator_size", retroDefaults ? 0.65D : 0.76D, 0.0D, 10.0D);
            hudIndicatorBackgroundOpacity = builder.comment("How opaque the background of the hud indicator is.")
                    .defineInRange("hud_indicator_background_opacity", retroDefaults ? 0.75D : 1.0D, 0.0D, 1.0D);
            hudIndicatorAlignLeft = builder.comment("True if the hud indicator appears on the left side of the screen, false for right.")
                    .define("hud_indicator_align_left", true);
            hudIndicatorAlignTop = builder.comment("True if the hud indicator appears on the top of the screen, false for bottom.")
                    .define("hud_indicator_align_top", true);
            hudIndicatorPositionX = builder.comment("How many pixels from the left/edge of the screen the hud indicator is.")
                    .defineInRange("hud_indicator_position_x", 15, Integer.MIN_VALUE, Integer.MAX_VALUE);
            hudIndicatorPositionY = builder.comment("How many pixels from the top/edge of the screen the hud indicator is.")
                    .defineInRange("hud_indicator_position_y", 15, Integer.MIN_VALUE, Integer.MAX_VALUE);
            hudEntitySize = builder.comment(retroDefaults
                            ? "The size in pixels a usual entity should render as in the hud indicator."
                            : "Base portrait scale factor for entities (classic Damage Indicators default was 22).")
                    .defineInRange("hud_entity_size", retroDefaults ? 38.0D : 22.0D, 0.0D, 2000.0D);
            hudNameTextOutline = builder.comment("Whether the name of the entity in the hud indicator should be outlined.")
                    .define("hud_name_text_outline", false);
            hudHealthTextOutline = builder.comment("Whether the health of the entity in the hud indicator should be outlined.")
                    .define("hud_health_text_outline", false);
            hudPotionEffects = builder.comment("Whether potion effects on the targeted entity are shown (classic style only; ignored by style 1).")
                    .define("hud_potion_effects", true);
            oldRenderEntities = builder.comment("List of entity_types to render as a model instead of with full entity context.")
                    .defineList("hud_old_render_entities", List.of("alexsmobs:giant_squid"), o -> o instanceof String);
            builder.pop();
        }
    }
}
