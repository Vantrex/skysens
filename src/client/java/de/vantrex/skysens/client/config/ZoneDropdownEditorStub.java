package de.vantrex.skysens.client.config;

import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

/**
 * Placeholder editor for {@code @ConfigEditorDynamicZoneDropdown} options. MoulConfig's processor drops every
 * option that has no editor, so the custom processor must return <i>some</i> editor to keep the {@code zones}
 * option in the option tree. The stub is never rendered (the custom config screen draws the zone dropdown itself,
 * see {@code DropdownWidget}); it only carries the {@link ProcessedOption}.
 */
public final class ZoneDropdownEditorStub extends GuiOptionEditor {

    public ZoneDropdownEditorStub(ProcessedOption option) {
        super(option);
    }
}
