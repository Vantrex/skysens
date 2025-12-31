package de.vantrex.skysens.client.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DungeonStatParser {
    private DungeonStatParser() {}

    // Adjust these patterns to whatever wording you see in-game on your items.
    private static final Pattern TIER_PATTERN =
            Pattern.compile("(?i)Dungeon\\s*Item\\s*Tier\\s*[:\\-]\\s*(\\w+)");
    private static final Pattern QUALITY_PATTERN =
            Pattern.compile("(?i)(?:Item\\s*)?Quality\\s*[:\\-]\\s*([A-Za-z0-9]+%?)");

    // Some dungeon items have stars in name line; this just detects and counts them.
    private static final Pattern STAR_PATTERN =
            Pattern.compile("([✪★]+)");

    public static Optional<DungeonStats> parse(List<Text> tooltipLines) {
        String tier = null;
        String quality = null;

        // Iterate all tooltip lines, stripped of color codes
        for (Text t : tooltipLines) {
            String raw = t.getString();
            String s = Formatting.strip(raw);
            if (s == null) continue;

            // Tier like: "Dungeon Item Tier: III" (or "3")
            if (tier == null) {
                Matcher m = TIER_PATTERN.matcher(s);
                if (m.find()) tier = m.group(1).trim();
            }

            // Quality like: "Quality: 73%" or "Item Quality: Fine"
            if (quality == null) {
                Matcher m = QUALITY_PATTERN.matcher(s);
                if (m.find()) quality = m.group(1).trim();
            }
        }

        // Heuristic fallback: look for stars in the first line (item name)
        // and treat star count as a "tier-ish" signal if you want.
        if (tier == null && !tooltipLines.isEmpty()) {
            String name = Formatting.strip(tooltipLines.get(0).getString());
            if (name != null) {
                Matcher m = STAR_PATTERN.matcher(name);
                if (m.find()) {
                    int stars = m.group(1).length();
                    tier = stars + "★";
                }
            }
        }

        // If you found nothing, return empty (don’t spam tooltips)
        if (tier == null && quality == null) return Optional.empty();

        return Optional.of(new DungeonStats(tier, quality));
    }

    public record DungeonStats(String tier, String quality) {}
}

