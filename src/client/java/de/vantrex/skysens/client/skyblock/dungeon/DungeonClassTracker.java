package de.vantrex.skysens.client.skyblock.dungeon;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.features.dungeon.DungeonRunStats;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.event.DungeonClassChangeEvent;
import de.vantrex.skysens.client.skyblock.event.DungeonClassLevelUpEvent;
import de.vantrex.skysens.client.skyblock.event.DungeonPartyClassesUpdateEvent;
import de.vantrex.skysens.client.skyblock.event.ProfileChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.skyblock.item.ItemIdUtil;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.player.PlayerService;
import de.vantrex.skysens.client.skyblock.player.ProfileService;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import de.vantrex.skysens.client.util.TabListUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the player's selected dungeon class (inside and outside dungeons, saved per profile in
 * {@link DungeonClassRepository}), whether a dungeon run is going on and, during a run, the class and level of every
 * party member. Fires {@link DungeonClassChangeEvent}, {@link DungeonClassLevelUpEvent} and
 * {@link DungeonPartyClassesUpdateEvent}, only on real changes.
 * <p>
 * Sources: chat ("You selected the Mage Dungeon Class!", the party finder join line "Party Finder > Name joined the
 * dungeon group! (Mage Level 30)"), the dungeon tab list ("[MVP+] Name (Mage XXV)", "Name (DEAD)") polled once a
 * second inside a run, and the class menu (a container whose title contains "Class": the entry marked selected).
 * Always active, client thread.
 */
public final class DungeonClassTracker implements GameMessageListeningFeature, AfterTickFeature {

    private static final DungeonClassTracker INSTANCE = new DungeonClassTracker();

    private static final String CLASSES = "Healer|Mage|Berserk|Berserker|Archer|Tank";
    private static final Pattern SELECTED = Pattern.compile("^You (?:have )?selected the (" + CLASSES + ") Dungeon Class!?$");
    private static final Pattern PARTY_FINDER = Pattern.compile(
            "^Party Finder > (\\w{1,16}) joined the dungeon group! \\((" + CLASSES + ") Level (\\d+)\\)$");
    /** "[MVP+] Name (Mage XXV)", the level is roman or arabic. */
    private static final Pattern TAB_MEMBER = Pattern.compile(
            "^(?:\\[[^\\]]*\\]\\s*)*(\\w{1,16})(?:\\s+[^\\s(]+)*\\s*\\((?:(" + CLASSES + ")(?: ([IVXLC]+|\\d+))?|(DEAD))\\)$");
    private static final Pattern SIDEBAR_FLOOR = Pattern.compile("The Catacombs \\(([FM]\\d)\\)");
    private static final Pattern MENU_LEVEL = Pattern.compile("Level:? (\\d+)");
    private static final int INTERVAL_TICKS = 20;

    private volatile @Nullable DungeonClass selectedClass;
    /** 0 while unknown. */
    private volatile int selectedClassLevel;
    private volatile @NotNull List<DungeonPartyMember> partyMembers = List.of();
    private volatile boolean inRun;
    private volatile @Nullable String floor;
    private int ticks;
    private @Nullable String lastMenuSelection;

    private DungeonClassTracker() {
        EventListenerRegistry.REGISTRY.registerListener(this::onProfileChange, ProfileChangeEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> this.clearRun(), SkyblockQuitEvent.class);
    }

    public static DungeonClassTracker getInstance() {
        return INSTANCE;
    }

    /** The player's selected class, null while unknown. */
    public @Nullable DungeonClass getSelectedClass() {
        return this.selectedClass;
    }

    /** The player's level in the selected class, 0 while unknown. */
    public int getSelectedClassLevel() {
        return this.selectedClassLevel;
    }

    /** Whether a dungeon run is going on now. */
    public boolean isInDungeon() {
        return this.inRun;
    }

    /** The floor of the current run ("F7", "M7"), null outside a run or while unknown. */
    public @Nullable String getFloor() {
        return this.inRun ? this.floor : null;
    }

    /** The party of the current run including the player; empty outside a run. */
    public @NotNull List<DungeonPartyMember> getPartyMembers() {
        return this.partyMembers;
    }

    public @Nullable DungeonPartyMember getPartyMember(@NotNull String name) {
        for (final DungeonPartyMember member : this.partyMembers) {
            if (member.name().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    /** The class of a party member of the current run, null if unknown or not in the run. */
    public @Nullable DungeonClass getClassOf(@NotNull String name) {
        final DungeonPartyMember member = this.getPartyMember(name);
        return member == null ? null : member.dungeonClass();
    }

    public boolean isSelectedClass(@NotNull DungeonClass dungeonClass) {
        return this.selectedClass == dungeonClass;
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String line = ChatUtil.normalize(ChatUtil.strip(message.getString()));
        final Matcher selected = SELECTED.matcher(line);
        if (selected.matches()) {
            this.setOwnClass(DungeonClass.fromDisplayName(selected.group(1)), 0);
            return;
        }
        final Matcher finder = PARTY_FINDER.matcher(line);
        if (finder.matches() && finder.group(1).equalsIgnoreCase(PlayerService.getInstance().getCurrentPlayerName())) {
            this.setOwnClass(DungeonClass.fromDisplayName(finder.group(2)), Integer.parseInt(finder.group(3)));
        }
    }

    @Override
    public void afterTick() {
        if (++this.ticks % INTERVAL_TICKS != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        this.readClassMenu();
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CATACOMBS) {
            this.clearRun();
            return;
        }
        this.inRun = true;
        this.floor = this.readFloor();
        this.readTabList();
    }

    private @Nullable String readFloor() {
        for (final String line : ScoreboardUtil.getSidebarLines()) {
            final Matcher matcher = SIDEBAR_FLOOR.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return DungeonRunStats.getInstance().getCurrentFloor();
    }

    private void readTabList() {
        final List<DungeonPartyMember> members = new ArrayList<>();
        for (final String line : TabListUtil.lines()) {
            final Matcher matcher = TAB_MEMBER.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            final String name = matcher.group(1);
            DungeonClass dungeonClass = DungeonClass.fromDisplayName(matcher.group(2));
            int level = matcher.group(3) == null ? 0 : parseLevel(matcher.group(3));
            final boolean alive = matcher.group(4) == null;
            if (!alive) {
                // a dead member's entry has no class, keep what was known
                final DungeonPartyMember previous = this.getPartyMember(name);
                if (previous != null) {
                    dungeonClass = previous.dungeonClass();
                    level = previous.level();
                }
            }
            members.add(new DungeonPartyMember(name, dungeonClass, level, alive));
        }
        if (members.isEmpty()) {
            return;
        }
        if (!members.equals(this.partyMembers)) {
            this.partyMembers = Collections.unmodifiableList(members);
            EventListenerRegistry.REGISTRY.fireEvent(new DungeonPartyClassesUpdateEvent(this.partyMembers));
        }
        final String own = PlayerService.getInstance().getCurrentPlayerName();
        for (final DungeonPartyMember member : members) {
            if (member.name().equalsIgnoreCase(own) && member.dungeonClass() != null && member.alive()) {
                this.setOwnClass(member.dungeonClass(), member.level());
            }
        }
    }

    /** Reads the class menu while it is open: the entry whose lore says it is selected. */
    private void readClassMenu() {
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("class")) {
            this.lastMenuSelection = null;
            return;
        }
        for (final Slot slot : screen.getMenu().slots) {
            final ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            final DungeonClass dungeonClass = DungeonClass.fromDisplayName(ChatUtil.strip(stack.getHoverName().getString()));
            final ItemLore lore = stack.get(DataComponents.LORE);
            if (dungeonClass == null || lore == null) {
                continue;
            }
            boolean selected = false;
            int level = 0;
            for (final Component line : lore.lines()) {
                final String text = ChatUtil.strip(line.getString());
                selected |= text.equalsIgnoreCase("Selected!") || text.equalsIgnoreCase("SELECTED");
                final Matcher levelMatcher = MENU_LEVEL.matcher(text);
                if (levelMatcher.find()) {
                    level = Integer.parseInt(levelMatcher.group(1));
                }
            }
            if (selected && !dungeonClass.name().equals(this.lastMenuSelection)) {
                this.lastMenuSelection = dungeonClass.name();
                this.setOwnClass(dungeonClass, level);
            }
        }
    }

    private void setOwnClass(@Nullable DungeonClass dungeonClass, int level) {
        if (dungeonClass == null) {
            return;
        }
        final DungeonClass old = this.selectedClass;
        final int oldLevel = this.selectedClassLevel;
        final String profile = ProfileService.getInstance().getCurrentProfile();
        boolean save = false;
        if (old != dungeonClass) {
            this.selectedClass = dungeonClass;
            this.selectedClassLevel = Math.max(level, this.storedLevel(profile, dungeonClass));
            save = true;
            if (profile != null) {
                RepositoryRegistry.DUNGEON_CLASS_REPOSITORY.selectedClasses.put(profile, dungeonClass.name());
            }
            EventListenerRegistry.REGISTRY.fireEvent(new DungeonClassChangeEvent(old, dungeonClass));
        } else if (level > 0 && level != oldLevel) {
            this.selectedClassLevel = level;
            save = true;
            if (oldLevel > 0 && level > oldLevel) {
                EventListenerRegistry.REGISTRY.fireEvent(new DungeonClassLevelUpEvent(dungeonClass, oldLevel, level));
            }
        }
        if (save && profile != null && this.selectedClassLevel > 0) {
            RepositoryRegistry.DUNGEON_CLASS_REPOSITORY.classLevels.put(profile + ":" + dungeonClass.name(), this.selectedClassLevel);
        }
        if (save) {
            RepositoryRegistry.saveRepository(RepositoryRegistry.DUNGEON_CLASS_REPOSITORY);
        }
    }

    private int storedLevel(@Nullable String profile, DungeonClass dungeonClass) {
        if (profile == null) {
            return 0;
        }
        return RepositoryRegistry.DUNGEON_CLASS_REPOSITORY.classLevels.getOrDefault(profile + ":" + dungeonClass.name(), 0);
    }

    private void onProfileChange(ProfileChangeEvent event) {
        if (event.getNewProfile() == null) {
            return;
        }
        final DungeonClass stored = DungeonClass.fromDisplayName(
                RepositoryRegistry.DUNGEON_CLASS_REPOSITORY.selectedClasses.get(event.getNewProfile()));
        final DungeonClass old = this.selectedClass;
        this.selectedClass = stored;
        this.selectedClassLevel = stored == null ? 0 : this.storedLevel(event.getNewProfile(), stored);
        if (old != stored) {
            EventListenerRegistry.REGISTRY.fireEvent(new DungeonClassChangeEvent(old, stored));
        }
    }

    private void clearRun() {
        this.inRun = false;
        this.floor = null;
        if (!this.partyMembers.isEmpty()) {
            this.partyMembers = List.of();
            EventListenerRegistry.REGISTRY.fireEvent(new DungeonPartyClassesUpdateEvent(List.of()));
        }
    }

    private static int parseLevel(String text) {
        return Character.isDigit(text.charAt(0)) ? Integer.parseInt(text) : ItemIdUtil.romanToInt(text);
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
