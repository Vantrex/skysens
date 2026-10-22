package de.vantrex.skysens.client.features.bazaar;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** The user's open bazaar orders, saved to config/skysens/repositories/BazaarOrderRepository.json. */
@NoArgsConstructor
public class BazaarOrderRepository {

    @Expose
    public List<Order> orders = new ArrayList<>();

    @NoArgsConstructor
    public static class Order {
        /** Display name as shown in chat and the order GUI, e.g. "Enchanted Diamond". */
        @Expose
        public String item;
        /** True for a buy order, false for a sell offer. */
        @Expose
        public boolean buy;
        @Expose
        public double pricePerUnit;
        @Expose
        public long amount;
        @Expose
        public long filled;
        /** Set when "[Bazaar] Your ... was filled!" arrived; such orders are only waiting to be claimed. */
        @Expose
        public boolean complete;
        /** The price that triggered the last outbid/undercut alert, 0 if none, so it only alerts again on a big change. */
        @Expose
        public double lastAlertedPrice;
        /** The price was derived from the chat message (sell offers include tax there), not read from the GUI. */
        @Expose
        public boolean estimated;

        public Order(String item, boolean buy, double pricePerUnit, long amount, long filled) {
            this.item = item;
            this.buy = buy;
            this.pricePerUnit = pricePerUnit;
            this.amount = amount;
            this.filled = filled;
        }

        public Order copy() {
            final Order copy = new Order(item, buy, pricePerUnit, amount, filled);
            copy.complete = complete;
            copy.lastAlertedPrice = lastAlertedPrice;
            copy.estimated = estimated;
            return copy;
        }
    }

}
