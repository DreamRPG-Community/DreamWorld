package cn.mythicland.dreamworld.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Resolves managed worlds through DreamWorld's service.
 */
public final class DreamWorldLookup {
    private final JavaPlugin plugin;
    private boolean failureLogged;

    /**
     * Creates a lookup against the owning server's services.
     *
     * @param plugin plugin owning the lookup
     */
    public DreamWorldLookup(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * @return whether DreamWorld is enabled
     */
    public boolean isEnabled() {
        Plugin worldManager = plugin.getServer().getPluginManager().getPlugin("DreamWorld");
        return worldManager != null && worldManager.isEnabled();
    }

    /**
     * Resolves the managed name, falling back to the Bukkit name.
     *
     * @param world loaded world
     * @return logical or Bukkit world name
     */
    public String logicalNameOrBukkitName(World world) {
        return logicalName(world).orElse(world.getName());
    }

    /**
     * Resolves a managed world's logical name when its service is available.
     *
     * @param world loaded world, or null
     * @return logical name when available
     */
    public Optional<String> logicalName(World world) {
        if (world == null) return Optional.empty();
        DreamWorldApi service = provider();
        if (service == null) return Optional.empty();
        try {
            return service.findLogicalName(world);
        } catch (RuntimeException exception) {
            logFailure(exception);
            return Optional.empty();
        }
    }

    /**
     * Returns a loaded world or requests that DreamWorld load it.
     *
     * @param logicalName world name
     * @return future completed with the loaded world or the failure
     */
    public CompletableFuture<World> resolveWorld(String logicalName) {
        World loaded = Bukkit.getWorld(logicalName);
        if (loaded != null) return CompletableFuture.completedFuture(loaded);
        DreamWorldApi service = provider();
        if (service == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("World is not loaded and DreamWorld is unavailable: " + logicalName)
            );
        }
        try {
            Optional<World> managed = service.find(logicalName);
            if (managed.isPresent()) return CompletableFuture.completedFuture(managed.orElseThrow());
            return service.load(logicalName);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("DreamWorld could not resolve world: " + logicalName, exception)
            );
        }
    }

    private DreamWorldApi provider() {
        if (!isEnabled()) return null;
        try {
            RegisteredServiceProvider<DreamWorldApi> registration = plugin.getServer()
                    .getServicesManager().getRegistration(DreamWorldApi.class);
            return registration == null ? null : registration.getProvider();
        } catch (RuntimeException exception) {
            logFailure(exception);
            return null;
        }
    }

    private void logFailure(RuntimeException exception) {
        if (failureLogged) return;
        failureLogged = true;
        plugin.getLogger().log(Level.WARNING, "DreamWorld public world API could not be used.", exception);
    }
}
