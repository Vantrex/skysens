package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.engine.InputChord;
import de.vantrex.skysens.client.features.macro.engine.PressMode;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Per-press snapshot of what placeholders and conditions read. One instance per resolution, shared by every path walked
 * for it; each facts object is created on first use. The screen is the one open at the press, even if it is closed
 * again by "close inventory first".
 */
public final class MacroContext {

    private final InputChord chord;
    private final PressMode mode;
    private final ScreenFacts screen;
    private LocationFacts location;
    private PartyFacts party;
    private SlayerFacts slayer;
    private DungeonFacts dungeon;
    private ItemFacts items;
    private MayorFacts mayor;
    private TimeFacts time;
    private SidebarFacts sidebar;
    private PetFacts pet;

    public MacroContext(InputChord chord, PressMode mode, @Nullable Screen screen) {
        this.chord = chord;
        this.mode = mode;
        this.screen = new ScreenFacts(screen);
    }

    public InputChord chord() {
        return this.chord;
    }

    public PressMode mode() {
        return this.mode;
    }

    public LocationFacts location() {
        if (this.location == null) {
            this.location = new LocationFacts();
        }
        return this.location;
    }

    public PartyFacts party() {
        if (this.party == null) {
            this.party = new PartyFacts();
        }
        return this.party;
    }

    public SlayerFacts slayer() {
        if (this.slayer == null) {
            this.slayer = new SlayerFacts();
        }
        return this.slayer;
    }

    public DungeonFacts dungeon() {
        if (this.dungeon == null) {
            this.dungeon = new DungeonFacts();
        }
        return this.dungeon;
    }

    public ItemFacts items() {
        if (this.items == null) {
            this.items = new ItemFacts();
        }
        return this.items;
    }

    public MayorFacts mayor() {
        if (this.mayor == null) {
            this.mayor = new MayorFacts();
        }
        return this.mayor;
    }

    public TimeFacts time() {
        if (this.time == null) {
            this.time = new TimeFacts();
        }
        return this.time;
    }

    public SidebarFacts sidebar() {
        if (this.sidebar == null) {
            this.sidebar = new SidebarFacts();
        }
        return this.sidebar;
    }

    public PetFacts pet() {
        if (this.pet == null) {
            this.pet = new PetFacts();
        }
        return this.pet;
    }

    public ScreenFacts screen() {
        return this.screen;
    }

    public ItemStack heldItem() {
        return this.items().held();
    }

    /** The held item's SkyBlock ExtraAttributes, read once per press. */
    public Optional<CompoundTag> heldExtraAttributes() {
        return this.items().heldExtraAttributes();
    }

}
