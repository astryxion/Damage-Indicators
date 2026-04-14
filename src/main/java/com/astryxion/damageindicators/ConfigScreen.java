package com.astryxion.damageindicators;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Damage Indicators"));

        builder.setSavingRunnable(() -> Config.SPEC.save());

        ConfigEntryBuilder eb = builder.entryBuilder();

        // --- HUD Indicator ---
        ConfigCategory hud = builder.getOrCreateCategory(Component.literal("HUD Indicator"));

        hud.addEntry(eb.startBooleanToggle(Component.literal("HUD Enabled"), Config.INSTANCE.hudIndicatorEnabled.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether the hud damage indicator is enabled."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorEnabled.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("Size"), Config.INSTANCE.hudIndicatorSize.get())
                .setDefaultValue(0.75)
                .setMin(0.0).setMax(10.0)
                .setTooltip(Component.literal("The relative size of the hud indicator."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorSize.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("Max Distance (blocks)"), Config.INSTANCE.maxDistance.get())
                .setDefaultValue(100.0)
                .setMin(3.0).setMax(10000.0)
                .setTooltip(Component.literal("How far away entities can be to appear in the indicator."))
                .setSaveConsumer(v -> Config.INSTANCE.maxDistance.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("Background Opacity"), Config.INSTANCE.hudIndicatorBackgroundOpacity.get())
                .setDefaultValue(0.75)
                .setMin(0.0).setMax(1.0)
                .setTooltip(Component.literal("How opaque the background of the hud indicator is."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorBackgroundOpacity.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Align Left"), Config.INSTANCE.hudIndicatorAlignLeft.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("True: indicator on left side. False: right side."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorAlignLeft.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Align Top"), Config.INSTANCE.hudIndicatorAlignTop.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("True: indicator at top. False: bottom."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorAlignTop.set(v))
                .build());

        hud.addEntry(eb.startIntField(Component.literal("Position X"), Config.INSTANCE.hudIndicatorPositionX.get())
                .setDefaultValue(10)
                .setTooltip(Component.literal("Pixels offset from the aligned edge (left or right)."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorPositionX.set(v))
                .build());

        hud.addEntry(eb.startIntField(Component.literal("Position Y"), Config.INSTANCE.hudIndicatorPositionY.get())
                .setDefaultValue(10)
                .setTooltip(Component.literal("Pixels offset from the aligned edge (top or bottom)."))
                .setSaveConsumer(v -> Config.INSTANCE.hudIndicatorPositionY.set(v))
                .build());

        hud.addEntry(eb.startIntSlider(Component.literal("Linger Time (ticks)"), Config.INSTANCE.hudLingerTime.get(), 0, 200)
                .setDefaultValue(30)
                .setTooltip(Component.literal("How long the indicator stays visible after losing line of sight."))
                .setSaveConsumer(v -> Config.INSTANCE.hudLingerTime.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("Entity Size (px)"), Config.INSTANCE.hudEntitySize.get())
                .setDefaultValue(38.0)
                .setMin(0.0).setMax(2000.0)
                .setTooltip(Component.literal("The pixel size a usual entity renders as in the portrait."))
                .setSaveConsumer(v -> Config.INSTANCE.hudEntitySize.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Colorblind Health Bar"), Config.INSTANCE.colorblindHealthBar.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Yellow/black health bar scheme for colorblind players."))
                .setSaveConsumer(v -> Config.INSTANCE.colorblindHealthBar.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Show Health Decimals"), Config.INSTANCE.healthDecimals.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether health appears with a decimal point."))
                .setSaveConsumer(v -> Config.INSTANCE.healthDecimals.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Health Separator ( | )"), Config.INSTANCE.healthSeperator.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("True: use | as separator. False: use /."))
                .setSaveConsumer(v -> Config.INSTANCE.healthSeperator.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("HP Bar Animated"), Config.INSTANCE.hpBarAnimated.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Smoothly animate the health bar when the entity takes damage or heals."))
                .setSaveConsumer(v -> Config.INSTANCE.hpBarAnimated.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("HP Bar Animation Speed"), Config.INSTANCE.hpBarAnimationSpeed.get())
                .setDefaultValue(0.15)
                .setMin(0.01).setMax(1.0)
                .setTooltip(Component.literal("Per-tick lerp speed. Higher = faster animation."))
                .setSaveConsumer(v -> Config.INSTANCE.hpBarAnimationSpeed.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Damage Flash"), Config.INSTANCE.damageFlash.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Flash the indicator red when the entity takes damage."))
                .setSaveConsumer(v -> Config.INSTANCE.damageFlash.set(v))
                .build());

        hud.addEntry(eb.startIntSlider(Component.literal("Flash Duration (ticks)"), Config.INSTANCE.damageFlashDuration.get(), 2, 30)
                .setDefaultValue(8)
                .setTooltip(Component.literal("How many ticks the red flash lasts (20 ticks = 1 second)."))
                .setSaveConsumer(v -> Config.INSTANCE.damageFlashDuration.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Name Text Outline"), Config.INSTANCE.hudNameTextOutline.get())
                .setDefaultValue(false)
                .setSaveConsumer(v -> Config.INSTANCE.hudNameTextOutline.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Health Text Outline"), Config.INSTANCE.hudHealthTextOutline.get())
                .setDefaultValue(false)
                .setSaveConsumer(v -> Config.INSTANCE.hudHealthTextOutline.set(v))
                .build());

        hud.addEntry(eb.startBooleanToggle(Component.literal("Show Mod Source"), Config.INSTANCE.showModSource.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Show which mod the entity comes from on the indicator."))
                .setSaveConsumer(v -> Config.INSTANCE.showModSource.set(v))
                .build());

        hud.addEntry(eb.startDoubleField(Component.literal("Mod Source Size"), Config.INSTANCE.modSourceSize.get())
                .setDefaultValue(1.0)
                .setMin(0.1).setMax(5.0)
                .setTooltip(Component.literal("Scale multiplier for the mod source text."))
                .setSaveConsumer(v -> Config.INSTANCE.modSourceSize.set(v))
                .build());

        hud.addEntry(eb.startIntField(Component.literal("Mod Source Offset X"), Config.INSTANCE.modSourceOffsetX.get())
                .setDefaultValue(0)
                .setTooltip(Component.literal("Horizontal pixel offset from the default position."))
                .setSaveConsumer(v -> Config.INSTANCE.modSourceOffsetX.set(v))
                .build());

        hud.addEntry(eb.startIntField(Component.literal("Mod Source Offset Y"), Config.INSTANCE.modSourceOffsetY.get())
                .setDefaultValue(5)
                .setTooltip(Component.literal("Vertical pixel offset from the default position."))
                .setSaveConsumer(v -> Config.INSTANCE.modSourceOffsetY.set(v))
                .build());

        hud.addEntry(eb.startColorField(Component.literal("Mod Source Color"), Config.INSTANCE.modSourceColor.get())
                .setDefaultValue(0xAAAAAA)
                .setTooltip(Component.literal("Color of the mod source text."))
                .setSaveConsumer(v -> Config.INSTANCE.modSourceColor.set(v))
                .build());

        // --- Damage Particles ---
        ConfigCategory particles = builder.getOrCreateCategory(Component.literal("Damage Particles"));

        particles.addEntry(eb.startBooleanToggle(Component.literal("Particles Enabled"), Config.INSTANCE.damageParticlesEnabled.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether pop-up damage/heal particles are enabled."))
                .setSaveConsumer(v -> Config.INSTANCE.damageParticlesEnabled.set(v))
                .build());

        particles.addEntry(eb.startDoubleField(Component.literal("Particle Size"), Config.INSTANCE.damageParticleSize.get())
                .setDefaultValue(1.0)
                .setMin(0.1).setMax(10.0)
                .setSaveConsumer(v -> Config.INSTANCE.damageParticleSize.set(v))
                .build());

        particles.addEntry(eb.startBooleanToggle(Component.literal("Particle Outline"), Config.INSTANCE.damageParticleOutline.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether damage numbers are outlined in a darker color."))
                .setSaveConsumer(v -> Config.INSTANCE.damageParticleOutline.set(v))
                .build());

        return builder.build();
    }
}
