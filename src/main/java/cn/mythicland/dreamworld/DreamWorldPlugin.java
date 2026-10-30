package cn.mythicland.dreamworld;

import cn.mythicland.dreamlib.bootstrap.DreamLibManagedPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Minimal Bukkit entry point for the Lib-managed world manager.
 */
public final class DreamWorldPlugin extends DreamLibManagedPlugin {

    private static final String COMPONENT_PACKAGE = "cn.mythicland.dreamworld";
    private static final String PROTECTION_PACKAGE = "cn.mythicland.dreamworldprotect";
    private static final String REGION_PACKAGE = "cn.mythicland.dreamworldregion";

    public DreamWorldPlugin() {
        super(COMPONENT_PACKAGE, PROTECTION_PACKAGE, REGION_PACKAGE);
    }

    /**
     * Reloads the Lib configuration snapshot and then rescans DreamWorld data.
     *
     * @return completion future for the world rescan
     */
    public CompletableFuture<Void> reloadDreamWorld() {
        bootstrap(COMPONENT_PACKAGE).reload();
        bootstrap(PROTECTION_PACKAGE).reload();
        bootstrap(REGION_PACKAGE).reload();
        return bootstrap(COMPONENT_PACKAGE).resolve(DreamWorldLifecycle.class).lastReload();
    }

    public void reloadWorldProtect() {
        bootstrap(PROTECTION_PACKAGE).reload();
    }

    public void reloadWorldRegion() {
        bootstrap(REGION_PACKAGE).reload();
    }
}
