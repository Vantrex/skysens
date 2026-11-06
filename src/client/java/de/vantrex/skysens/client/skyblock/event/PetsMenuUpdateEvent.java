package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.pet.Pet;
import lombok.Data;

import java.util.List;

/** Fired by {@code PetTracker} when the /pets menu showed pets that change what is known about the owned pets. */
@Data
public final class PetsMenuUpdateEvent implements SkysensEvent {

    /** All pets seen in the menu since joining this profile, a copy. */
    private final List<Pet> knownPets;

}
