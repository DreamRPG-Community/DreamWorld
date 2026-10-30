package cn.mythicland.dreamworld;

import cn.mythicland.dreamlib.api.DreamLibApi;
import cn.mythicland.dreamlib.bootstrap.PluginBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Minimal Bukkit entry point for the Lib-managed world manager.
 */
public final class DreamWorldPlugin extends JavaPlugin {

    private static final String COMPONENT_PACKAGE = "cn.mythicland.dreamworld";

    private PluginBootstrap bootstrap;
    private PluginBootstrap protection;
    private PluginBootstrap regions;

    /**
     * Starts the Lib-managed DreamWorld component graph.
     */
    @Override
    @SuppressWarnings("resource")
    public void onEnable() {
        try {
            DreamLibApi lib = DreamLibApi.require(this);
            bootstrap = lib.createPluginBootstrap(this, COMPONENT_PACKAGE);
            bootstrap.enable();
            protection = lib.createPluginBootstrap(this, "cn.mythicland.dreamworldprotect");
            protection.enable();
            regions = lib.createPluginBootstrap(this, "cn.mythicland.dreamworldregion");
            regions.enable();
        } catch (RuntimeException exception) {
            getLogger().log(
                    Level.SEVERE,
                    "DreamWorld failed to enable: " + DreamLibApi.rootCauseMessage(exception),
                    exception
            );
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Closes the Lib-managed DreamWorld component graph.
     */
    @Override
    public void onDisable() {
        if (regions != null) regions.disable();
        regions = null;
        if (protection != null) protection.disable();
        protection = null;
        if (bootstrap != null) bootstrap.disable();
        bootstrap = null;
    }

    /**
     * Reloads the Lib configuration snapshot and then rescans DreamWorld data.
     *
     * @return completion future for the world rescan
     */
    public CompletableFuture<Void> reloadDreamWorld() {
        Objects.requireNonNull(bootstrap, "DreamWorld bootstrap is unavailable").reload();
        protection.reload();
        regions.reload();
        return bootstrap.resolve(DreamWorldLifecycle.class).lastReload();
    }

    public void reloadWorldProtect() {
        protection.reload();
    }

    public void reloadWorldRegion() {
        regions.reload();
    }
}
