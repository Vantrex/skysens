package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.pet.Pet;
import de.vantrex.skysens.client.skyblock.pet.PetChangeSource;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Fired by {@code PetTracker} when the active pet changes; {@code newPet} is null if the pet was despawned.
 * Not fired for a pet restored after joining, nor for a level change of the same pet (see {@link PetLevelUpEvent}).
 */
@Data
public final class PetChangeEvent implements SkysensEvent {

    private final @Nullable Pet oldPet;
    private final @Nullable Pet newPet;
    private final PetChangeSource source;

}
