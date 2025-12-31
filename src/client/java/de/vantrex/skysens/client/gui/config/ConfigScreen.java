package de.vantrex.skysens.client.gui.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private final List<ConfigCategory> categories;
    private ConfigCategory selectedCategory;
    
    private TextFieldWidget searchField;
    private final List<ConfigEntry> searchResults = new ArrayList<>();
    private boolean isSearching = false;

    private int leftPaneWidth = 120;
    private int topBarHeight = 40;

    public ConfigScreen(Text title, List<ConfigCategory> categories) {
        super(title);
        this.categories = categories;
        if (!categories.isEmpty()) {
            this.selectedCategory = categories.get(0);
        }
    }

    @Override
    protected void init() {
        this.searchField = new TextFieldWidget(this.textRenderer, 10, 10, leftPaneWidth - 20, 20, Text.literal("Search..."));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addSelectableChild(this.searchField);

        // Adjust layout if needed
        leftPaneWidth = Math.max(120, this.width / 4);
        this.searchField.setWidth(leftPaneWidth - 20);
    }

    private void onSearchChanged(String query) {
        if (query == null || query.isEmpty()) {
            isSearching = false;
            searchResults.clear();
        } else {
            isSearching = true;
            searchResults.clear();
            for (ConfigCategory category : categories) {
                searchResults.addAll(category.searchEntries(query));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Draw background panels
        drawPanels(context);

        // Render Search Bar
        this.searchField.render(context, mouseX, mouseY, delta);

        // Render Categories
        renderCategories(context, mouseX, mouseY);

        // Render Entries
        renderEntries(context, mouseX, mouseY);
    }

    private void drawPanels(DrawContext context) {
        // Left pane background
        context.fill(0, 0, leftPaneWidth, this.height, 0x88000000);
        // Top bar background (optional, just for search area)
        context.fill(leftPaneWidth, 0, this.width, topBarHeight, 0xAA000000);
        // Main area background
        context.fill(leftPaneWidth, topBarHeight, this.width, this.height, 0x44000000);
    }

    private void renderCategories(DrawContext context, int mouseX, int mouseY) {
        int yOffset = 40;
        for (ConfigCategory category : categories) {
            yOffset = renderCategory(context, category, 10, yOffset, mouseX, mouseY, 0);
        }
    }

    private int renderCategory(DrawContext context, ConfigCategory category, int x, int y, int mouseX, int mouseY, int depth) {
        boolean isSelected = category == selectedCategory && !isSearching;
        int color = isSelected ? 0xFFFFFF00 : 0xFFFFFFFF;
        
        // Draw category name
        context.drawTextWithShadow(this.textRenderer, category.getName(), x + (depth * 10), y, color);
        
        int currentY = y + 12;
        for (ConfigCategory sub : category.getSubCategories()) {
            currentY = renderCategory(context, sub, x, currentY, mouseX, mouseY, depth + 1);
        }
        return currentY;
    }

    private void renderEntries(DrawContext context, int mouseX, int mouseY) {
        List<ConfigEntry> toRender = isSearching ? searchResults : (selectedCategory != null ? selectedCategory.getEntries() : List.of());
        
        int x = leftPaneWidth + 10;
        int y = topBarHeight + 10;
        int entryWidth = this.width - leftPaneWidth - 20;
        int entryHeight = 30;

        for (ConfigEntry entry : toRender) {
            context.fill(x, y, x + entryWidth, y + entryHeight, 0x22FFFFFF);
            entry.render(context, x, y, entryWidth, entryHeight, mouseX, mouseY);
            y += entryHeight + 5;
            
            if (y > this.height) break; // Basic culling
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Handle category selection
        if (mouseX < leftPaneWidth) {
            int yOffset = 40;
            if (handleCategoryClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Handle entry interaction
        if (mouseX > leftPaneWidth && mouseY > topBarHeight) {
             List<ConfigEntry> entries = isSearching ? searchResults : (selectedCategory != null ? selectedCategory.getEntries() : List.of());
             int x = leftPaneWidth + 10;
             int y = topBarHeight + 10;
             int entryWidth = this.width - leftPaneWidth - 20;
             int entryHeight = 30;

             for (ConfigEntry entry : entries) {
                 if (mouseX >= x && mouseX <= x + entryWidth && mouseY >= y && mouseY <= y + entryHeight) {
                     if (entry.mouseClicked(mouseX, mouseY, button)) {
                         return true;
                     }
                 }
                 y += entryHeight + 5;
             }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCategoryClick(double mouseX, double mouseY, int button) {
        // Simple recursive click check for categories
        // In a real implementation, you'd want to store positions or use a proper list widget
        MutableInt yOffset = new MutableInt(40);
        return checkCategoryClick(categories, mouseX, mouseY, yOffset, 0);
    }

    private boolean checkCategoryClick(List<ConfigCategory> categories, double mouseX, double mouseY, MutableInt yOffset, int depth) {
        for (ConfigCategory category : categories) {
            int y = yOffset.get();
            if (mouseY >= y && mouseY < y + 12) {
                this.selectedCategory = category;
                this.isSearching = false;
                this.searchField.setText("");
                return true;
            }
            yOffset.add(12);
            if (checkCategoryClick(category.getSubCategories(), mouseX, mouseY, yOffset, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    // Helper class for mutable integer in lambda/recursion
    private static class MutableInt {
        private int value;
        public MutableInt(int value) { this.value = value; }
        public int get() { return value; }
        public void add(int v) { this.value += v; }
    }
}
