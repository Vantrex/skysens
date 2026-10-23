package de.vantrex.skysens.client.ui;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.widget.AccordionWidget;
import de.vantrex.skysens.client.ui.widget.ColourWidget;
import de.vantrex.skysens.client.ui.widget.DropdownWidget;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import de.vantrex.skysens.client.ui.widget.SliderWidget;
import de.vantrex.skysens.client.ui.widget.TextWidget;
import de.vantrex.skysens.client.ui.widget.ToggleWidget;
import de.vantrex.skysens.client.ui.widget.UnsupportedWidget;

/** Creates the widget matching an option's kind. */
public final class WidgetFactory {

    private WidgetFactory() {
    }

    public static OptionWidget create(OptionNode node) {
        return switch (node.kind()) {
            case TOGGLE -> new ToggleWidget(node);
            case SLIDER -> new SliderWidget(node);
            case DROPDOWN, ZONE_DROPDOWN -> new DropdownWidget(node);
            case COLOUR -> new ColourWidget(node);
            case TEXT -> new TextWidget(node);
            case ACCORDION -> new AccordionWidget(node, WidgetFactory::create);
            case UNSUPPORTED -> new UnsupportedWidget(node);
        };
    }
}
