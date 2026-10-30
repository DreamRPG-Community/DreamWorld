package cn.mythicland.dreamworld;

import java.nio.file.Path;
import java.util.Objects;

record DreamWorldSettings(
        Path worldsRoot,
        Path runtimeRoot,
        Path initialWorldDirectory,
        String initialWorldName,
        String bukkitDirectory,
        boolean cleanWorldResources,
        boolean autoResetWorlds,
        String fallbackWorld
) {

    DreamWorldSettings withReloadableOptions(DreamWorldSettings refreshed) {
        Objects.requireNonNull(refreshed, "refreshed");
        if (!worldsRoot.equals(refreshed.worldsRoot())
                || !runtimeRoot.equals(refreshed.runtimeRoot())
                || !initialWorldDirectory.equals(refreshed.initialWorldDirectory())
                || !initialWorldName.equals(refreshed.initialWorldName())
                || !bukkitDirectory.equals(refreshed.bukkitDirectory())) {
            throw new IllegalStateException(
                    "World path settings changed; restart DreamWorld to apply world-directory "
                            + "or initial-world-name changes"
            );
        }
        return new DreamWorldSettings(
                worldsRoot,
                runtimeRoot,
                initialWorldDirectory,
                initialWorldName,
                bukkitDirectory,
                refreshed.cleanWorldResources(),
                refreshed.autoResetWorlds(),
                refreshed.fallbackWorld()
        );
    }
}
