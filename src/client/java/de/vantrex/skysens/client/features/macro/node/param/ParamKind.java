package de.vantrex.skysens.client.features.macro.node.param;

/** How a node parameter is edited. */
public enum ParamKind {
    TEXT, TEXT_MULTILINE, TEMPLATE_TEXT, INT, BOOL, ENUM, ENUM_MULTI, KEYBIND, ZONE, CONFIG_TOGGLE, RULE_LIST,
    /** Free text with a drop-down of suggestions (the spec choices); any value may be typed. */
    TEXT_CHOICES
}
