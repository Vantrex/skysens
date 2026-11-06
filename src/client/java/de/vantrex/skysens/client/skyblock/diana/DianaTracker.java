package de.vantrex.skysens.client.skyblock.diana;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.skyblock.event.DianaDoingChangeEvent;
import de.vantrex.skysens.client.skyblock.event.DianaRitualChangeEvent;
import de.vantrex.skysens.client.skyblock.event.DianaTreasureEvent;
import de.vantrex.skysens.client.skyblock.event.GriffinBurrowDugEvent;
import de.vantrex.skysens.client.skyblock.event.LocationChangeEvent;
import de.vantrex.skysens.client.skyblock.event.MythologicalMobSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.mayor.MayorService;
import de.vantrex.skysens.client.skyblock.pet.PetTracker;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows whether the Mythological Ritual (Diana) is running, whether the player is doing it and how far the current
 * Griffin burrow chain is, and fires {@link DianaRitualChangeEvent}, {@link DianaDoingChangeEvent},
 * {@link GriffinBurrowDugEvent}, {@link MythologicalMobSpawnEvent} and {@link DianaTreasureEvent}.
 * <p>
 * Several answers can be <b>unknown</b>, e.g. right after joining before the mayor data arrived or before the first
 * burrow was dug; the read API makes that visible ({@link #getRitualActive()}, {@link #getDoingDiana()},
 * {@link #getChainProgress()}) so callers can tell UNKNOWN from FALSE. The plain boolean getters treat unknown as false.
 * <ul>
 *   <li>Ritual: {@link MayorService#isMythologicalRitualActive()}, overridden to false by the chat line "The mythological
 *       ritual isn't active!" until the next successful mayor refresh.</li>
 *   <li>Spade: a Hypixel item id of {@link #SPADES} in the hotbar (or the main hand).</li>
 *   <li>Burrow chain progress and mob/treasure drops: chat only, so unknown until the first line since joining.</li>
 * </ul>
 * Always active, independent of any feature. Chat and tick run on the client thread; the read API is safe from any thread.
 */
public final class DianaTracker implements GameMessageListeningFeature, AfterTickFeature {

    @Getter
    private static final DianaTracker instance = new DianaTracker();

    /** Hypixel item ids of the Diana spades. */
    public static final Set<String> SPADES = Set.of("ANCESTRAL_SPADE", "ARCHAIC_SPADE", "DEIFIC_SPADE");

    /** How long a rare mob counts as "spawned recently". */
    public static final long RECENT_RARE_MS = 60_000L;

    // Colourless versions of the patterns in docs/specs/skyblock-events-research.md
    private static final Pattern BURROW = Pattern.compile(
            "^You (finished the Griffin burrow chain!|dug out a Griffin Burrow!) \\((\\d+)/(\\d+)\\)$");
    private static final Pattern MOB_SPAWN = Pattern.compile(
            "^(?:Oh|Uh oh|Yikes|Oi|Good Grief|Danger|Woah)! You dug out (?:a )?(.+?)!$");
    private static final Pattern COINS = Pattern.compile("^Wow! You dug out ([\\d,.]+) coins!$");
    private static final Pattern TREASURE = Pattern.compile("^(?:RARE DROP!|Wow!) You dug out(?: a)? (.+?)!$");
    private static final String RITUAL_INACTIVE = "The mythological ritual isn't active!";

    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final int HOTBAR_SLOTS = 9;

    /** The progress of the burrow chain, e.g. 2 of 4 (or 8). */
    public record ChainProgress(int current, int max, boolean finished) {
    }

    /** Mayor data version at which the chat said the ritual isn't active; -1 if it didn't. */
    private volatile long inactiveOverrideAtVersion = -1;
    private volatile boolean spadeInHotbar;
    private volatile boolean spadeKnown;
    private volatile @Nullable ChainProgress chainProgress;
    private volatile @Nullable MythologicalMob lastMob;
    private volatile long lastMobAtMs;
    private volatile long lastRareMobAtMs;

    /** Last values an event was fired for; null while unknown. Client thread only. */
    private @Nullable Boolean lastRitual;
    private @Nullable Boolean lastDoing;
    private int ticks;

    private DianaTracker() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.reset(), SkyblockQuitEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> this.resetBurrowState(), LocationChangeEvent.class);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    // ---------------------------------------------------------------- read API

    /** Whether the ritual is known to be running or not; empty if that isn't known yet (no mayor data so far). */
    public Optional<Boolean> getRitualActive() {
        if (this.inactiveOverrideAtVersion >= 0) {
            return Optional.of(false);
        }
        final MayorService mayor = MayorService.getInstance();
        return mayor.isDataKnown() ? Optional.of(mayor.isMythologicalRitualActive()) : Optional.empty();
    }

    /** Whether the ritual is running; false if it isn't or isn't known. */
    public boolean isRitualActive() {
        return this.getRitualActive().orElse(false);
    }

    public boolean isRitualKnown() {
        return this.getRitualActive().isPresent();
    }

    /** Whether a spade is in the hotbar; empty while there is no player. */
    public Optional<Boolean> getSpadeInHotbar() {
        return this.spadeKnown ? Optional.of(this.spadeInHotbar) : Optional.empty();
    }

    public boolean hasSpadeInHotbar() {
        return this.spadeKnown && this.spadeInHotbar;
    }

    public boolean isOnHub() {
        return LocationService.getInstance().getCurrentLocation() == SkyblockLocationEnum.HUB;
    }

    /**
     * Hub, ritual active and a spade in the hotbar. Empty if that can't be told yet; it is false as soon as one part is
     * known to be false (e.g. not on the hub) even if another is unknown.
     */
    public Optional<Boolean> getDoingDiana() {
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            return Optional.of(false);
        }
        final SkyblockLocationEnum location = LocationService.getInstance().getCurrentLocation();
        if (location != null && location != SkyblockLocationEnum.HUB) {
            return Optional.of(false);
        }
        final Optional<Boolean> spade = this.getSpadeInHotbar();
        final Optional<Boolean> ritual = this.getRitualActive();
        if (spade.isPresent() && !spade.get() || ritual.isPresent() && !ritual.get()) {
            return Optional.of(false);
        }
        if (location == null || spade.isEmpty() || ritual.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(true);
    }

    /** {@link #getDoingDiana()} with unknown as false. */
    public boolean isDoingDiana() {
        return this.getDoingDiana().orElse(false);
    }

    /** Whether the Griffin pet is the active pet (as far as {@link PetTracker} knows). */
    public boolean isGriffinPetActive() {
        return PetTracker.getInstance().isActive("GRIFFIN");
    }

    /** The main hand holds a spade. Client thread. */
    public boolean isHoldingSpade() {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isSpade(player.getMainHandItem());
    }

    /** The chain progress from the last burrow line since joining; null while unknown. */
    public @Nullable ChainProgress getChainProgress() {
        return this.chainProgress;
    }

    /** The last mob a burrow spawned since joining, null if none. */
    public @Nullable MythologicalMob getLastMob() {
        return this.lastMob;
    }

    /** When {@link #getLastMob()} spawned, in epoch milliseconds; 0 if none. */
    public long getLastMobSpawnTime() {
        return this.lastMobAtMs;
    }

    /** Whether a rare mob (Minos Inquisitor, Sphinx, King Minos, Manticore) spawned within {@link #RECENT_RARE_MS}. */
    public boolean hasRareMobSpawnedRecently() {
        final long at = this.lastRareMobAtMs;
        return at > 0 && System.currentTimeMillis() - at <= RECENT_RARE_MS;
    }

    // ---------------------------------------------------------------- chat

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String line = ChatFormatting.stripFormatting(message.getString());
        if (line == null) {
            return;
        }
        final String text = line.trim();

        if (text.equals(RITUAL_INACTIVE)) {
            this.inactiveOverrideAtVersion = MayorService.getInstance().getDataVersion();
            this.checkRitual();
            return;
        }
        Matcher matcher = BURROW.matcher(text);
        if (matcher.matches()) {
            final int current = Integer.parseInt(matcher.group(2));
            final int max = Integer.parseInt(matcher.group(3));
            final boolean finished = matcher.group(1).startsWith("finished");
            this.chainProgress = new ChainProgress(current, max, finished);
            EventListenerRegistry.REGISTRY.fireEvent(new GriffinBurrowDugEvent(current, max, finished));
            return;
        }
        matcher = COINS.matcher(text);
        if (matcher.matches()) {
            EventListenerRegistry.REGISTRY.fireEvent(new DianaTreasureEvent(matcher.group(1) + " coins", parseCoins(matcher.group(1))));
            return;
        }
        matcher = MOB_SPAWN.matcher(text);
        if (matcher.matches()) {
            final MythologicalMob mob = MythologicalMob.fromName(matcher.group(1));
            if (mob != null) {
                final long now = System.currentTimeMillis();
                this.lastMob = mob;
                this.lastMobAtMs = now;
                if (mob.isRare()) {
                    this.lastRareMobAtMs = now;
                }
                EventListenerRegistry.REGISTRY.fireEvent(new MythologicalMobSpawnEvent(mob, mob.isRare()));
            }
            return;
        }
        matcher = TREASURE.matcher(text);
        if (matcher.matches()) {
            EventListenerRegistry.REGISTRY.fireEvent(new DianaTreasureEvent(matcher.group(1), null));
        }
    }

    private static @Nullable Long parseCoins(String text) {
        try {
            return Long.parseLong(text.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void afterTick() {
        if (++this.ticks % CHECK_INTERVAL_TICKS != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final MayorService mayor = MayorService.getInstance();
        mayor.refreshIfNeeded();
        if (this.inactiveOverrideAtVersion >= 0 && mayor.getDataVersion() > this.inactiveOverrideAtVersion) {
            this.inactiveOverrideAtVersion = -1;
        }
        this.readHotbar();
        this.checkRitual();
        this.checkDoing();
    }

    private void readHotbar() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.spadeKnown = false;
            return;
        }
        boolean found = false;
        for (int slot = 0; slot < HOTBAR_SLOTS && !found; slot++) {
            found = isSpade(player.getInventory().getItem(slot));
        }
        this.spadeInHotbar = found;
        this.spadeKnown = true;
    }

    private static boolean isSpade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return HypixelExtraAttributes.getExtraAttributes(stack)
                .flatMap(attributes -> HypixelExtraAttributes.getString(attributes, "id"))
                .filter(SPADES::contains)
                .isPresent();
    }

    private void checkRitual() {
        final Optional<Boolean> ritual = this.getRitualActive();
        if (ritual.isEmpty()) {
            return;
        }
        final Boolean previous = this.lastRitual;
        this.lastRitual = ritual.get();
        if (previous != null && !previous.equals(ritual.get())) {
            EventListenerRegistry.REGISTRY.fireEvent(new DianaRitualChangeEvent(ritual.get()));
        }
    }

    private void checkDoing() {
        final Optional<Boolean> doing = this.getDoingDiana();
        if (doing.isEmpty()) {
            return;
        }
        final Boolean previous = this.lastDoing;
        this.lastDoing = doing.get();
        if (previous != null && !previous.equals(doing.get())) {
            EventListenerRegistry.REGISTRY.fireEvent(new DianaDoingChangeEvent(doing.get()));
        }
    }

    // ---------------------------------------------------------------- reset

    /** A new island: the chain and spawns belong to the old one. */
    private void resetBurrowState() {
        this.chainProgress = null;
        this.lastMob = null;
        this.lastMobAtMs = 0;
        this.lastRareMobAtMs = 0;
    }

    private void reset() {
        this.resetBurrowState();
        this.spadeInHotbar = false;
        this.spadeKnown = false;
        this.lastDoing = null;
        this.lastRitual = null;
    }

}
