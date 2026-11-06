package cn.mythicland.dreamworld;

import cn.mythicland.dreamlib.api.DreamLibApi;
import cn.mythicland.dreamworld.api.DreamWorldApi;
import cn.mythicland.framework.bukkit.player.StartupJoinGate;
import cn.mythicland.framework.bukkit.task.PluginTaskScope;
import cn.mythicland.framework.component.LifecycleComponent;
import cn.mythicland.framework.component.ManagedLifecycle;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.bukkit.scheduler.BukkitTask;

/**
 * Owns DreamWorld construction, service registration, command binding, and startup discovery.
 */
@LifecycleComponent
public final class DreamWorldLifecycle implements ManagedLifecycle {

    private final DreamWorldPlugin plugin;
    private final DreamLibApi lib;
    private final PluginTaskScope tasks;
    private final DreamWorldConfiguration configuration;
    private final WorldSpawnStore spawnStore;
    private DreamWorldService service;
    private BukkitTask startupTask;
    private StartupJoinGate startupJoinGate;
    private CompletableFuture<Void> reloadFuture = CompletableFuture.completedFuture(null);

    /**
     * Creates the lifecycle module from Lib-provided dependencies.
     *
     * @param plugin        plugin entry point
     * @param lib           shared Lib service
     * @param tasks         plugin task scope
     * @param configuration runtime configuration
     * @param spawnStore    persistent per-world spawn store
     */
    public DreamWorldLifecycle(
            DreamWorldPlugin plugin,
            DreamLibApi lib,
            PluginTaskScope tasks,
            DreamWorldConfiguration configuration,
            WorldSpawnStore spawnStore
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lib = Objects.requireNonNull(lib, "lib");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.spawnStore = Objects.requireNonNull(spawnStore, "spawnStore");
    }

    /**
     * Restores the initial world, registers the API and command, and starts discovery.
     */
    @Override
    public void load() {
        CompletableFuture<Void> startup = new CompletableFuture<>();
        startupJoinGate = new StartupJoinGate(plugin, "§c服务器正在启动中, 请稍后重试", startup);
        DreamWorldSettings settings = configuration.snapshot();
        DreamWorldSettingsLoader.ensureDirectories(settings);
        spawnStore.load();
        service = new DreamWorldService(plugin, lib, settings, configuration, spawnStore);
        try {
            service.restoreInitialWorld();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not restore initial world '" + settings.initialWorldName() + "'",
                    exception
            );
        }

        startupTask = tasks.runLater(1L, () -> service.discoverAndLoadAll().whenComplete((ignored, error) -> {
            if (error != null) {
                startup.completeExceptionally(error);
                plugin.getLogger().warning(
                        "Startup world loading finished with failures: " + DreamLibApi.rootCauseMessage(error)
                );
                return;
            }
            plugin.getLogger().info("Startup world discovery and loading completed.");
            startup.complete(null);
        }));
    }

    /**
     * Reloads runtime configuration and re-scans persistent world snapshots.
     *
     * <p>World path settings remain immutable for the server lifetime. A path or initial-world
     * change requires a restart, while runtime options and newly added snapshot directories can
     * be loaded here.</p>
     */
    @Override
    public void reload() {
        reloadFuture = reloadConfiguration();
    }

    CompletableFuture<Void> reloadConfiguration() {
        if (service == null) return CompletableFuture.failedFuture(
                new IllegalStateException("DreamWorld service is unavailable")
        );
        CompletableFuture<Void> current = service.reload();
        current.whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().warning(
                        "DreamWorld configuration reload finished with failures: "
                                + DreamLibApi.rootCauseMessage(error)
                );
                return;
            }
            plugin.getLogger().info("DreamWorld configuration and snapshot reload completed.");
        });
        return current;
    }

    CompletableFuture<Void> lastReload() {
        return reloadFuture;
    }

    /**
     * Unregisters the API and closes managed world resources.
     */
    @Override
    public void unload() {
        if (startupJoinGate != null) startupJoinGate.close();
        startupJoinGate = null;
        tasks.cancel(startupTask);
        startupTask = null;
        if (service == null) return;
        service.close();
        service = null;
    }

    /**
     * Returns the active world manager service to annotation-driven commands.
     *
     * @return active world manager service
     */
    public DreamWorldApi service() {
        return Objects.requireNonNull(service, "DreamWorld service is unavailable");
    }

}
