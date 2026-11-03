package de.vantrex.skysens.client.integration.skyhanni;

import de.vantrex.skysens.client.chatcommand.ChatCommandBridge;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * SkyHanni answers !ping and !tps in party chat when enabled (both off by default). It only answers players
 * of the configured trust level; we can't check SkyHanni's friend list, so we only defer for "Anyone".
 * Reads {@code SkyHanniMod.feature.misc.partyCommands} via reflection.
 */
public final class SkyHanniPartyCommandsBridge implements ChatCommandBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    private static final Map<String, String> TOGGLE_BY_LABEL = Map.of(
            "ping", "pingCommand",
            "tps", "tpsCommand"
    );

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("skyhanni");
    private boolean broken;

    @Override
    public @NotNull String name() {
        return "SkyHanni";
    }

    @Override
    public boolean handles(@NotNull ChatChannel channel, @NotNull String label, @NotNull String sender) {
        final String toggle = TOGGLE_BY_LABEL.get(label);
        if (channel != ChatChannel.PARTY || toggle == null || !this.loaded || this.broken) {
            return false;
        }
        try {
            final Object feature = Class.forName("at.hannibal2.skyhanni.SkyHanniMod").getField("feature").get(null);
            final Object config = property(property(feature, "misc"), "partyCommands");
            if (!Boolean.TRUE.equals(property(config, toggle))) {
                return false;
            }
            final var player = Minecraft.getInstance().player;
            if (player != null && player.getName().getString().equalsIgnoreCase(sender)
                    && !Boolean.TRUE.equals(property(config, "selfTriggerCommands"))) {
                return false;
            }
            final Object trustLevel = property(config, "requiredTrustLevel");
            return trustLevel instanceof Enum<?> level && level.name().equals("ANYONE");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            this.broken = true;
            LOGGER.warn("Could not read SkyHanni's party command config", e);
            return false;
        }
    }

    /** Reads a Kotlin property, either exposed as a public field (@JvmField) or through its getter. */
    private static @Nullable Object property(Object target, String name) throws ReflectiveOperationException {
        try {
            return target.getClass().getField(name).get(target);
        } catch (NoSuchFieldException e) {
            return target.getClass().getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1)).invoke(target);
        }
    }

}
