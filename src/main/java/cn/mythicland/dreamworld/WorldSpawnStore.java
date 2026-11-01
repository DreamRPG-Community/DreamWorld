package cn.mythicland.dreamworld;

import cn.mythicland.framework.bukkit.content.ContentUpdate;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.framework.bukkit.content.ContentWorkspace;
import cn.mythicland.framework.path.SafePathResolver;
import cn.mythicland.framework.bukkit.storage.AtomicYamlStore;
import cn.mythicland.dreamworld.api.WorldSpawn;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads and atomically persists DreamWorld's custom per-world spawn points.
 */
@InjectComponent
public final class WorldSpawnStore {

    private static final String RESOURCE_NAME = "spawns.yml";
    private final JavaPlugin plugin;
    private final SafePathResolver nameResolver;
    private final Path target;
    private volatile Map<String, WorldSpawn> spawns = Map.of();

    /**
     * Creates the spawn store below the DreamWorld data directory.
     *
     * @param plugin owning plugin
     */
    public WorldSpawnStore(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        this.nameResolver = new SafePathResolver(dataDirectory);
        this.target = cn.mythicland.dreamlib.api.DreamLibApi.contentFor(plugin).resolve("DreamWorld/" + RESOURCE_NAME, RESOURCE_NAME);
    }

    /**
     * Reloads spawn points from disk.
     */
    public synchronized void load() {
        ensureTarget();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(target.toFile());
        ConfigurationSection section = configuration.getConfigurationSection("spawns");
        if (section == null) {
            spawns = Map.of();
            return;
        }

        Map<String, WorldSpawn> loaded = new LinkedHashMap<>();
        boolean normalized = false;
        for (String rawName : section.getKeys(false)) {
            String name = nameResolver.normalizeSingleSegment(rawName);
            String path = "spawns." + rawName;
            WorldSpawn spawn = new WorldSpawn(
                    name,
                    requiredDouble(configuration, path + ".x"),
                    requiredDouble(configuration, path + ".y"),
                    requiredDouble(configuration, path + ".z"),
                    requiredFloat(configuration, path + ".yaw"),
                    requiredFloat(configuration, path + ".pitch")
            );
            loaded.put(name, spawn);
            Map<String, Number> coordinates = Map.of(
                    "x", spawn.x(), "y", spawn.y(), "z", spawn.z(),
                    "yaw", spawn.yaw(), "pitch", spawn.pitch()
            );
            for (var coordinate : coordinates.entrySet()) {
                String field = path + "." + coordinate.getKey();
                if (configuration.getDouble(field) != coordinate.getValue().doubleValue()) {
                    configuration.set(field, coordinate.getValue());
                    normalized = true;
                }
            }
        }
        if (normalized) {
            try {
                AtomicYamlStore.write(target, configuration);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not persist normalized DreamWorld spawns: " + target, exception);
            }
        }
        spawns = Map.copyOf(loaded);
    }

    /** @return a validated spawn update without rewriting coordinates or system settings */
    public synchronized ContentUpdate prepareContentReload() {
        var yaml = ContentWorkspace.readYaml(target);
        var section = yaml.getConfigurationSection("spawns");
        if (section == null) throw new IllegalArgumentException("Missing spawns section in " + target);
        Map<String, WorldSpawn> next = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            String path = "spawns." + id;
            next.put(id, new WorldSpawn(nameResolver.normalizeSingleSegment(id),
                    requiredDouble(yaml, path + ".x"), requiredDouble(yaml, path + ".y"), requiredDouble(yaml, path + ".z"),
                    requiredFloat(yaml, path + ".yaw"), requiredFloat(yaml, path + ".pitch")));
        }
        var previous = spawns;
        return new ContentUpdate(() -> spawns = Map.copyOf(next), () -> spawns = previous);
    }

    /**
     * Finds a configured spawn point.
     *
     * @param logicalName logical world name
     * @return configured spawn point
     */
    public Optional<WorldSpawn> find(String logicalName) {
        if (logicalName == null) return Optional.empty();
        return Optional.ofNullable(spawns.get(logicalName));
    }

    /**
     * Returns an immutable candidate map containing one updated spawn.
     *
     * @param spawn replacement spawn
     * @return candidate map
     */
    public synchronized Map<String, WorldSpawn> with(WorldSpawn spawn) {
        WorldSpawn value = Objects.requireNonNull(spawn, "spawn");
        Map<String, WorldSpawn> candidate = new LinkedHashMap<>(spawns);
        candidate.put(value.worldName(), value);
        return Map.copyOf(candidate);
    }

    /**
     * Replaces the in-memory snapshot after its candidate file has been written successfully.
     *
     * @param replacement immutable replacement map
     */
    public synchronized void replace(Map<String, WorldSpawn> replacement) {
        spawns = immutableCopy(replacement);
    }

    /**
     * Saves a complete spawn snapshot atomically.
     *
     * @param values spawn map to persist
     */
    public synchronized void save(Map<String, WorldSpawn> values) {
        Map<String, WorldSpawn> snapshot = immutableCopy(values);
        ensureTarget();
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection("spawns");
        snapshot.values().stream()
                .sorted(Comparator.comparing(WorldSpawn::worldName))
                .forEach(spawn -> {
                    ConfigurationSection world = section.createSection(spawn.worldName());
                    world.set("x", spawn.x());
                    world.set("y", spawn.y());
                    world.set("z", spawn.z());
                    world.set("yaw", spawn.yaw());
                    world.set("pitch", spawn.pitch());
                });
        try {
            AtomicYamlStore.write(target, configuration);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save DreamWorld spawns to " + target, exception);
        }
    }

    private static Map<String, WorldSpawn> immutableCopy(Map<String, WorldSpawn> values) {
        Objects.requireNonNull(values, "values");
        Map<String, WorldSpawn> copy = new LinkedHashMap<>();
        values.forEach((name, spawn) -> {
            WorldSpawn value = Objects.requireNonNull(spawn, "spawn");
            if (!Objects.equals(name, value.worldName())) {
                throw new IllegalArgumentException("Spawn map key does not match world name: " + name);
            }
            copy.put(name, value);
        });
        return Map.copyOf(copy);
    }

    private static double requiredDouble(YamlConfiguration configuration, String path) {
        Object raw = requiredValue(configuration, path);
        if (!(raw instanceof Number number)) {
            throw new IllegalStateException("Spawn value is not numeric: " + path);
        }
        return number.doubleValue();
    }

    private static float requiredFloat(YamlConfiguration configuration, String path) {
        return (float) requiredDouble(configuration, path);
    }

    private static Object requiredValue(YamlConfiguration configuration, String path) {
        if (!configuration.contains(path)) throw new IllegalStateException("Missing numeric spawn value: " + path);
        return configuration.get(path);
    }

    private void ensureTarget() {
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create DreamWorld data directory", exception);
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(target)) {
            cn.mythicland.dreamlib.api.DreamLibApi.contentFor(plugin).resource(plugin, RESOURCE_NAME, RESOURCE_NAME);
        }
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("DreamWorld spawn file is not a regular file: " + target);
        }
    }
}
