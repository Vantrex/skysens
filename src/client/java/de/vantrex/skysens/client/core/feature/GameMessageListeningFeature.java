package de.vantrex.skysens.client.core.feature;

import net.minecraft.network.chat.Component;

public interface GameMessageListeningFeature extends Feature {

    void onGameMessage(Component message, boolean overlay);
}
