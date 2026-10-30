package cn.mythicland.dreamworldprotect.service;

import cn.mythicland.dreamworldprotect.api.DreamWorldProtectionApi;
import cn.mythicland.dreamworldprotect.api.WorldProtectionPolicy;
import cn.mythicland.dreamworldprotect.storage.WorldConfigStore;
import org.bukkit.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Bukkit service implementation for the public WorldProtect API.
 */
public final class WorldProtectService implements DreamWorldProtectionApi {

    private final WorldConfigStore worldConfigs;
    private final EditModeTracker editModes;

    public WorldProtectService(WorldConfigStore worldConfigs) {
        this(worldConfigs, new EditModeTracker());
    }

    public WorldProtectService(WorldConfigStore worldConfigs, EditModeTracker editModes) {
        this.worldConfigs = worldConfigs;
        this.editModes = editModes;
    }

    @Override
    public Optional<WorldProtectionPolicy> find(World world) {
        return worldConfigs.findPolicy(world);
    }

    @Override
    public boolean isEditMode(UUID playerId) {
        return editModes.isEnabled(playerId);
    }
}
