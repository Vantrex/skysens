package de.vantrex.skysens.client.config.categories.bazaar;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class BazaarCategory {

    @ConfigOption(name = "Disable click on non self-owned listings", desc = "Disable clicks on non self-owned listings in the Bazaar GUI. Bypassable by holding STRG.")
    @ConfigEditorBoolean
    @Expose
    public boolean disableClicksOnNonSelfOwned = true;

    @Accordion
    @ConfigOption(name = "Highlight own Buy Orders", desc = "Highlight your own buy orders in the Bazaar GUI")
    @Expose
    public HighlightOwnSellOrder highlightOwnSellOrders = new HighlightOwnSellOrder();

    @ConfigOption(name = "Highlight own Sell Orders", desc = "Highlight your own sell orders in the Bazaar GUI")
    @Accordion
    @Expose
    public HighlightOwnBuyOrder highlightOwnBuyOrders = new HighlightOwnBuyOrder();


}
