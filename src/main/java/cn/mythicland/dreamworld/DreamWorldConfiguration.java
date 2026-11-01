package cn.mythicland.dreamworld;

import cn.mythicland.framework.bukkit.config.ConfigView;
import cn.mythicland.framework.bukkit.config.ConfigurableComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigProperty;

import java.util.Objects;

/**
 * Binds DreamWorld's global configuration before its world lifecycle starts.
 */
@ConfigComponent
final class DreamWorldConfiguration implements ConfigurableComponent {

    private final DreamWorldPlugin plugin;
    private volatile DreamWorldSettings snapshot;

    DreamWorldConfiguration(DreamWorldPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void reload(ConfigView configuration) {
        RawSettings raw = Objects.requireNonNull(configuration, "configuration")
                .bind(RawSettings.class);
        snapshot = DreamWorldSettingsLoader.load(plugin, raw);
    }

    DreamWorldSettings snapshot() {
        DreamWorldSettings value = snapshot;
        if (value == null) throw new IllegalStateException("DreamWorld settings are not loaded");
        return value;
    }

    record RawSettings(
            @ConfigProperty(
                    path = "world-directory",
                    defaultValue = "worlds",
                    nonBlank = true
            )
            String worldDirectory,
            @ConfigProperty(
                    path = "initial-world-name",
                    defaultValue = "world",
                    nonBlank = true
            )
            String initialWorldName,
            @ConfigProperty(
                    path = "clean-world-resources",
                    defaultValue = "false"
            )
            boolean cleanWorldResources,
            @ConfigProperty(
                    path = "auto-reset-worlds",
                    defaultValue = "false"
            )
            boolean autoResetWorlds,
            @ConfigProperty(
                    path = "fallback-world",
                    defaultValue = "world",
                    nonBlank = true
            )
            String fallbackWorld
    ) {
    }
}
