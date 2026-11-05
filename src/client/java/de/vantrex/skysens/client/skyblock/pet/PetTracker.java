package de.vantrex.skysens.client.skyblock.pet;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.HandledScreenFeature;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.event.PetChangeEvent;
import de.vantrex.skysens.client.skyblock.event.PetLevelUpEvent;
import de.vantrex.skysens.client.skyblock.event.PetsMenuUpdateEvent;
import de.vantrex.skysens.client.skyblock.event.ProfileChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.skyblock.item.ItemIdUtil;
import de.vantrex.skysens.client.skyblock.player.ProfileService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.TabListUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows the active pet, whatever changed it, and fires {@link PetChangeEvent}, {@link PetLevelUpEvent} and
 * {@link PetsMenuUpdateEvent}. Always active, independent of any feature. Client thread; the read API is safe from
 * any thread.
 * <p>
 * Sources, each tagged with a {@link PetChangeSource}:
 * <ul>
 *   <li>chat "You summoned your X!" / "You despawned your X!" ({@code CHAT}, or {@code PETS_MENU} if a pet in the
 *       /pets menu was clicked within 5 seconds before)</li>
 *   <li>chat "Autopet equipped your [Lvl N] X! VIEW RULE" ({@code AUTOPET})</li>
 *   <li>the /pets menu: the pet whose item is marked active ({@code petInfo.active} or "Click to despawn!") ({@code PETS_MENU});
 *       every pet seen there is cached in {@link #getKnownPets()}</li>
 *   <li>chat "Your X leveled up to level N!" updates the level and fires {@link PetLevelUpEvent}</li>
 *   <li>the "Pet:" widget of the tab list, read once a second, as the source of truth ({@code TAB_LIST}, or
 *       {@code LOADOUT} within 10 seconds after "You equipped Loadout N!"). A tab reading only counts once it
 *       was the same twice in a row, and not within 6 seconds after a change from another source, because the widget
 *       lags behind; that is the debounce that makes chat and tab list fire only one event for one change.</li>
 * </ul>
 * The active pet is saved per profile in {@link ActivePetRepository} and restored silently (no event) when the
 * profile becomes known, until the tab list says otherwise.
 */
public final class PetTracker implements GameMessageListeningFeature, AfterTickFeature, HandledScreenFeature {

    @Getter
    private static final PetTracker instance = new PetTracker();

    private static final Pattern SUMMON = Pattern.compile("^You summoned your (.+?)!$");
    private static final Pattern DESPAWN = Pattern.compile("^You despawned your (.+?)!$");
    private static final Pattern AUTOPET = Pattern.compile("^Autopet equipped your \\[Lvl (\\d+)] (.+?)!(?: VIEW RULE!?)?$");
    private static final Pattern LEVEL_UP = Pattern.compile("^Your (.+?) leveled up to level (\\d+)!$");
    /** Switching a loadout only says "You equipped Loadout 7!"; the pet change itself shows up in the tab list. */
    private static final Pattern LOADOUT_CHAT = Pattern.compile("^You equipped Loadout \\d+!$");
    private static final Pattern TAB_HEADER = Pattern.compile("^Pet:\\s*(.*)$");
    private static final Pattern INLINE_PET = Pattern.compile("(\\[Lvl \\d+] .+)$");
    private static final Pattern PETS_MENU = Pattern.compile("^Pets(?: \\(\\d+/\\d+\\))?$");

    private static final long CLICK_WINDOW_MS = 5_000L;
    private static final long TAB_GRACE_MS = 6_000L;
    private static final long LOADOUT_WINDOW_MS = 10_000L;
    private static final int TAB_INTERVAL_TICKS = 20;
    private static final int MENU_INTERVAL_TICKS = 5;
    /** The player inventory slots at the end of a container menu. */
    private static final int PLAYER_SLOTS = 36;

    private volatile @Nullable Pet activePet;
    private final Map<String, Pet> knownPets = new LinkedHashMap<>();

    /** Last pets menu click: the pet and whether the click despawns it. Client thread only. */
    private @Nullable Pet clickedPet;
    private boolean clickedDespawn;
    private long clickedAtMs;
    private long lastNonTabChangeMs;
    private long loadoutHintMs;
    /** The tab reading seen last time and not applied yet; {@code NO_READING} if none. */
    private @Nullable TabReading pendingTab;
    private int ticks;

    private PetTracker() {
        EventListenerRegistry.REGISTRY.registerListener(this::onProfileChange, ProfileChangeEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> this.pendingTab = null, SkyblockQuitEvent.class);
    }

    // ---------------------------------------------------------------- read API

    /** The active pet, empty if there is none or it isn't known. */
    public @NotNull Optional<Pet> getActivePet() {
        return Optional.ofNullable(this.activePet);
    }

    public boolean hasActivePet() {
        return this.activePet != null;
    }

    /** Whether the active pet is of this type, e.g. {@code isActive("GOLDEN_DRAGON")}. */
    public boolean isActive(@NotNull String petType) {
        final Pet pet = this.activePet;
        return pet != null && pet.type().equalsIgnoreCase(petType);
    }

    /** The pets seen in the /pets menu on this profile so far (may include pets that were converted to items since). */
    public @NotNull List<Pet> getKnownPets() {
        synchronized (this.knownPets) {
            return Collections.unmodifiableList(new ArrayList<>(this.knownPets.values()));
        }
    }

    // ---------------------------------------------------------------- chat

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String line = PetInfoParser.clean(message.getString());
        final long now = System.currentTimeMillis();

        Matcher matcher = SUMMON.matcher(line);
        if (matcher.matches()) {
            this.onSummon(message, PetInfoParser.cleanName(matcher.group(1)), now);
            return;
        }
        if (DESPAWN.matcher(line).matches()) {
            final boolean fromMenu = this.recentClick(now) && this.clickedDespawn;
            this.change(null, fromMenu ? PetChangeSource.PETS_MENU : PetChangeSource.CHAT);
            return;
        }
        matcher = AUTOPET.matcher(line);
        if (matcher.matches()) {
            final String name = PetInfoParser.cleanName(matcher.group(2));
            final Pet pet = this.enrich(Pet.ofName(name, PetInfoParser.rarityOf(message, name), Integer.parseInt(matcher.group(1))));
            this.change(pet, PetChangeSource.AUTOPET);
            return;
        }
        matcher = LEVEL_UP.matcher(line);
        if (matcher.matches()) {
            this.onLevelUp(PetInfoParser.cleanName(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            return;
        }
        if (LOADOUT_CHAT.matcher(line).matches()) {
            this.loadoutHintMs = now;
        }
    }

    private void onSummon(Component message, String name, long now) {
        final String type = ItemIdUtil.toId(name);
        final Pet clicked = this.clickedPet;
        if (this.recentClick(now) && !this.clickedDespawn && clicked != null && clicked.type().equals(type)) {
            this.change(clicked, PetChangeSource.PETS_MENU);
            return;
        }
        final Pet previous = this.activePet;
        final int level = previous != null && previous.type().equals(type) ? previous.level() : 0;
        this.change(this.enrich(Pet.ofName(name, PetInfoParser.rarityOf(message, name), level)), PetChangeSource.CHAT);
    }

    private void onLevelUp(String name, int level) {
        final Pet pet = this.activePet;
        if (pet == null || !pet.type().equals(ItemIdUtil.toId(name))) {
            return;
        }
        this.change(pet.withLevel(level), PetChangeSource.CHAT);
    }

    private boolean recentClick(long now) {
        return this.clickedPet != null && now - this.clickedAtMs <= CLICK_WINDOW_MS;
    }

    // ---------------------------------------------------------------- pets menu

    @Override
    public Pattern inventoryName() {
        return PETS_MENU;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void mouseClicked$Head(MouseButtonEvent click, boolean doubled, AbstractContainerMenu menu, int screenX, int screenY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final Slot slot = this.getSlotAt(click.x(), click.y(), menu, screenX, screenY);
        if (slot == null || slot.index >= menu.slots.size() - PLAYER_SLOTS) {
            return;
        }
        final ItemStack stack = slot.getItem();
        PetInfoParser.fromStack(stack).ifPresent(pet -> {
            this.clickedPet = pet;
            this.clickedDespawn = PetInfoParser.isMarkedActive(stack);
            this.clickedAtMs = System.currentTimeMillis();
        });
    }

    private void scanMenu() {
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !this.isInventory(screen.getTitle())) {
            return;
        }
        final AbstractContainerMenu menu = screen.getMenu();
        Pet active = null;
        boolean changed = false;
        final int end = menu.slots.size() - PLAYER_SLOTS;
        for (int i = 0; i < end; i++) {
            final ItemStack stack = menu.slots.get(i).getItem();
            final Optional<Pet> pet = PetInfoParser.fromStack(stack);
            if (pet.isEmpty()) {
                continue;
            }
            if (PetInfoParser.isMarkedActive(stack)) {
                active = pet.get();
            }
            synchronized (this.knownPets) {
                changed |= !pet.get().equals(this.knownPets.put(knownKey(pet.get()), pet.get()));
            }
        }
        if (changed) {
            EventListenerRegistry.REGISTRY.fireEvent(new PetsMenuUpdateEvent(this.getKnownPets()));
        }
        // a page without an active pet says nothing: it may be on another page; a despawn comes through chat or the tab list
        if (active != null) {
            this.change(active, PetChangeSource.PETS_MENU);
        }
    }

    private static String knownKey(Pet pet) {
        return pet.uuid() != null ? pet.uuid() : pet.type() + "#" + pet.level() + "#" + pet.heldItem();
    }

    /** Adds what the menu knows (uuid, held item, skin) to a pet that came from a chat message. */
    private Pet enrich(Pet pet) {
        Pet match = null;
        synchronized (this.knownPets) {
            for (final Pet known : this.knownPets.values()) {
                if (known.type().equals(pet.type()) && (pet.level() <= 0 || known.level() == pet.level())) {
                    if (match != null) {
                        return pet; // ambiguous
                    }
                    match = known;
                }
            }
        }
        if (match == null) {
            return pet;
        }
        return new Pet(pet.type(), pet.name(), pet.rarity() != PetRarity.UNKNOWN ? pet.rarity() : match.rarity(),
                pet.level() > 0 ? pet.level() : match.level(), match.heldItem(), match.skin(), match.uuid(), match.candyUsed());
    }

    // ---------------------------------------------------------------- tab list

    @Override
    public void afterTick() {
        this.ticks++;
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        if (this.ticks % MENU_INTERVAL_TICKS == 0) {
            this.scanMenu();
        }
        if (this.ticks % TAB_INTERVAL_TICKS == 0) {
            this.readTabList();
        }
    }

    private void readTabList() {
        final TabReading reading = parseTabList(TabListUtil.components());
        if (reading == null) {
            this.pendingTab = null;
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - this.lastNonTabChangeMs < TAB_GRACE_MS) {
            this.pendingTab = null;
            return;
        }
        if (!reading.equals(this.pendingTab)) {
            this.pendingTab = reading;
            return;
        }
        final boolean loadout = now - this.loadoutHintMs <= LOADOUT_WINDOW_MS;
        this.change(reading.pet(), loadout ? PetChangeSource.LOADOUT : PetChangeSource.TAB_LIST);
    }

    /** A "Pet:" tab list reading; {@code pet} is null for "No pet selected". */
    record TabReading(@Nullable Pet pet) {
    }

    /** The pet from the tab list widget ("Pet:" followed by "[Lvl 100] Golden Dragon"), null if the widget isn't there. */
    static @Nullable TabReading parseTabList(@NotNull List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            final Matcher header = TAB_HEADER.matcher(PetInfoParser.clean(lines.get(i).getString()));
            if (!header.matches()) {
                continue;
            }
            final boolean nextLine = header.group(1).trim().isEmpty() && i + 1 < lines.size();
            final Component valueComponent = nextLine ? lines.get(i + 1) : lines.get(i);
            final String value = nextLine ? PetInfoParser.clean(valueComponent.getString()).trim() : header.group(1).trim();
            if (value.toLowerCase(Locale.ROOT).startsWith("no pet")) {
                return new TabReading(null);
            }
            final Optional<Pet> pet = PetInfoParser.fromDisplay(valueComponent);
            if (pet.isPresent()) {
                return new TabReading(pet.get());
            }
            // "Pet: [Lvl 100] X" on one line: the color is in the same component
            final Matcher inline = INLINE_PET.matcher(value);
            if (inline.find()) {
                final String text = inline.group(1);
                final Optional<Pet> plain = PetInfoParser.fromDisplay(text, PetRarity.UNKNOWN);
                return plain.map(p -> new TabReading(p.withRarity(PetInfoParser.rarityOf(valueComponent, p.name())))).orElse(null);
            }
            return null;
        }
        return null;
    }

    // ---------------------------------------------------------------- state

    private void onProfileChange(ProfileChangeEvent event) {
        this.pendingTab = null;
        this.clickedPet = null;
        synchronized (this.knownPets) {
            this.knownPets.clear();
        }
        // restored silently: it is only what we saw last, the tab list has the final word
        this.activePet = event.getNewProfile() == null ? null : RepositoryRegistry.ACTIVE_PET_REPOSITORY.get(event.getNewProfile());
        this.lastNonTabChangeMs = 0;
    }

    /**
     * Applies a pet reading. The same pet (same type and uuid, or no uuid and no lower level) only gets its details
     * merged, which fires a level-up when its level grew; anything else is a change of the active pet.
     */
    private void change(@Nullable Pet newPet, PetChangeSource source) {
        final Pet old = this.activePet;
        if (source != PetChangeSource.TAB_LIST && source != PetChangeSource.LOADOUT) {
            this.lastNonTabChangeMs = System.currentTimeMillis();
        }
        if (old == null && newPet == null) {
            return;
        }
        if (old != null && newPet != null && samePet(old, newPet)) {
            final Pet merged = merge(old, newPet, source == PetChangeSource.PETS_MENU);
            if (!merged.equals(old)) {
                this.setActive(merged);
                if (old.level() > 0 && merged.level() > old.level()) {
                    EventListenerRegistry.REGISTRY.fireEvent(new PetLevelUpEvent(merged, old.level(), merged.level()));
                }
            }
            return;
        }
        this.setActive(newPet);
        ClientUtil.sendDebug("Pet: " + (old == null ? "none" : old.displayName()) + " -> "
                + (newPet == null ? "none" : newPet.displayName()) + " (" + source + ")");
        EventListenerRegistry.REGISTRY.fireEvent(new PetChangeEvent(old, newPet, source));
    }

    private void setActive(@Nullable Pet pet) {
        this.activePet = pet;
        final String profile = ProfileService.getInstance().getCurrentProfile();
        if (profile == null) {
            // Not saved before the profile is known: it would land under no profile and never be restored.
            return;
        }
        try {
            RepositoryRegistry.ACTIVE_PET_REPOSITORY.set(profile, pet);
            RepositoryRegistry.saveRepository(RepositoryRegistry.ACTIVE_PET_REPOSITORY);
        } catch (RuntimeException e) {
            ClientUtil.sendDebug("Couldn't save the active pet: " + e.getMessage());
        }
    }

    private static boolean samePet(Pet a, Pet b) {
        if (!a.type().equals(b.type())) {
            return false;
        }
        if (a.uuid() != null && b.uuid() != null) {
            return a.uuid().equals(b.uuid());
        }
        // another pet of the same type has a lower level; the same one never loses levels
        return a.level() <= 0 || b.level() <= 0 || b.level() >= a.level();
    }

    private static Pet merge(Pet old, Pet fresh, boolean authoritative) {
        if (authoritative) {
            // an item knows everything, including that the held item is gone
            return new Pet(fresh.type(), fresh.name(), fresh.rarity() != PetRarity.UNKNOWN ? fresh.rarity() : old.rarity(),
                    fresh.level() > 0 ? fresh.level() : old.level(), fresh.heldItem(), fresh.skin(),
                    fresh.uuid() != null ? fresh.uuid() : old.uuid(), fresh.candyUsed());
        }
        return new Pet(old.type(), fresh.name(), fresh.rarity() != PetRarity.UNKNOWN ? fresh.rarity() : old.rarity(),
                fresh.level() > 0 ? fresh.level() : old.level(),
                Objects.requireNonNullElse(fresh.heldItem(), old.heldItem()),
                Objects.requireNonNullElse(fresh.skin(), old.skin()),
                Objects.requireNonNullElse(fresh.uuid(), old.uuid()), Math.max(fresh.candyUsed(), old.candyUsed()));
    }

}
