package de.vantrex.skysens.client.persistence;

import de.vantrex.skysens.client.features.bazaar.BazaarOrderRepository;
import de.vantrex.skysens.client.features.dungeon.DungeonRunRepository;
import de.vantrex.skysens.client.features.fishing.trophyfrog.TrophyFrogRepository;
import de.vantrex.skysens.client.features.macro.MacroRepository;
import de.vantrex.skysens.client.features.slayer.rng.SlayerRngRepository;
import de.vantrex.skysens.client.features.witch.WitchIngredientRepository;
import de.vantrex.skysens.client.skyblock.coop.CoopRepository;
import de.vantrex.skysens.client.skyblock.pet.ActivePetRepository;
import de.vantrex.skysens.client.skyblock.profit.ProfitRepository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.vantrex.skysens.client.features.sensitivity.SensitivityConfiguration;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@SuppressWarnings({"unchecked", "deprecation"})
public final class RepositoryRegistry {

    public static SimpleKeyValueRepository KEY_VALUE_REPOSITORY;
    public static ProfitRepository PROFIT_REPOSITORY;
    public static CoopRepository COOP_REPOSITORY;
    public static BazaarOrderRepository BAZAAR_ORDER_REPOSITORY;
    public static DungeonRunRepository DUNGEON_RUN_REPOSITORY;
    public static SlayerRngRepository SLAYER_RNG_REPOSITORY;
    public static TrophyFrogRepository TROPHY_FROG_REPOSITORY;
    public static WitchIngredientRepository WITCH_INGREDIENT_REPOSITORY;
    public static ActivePetRepository ACTIVE_PET_REPOSITORY;
    public static de.vantrex.skysens.client.skyblock.dungeon.DungeonClassRepository DUNGEON_CLASS_REPOSITORY;
    public static MacroRepository MACRO_REPOSITORY;
    public static de.vantrex.skysens.client.skyblock.server.TransferCooldownRepository TRANSFER_COOLDOWN_REPOSITORY;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RepositoryRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("skysens/repositories");

    public static void loadRepositories() {
        initBaseDir();
        KEY_VALUE_REPOSITORY = loadRepository(SimpleKeyValueRepository.class);
        PROFIT_REPOSITORY = loadRepository(ProfitRepository.class);
        COOP_REPOSITORY = loadRepository(CoopRepository.class);
        BAZAAR_ORDER_REPOSITORY = loadRepository(BazaarOrderRepository.class);
        DUNGEON_RUN_REPOSITORY = loadRepository(DungeonRunRepository.class);
        SLAYER_RNG_REPOSITORY = loadRepository(SlayerRngRepository.class);
        TROPHY_FROG_REPOSITORY = loadRepository(TrophyFrogRepository.class);
        WITCH_INGREDIENT_REPOSITORY = loadRepository(WitchIngredientRepository.class);
        ACTIVE_PET_REPOSITORY = loadRepository(ActivePetRepository.class);
        DUNGEON_CLASS_REPOSITORY = loadRepository(de.vantrex.skysens.client.skyblock.dungeon.DungeonClassRepository.class);
        MACRO_REPOSITORY = loadRepository(MacroRepository.class);
        TRANSFER_COOLDOWN_REPOSITORY = loadRepository(de.vantrex.skysens.client.skyblock.server.TransferCooldownRepository.class);
    }

    /** Re-reads MacroRepository.json from disk, discarding the in-memory state. */
    public static void reloadMacroRepository() {
        MACRO_REPOSITORY = loadRepository(MacroRepository.class);
    }

    private static <T> T loadRepository(Class<T> clazz) {
        final String fileName = clazz.getSimpleName() + ".json";
        final File file = BASE_DIR.resolve(fileName).toFile();
        if (!file.exists()) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            final T loaded = GSON.fromJson(Files.readString(file.toPath()), clazz);
            return loaded != null ? loaded : newInstance(clazz);
        } catch (JsonParseException | IllegalStateException e) {
            // A hand edit or bad import must not crash client init: keep the broken file aside and start empty
            final Path backup = BASE_DIR.resolve(fileName + ".broken-" + System.currentTimeMillis());
            try {
                Files.copy(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException copyError) {
                LOGGER.warn("Could not back up the broken repository {}", fileName, copyError);
            }
            LOGGER.warn("Repository {} is not valid JSON, starting empty. The broken file was kept as {}", fileName, backup.getFileName(), e);
            return newInstance(clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveRepositories() {
        initBaseDir();
        saveRepository(KEY_VALUE_REPOSITORY);
        saveRepository(PROFIT_REPOSITORY);
        saveRepository(COOP_REPOSITORY);
        saveRepository(BAZAAR_ORDER_REPOSITORY);
        saveRepository(DUNGEON_RUN_REPOSITORY);
        saveRepository(SLAYER_RNG_REPOSITORY);
        saveRepository(TROPHY_FROG_REPOSITORY);
        saveRepository(WITCH_INGREDIENT_REPOSITORY);
        saveRepository(ACTIVE_PET_REPOSITORY);
        saveRepository(DUNGEON_CLASS_REPOSITORY);
        saveRepository(MACRO_REPOSITORY);
        saveRepository(TRANSFER_COOLDOWN_REPOSITORY);
    }

    public static void saveRepository(Object repository) {
        initBaseDir();
        final String fileName = repository.getClass().getSimpleName() + ".json";
        final File file = BASE_DIR.resolve(fileName).toFile();
        try {
            Files.writeString(file.toPath(), GSON.toJson(repository));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initBaseDir() {
        File baseDirFile = BASE_DIR.toFile();
        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }
    }

}
