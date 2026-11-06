package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.pet.Pet;
import lombok.Data;

/** Fired by {@code PetTracker} when the active pet gains a level; {@code pet} already has the new level. */
@Data
public final class PetLevelUpEvent implements SkysensEvent {

    private final Pet pet;
    private final int oldLevel;
    private final int newLevel;

}
