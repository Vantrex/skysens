package de.vantrex.skysens.client.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A dropdown with a checkbox per entry, for a {@code List<SomeEnum>} field: the list holds the checked constants.
 * Drawn by {@code MultiSelectWidget}; MoulConfig only sees {@code MultiSelectEditorStub}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEditorMultiSelect {
}
