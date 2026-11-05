package de.vantrex.skysens.client.core.feature;

import net.minecraft.network.chat.Component;

/**
 * Gets every game message exactly as the server sent it, before Fabric's message events and so before other mods
 * (Skyblocker, SkyHanni, ...) cancel or rewrite it. For parsers that need Hypixel's original text and hovers, or
 * messages other mods hide (e.g. "Profile ID: ..."). Features that care about what is actually shown use
 * {@link GameMessageListeningFeature} instead.
 *
 * <p>Called on the client thread, unconditionally: check {@code isActive()} and {@code isOnSkyBlock()} yourself.
 * Messages SkySens sends itself never come through here, they aren't packets.
 */
public interface RawGameMessageListeningFeature extends Feature {

    void onRawGameMessage(Component message, boolean overlay);
}
