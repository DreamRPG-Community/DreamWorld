package cn.mythicland.dreamworldregion;

import cn.mythicland.framework.bukkit.config.ConfigView;
import cn.mythicland.framework.bukkit.config.ConfigurableComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigComponent;
import cn.mythicland.framework.bukkit.config.annotation.ConfigProperty;

import java.util.Locale;
import java.util.Objects;

/**
 * Mutable configuration holder for WorldRegion's selection and particle runtime.
 */
@ConfigComponent
public final class WorldRegionSettings implements ConfigurableComponent {

    private volatile SettingsSnapshot snapshot;

    /**
     * Creates the Lib-managed WorldRegion configuration component.
     */
    public WorldRegionSettings() {
    }

    private static SettingsSnapshot read(RawSettings configuration) {
        String wandName = configuration.wand().toUpperCase(Locale.ROOT);
        if (!wandName.equals("WOOD_SPADE")) {
            throw new IllegalStateException("DreamWorld region selection.wand must be WOOD_SPADE: " + wandName);
        }
        return new SettingsSnapshot(
                configuration.particlesEnabled(),
                configuration.intervalTicks(),
                configuration.renderDistance(),
                configuration.maxRegionsPerPlayer()
        );
    }

    /**
     * Binds and validates the current configuration snapshot.
     *
     * @param configuration Lib-owned configuration view
     */
    @Override
    public void reload(ConfigView configuration) {
        Objects.requireNonNull(configuration, "configuration");
        snapshot = read(configuration.bind(RawSettings.class));
    }

    /**
     * Returns the immutable current settings.
     *
     * @return current settings snapshot
     */
    public SettingsSnapshot snapshot() {
        return snapshot;
    }

    private record RawSettings(
            @ConfigProperty(
                    path = "selection.wand",
                    defaultValue = "WOOD_SPADE",
                    nonBlank = true
            )
            String wand,
            @ConfigProperty(
                    path = "particle.enabled",
                    defaultValue = "true"
            )
            boolean particlesEnabled,
            @ConfigProperty(
                    path = "particle.interval-ticks",
                    defaultValue = "10",
                    positive = true
            )
            long intervalTicks,
            @ConfigProperty(
                    path = "particle.render-distance",
                    defaultValue = "64",
                    positive = true
            )
            int renderDistance,
            @ConfigProperty(
                    path = "particle.max-regions-per-player",
                    defaultValue = "32",
                    positive = true
            )
            int maxRegionsPerPlayer
    ) {
    }

    /**
     * Immutable particle and selection settings.
     *
     * @param particlesEnabled    whether boundary particles are enabled
     * @param intervalTicks       redraw interval
     * @param renderDistance      maximum distance from a viewer
     * @param maxRegionsPerPlayer maximum rendered regions per viewer
     */
    public record SettingsSnapshot(
            boolean particlesEnabled,
            long intervalTicks,
            int renderDistance,
            int maxRegionsPerPlayer
    ) {
    }
}
