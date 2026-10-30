package cn.mythicland.dreamworld;

import cn.mythicland.dreamlib.bootstrap.annotation.ConfigComponent;
import cn.mythicland.dreamlib.config.ConfigValue;
import cn.mythicland.dreamlib.config.ConfigView;
import cn.mythicland.dreamlib.config.ConfigurableComponent;

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
            @ConfigValue(
                    path = "world-directory",
                    defaultValue = "worlds",
                    nonBlank = true
            )
            String worldDirectory,
            @ConfigValue(
                    path = "initial-world-name",
                    defaultValue = "world",
                    nonBlank = true
            )
            String initialWorldName,
            @ConfigValue(
                    path = "clean-world-resources",
                    defaultValue = "false"
            )
            boolean cleanWorldResources,
            @ConfigValue(
                    path = "auto-reset-worlds",
                    defaultValue = "false"
            )
            boolean autoResetWorlds,
            @ConfigValue(
                    path = "fallback-world",
                    defaultValue = "world",
                    nonBlank = true
            )
            String fallbackWorld
    ) {
    }
}
