package cn.mythicland.dreamworldprotect.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds temporary per-player edit mode state.
 */
public final class EditModeTracker {

    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();

    public boolean toggle(UUID playerId) {
        if (enabledPlayers.remove(playerId)) return false;
        enabledPlayers.add(playerId);
        return true;
    }

    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public void remove(UUID playerId) {
        enabledPlayers.remove(playerId);
    }

    public void clear() {
        enabledPlayers.clear();
    }
}
