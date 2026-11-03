package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.features.fishing.config.TrophyFrogAccordion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Trophy Frog table: one row per frog with its amount per tier and the total. The rows are built in
 * {@link #update} (once per tick), {@link #draw} only draws them.
 */
final class TrophyFrogHud {

    private static final String TITLE = "§e§lTrophy Frogs";
    private static final int PADDING = 3;
    private static final int COLUMN_GAP = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0x80000000;

    /** Lines above the table (title, hints), each drawn as one piece. */
    private List<Component> header = List.of();
    /** Table cells, the first column is left aligned and the others right aligned. */
    private List<List<Component>> rows = List.of();

    /** Rebuilds the lines for the profile, null while the profile is unknown. */
    void update(@Nullable TrophyFrogRepository.Profile profile, @NotNull TrophyFrogAccordion config) {
        final List<Component> header = new ArrayList<>();
        header.add(Component.literal(TITLE));
        if (profile == null) {
            header.add(Component.literal("§7Waiting for your SkyBlock profile..."));
            this.header = header;
            this.rows = List.of();
            return;
        }
        if (!profile.menuRead) {
            header.add(Component.literal("§cOpen the Trophy Frogs menu at"));
            header.add(Component.literal("§cResearcher Ribery to load your counts."));
        }
        final List<List<Component>> rows = new ArrayList<>();
        if (!profile.frogs.isEmpty()) {
            rows.add(columnHeader(config));
        }
        final TrophyFrogService service = TrophyFrogService.getInstance();
        for (final Map.Entry<String, Map<TrophyFrogTier, Integer>> frog : profile.frogs.entrySet()) {
            final Map<TrophyFrogTier, Integer> counts = frog.getValue();
            if (config.hideCompleted && counts.getOrDefault(TrophyFrogTier.DIAMOND, 0) > 0) {
                continue;
            }
            final TrophyFrogTier highlighted = config.highlightNew ? service.highlightedTier(frog.getKey()) : null;
            final List<Component> row = new ArrayList<>();
            row.add(Component.literal((highlighted != null ? "§a" : "§f") + frog.getKey()));
            for (final TrophyFrogTier tier : TrophyFrogTier.values()) {
                final int amount = counts.getOrDefault(tier, 0);
                final String color = tier == highlighted ? "§a" : tier.colorCode();
                row.add(Component.literal(amount == 0 ? "§c✖" : color + String.format("%,d", amount)));
            }
            if (config.showTotal) {
                row.add(Component.literal((highlighted != null ? "§a" : "§5") + String.format("%,d", TrophyFrogService.total(counts))));
            }
            rows.add(row);
        }
        // only the column header is left: every frog was hidden as completed
        if (config.hideCompleted && profile.menuRead && rows.size() == 1) {
            header.add(Component.literal("§aAll frogs caught in Diamond tier!"));
            rows.clear();
        }
        this.header = header;
        this.rows = rows;
    }

    private static List<Component> columnHeader(TrophyFrogAccordion config) {
        final List<Component> row = new ArrayList<>();
        row.add(Component.empty());
        for (final TrophyFrogTier tier : TrophyFrogTier.values()) {
            row.add(Component.literal(tier.colorCode() + "§l" + tier.name().charAt(0)));
        }
        if (config.showTotal) {
            row.add(Component.literal("§5§lΣ"));
        }
        return row;
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

    void draw(@NotNull GuiGraphicsExtractor graphics, @NotNull TrophyFrogAccordion config) {
        final Minecraft client = Minecraft.getInstance();
        final float scale = config.scale;
        final float x = client.getWindow().getGuiScaledWidth() * config.x / 100f;
        final float y = client.getWindow().getGuiScaledHeight() * config.y / 100f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        this.drawContent(graphics);
        graphics.pose().popMatrix();
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
