package cn.mythicland.dreamworldregion;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.dreamworld.api.DreamWorldLookup;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Injects DreamWorld's world lookup into the region component graph.
 */
@InjectComponent
final class DreamWorldIntegration {
    private final DreamWorldLookup lookup;

    DreamWorldIntegration(JavaPlugin plugin) {
        lookup = new DreamWorldLookup(plugin);
    }

    String logicalNameOrBukkitName(World world) {
        return lookup.logicalNameOrBukkitName(world);
    }

    CompletableFuture<World> resolveWorld(String logicalName) {
        return lookup.resolveWorld(logicalName);
    }
}
