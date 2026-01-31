package de.vantrex.skysens.client.feature;

import net.minecraft.text.Text;

public interface GameMessageListeningFeature extends Feature {

    void onGameMessage(Text message, boolean overlay);
}
