package de.vantrex.skysens.client.config.categories.bazaar;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.awt.*;

public class HighlightOwnBuyOrder {

    @ConfigOption(name = "Highlight own Buy Orders", desc = "Highlight your own buy orders in the Bazaar GUI")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightOwnBuyOrders = true;

    @ConfigOption(name = "Highlight Color", desc = "Color of the highlight")
    @ConfigEditorColour
    @Expose
    public ChromaColour nonFilledHighlightColor = ChromaColour.fromStaticRGB(Color.ORANGE.getRed(), Color.ORANGE.getGreen(), Color.ORANGE.getBlue(), Color.GREEN.getAlpha());

    @ConfigOption(name = "Use different color for filled items", desc = "Use a different color for filled items in the Bazaar GUI")
    @ConfigEditorBoolean
    @Expose
    public boolean useDifferentColorFilledItems = true;

    @ConfigOption(name = "Filled item color", desc = "Color of filled items in the Bazaar GUI")
    @ConfigEditorColour
    @Expose
    public ChromaColour filledItemColor = ChromaColour.fromStaticRGB(Color.GREEN.getRed(), Color.GREEN.getGreen(), Color.GREEN.getBlue(), Color.RED.getAlpha());

}
