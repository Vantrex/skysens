package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.features.fishing.config.TrophyFrogAccordion;
import de.vantrex.skysens.client.util.ScreenUtil;
import de.vantrex.skysens.client.util.TimeUtil;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
    /** A color code (or reset), after which a struck cell has to turn the strikethrough on again. */
    private static final Pattern STRUCK_COLOR = Pattern.compile("§[0-9a-fr]");

    /** Lines above the table (title, hints), each drawn as one piece. */
    private List<Component> header = List.of();
    /** Table cells, the first column is left aligned and the others right aligned. */
    private List<List<Component>> rows = List.of();
    /** The tooltip of each row in {@link #rows} while hovered with the chat or an inventory open, null for the column header. */
    private List<List<Component>> tooltips = List.of();

    /** Rebuilds the lines for the profile, null while the profile is unknown. */
    void update(@Nullable TrophyFrogRepository.Profile profile, @NotNull TrophyFrogAccordion config) {
        final List<Component> header = new ArrayList<>();
        header.add(Component.literal(TITLE));
        if (profile == null) {
            header.add(Component.literal("§7Waiting for your SkyBlock profile..."));
            this.header = header;
            this.rows = List.of();
            this.tooltips = List.of();
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
        final List<List<Component>> tooltips = new ArrayList<>();
        if (!rows.isEmpty()) {
            tooltips.add(null);
        }
        final TrophyFrogService service = TrophyFrogService.getInstance();
        final Map<String, List<TrophyFrogConditions.Condition>> conditions = TrophyFrogConditions.evaluate();
        for (final Map.Entry<String, Map<TrophyFrogTier, Integer>> frog : profile.frogs.entrySet()) {
            final Map<TrophyFrogTier, Integer> counts = frog.getValue();
            if (config.hideCompleted && counts.getOrDefault(TrophyFrogTier.DIAMOND, 0) > 0) {
                continue;
            }
            final TrophyFrogTier highlighted = config.highlightNew ? service.highlightedTier(frog.getKey()) : null;
            final List<TrophyFrogConditions.Condition> frogConditions = conditions.get(frog.getKey());
            final boolean struck = config.strikeUncatchable && TrophyFrogConditions.isUncatchable(frogConditions);
            final List<Component> tooltip = tooltip(frog.getKey(), frogConditions);
            tooltip.addAll(pityLines(frog.getKey(), counts, service.catchRate(frog.getKey()), profile.menuRead));
            tooltips.add(tooltip);
            final List<Component> row = new ArrayList<>();
            row.add(cell((highlighted != null ? "§a" : struck ? "§7" : "§f") + frog.getKey(), struck));
            for (final TrophyFrogTier tier : TrophyFrogTier.values()) {
                final int amount = counts.getOrDefault(tier, 0);
                final String color = tier == highlighted ? "§a" : tier.colorCode();
                row.add(cell(amount == 0 ? "§c✖" : color + String.format("%,d", amount), struck));
            }
            if (config.showTotal) {
                row.add(cell((highlighted != null ? "§a" : "§5") + String.format("%,d", TrophyFrogService.total(counts)), struck));
            }
            rows.add(row);
        }
        // only the column header is left: every frog was hidden as completed
        if (config.hideCompleted && profile.menuRead && rows.size() == 1) {
            header.add(Component.literal("§aAll frogs caught in Diamond tier!"));
            rows.clear();
            tooltips.clear();
        }
        this.header = header;
        this.rows = rows;
        this.tooltips = tooltips;
    }

    /** The frog's requirements, each marked as met, not met or unknown (off the Lotus Atoll or not detectable). */
    private static List<Component> tooltip(String frog, @Nullable List<TrophyFrogConditions.Condition> conditions) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§e" + frog));
        if (conditions == null) {
            lines.add(Component.literal("§7No known requirements"));
            return lines;
        }
        for (final TrophyFrogConditions.Condition condition : conditions) {
            final String mark = condition.met() == null ? "§7• " : condition.met() ? "§a✔ " : "§c✖ ";
            lines.add(Component.literal(mark + "§f" + condition.description()));
        }
        return lines;
    }

    /**
     * The catches until the frog's guaranteed Gold and Diamond (see {@link TrophyFrogPity}) and how long they take at
     * its catch rate of the last {@link TrophyFrogService#RATE_WINDOW_MS}. Nothing once the frog was caught in Diamond,
     * there is no pity left then.
     */
    private static List<Component> pityLines(String frog, Map<TrophyFrogTier, Integer> counts,
                                             @Nullable TrophyFrogService.CatchRate rate, boolean menuRead) {
        if (counts.getOrDefault(TrophyFrogTier.DIAMOND, 0) > 0) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        for (final TrophyFrogTier tier : List.of(TrophyFrogTier.GOLD, TrophyFrogTier.DIAMOND)) {
            final String name = tier.colorCode() + tier.name().charAt(0) + tier.name().substring(1).toLowerCase(Locale.ROOT);
            final Integer left = TrophyFrogPity.catchesLeft(frog, tier, counts);
            if (left == null) {
                lines.add(Component.literal(name + "§7: already caught"));
                continue;
            }
            final Long time = rate == null ? null : rate.timeFor(left);
            lines.add(Component.literal(name + " pity§7 in §f" + String.format("%,d", left)
                    + (left == 1 ? "§7 catch" : "§7 catches")
                    + (time == null ? "" : " §8(§e~" + TimeUtil.formatDuration(time) + "§8)")));
        }
        if (rate == null) {
            lines.add(Component.literal("§8Measuring your catch rate..."));
        } else if (rate.catches() == 0) {
            lines.add(Component.literal("§8None caught in your last " + TimeUtil.formatDuration(rate.windowMs())
                    + " on the Lotus Atoll"));
        } else {
            lines.add(Component.literal("§8Based on " + String.format("%,d", rate.catches()) + " caught in your last "
                    + TimeUtil.formatDuration(rate.windowMs()) + " on the Lotus Atoll"));
        }
        if (!menuRead) {
            lines.add(Component.literal("§cCounts incomplete, open the Trophy Frogs menu"));
        }
        return lines;
    }

    /**
     * A table cell, struck through for a frog that can't be caught right now. A color code ends the strikethrough, so
     * it is repeated after every color code.
     */
    private static Component cell(String text, boolean struck) {
        return Component.literal(struck ? STRUCK_COLOR.matcher(text).replaceAll("$0§m") : text);
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

    /** The requirements of the frog under the mouse, the chat or an inventory has to be open to move the mouse. */
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
        if (row >= this.tooltips.size() || this.tooltips.get(row) == null) {
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
