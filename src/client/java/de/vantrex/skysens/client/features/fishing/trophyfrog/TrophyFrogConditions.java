package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.skyblock.location.Zone;
import de.vantrex.skysens.client.skyblock.location.zone.LotusAtollZoneEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * The requirements to catch each Trophy Frog, from the wiki, and whether they are met right now. Requirements are only
 * checked on the Lotus Atoll; elsewhere, and for requirements the client can't see (Wormholes, a Frogcoin blessing,
 * an unknown zone), their state is unknown. A frog only counts as uncatchable when a requirement clearly fails.
 */
final class TrophyFrogConditions {

    /** One requirement of a frog. {@code met} is null when it can't be told. */
    record Condition(@NotNull String description, @Nullable Boolean met) {
    }

    private static final String RED_SWEATER_ID = "RED_SWEATER";
    /** Tropical Rain and Blossoming run from XX:35 to XX:55 UTC, both let Wetlands Frogs bite. */
    private static final int WEATHER_START_MINUTE = 35;
    private static final int WEATHER_END_MINUTE = 55;

    private TrophyFrogConditions() {
    }

    /** The requirements per frog name (as in the Trophy Frogs menu). Frogs missing here have no known requirements. */
    static @NotNull Map<String, List<Condition>> evaluate() {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        final ClientLevel level = client.level;
        final LocationService location = LocationService.getInstance();
        final boolean check = player != null && level != null
                && location.getCurrentLocation() == SkyblockLocationEnum.LOTUS_ATOLL;
        final LotusAtollZoneEnum zone = check ? zone(location.getCurrentZone()) : null;

        final Boolean inCave = zone == null ? null : zone == LotusAtollZoneEnum.LOTUS_EATERS_CAVE;
        final Boolean inHighlands = zone == null ? null : zone == LotusAtollZoneEnum.LOTUS_HIGHLANDS;
        // Wormholes and the Frogcoin blessing can't be seen, but they only exist in the Lotus Eater's Cave
        final Boolean notInHighlands = Boolean.TRUE.equals(inHighlands) ? Boolean.FALSE : null;
        return Map.ofEntries(
                Map.entry("Common Frog", List.of(new Condition("Caught everywhere", check ? true : null))),
                Map.entry("Exploding Frog", List.of(new Condition("Combine Lily Pads until they explode", null))),
                Map.entry("Leap Frog", List.of(new Condition("Caught while midair",
                        check ? !player.onGround() && !player.isInWater() : null))),
                Map.entry("Wetlands Frog", List.of(new Condition("Caught during rain (XX:35 - XX:55 UTC)",
                        check ? isRaining(level) : null))),
                Map.entry("Reality Hopper", List.of(new Condition("Caught in a Wormhole (Lotus Eater's Cave)", notInHighlands))),
                Map.entry("Blessed Frog", List.of(new Condition("Active Frogcoin blessing (Lotus Eater's Cave)", notInHighlands))),
                Map.entry("Bullfrog", List.of(new Condition("Wearing a Red Sweater", check ? wearsRedSweater(player) : null))),
                Map.entry("Sea Frog", List.of(new Condition("Caught while underwater", check ? player.isUnderWater() : null))),
                Map.entry("Cave Frog", List.of(new Condition("In the Lotus Eater's Cave", inCave))),
                Map.entry("Highlands Frog", List.of(new Condition("In the Lotus Highlands", inHighlands))),
                Map.entry("Tree Frog", List.of(new Condition("Standing on leaves", check ? standsOnLeaves(player, level) : null))),
                Map.entry("Puddle Jumper", List.of(new Condition("Drops from the Puddle Jumper sea creature", null))));
    }

    /** Whether a requirement of the frog clearly fails. */
    static boolean isUncatchable(@Nullable List<Condition> conditions) {
        return conditions != null && conditions.stream().anyMatch(condition -> Boolean.FALSE.equals(condition.met()));
    }

    private static @Nullable LotusAtollZoneEnum zone(@Nullable Zone<?> zone) {
        return zone != null && zone.getZoneEnum() instanceof LotusAtollZoneEnum atollZone ? atollZone : null;
    }

    /** The rain the server sends, or the weather slot by the clock in case a weather comes without vanilla rain. */
    private static boolean isRaining(@NotNull ClientLevel level) {
        final int minute = LocalTime.now(ZoneOffset.UTC).getMinute();
        return level.isRaining() || (minute >= WEATHER_START_MINUTE && minute < WEATHER_END_MINUTE);
    }

    private static boolean wearsRedSweater(@NotNull LocalPlayer player) {
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            return false;
        }
        final boolean byId = HypixelExtraAttributes.getExtraAttributes(chest)
                .flatMap(attributes -> HypixelExtraAttributes.getString(attributes, "id"))
                .filter(RED_SWEATER_ID::equals)
                .isPresent();
        return byId || chest.getHoverName().getString().contains("Red Sweater");
    }

    /**
     * A leaf block carrying the player (also on its edge), below the feet, or a thin one (lily pad, dripleaf) at foot
     * level.
     */
    private static boolean standsOnLeaves(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        final BlockPos feet = player.blockPosition();
        return isLeaf(level.getBlockState(player.getOnPos()))
                || isLeaf(level.getBlockState(feet))
                || isLeaf(level.getBlockState(feet.below()));
    }

    private static boolean isLeaf(@NotNull BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(Blocks.LILY_PAD) || state.is(Blocks.BIG_DRIPLEAF);
    }

}
