package cn.mythicland.dreamworldprotect.storage;

import cn.mythicland.framework.policy.ListMode;
import cn.mythicland.framework.policy.ListPolicy;
import cn.mythicland.dreamworldprotect.api.DreamWorldProtectionApi;
import cn.mythicland.dreamworldprotect.api.WorldProtectionPolicy;
import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;
import cn.mythicland.dreamworldprotect.service.WorldProtectService;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class WorldConfigStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void inheritsDefaultsAndPreservesExplicitOverridesAndUnknownValues() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("worlds"));
        WorldProtectionRules defaults = defaults();
        WorldConfigStore store = new WorldConfigStore(
                Logger.getLogger("WorldConfigStoreTest"),
                defaults,
                resolver
        );
        store.ensureRootDirectory();

        World alpha = world("alpha", UUID.randomUUID());
        World beta = world("beta", UUID.randomUUID());
        store.load(alpha, "alpha");
        store.load(beta, "beta");

        Path alphaFile = temporaryDirectory.resolve("worlds").resolve("alpha.yml");
        Path betaFile = temporaryDirectory.resolve("worlds").resolve("beta.yml");
        assertFalse(Files.exists(alphaFile));
        assertFalse(Files.exists(betaFile));

        YamlConfiguration alphaConfiguration = YamlConfiguration.loadConfiguration(alphaFile.toFile());
        alphaConfiguration.set("rules.block-placement", false);
        alphaConfiguration.set("custom.keep", "value");
        alphaConfiguration.save(alphaFile.toFile());

        store.unload(alpha);
        WorldProtectionRules reloaded = store.load(alpha, "alpha");

        assertFalse(reloaded.blockPlace());
        YamlConfiguration preserved = YamlConfiguration.loadConfiguration(alphaFile.toFile());
        assertEquals("value", preserved.getString("custom.keep"));
        assertTrue(store.get(beta).blockPlace());
    }

    @Test
    void changingDefaultsAffectsOnlyInheritedValuesWithoutWritingFiles() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("protection"));
        var store = new WorldConfigStore(Logger.getLogger("WorldConfigStoreTest"), defaults(), resolver);
        store.ensureRootDirectory();
        World arena = world("arena", UUID.randomUUID());
        Path file = temporaryDirectory.resolve("protection/arena.yml");
        Files.writeString(file, "rules:\n  block-breaking: false\ncustom: keep\n");
        String original = Files.readString(file);
        assertTrue(store.load(arena, "arena").blockPlace());
        var changedDefaults = WorldProtectionRules.builder().blockPlace(false).blockBreak(true).build();
        store.reload(changedDefaults, resolver, List.of(arena), ignored -> "arena");
        assertFalse(store.get(arena).blockPlace());
        assertFalse(store.get(arena).blockBreak());
        assertEquals(original, Files.readString(file));
    }

    @Test
    void oldConfigurationKeysAreIgnored() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("worlds"));
        WorldConfigStore store = new WorldConfigStore(
                Logger.getLogger("WorldConfigStoreTest"),
                defaults(),
                resolver
        );
        store.ensureRootDirectory();

        World arena = world("arena", UUID.randomUUID());
        store.load(arena, "arena");
        Path arenaFile = temporaryDirectory.resolve("worlds").resolve("arena.yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(arenaFile.toFile());
        configuration.set("prevent.block-placement", false);
        configuration.set("interaction.mode", "whitelist");
        configuration.save(arenaFile.toFile());

        store.unload(arena);
        WorldProtectionRules rules = store.load(arena, "arena");

        assertTrue(rules.blockPlace());
        assertFalse(rules.blocksInteraction(54));
        assertFalse(rules.blocksInteraction(58));
    }

    @Test
    void unsafeWorldNamesUseUuidFallbackFiles() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("worlds"));
        WorldConfigStore store = new WorldConfigStore(
                Logger.getLogger("WorldConfigStoreTest"),
                defaults(),
                resolver
        );
        store.ensureRootDirectory();

        UUID worldId = UUID.randomUUID();
        World unsafeWorld = world("plugins/WorldManager/.runtime/arena", worldId);
        store.load(unsafeWorld, unsafeWorld.getName());

        assertFalse(Files.isRegularFile(
                temporaryDirectory.resolve("worlds").resolve("by-uuid").resolve(worldId + ".yml")
        ));
    }

    @Test
    void publicApiExposesOnlyLoadedImmutablePolicies() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("worlds"));
        WorldConfigStore store = new WorldConfigStore(
                Logger.getLogger("WorldConfigStoreTest"),
                defaults(),
                resolver
        );
        store.ensureRootDirectory();

        World arena = world("plugins/WorldManager/.runtime/arena", UUID.randomUUID());
        store.load(arena, "arena");

        DreamWorldProtectionApi api = new WorldProtectService(store);
        WorldProtectionPolicy policy = api.find(arena).orElseThrow();

        assertEquals("arena", policy.logicalName());
        assertTrue(policy.blockPlace());
        assertTrue(policy.naturalMobSpawning());
        assertTrue(policy.blocksCommand("/TELL"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> policy.interactionPolicy().entries().add(1)
        );
        assertTrue(api.find(null).isEmpty());

        store.unload(arena);
        assertTrue(api.find(arena).isEmpty());
    }

    @Test
    void worldFilesSupportBlacklistAndWhitelistModes() throws Exception {
        WorldConfigFileResolver resolver = new WorldConfigFileResolver(temporaryDirectory.resolve("worlds"));
        WorldConfigStore store = new WorldConfigStore(
                Logger.getLogger("WorldConfigStoreTest"),
                defaults(),
                resolver
        );
        store.ensureRootDirectory();

        World arena = world("arena", UUID.randomUUID());
        store.load(arena, "arena");
        Path arenaFile = temporaryDirectory.resolve("worlds").resolve("arena.yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(arenaFile.toFile());
        configuration.set("interact.mode", "blacklist");
        configuration.set("interact.block-ids", List.of(54));
        configuration.set("commands.mode", "whitelist");
        configuration.set("commands.names", List.of("say"));
        configuration.save(arenaFile.toFile());

        store.unload(arena);
        WorldProtectionRules rules = store.load(arena, "arena");

        assertTrue(rules.blocksInteraction(54));
        assertFalse(rules.blocksInteraction(58));
        assertFalse(rules.blocksCommand("say"));
        assertTrue(rules.blocksCommand("tell"));
    }

    private WorldProtectionRules defaults() {
        return WorldProtectionRules.builder()
                .blockPlace(true)
                .blockBreak(true)
                .interactionPolicy(new ListPolicy<>(ListMode.DISABLED, Set.of(54)))
                .bucket(true)
                .leafDecay(true)
                .blockFade(true)
                .blockIgnite(true)
                .fireSpread(true)
                .explosions(true)
                .fallDamage(true)
                .enderPearl(false)
                .naturalMobSpawning(true)
                .commandPolicy(new ListPolicy<>(ListMode.BLACKLIST, Set.of("tell")))
                .build();
    }

    private World world(String name, UUID uuid) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getName")) return name;
                    if (method.getName().equals("getUID")) return uuid;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == float.class) return 0.0F;
                    if (method.getReturnType() == double.class) return 0.0D;
                    if (method.getReturnType() == short.class) return (short) 0;
                    if (method.getReturnType() == byte.class) return (byte) 0;
                    if (method.getReturnType() == char.class) return (char) 0;
                    return null;
                }
        );
    }
}
