package de.vantrex.skysens.client.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.vantrex.skysens.client.config.SensitivityConfiguration;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings({"unchecked", "deprecation"})
public final class RepositoryRegistry {

    public static SimpleKeyValueRepository KEY_VALUE_REPOSITORY;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("skysens/repositories");

    public static void loadRepositories() {
        initBaseDir();
        KEY_VALUE_REPOSITORY = loadRepository(SimpleKeyValueRepository.class);
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
            return (T) GSON.fromJson(Files.readString(file.toPath()), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveRepositories() {
        initBaseDir();
        saveRepository(KEY_VALUE_REPOSITORY);
    }

    private static void saveRepository(Object repository) {
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
