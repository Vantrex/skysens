package de.vantrex.skysens.client.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the boolean that switches its container (a category or an accordion) on or off. While it is off, the
 * sibling options are shown dimmed in the config GUI (they stay editable), and the accordion header shows
 * an On/Off badge.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigMasterToggle {
}
