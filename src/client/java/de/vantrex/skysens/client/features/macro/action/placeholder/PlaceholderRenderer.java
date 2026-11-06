package de.vantrex.skysens.client.features.macro.action.placeholder;

import de.vantrex.skysens.client.features.macro.action.MacroSender;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fills {{ path }} and {{ path | default "text" }} in a template. Values are sanitised before insertion. */
public final class PlaceholderRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*(?:\\|\\s*default\\s*\"([^\"]*)\"\\s*)?}}");

    private PlaceholderRenderer() {
    }

    /** The paths of all placeholders in the template (also those with a default). */
    public static List<String> paths(String template) {
        final List<String> out = new java.util.ArrayList<>();
        final Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            out.add(matcher.group(1));
        }
        return out;
    }

    /** Paths without a value (and without a default) are replaced by nothing and added to unresolved. */
    public static String render(String template, MacroContext ctx, List<String> unresolved) {
        final Matcher matcher = PLACEHOLDER.matcher(template);
        final StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            final String path = matcher.group(1);
            String value = PlaceholderRegistry.resolve(path, ctx);
            if (value == null || value.isBlank()) {
                value = matcher.group(2);
            }
            if (value == null) {
                unresolved.add(path);
                value = "";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(MacroSender.sanitize(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

}
