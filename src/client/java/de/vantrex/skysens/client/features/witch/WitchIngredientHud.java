package de.vantrex.skysens.client.features.witch;

import de.vantrex.skysens.client.features.witch.config.YearOfTheWitchAccordion;
import de.vantrex.skysens.client.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The ingredient table: one row per shown ingredient with its amount in the Witch's Sack and its goal. The rows are
 * built in {@link #update} (once per tick), {@link #draw} only draws them.
 */
final class WitchIngredientHud {

    private static final String TITLE = "§5§lYear of the Witch";
    private static final int PADDING = 3;
    private static final int COLUMN_GAP = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0x80000000;

    /** Lines above the table (title, hints), each drawn as one piece. */
    private List<Component> header = List.of();
    /** Table cells, the first column is left aligned and the others right aligned. */
    private List<List<Component>> rows = List.of();
    /** The tooltip of each row in {@link #rows} while hovered with the chat or an inventory open. */
    private List<List<Component>> tooltips = List.of();

    /** Rebuilds the lines for the profile, null while the profile is unknown. */
    void update(@Nullable WitchIngredientRepository.Profile profile, @NotNull YearOfTheWitchAccordion config) {
        final List<Component> header = new ArrayList<>();
        header.add(Component.literal(TITLE));
        if (profile == null) {
            header.add(Component.literal("§7Waiting for your SkyBlock profile..."));
            this.header = header;
            this.rows = List.of();
            this.tooltips = List.of();
            return;
        }
        if (!profile.sackRead) {
            header.add(Component.literal("§cOpen your Witch's Sack"));
            header.add(Component.literal("§cto load your amounts."));
        }
        final WitchIngredientService service = WitchIngredientService.getInstance();
        final List<List<Component>> rows = new ArrayList<>();
        final List<List<Component>> tooltips = new ArrayList<>();
        // enum order (the sack's order), not the order they were picked in
        for (final WitchIngredient ingredient : WitchIngredient.values()) {
            if (!config.shownIngredients.contains(ingredient)) {
                continue;
            }
            final int amount = profile.amount(ingredient);
            final boolean highlighted = config.highlightNew && service.isHighlighted(ingredient);
            final boolean reached = amount >= ingredient.goal();
            final List<Component> row = new ArrayList<>();
            row.add(Component.literal((highlighted ? "§a" : "§f") + ingredient.displayName()));
            final String color = highlighted || config.showGoal && reached ? "§a" : amount == 0 ? "§7" : "§e";
            row.add(Component.literal(color + String.format("%,d", amount)));
            if (config.showGoal) {
                row.add(Component.literal("§8/ " + (reached ? "§a" : "§7") + String.format("%,d", ingredient.goal())));
            }
            rows.add(row);
            tooltips.add(tooltip(ingredient, amount, profile.sackRead));
        }
        if (rows.isEmpty()) {
            header.add(Component.literal("§7No ingredients selected, pick them"));
            header.add(Component.literal("§7in /skysens > Misc > Year of the Witch."));
        }
        this.header = header;
        this.rows = rows;
        this.tooltips = tooltips;
    }

    /** Where the ingredient drops and what it is used for. */
    private static List<Component> tooltip(WitchIngredient ingredient, int amount, boolean sackRead) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§a" + ingredient.displayName()));
        lines.add(Component.literal("§7Dropped by §c" + ingredient.mobType() + "§7 mobs, §a" + ingredient.dropChance()
                + "§7 chance"));
        for (final String where : ingredient.where()) {
            lines.add(Component.literal("§f" + where));
        }
        lines.add(Component.empty());
        lines.add(Component.literal("§7Needed for:"));
        for (final WitchIngredient.Use use : ingredient.uses()) {
            lines.add(Component.literal("§8• §f" + use.item() + " §8x" + use.amount()));
        }
        final int missing = ingredient.goal() - amount;
        lines.add(Component.literal(missing > 0
                ? "§7Still missing: §e" + String.format("%,d", missing)
                : "§aYou have all " + String.format("%,d", ingredient.goal()) + " you need"));
        lines.add(Component.empty());
        lines.add(Component.literal("§8Only the killing blow rolls for ingredients."));
        lines.add(Component.literal("§8Critter Safari drops go to /pickupsafaristash."));
        if (!sackRead) {
            lines.add(Component.literal("§cOpen your Witch's Sack to load the amount"));
        }
        return lines;
    }

    /** Whether {@link #update} hasn't run yet, e.g. because the feature is switched off. */
    boolean isEmpty() {
        return this.header.isEmpty() && this.rows.isEmpty();
    }

    /** Width of the background, before scaling. */
    int width() {
        final Font font = Minecraft.getInstance().font;
        final int[] columnWidths = this.columnWidths(font);
        int tableWidth = 0;
        for (final int width : columnWidths) {
            tableWidth += width;
        }
        tableWidth += Math.max(0, columnWidths.length - 1) * COLUMN_GAP;
        int width = tableWidth;
        for (final Component line : this.header) {
            width = Math.max(width, font.width(line));
        }
        return width + 2 * PADDING;
    }

    /** Height of the background, before scaling. */
    int height() {
        return (this.header.size() + this.rows.size()) * LINE_HEIGHT + 2 * PADDING - 2;
    }

    void draw(@NotNull GuiGraphicsExtractor graphics, @NotNull YearOfTheWitchAccordion config) {
        final Minecraft client = Minecraft.getInstance();
        final float scale = config.scale;
        final float x = client.getWindow().getGuiScaledWidth() * config.x / 100f;
        final float y = client.getWindow().getGuiScaledHeight() * config.y / 100f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        this.drawContent(graphics);
        graphics.pose().popMatrix();
        if (showsTooltip(client.gui.screen())) {
            this.drawTooltip(graphics, x, y, scale);
        }
    }

    /**
     * Whether the mouse is free to hover the HUD: with the chat or an inventory open. The HUD's tooltip is set before
     * the screen's and the first one wins, so not over an item in an inventory, where the item's tooltip goes first.
     */
    private static boolean showsTooltip(@Nullable Screen screen) {
        return screen instanceof ChatScreen
                || screen instanceof AbstractContainerScreen<?> && !ScreenUtil.isOverItem(screen);
    }

    /** Where the ingredient under the mouse drops, the chat or an inventory has to be open to move the mouse. */
    private void drawTooltip(@NotNull GuiGraphicsExtractor graphics, float x, float y, float scale) {
        final Minecraft client = Minecraft.getInstance();
        final double mouseX = client.mouseHandler.getScaledXPos(client.getWindow());
        final double mouseY = client.mouseHandler.getScaledYPos(client.getWindow());
        final double localX = (mouseX - x) / scale;
        final double localY = (mouseY - y) / scale - PADDING - this.header.size() * LINE_HEIGHT;
        if (localX < 0 || localX >= this.width() || localY < 0) {
            return;
        }
        final int row = (int) (localY / LINE_HEIGHT);
        if (row >= this.tooltips.size()) {
            return;
        }
        graphics.setComponentTooltipForNextFrame(client.font, this.tooltips.get(row), (int) mouseX, (int) mouseY);
    }

    /** Draws the table with its top left corner at 0, 0, unscaled. */
    void drawContent(@NotNull GuiGraphicsExtractor graphics) {
        final Font font = Minecraft.getInstance().font;
        final int[] columnWidths = this.columnWidths(font);
        graphics.fill(0, 0, this.width(), this.height(), BACKGROUND);

        int lineY = PADDING;
        for (final Component line : this.header) {
            graphics.text(font, line, PADDING, lineY, 0xFFFFFFFF, true);
            lineY += LINE_HEIGHT;
        }
        for (final List<Component> row : this.rows) {
            int cellX = PADDING;
            for (int column = 0; column < row.size(); column++) {
                final Component cell = row.get(column);
                // numbers line up on the right edge of their column
                final int offset = column == 0 ? 0 : columnWidths[column] - font.width(cell);
                graphics.text(font, cell, cellX + offset, lineY, 0xFFFFFFFF, true);
                cellX += columnWidths[column] + COLUMN_GAP;
            }
            lineY += LINE_HEIGHT;
        }
    }

    private int[] columnWidths(Font font) {
        int columns = 0;
        for (final List<Component> row : this.rows) {
            columns = Math.max(columns, row.size());
        }
        final int[] widths = new int[columns];
        for (final List<Component> row : this.rows) {
            for (int column = 0; column < row.size(); column++) {
                widths[column] = Math.max(widths[column], font.width(row.get(column)));
            }
        }
        return widths;
    }

}
