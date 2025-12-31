package de.vantrex.skysens.client.gui.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.text.Text;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A simple implementation of ConfigEntry for a boolean toggle.
 */
public class SimpleConfigEntry implements ConfigEntry {

    private final String name;
    private final String description;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public SimpleConfigEntry(String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTextWithShadow(client.textRenderer, name, x + 5, y + (height / 2) - 4, 0xFFFFFFFF);
        
        String valueText = getter.get() ? "ON" : "OFF";
        int valueColor = getter.get() ? 0xFF00FF00 : 0xFFFF0000;
        
        int valueWidth = client.textRenderer.getWidth(valueText);
        context.drawTextWithShadow(client.textRenderer, valueText, x + width - valueWidth - 10, y + (height / 2) - 4, valueColor);

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height && !description.isEmpty()) {
             context.drawTooltip(client.textRenderer, Text.literal(description), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        setter.accept(!getter.get());
        return true;
    }
}
