package de.vantrex.skysens.client.chatcommand.impl;

import de.vantrex.skysens.client.chatcommand.ChatCommand;
import de.vantrex.skysens.client.chatcommand.ChatCommandContext;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.mining.notifcations.MiningConsumableBuffsNotificationAccordion;
import de.vantrex.skysens.client.feature.mining.notification.FiletOFortuneNotificationFeature;
import de.vantrex.skysens.client.feature.mining.notification.PowderPumpkinBuffNotificationFeature;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code !buffs} sends the remaining time of the consumable buffs SkySens tracks. A buff is only tracked
 * while its notification is enabled, because that feature records when it was eaten.
 */
public class BuffsCommand implements ChatCommand {

    @Override
    public @NotNull String name() {
        return "buffs";
    }

    @Override
    public @NotNull List<String> aliases() {
        return List.of("buff");
    }

    @Override
    public @NotNull String description() {
        return "Shows my remaining consumable buffs";
    }

    @Override
    public boolean isEnabled() {
        return SkysensConfig.getInstance().chatCommandsCategory.commands.buffs;
    }

    @Override
    public void execute(@NotNull ChatCommandContext context) {
        final MiningConsumableBuffsNotificationAccordion tracked =
                SkysensConfig.getInstance().miningCategory.notifications.consumableBuffs;
        final List<String> buffs = new ArrayList<>();
        if (tracked.filetOFortune) {
            buffs.add("Filet O' Fortune " + remaining(FiletOFortuneNotificationFeature.KEY));
        }
        if (tracked.powderPumpkin) {
            buffs.add("Powder Pumpkin " + remaining(PowderPumpkinBuffNotificationFeature.KEY));
        }
        context.reply(buffs.isEmpty() ? "Buffs: not tracked" : "Buffs: " + String.join(", ", buffs));
    }

    private static String remaining(String key) {
        final Number expiresAt = RepositoryRegistry.KEY_VALUE_REPOSITORY.get(key);
        if (expiresAt == null) {
            return "inactive";
        }
        final long minutes = (expiresAt.longValue() - System.currentTimeMillis()) / 60_000;
        if (minutes < 0) {
            return "inactive";
        }
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : Math.max(1, minutes) + "m";
    }

}
