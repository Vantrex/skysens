package de.vantrex.skysens.client.config;

import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

/**
 * Placeholder editor for {@code @ConfigEditorMultiSelect} options, so MoulConfig's processor keeps them in the option
 * tree (it drops options without an editor). Never rendered: the custom config screen draws them with
 * {@code MultiSelectWidget}. See {@link ZoneDropdownEditorStub}.
 */
public final class MultiSelectEditorStub extends GuiOptionEditor {

    public MultiSelectEditorStub(ProcessedOption option) {
        super(option);
    }
}
