package cn.mythicland.dreamworld;

import cn.mythicland.framework.bukkit.content.ContentUpdate;
import cn.mythicland.dreamworldregion.WorldRegionDataStore;
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

    /** Reloads editable region, landmark and spawn definitions without reading system settings. */
    public void reloadContent() {
        bootstrap(REGION_PACKAGE).resolve(WorldRegionDataStore.class).reloadContent();
        bootstrap(COMPONENT_PACKAGE).resolve(WorldSpawnStore.class).load();
    }

    /** @return validated editable world snapshots and their inverse updates */
    public java.util.List<ContentUpdate> prepareContentReload() {
        return java.util.List.of(
                bootstrap(REGION_PACKAGE).resolve(WorldRegionDataStore.class).prepareContentReload(),
                bootstrap(COMPONENT_PACKAGE).resolve(WorldSpawnStore.class).prepareContentReload());
    }
}
