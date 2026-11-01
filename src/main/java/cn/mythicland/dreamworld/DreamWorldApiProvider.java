package cn.mythicland.dreamworld;

import cn.mythicland.dreamworld.api.DreamWorldApi;
import cn.mythicland.dreamworld.api.WorldInfo;
import cn.mythicland.dreamworld.api.WorldSpawn;
import cn.mythicland.framework.bukkit.component.ServiceComponent;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Publishes the active DreamWorld lifecycle service through Lib's service annotation.
 */
@ServiceComponent(DreamWorldApi.class)
public final class DreamWorldApiProvider implements DreamWorldApi {

    private final DreamWorldLifecycle lifecycle;
    private final DreamWorldPlugin plugin;

    public DreamWorldApiProvider(DreamWorldPlugin plugin, DreamWorldLifecycle lifecycle) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    @Override
    public Collection<WorldInfo> list() {
        return lifecycle.service().list();
    }

    @Override
    public CompletableFuture<Void> reload() {
        return plugin.reloadDreamWorld();
    }

    @Override
    public Optional<String> findLogicalName(World world) {
        return lifecycle.service().findLogicalName(world);
    }

    @Override
    public Optional<World> find(String logicalName) {
        return lifecycle.service().find(logicalName);
    }

    @Override
    public Optional<WorldSpawn> findSpawn(String logicalName) {
        return lifecycle.service().findSpawn(logicalName);
    }

    @Override
    public CompletableFuture<WorldSpawn> setSpawn(String logicalName, Location location) {
        return lifecycle.service().setSpawn(logicalName, location);
    }

    @Override
    public CompletableFuture<World> load(String logicalName) {
        return lifecycle.service().load(logicalName);
    }

    @Override
    public CompletableFuture<Boolean> unload(String logicalName, boolean force) {
        return lifecycle.service().unload(logicalName, force);
    }

    @Override
    public CompletableFuture<Integer> clean(String logicalName) {
        return lifecycle.service().clean(logicalName);
    }

    @Override
    public CompletableFuture<Integer> save(String logicalName) {
        return lifecycle.service().save(logicalName);
    }

    @Override
    public CompletableFuture<Boolean> teleport(Player player, String logicalName) {
        return lifecycle.service().teleport(player, logicalName);
    }
}
