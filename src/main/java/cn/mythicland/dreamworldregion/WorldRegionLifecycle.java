package cn.mythicland.dreamworldregion;

import cn.mythicland.framework.bukkit.task.PluginTaskScope;
import cn.mythicland.framework.component.LifecycleComponent;
import cn.mythicland.framework.component.ManagedLifecycle;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns WorldRegion's scheduled particle lifecycle and reload boundary.
 */
@LifecycleComponent
final class WorldRegionLifecycle implements ManagedLifecycle {

    private final JavaPlugin plugin;
    private final PluginTaskScope tasks;
    private final WorldRegionSettings settings;
    private final WorldRegionDataStore dataStore;
    private final RegionParticleRenderer particles;
    private final RegionSelectionService selections;
    private BukkitTask particleTask;

    WorldRegionLifecycle(
            JavaPlugin plugin,
            PluginTaskScope tasks,
            WorldRegionSettings settings,
            WorldRegionDataStore dataStore,
            RegionParticleRenderer particles,
            RegionSelectionService selections
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.particles = Objects.requireNonNull(particles, "particles");
        this.selections = Objects.requireNonNull(selections, "selections");
    }

    @Override
    public void load() {
        restartParticleTask();
        plugin.getLogger().info("DreamWorld region enabled.");
    }

    @Override
    public void reload() {
        dataStore.reload();
        restartParticleTask();
        plugin.getLogger().info("DreamWorld region configuration reloaded.");
    }

    @Override
    public void unload() {
        tasks.cancel(particleTask);
        particleTask = null;
        selections.clearAll();
    }

    private void restartParticleTask() {
        tasks.cancel(particleTask);
        particleTask = null;
        WorldRegionSettings.SettingsSnapshot current = settings.snapshot();
        if (!current.particlesEnabled()) return;
        particleTask = tasks.runTimer(
                1L,
                current.intervalTicks(),
                particles::renderAll
        );
    }
}
