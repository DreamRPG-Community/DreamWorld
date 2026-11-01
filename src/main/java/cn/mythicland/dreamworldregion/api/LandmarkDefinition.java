package cn.mythicland.dreamworldregion.api;

import cn.mythicland.framework.validation.TextValidation;
import cn.mythicland.framework.bukkit.location.LocationSnapper;

/**
 * Immutable public definition of one named teleport landmark.
 *
 * @param id          stable internal identifier
 * @param displayName player-facing landmark name
 * @param worldName   DreamWorld logical name or Bukkit world name
 * @param x           landmark X coordinate
 * @param y           landmark Y coordinate
 * @param z           landmark Z coordinate
 * @param yaw         landmark yaw
 * @param pitch       landmark pitch
 */
public record LandmarkDefinition(
        String id,
        String displayName,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    /**
     * Validates the public landmark model.
     */
    public LandmarkDefinition {
        id = requireIdentifier(id);
        displayName = TextValidation.requireText(displayName, "displayName");
        worldName = TextValidation.requireText(worldName, "worldName");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Landmark coordinates must be finite");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Landmark rotation must be finite");
        }
        x = LocationSnapper.snapHorizontalCoordinate(x);
        y = LocationSnapper.snapStandingHeight(y);
        z = LocationSnapper.snapHorizontalCoordinate(z);
        yaw = LocationSnapper.snapYaw(yaw);
        pitch = 0.0F;
    }

    private static String requireIdentifier(String value) {
        String identifier = TextValidation.requireText(value, "id");
        if (!identifier.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("id contains unsupported characters: " + identifier);
        }
        return identifier;
    }

}
