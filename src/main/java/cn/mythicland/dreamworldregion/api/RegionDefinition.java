package cn.mythicland.dreamworldregion.api;

import cn.mythicland.framework.validation.TextValidation;

import java.util.Objects;

/**
 * Immutable public definition of one named world region.
 *
 * @param id          stable internal identifier
 * @param displayName colored name shown to players
 * @param priority    matching priority, where larger values win
 * @param worldName   DreamWorld logical name or Bukkit world name
 * @param bounds      inclusive three-dimensional region bounds
 */
public record RegionDefinition(
        String id,
        String displayName,
        int priority,
        String worldName,
        RegionBounds bounds
) {

    /**
     * Validates the public region model.
     */
    public RegionDefinition {
        id = requireIdentifier(id);
        displayName = TextValidation.requireText(displayName, "displayName");
        worldName = TextValidation.requireText(worldName, "worldName");
        Objects.requireNonNull(bounds, "bounds");
    }

    private static String requireIdentifier(String value) {
        String identifier = TextValidation.requireText(value, "id");
        if (!identifier.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("id contains unsupported characters: " + identifier);
        }
        return identifier;
    }

}
