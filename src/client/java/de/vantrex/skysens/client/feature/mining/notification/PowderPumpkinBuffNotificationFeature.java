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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
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
        ClientUtil.sendDebug("Powder Pumpkin on right click detected");
        if (!isMiningPumpkin(item)) {
            ClientUtil.sendDebug("Item is not a Powder Pumpkin");
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
                .subtitle(Text.literal("You will be notified once the buff is about to expire!").
                        fillStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.WHITE))))
                .build());
        ClientUtil.sendDebug("Powder Pumpkin buff consumed");
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

    private Text buildTitle() {
        return Text
                .literal("Powder Pumpkin")
                .fillStyle(Style.EMPTY.withColor(Formatting.GREEN));
    }

    private void sendWarning() {
        notificationService
                .sendNotification(
                        Notification.builder()
                                .text(this.buildTitle())
                                .subtitle(Text.literal("Your Filet O' Fortune buff is about to expire!").fillStyle(
                                        Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))
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
                                .subtitle(Text.literal("Your Powder Pumpkin buff has expired!").fillStyle(
                                        Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))
                                ))
                                .build()
                );
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().miningCategory.notifications.consumableBuffs.powderPumpkin;
    }

    private boolean isMiningPumpkin(final @NotNull ItemStack item) {
        NbtComponent test = item.get(DataComponentTypes.CUSTOM_DATA);
        ClientUtil.sendDebug("NBT Component: " + test);
        HypixelExtraAttributes.getExtraAttributes(item)
                .ifPresent(nbtCompound -> {
                    ClientUtil.sendDebug("NBT Compound: " + nbtCompound.toString());
                });
        return HypixelExtraAttributes.getExtraAttributes(item)
                .flatMap(nbtCompound -> nbtCompound.getString("id"))
                .filter(id -> id.equals(ITEM_ID))
                .isPresent();
    }

}
