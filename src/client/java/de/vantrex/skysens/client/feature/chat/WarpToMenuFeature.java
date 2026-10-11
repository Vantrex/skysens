package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.chatcommand.warp.CoopMemberTracker;
import de.vantrex.skysens.client.chatcommand.warp.WarpToMenuScreen;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;

/** Keeps the co-op members for the {@link WarpToMenuScreen} up to date while the tab list shows them. */
@SkySensFeature
public class WarpToMenuFeature implements AfterTickFeature {

    private static final int REFRESH_TICKS = 20;

    private int ticks;

    @Override
    public void afterTick() {
        if (++this.ticks >= REFRESH_TICKS) {
            this.ticks = 0;
            CoopMemberTracker.getInstance().refresh();
        }
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.warpToMenu;
    }

}
