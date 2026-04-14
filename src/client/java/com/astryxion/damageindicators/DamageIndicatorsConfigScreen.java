package com.astryxion.damageindicators;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Path;

/**
 * Vanilla screen using the 26.1 {@link GuiGraphicsExtractor} pipeline ({@link Screen#extractRenderState}).
 */
public class DamageIndicatorsConfigScreen extends Screen {

    private final Screen parent;

    public DamageIndicatorsConfigScreen(Screen parent) {
        super(Component.literal("Damage Indicators"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;

        Path path = Config.getConfigPath();

        this.addRenderableWidget(
                Button.builder(Component.literal("Reload from disk"), b -> {
                    Config.load();
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("Reloaded damageindicators.json"));
                    }
                }).bounds(cx - 100, y, 200, 20).build()
        );
        y += 26;

        this.addRenderableWidget(
                Button.builder(Component.literal("Save to disk"), b -> {
                    Config.save();
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("Saved damageindicators.json"));
                    }
                }).bounds(cx - 100, y, 200, 20).build()
        );
        y += 26;

        this.addRenderableWidget(
                Button.builder(Component.literal("Open config folder"), b -> {
                    Path dir = path.getParent();
                    if (dir != null) {
                        Util.getPlatform().openUri(dir.toUri());
                    }
                }).bounds(cx - 100, y, 200, 20).build()
        );
        y += 34;

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), b -> this.minecraft.setScreen(this.parent))
                        .bounds(cx - 100, this.height - 36, 200, 20).build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Path path = Config.getConfigPath();
        String pathStr = path.toAbsolutePath().toString();
        int cx = this.width / 2;
        graphics.text(this.font, this.title, cx - this.font.width(this.title) / 2, 40, 0xFFFFFF, false);
        graphics.text(this.font, Component.literal("Edit options in:"), cx - this.font.width("Edit options in:") / 2, 60, 0xA0A0A0, false);
        graphics.text(this.font, pathStr, 12, 76, 0xE0E0E0, false);
        graphics.text(this.font, Component.literal("Use Reload after editing the file."), cx - this.font.width("Use Reload after editing the file.") / 2, this.height / 2, 0xA0A0A0, false);
    }
}
