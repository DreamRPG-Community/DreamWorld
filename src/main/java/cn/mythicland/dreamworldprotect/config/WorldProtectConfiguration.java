package cn.mythicland.dreamworldprotect.config;

import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;
import cn.mythicland.framework.bukkit.config.ConfigView;
import cn.mythicland.framework.bukkit.config.ConfigurableComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigProperty;
import cn.mythicland.framework.policy.ListMode;
import cn.mythicland.framework.policy.ListPolicy;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Binds global WorldProtect settings while leaving per-world files to the domain store.
 */
@ConfigComponent
public final class WorldProtectConfiguration implements ConfigurableComponent {

    private volatile WorldProtectSettings snapshot;

    static WorldProtectSettings bind(FileConfiguration configuration) {
        return bind(configuration, ignored -> {
        });
    }

    static WorldProtectSettings bind(
            FileConfiguration configuration,
            Consumer<String> warningConsumer
    ) {
        return build(cn.mythicland.framework.bukkit.config.ConfigSupport.bind(
                configuration,
                RawSettings.class,
                warningConsumer
        ));
    }

    private static WorldProtectSettings build(RawSettings raw) {
        WorldProtectionRules defaults = WorldProtectionRules.builder()
                .blockPlace(raw.blockPlace())
                .blockBreak(raw.blockBreak())
                .interactionPolicy(new ListPolicy<>(raw.interactionMode(), raw.interactionBlockIds()))
                .bucket(raw.bucket())
                .leafDecay(raw.leafDecay())
                .blockFade(raw.blockFade())
                .blockIgnite(raw.blockIgnite())
                .fireSpread(raw.fireSpread())
                .explosions(raw.explosions())
                .fallDamage(raw.fallDamage())
                .enderPearl(raw.enderPearl())
                .naturalMobSpawning(raw.naturalMobSpawning())
                .commandPolicy(new ListPolicy<>(raw.commandMode(), raw.commandNames()))
                .build();
        return WorldProtectSettings.builder()
                .worldConfigDirectory(raw.worldConfigDirectory())
                .editPermission(raw.editPermission())
                .worldDefaults(defaults)
                .build();
    }

    /**
     * Binds global protection settings and publishes a complete domain snapshot.
     *
     * @param configuration Lib-owned configuration view
     */
    @Override
    public void reload(ConfigView configuration) {
        snapshot = build(Objects.requireNonNull(configuration, "configuration")
                .bind(RawSettings.class));
    }

    /**
     * Returns the immutable global configuration snapshot.
     *
     * @return current settings
     */
    public WorldProtectSettings snapshot() {
        WorldProtectSettings value = snapshot;
        if (value == null) throw new IllegalStateException("DreamWorld protection settings are not loaded");
        return value;
    }

    record RawSettings(
            @ConfigProperty(
                    path = "world-config-directory",
                    defaultValue = "worlds",
                    nonBlank = true
            )
            String worldConfigDirectory,
            @ConfigProperty(
                    path = "edit-mode.permission",
                    defaultValue = "dreamworld.protect.edit",
                    nonBlank = true
            )
            String editPermission,
            @ConfigProperty(
                    path = "world-defaults.rules.block-placement",
                    defaultValue = "true"
            )
            boolean blockPlace,
            @ConfigProperty(
                    path = "world-defaults.rules.block-breaking",
                    defaultValue = "true"
            )
            boolean blockBreak,
            @ConfigProperty(
                    path = "world-defaults.interact.mode",
                    defaultValue = "DISABLED"
            )
            ListMode interactionMode,
            @ConfigProperty(
                    path = "world-defaults.interact.block-ids",
                    defaultValue = "0,54,58,61,62,63,68,116,117,118,130,146,323,397,330,70,148,72,147,33,23,77",
                    nonNegative = true
            )
            List<Integer> interactionBlockIds,
            @ConfigProperty(
                    path = "world-defaults.rules.bucket-actions",
                    defaultValue = "true"
            )
            boolean bucket,
            @ConfigProperty(
                    path = "world-defaults.rules.leaf-decay",
                    defaultValue = "true"
            )
            boolean leafDecay,
            @ConfigProperty(
                    path = "world-defaults.rules.block-fade",
                    defaultValue = "true"
            )
            boolean blockFade,
            @ConfigProperty(
                    path = "world-defaults.rules.block-ignition",
                    defaultValue = "true"
            )
            boolean blockIgnite,
            @ConfigProperty(
                    path = "world-defaults.rules.fire-spread",
                    defaultValue = "true"
            )
            boolean fireSpread,
            @ConfigProperty(
                    path = "world-defaults.rules.explosion-block-damage",
                    defaultValue = "true"
            )
            boolean explosions,
            @ConfigProperty(
                    path = "world-defaults.rules.player-fall-damage",
                    defaultValue = "true"
            )
            boolean fallDamage,
            @ConfigProperty(
                    path = "world-defaults.rules.ender-pearl-launch",
                    defaultValue = "false"
            )
            boolean enderPearl,
            @ConfigProperty(
                    path = "world-defaults.rules.natural-mob-spawning",
                    defaultValue = "false"
            )
            boolean naturalMobSpawning,
            @ConfigProperty(
                    path = "world-defaults.commands.mode",
                    defaultValue = "BLACKLIST"
            )
            ListMode commandMode,
            @ConfigProperty(
                    path = "world-defaults.commands.names",
                    defaultValue = "?,bukkit:?,help,bukkit:help,pl,plugins,bukkit:pl,bukkit:plugins",
                    nonBlank = true
            )
            List<String> commandNames
    ) {
    }
}
