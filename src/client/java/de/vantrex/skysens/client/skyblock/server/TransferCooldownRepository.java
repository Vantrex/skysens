package de.vantrex.skysens.client.skyblock.server;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Command roots learned to be transfer-limited from Hypixel's cooldown message, saved to
 * config/skysens/repositories/TransferCooldownRepository.json. Don't rename the class or its fields, they are the
 * file name and the JSON keys.
 */
@NoArgsConstructor
public class TransferCooldownRepository {

    /** Lower case command roots, e.g. "warp" or "p warp". */
    @Expose
    public List<String> learnedCommands = new ArrayList<>();

}
