package de.vantrex.skysens.client.features.slayer.rng;

import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.features.slayer.rng.drop.InfernoRngDropEnum;
import de.vantrex.skysens.client.features.slayer.rng.drop.RevenantRngDropEnum;
import de.vantrex.skysens.client.features.slayer.rng.drop.RiftstalkerRngDropEnum;
import de.vantrex.skysens.client.features.slayer.rng.drop.SvenRngDropEnum;
import de.vantrex.skysens.client.features.slayer.rng.drop.TarantulaRngDropEnum;
import de.vantrex.skysens.client.features.slayer.rng.drop.VoidgloomRngDropEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** The slayers with an RNG meter. The constant names are keys in {@link SlayerRngRepository}, don't rename them. */
@Getter
@RequiredArgsConstructor
public enum SlayerTypeEnum {

    REVENANT("Revenant Horror", "Zombie", RevenantRngDropEnum.values(), config -> config.revenantHorror.rngDrop,
            (config, drop) -> config.revenantHorror.rngDrop = (RevenantRngDropEnum) drop),
    TARANTULA("Tarantula Broodfather", "Spider", TarantulaRngDropEnum.values(), config -> config.tarantulaBroodfather.rngDrop,
            (config, drop) -> config.tarantulaBroodfather.rngDrop = (TarantulaRngDropEnum) drop),
    SVEN("Sven Packmaster", "Wolf", SvenRngDropEnum.values(), config -> config.svenPackmaster.rngDrop,
            (config, drop) -> config.svenPackmaster.rngDrop = (SvenRngDropEnum) drop),
    VOIDGLOOM("Voidgloom Seraph", "Enderman", VoidgloomRngDropEnum.values(), config -> config.voidgloomSeraph.rngDrop,
            (config, drop) -> config.voidgloomSeraph.rngDrop = (VoidgloomRngDropEnum) drop),
    INFERNO("Inferno Demonlord", "Blaze", InfernoRngDropEnum.values(), config -> config.infernoDemonlord.rngDrop,
            (config, drop) -> config.infernoDemonlord.rngDrop = (InfernoRngDropEnum) drop),
    RIFTSTALKER("Riftstalker Bloodfiend", "Vampire", RiftstalkerRngDropEnum.values(), config -> config.riftstalkerBloodfiend.rngDrop,
            (config, drop) -> config.riftstalkerBloodfiend.rngDrop = (RiftstalkerRngDropEnum) drop);

    /** The boss name, also the start of its RNG meter menu title ("Revenant Horror RNG Meter"). */
    private final String bossName;
    /** The slayer's name in the level line of the completion message ("Zombie Slayer LVL 9 - ..."). */
    private final String slayerName;
    private final SlayerRngDrop[] drops;
    private final Function<SlayerCategory, SlayerRngDrop> trackedDrop;
    private final BiConsumer<SlayerCategory, SlayerRngDrop> trackedDropSetter;

    public static @Nullable SlayerTypeEnum bySlayerName(@NotNull String slayerName) {
        for (final SlayerTypeEnum type : values()) {
            if (type.slayerName.equalsIgnoreCase(slayerName)) {
                return type;
            }
        }
        return null;
    }

    /** By a boss name with or without tier, e.g. "Revenant Horror IV". */
    public static @Nullable SlayerTypeEnum byBoss(@NotNull String boss) {
        for (final SlayerTypeEnum type : values()) {
            if (boss.startsWith(type.bossName)) {
                return type;
            }
        }
        return null;
    }

    /** The drop with this name (from the RNG meter menu) and minimum tier, or null. The tier tells same-named drops apart. */
    public @Nullable SlayerRngDrop dropByName(@NotNull String itemName, int minTier) {
        final String wanted = SlayerRngDrop.normalize(itemName);
        SlayerRngDrop byName = null;
        for (final SlayerRngDrop drop : this.drops) {
            if (!drop.isNone() && SlayerRngDrop.normalize(drop.getItemName()).equals(wanted)) {
                if (drop.getMinTier() == minTier) {
                    return drop;
                }
                byName = byName == null ? drop : byName;
            }
        }
        return byName;
    }

    /** Looks a drop up by its enum constant name, as stored in the repository. */
    public @Nullable SlayerRngDrop dropByKey(@Nullable String key) {
        for (final SlayerRngDrop drop : this.drops) {
            if (((Enum<?>) drop).name().equals(key)) {
                return drop;
            }
        }
        return null;
    }

}
