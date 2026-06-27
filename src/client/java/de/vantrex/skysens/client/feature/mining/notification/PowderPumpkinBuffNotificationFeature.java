package de.vantrex.skysens.client.feature.mining.notification;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.ItemRightClickFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.model.Notification;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import de.vantrex.skysens.client.repository.SimpleKeyValueRepository;
import de.vantrex.skysens.client.service.NotificationService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.HypixelExtraAttributes;
import de.vantrex.skysens.client.util.Lazy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

/**
 {
 id: "MINING_PUMPKIN"
 }
 */
@SkySensFeature
public class PowderPumpkinBuffNotificationFeature implements ItemRightClickFeature, AfterTickFeature {

    private static final String KEY = "mining_pumpkin_notification";
    private static final String ITEM_ID = "MINING_PUMPKIN";
    private static final long _1HOUR = 3600000L;
    private static final long _2MINUTES = 120000L;

    private final Lazy<SimpleKeyValueRepository> keyValueStore = new Lazy<>(() -> RepositoryRegistry.KEY_VALUE_REPOSITORY);
    private final NotificationService notificationService = NotificationService.getInstance();
    private boolean warningReceived = false;

    @Override
    public void onItemRightClick(@NotNull ItemStack item) {
        if (!isMiningPumpkin(item)) {
            return;
        }
        final Number current = keyValueStore.get().get(KEY);
        if (current == null) {
            keyValueStore.get().put(KEY, System.currentTimeMillis() + _1HOUR);
        } else {
            long currentValue = current.longValue();
            keyValueStore.get().put(KEY, currentValue + _1HOUR); // add 1 hour
        }

        this.warningReceived = false;
        notificationService.sendNotification(Notification.builder()
                .text(this.buildTitle())
                .subtitle(Component.literal("You will be notified once the buff is about to expire!").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE))))
                .build());
    }

    @Override
    public void afterTick() {
        final var store = keyValueStore.get();
        final Number unparsedExpiresAt = store.get(KEY);
        if (unparsedExpiresAt == null) {
            return;
        }
        final long expiresAt = unparsedExpiresAt.longValue();
        if (!warningReceived && System.currentTimeMillis() - _2MINUTES <= expiresAt) {
            this.sendWarning();
            return;
        }
        if (System.currentTimeMillis() <= expiresAt) {
            this.sendExpired();
            store.remove(KEY);
        }
    }

    private Component buildTitle() {
        return Component
                .literal("Powder Pumpkin")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
    }

    private void sendWarning() {
        notificationService
                .sendNotification(
                        Notification.builder()
                                .text(this.buildTitle())
                                .subtitle(Component.literal("Your Filet O' Fortune buff is about to expire!").withStyle(
                                        Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW))
                                ))
                                .build()
                );
        this.warningReceived = true;
    }

    private void sendExpired() {
        notificationService
                .sendNotification(
                        Notification.builder()
                                .text(this.buildTitle())
                                .subtitle(Component.literal("Your Powder Pumpkin buff has expired!").withStyle(
                                        Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))
                                ))
                                .build()
                );
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().miningCategory.notifications.consumableBuffs.powderPumpkin;
    }

    private boolean isMiningPumpkin(final @NotNull ItemStack item) {
        CustomData test = item.get(DataComponents.CUSTOM_DATA);
        return HypixelExtraAttributes.getExtraAttributes(item)
                .flatMap(nbtCompound -> nbtCompound.getString("id"))
                .filter(id -> id.equals(ITEM_ID))
                .isPresent();
    }

}
