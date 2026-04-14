package com.astryxion.damageindicators;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final Config INSTANCE;
    public final ModConfigSpec.BooleanValue damageParticlesEnabled;
    public final ModConfigSpec.DoubleValue damageParticleSize;
    public final ModConfigSpec.BooleanValue damageParticleOutline;
    public final ModConfigSpec.BooleanValue hudIndicatorEnabled;
    public final ModConfigSpec.DoubleValue maxDistance;
    public final ModConfigSpec.BooleanValue colorblindHealthBar;
    public final ModConfigSpec.BooleanValue healthDecimals;
    public final ModConfigSpec.BooleanValue healthSeperator;
    public final ModConfigSpec.IntValue hudLingerTime;
    public final ModConfigSpec.DoubleValue hudIndicatorSize;
    public final ModConfigSpec.DoubleValue hudIndicatorBackgroundOpacity;
    public final ModConfigSpec.BooleanValue hudIndicatorAlignLeft;
    public final ModConfigSpec.BooleanValue hudIndicatorAlignTop;
    public final ModConfigSpec.IntValue hudIndicatorPositionX;
    public final ModConfigSpec.IntValue hudIndicatorPositionY;
    public final ModConfigSpec.DoubleValue hudEntitySize;
    public final ModConfigSpec.BooleanValue hudNameTextOutline;
    public final ModConfigSpec.BooleanValue hudHealthTextOutline;
    public final ModConfigSpec.BooleanValue hpBarAnimated;
    public final ModConfigSpec.DoubleValue hpBarAnimationSpeed;
    public final ModConfigSpec.BooleanValue damageFlash;
    public final ModConfigSpec.IntValue damageFlashDuration;
    public final ModConfigSpec.BooleanValue showModSource;
    public final ModConfigSpec.DoubleValue modSourceSize;
    public final ModConfigSpec.IntValue modSourceOffsetX;
    public final ModConfigSpec.IntValue modSourceOffsetY;
    public final ModConfigSpec.IntValue modSourceColor;
    public final ModConfigSpec.ConfigValue<List<? extends String>> oldRenderEntities;

    public Config(ModConfigSpec.Builder builder) {
        builder.push("damage-particles");
        this.damageParticlesEnabled = builder.comment("Whether the pop-up particles when a mob is injured or healed are enabled.").translation("damage_particles_enabled").define("damage_particles_enabled", true);
        this.damageParticleSize = builder.comment("The relative size of damage particles.").translation("damage_particle_size").defineInRange("damage_particle_size", (double)1.0F, 0.1, (double)10.0F);
        this.damageParticleOutline = builder.comment("Whether the numbers that appear as pop-up particles are outlined in a darker color.").translation("damage_particle_outline").define("damage_particle_outline", true);
        builder.pop();
        builder.push("hud-indicator");
        this.hudIndicatorEnabled = builder.comment("Whether the hud damage indicator is enabled.").translation("hud_indicator_enabled").define("hud_indicator_enabled", true);
        this.maxDistance = builder.comment("How far away (in blocks) entities can be to appear in the hud health indicator").translation("max_distance").defineInRange("max_distance", (double)100.0F, (double)3.0F, (double)10000.0F);
        this.colorblindHealthBar = builder.comment("Whether health appears with a more visible yellow/black scheme.").translation("colorblind_health_bar").define("colorblind_health_bar", false);
        this.healthDecimals = builder.comment("Whether health appears with a decimal point.").translation("health_decimals").define("health_decimals", true);
        this.healthSeperator = builder.comment("Whether health appears appears as a | (true) or / (false).").translation("health_separator").define("health_separator", true);
        this.hudLingerTime = builder.comment("How long after mousing over an entity the hud damage indicator remains on screen, in game ticks.").translation("hud_linger_time").defineInRange("hud_linger_time", 30, 0, 1200);
        this.hudIndicatorSize = builder.comment("The relative size of hud indicator.").translation("hud_indicator_size").defineInRange("hud_indicator_size", (double)0.75F, (double)0.0F, (double)10.0F);
        this.hudIndicatorBackgroundOpacity = builder.comment("How opaque the background of the hud indicator is.").translation("hud_indicator_background_opacity").defineInRange("hud_indicator_background_opacity", (double)0.75F, (double)0.0F, (double)10.0F);
        this.hudIndicatorAlignLeft = builder.comment("True if the hud indicator appears on the left side of the screen, false for right.").translation("hud_indicator_align_left").define("hud_indicator_align_left", true);
        this.hudIndicatorAlignTop = builder.comment("True if the hud indicator appears on the top of the screen, false for bottom.").translation("hud_indicator_align_top").define("hud_indicator_align_top", true);
        this.hudIndicatorPositionX = builder.comment("How many pixels from the left side of the screen the hud indicator is.").translation("hud_indicator_position_x").defineInRange("hud_indicator_position_x", 10, Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.hudIndicatorPositionY = builder.comment("How many pixels from the top of the screen the hud indicator is.").translation("hud_indicator_position_y").defineInRange("hud_indicator_position_y", 10, Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.hudEntitySize = builder.comment("The size in pixels a usual entity should render as in the hud indicator.").translation("hud_entity_size").defineInRange("hud_entity_size", (double)38.0F, (double)0.0F, (double)2000.0F);
        this.hudNameTextOutline = builder.comment("Whether the name of the entity in the hud indicator should be outlined.").translation("hud_name_text_outline").define("hud_name_text_outline", false);
        this.hudHealthTextOutline = builder.comment("Whether the health of the entity in the hud indicator should be outlined.").translation("hud_health_text_outline").define("hud_health_text_outline", false);
        this.hpBarAnimated = builder.comment("Whether the health bar smoothly animates when the entity takes damage or heals.").translation("hp_bar_animated").define("hp_bar_animated", true);
        this.hpBarAnimationSpeed = builder.comment("How fast the health bar animation is (per tick lerp factor). Higher = faster.").translation("hp_bar_animation_speed").defineInRange("hp_bar_animation_speed", 0.15, 0.01, 1.0);
        this.damageFlash = builder.comment("Whether the indicator flashes red when the entity takes damage.").translation("damage_flash").define("damage_flash", true);
        this.damageFlashDuration = builder.comment("How many ticks the damage flash lasts.").translation("damage_flash_duration").defineInRange("damage_flash_duration", 8, 2, 30);
        this.showModSource = builder.comment("Whether to show the name of the mod the entity comes from on the hud indicator.").translation("show_mod_source").define("show_mod_source", false);
        this.modSourceSize = builder.comment("Scale multiplier for the mod source text.").translation("mod_source_size").defineInRange("mod_source_size", 1.0, 0.1, 5.0);
        this.modSourceOffsetX = builder.comment("Horizontal offset of the mod source text in pixels (relative to default position).").translation("mod_source_offset_x").defineInRange("mod_source_offset_x", 0, -500, 500);
        this.modSourceOffsetY = builder.comment("Vertical offset of the mod source text in pixels (relative to default position).").translation("mod_source_offset_y").defineInRange("mod_source_offset_y", 5, -500, 500);
        this.modSourceColor = builder.comment("Color of the mod source text as an RGB integer (e.g. 0xAAAAAA = grey).").translation("mod_source_color").defineInRange("mod_source_color", 0xAAAAAA, 0x000000, 0xFFFFFF);
        this.oldRenderEntities = builder.comment("List of all entity_types to just render as a model instead of with entity context. add to this if an entity is rendering strangely.").defineList("hud_old_render_entities", List.of("alexsmobs:giant_squid"), (o) -> o instanceof String);
        builder.pop();
    }

    static {
        Pair<Config, ModConfigSpec> clientPair = (new ModConfigSpec.Builder()).configure(Config::new);
        SPEC = (ModConfigSpec)clientPair.getRight();
        INSTANCE = (Config)clientPair.getLeft();
    }
}
