package de.vantrex.skysens.client.enums.location.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiftZoneEnum implements ZoneEnum<RiftZoneEnum> {

    AROUND_COLOSSEUM("Around Colosseum", "ф Around Colosseum"),
    BARRIER_STREET("Barrier Street", "ф Barrier Street"),
    BARRY_CENTER("Barry Center", "ф Barry Center"),
    BARRY_HQ("Barry HQ", "ф Barry HQ"),
    BLACK_LAGOON("Black Lagoon", "ф Black Lagoon"),
    BOOK_IN_A_BOOK("Book in a Book", "ф Book in a Book"),
    BROKEN_CAGE("Broken Cage", "ф Broken Cage"),
    CAKE_HOUSE("Cake House", "ф Cake House"),
    CEREBRAL_CITADEL("Cerebral Citadel", "ф Cerebral Citadel"),
    COLOSSEUM("Colosseum", "ф Colosseum"),
    CONTINUUM("Continuum", "ф Continuum"),
    DOLPHIN_TRAINER("Dolphin Trainer", "ф Dolphin Trainer"),
    DREADFARM("Dreadfarm", "ф Dreadfarm"),
    DEJA_VU_ALLEY("Déjà Vu Alley", "ф Déjà Vu Alley"),
    EMPTY_BANK("Empty Bank", "ф Empty Bank"),
    ENIGMAS_CRIB("Enigma's Crib", "ф Enigma's Crib"),
    FAIRYLOSOPHY_TOWER("Fairylosophy Tower", "ф Fairylosophy Tower"),
    GREAT_BEANSTALK("Great Beanstalk", "ф Great Beanstalk"),
    HALF_EATEN_CAVE("Half-Eaten Cave", "ф Half-Eaten Cave"),
    INFESTED_HOUSE("Infested House", "ф Infested House"),
    LAGOON_CAVE("Lagoon Cave", "ф Lagoon Cave"),
    LAGOON_HUT("Lagoon Hut", "ф Lagoon Hut"),
    LEECHES_LAIR("Leeches Lair", "ф Leeches Lair"),
    LIVING_CAVE("Living Cave", "ф Living Cave"),
    LIVING_STILLNESS("Living Stillness", "ф Living Stillness"),
    LONELY_TERRACE("Lonely Terrace", "ф Lonely Terrace"),
    MIRRORVERSE("Mirrorverse", "ф Mirrorverse"),
    THE_MOUNTAINTOP("The Mountaintop", "ф The Mountaintop"),
    MURDER_HOUSE("Murder House", "ф Murder House"),
    OTHERSIDE("Otherside", "ф Otherside"),
    OUBLIETTE("Oubliette", "ф Oubliette"),
    PHOTON_PATHWAY("Photon Pathway", "ф Photon Pathway"),
    PUMPGROTTO("Pumpgrotto", "ф Pumpgrotto"),
    RIFT_GALLERY_ENTRANCE("Rift Gallery Entrance", "ф Rift Gallery Entrance"),
    RIFT_GALLERY("Rift Gallery", "ф Rift Gallery"),
    ROSES_END("Rose's End", "ф Rose's End"),
    SHIFTED_TAVERN("Shifted Tavern", "ф Shifted Tavern"),
    STILLGORE_CHATEAU("Stillgore Chateau", "ф Stillgore Château"),
    TAYLORS("Taylor's", "ф Taylor's"),
    THE_BASTION("The Bastion", "ф The Bastion"),
    THE_VENTS("The Vents", "ф The Vents"),
    TIME_CHAMBER("Time Chamber", "ф Time Chamber"),
    TRIAL_GROUNDS("Trial Grounds", "ф Trial Grounds"),
    VILLAGE_PLAZA("Village Plaza", "ф Village Plaza"),
    WALK_OF_FAME("Walk of Fame", "ф Walk of Fame"),
    WEST_VILLAGE("West Village", "ф West Village"),
    WIZARDMAN_BUREAU("Wizardman Bureau", "ф Wizardman Bureau"),
    WIZARD_BRAWL("Wizard Brawl", "ф Wizard Brawl"),
    WIZARD_TOWER("Wizard Tower", "ф Wizard Tower"),
    WYLD_WOODS("Wyld Woods", "ф Wyld Woods"),
    YOUR_ISLAND("Your Island", "ф \"Your\" Island"),
    ;

    private final String displayName;
    private final String scoreboardName;

    @Override
    public Class<RiftZoneEnum> getZoneClass() {
        return RiftZoneEnum.class;
    }
}

