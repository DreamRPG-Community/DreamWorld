package cn.mythicland.dreamworldprotect.config;

import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Global WorldProtect settings and the template used for new worlds.
 */
public final class WorldProtectSettings {

    private final String worldConfigDirectory;
    private final String editPermission;
    private final WorldProtectionRules worldDefaults;

    private WorldProtectSettings(Builder builder) {
        worldConfigDirectory = requireNonBlank(builder.worldConfigDirectory, "worldConfigDirectory");
        editPermission = requireNonBlank(builder.editPermission, "editPermission");
        worldDefaults = Objects.requireNonNull(builder.worldDefaults, "worldDefaults");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WorldProtectSettings load(JavaPlugin plugin, FileConfiguration configuration) {
        Objects.requireNonNull(plugin, "plugin");
        return WorldProtectConfiguration.bind(configuration, plugin.getLogger()::warning);
    }


    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public String worldConfigDirectory() {
        return worldConfigDirectory;
    }

    public String editPermission() {
        return editPermission;
    }

    public WorldProtectionRules worldDefaults() {
        return worldDefaults;
    }

    /**
     * Named construction API for global settings and the world defaults template.
     */
    public static final class Builder {

        private String worldConfigDirectory;
        private String editPermission;
        private WorldProtectionRules worldDefaults;

        public Builder worldConfigDirectory(String value) {
            worldConfigDirectory = value;
            return this;
        }

        public Builder editPermission(String value) {
            editPermission = value;
            return this;
        }

        public Builder worldDefaults(WorldProtectionRules value) {
            worldDefaults = value;
            return this;
        }

        public WorldProtectSettings build() {
            return new WorldProtectSettings(this);
        }
    }
}
