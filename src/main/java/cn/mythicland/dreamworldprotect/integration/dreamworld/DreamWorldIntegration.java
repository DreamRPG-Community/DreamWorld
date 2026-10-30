package cn.mythicland.dreamworldprotect.integration.dreamworld;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Optional, class-loader-safe access to the DreamWorld service.
 *
 * <p>DreamWorld is deliberately not a hard runtime dependency. Reflection
 * is used here because Bukkit gives each plugin its own class loader and the
 * DreamWorld API is unavailable when that plugin is not installed.</p>
 */
public final class DreamWorldIntegration {

    private static final String WORLD_MANAGER_NAME = "DreamWorld";
    private static final String API_CLASS_NAME = "cn.mythicland.dreamworld.api.DreamWorldApi";

    private final JavaPlugin plugin;
    private Object provider;
    private Method findLogicalName;
    private boolean lookupFailed;
    private boolean failureLogged;

    public DreamWorldIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        Plugin worldManager = plugin.getServer().getPluginManager().getPlugin(WORLD_MANAGER_NAME);
        return worldManager != null && worldManager.isEnabled();
    }

    public Optional<String> logicalName(World world) {
        if (world == null || !isEnabled()) return Optional.empty();
        if (!ensureProvider()) return Optional.empty();

        try {
            Object result = findLogicalName.invoke(provider, world);
            if (!(result instanceof Optional<?> optional)) return Optional.empty();
            return optional.filter(String.class::isInstance).map(String.class::cast);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            lookupFailed = true;
            logLookupFailure(exception);
            return Optional.empty();
        }
    }

    public String logicalNameOrBukkitName(World world) {
        return logicalName(world).orElse(world.getName());
    }

    private boolean ensureProvider() {
        if (provider != null && findLogicalName != null) return true;
        if (lookupFailed) return false;

        Plugin worldManager = plugin.getServer().getPluginManager().getPlugin(WORLD_MANAGER_NAME);
        if (worldManager == null || !worldManager.isEnabled()) return false;

        try {
            ClassLoader classLoader = worldManager.getClass().getClassLoader();
            Class<?> apiClass = Class.forName(API_CLASS_NAME, true, classLoader);
            RegisteredServiceProvider<?> registration = getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) {
                return false;
            }

            Object service = registration.getProvider();
            Method method = service.getClass().getMethod("findLogicalName", World.class);
            provider = service;
            findLogicalName = method;
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            lookupFailed = true;
            logLookupFailure(exception);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private RegisteredServiceProvider<?> getRegistration(Class<?> apiClass) {
        return plugin.getServer().getServicesManager().getRegistration((Class<Object>) apiClass);
    }

    private void logLookupFailure(Throwable exception) {
        if (failureLogged) return;
        failureLogged = true;
        plugin.getLogger().log(
                Level.WARNING,
                "DreamWorld is enabled but its logical-world API could not be used; "
                        + "falling back to Bukkit world names.",
                exception
        );
    }
}
