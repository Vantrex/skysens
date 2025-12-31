package de.vantrex.skysens.client.gui.config;

import net.minecraft.client.gui.DrawContext;

/**
 * Represents a single configuration entry (e.g., a toggle, slider, or text input).
 * This is a wrapper to be implemented by actual configuration types.
 */
public interface ConfigEntry {

    /**
     * @return The display name of this configuration entry.
     */
    String getName();

    /**
     * @return A short description/tooltip for this entry.
     */
    default String getDescription() {
        return "";
    }

    /**
     * Used for filtering in the search bar.
     * @return Keywords associated with this entry.
     */
    default String[] getSearchKeywords() {
        return new String[]{getName()};
    }

    /**
     * Renders the entry in the config list.
     * 
     * @param context The draw context.
     * @param x The x position.
     * @param y The y position.
     * @param width The width of the entry area.
     * @param height The height of the entry area.
     * @param mouseX Current mouse X.
     * @param mouseY Current mouse Y.
     */
    void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY);

    /**
     * Handles mouse click on this entry.
     * 
     * @param mouseX Mouse X position.
     * @param mouseY Mouse Y position.
     * @param button Mouse button pressed.
     * @return true if the click was handled.
     */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}
