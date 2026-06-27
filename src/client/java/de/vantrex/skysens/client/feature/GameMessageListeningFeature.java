package de.vantrex.skysens.client.feature;

import net.minecraft.network.chat.Component;

public interface GameMessageListeningFeature extends Feature {

    void onGameMessage(Component message, boolean overlay);
}
