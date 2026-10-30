package cn.mythicland.dreamworld.api;

import cn.mythicland.dreamlib.location.LocationSnapper;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * Immutable DreamWorld spawn point independent of a mutable Bukkit location object.
 */
public final class WorldSpawn {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Creates a world spawn value.
     *
     * @param worldName logical world name
     * @param x         spawn X coordinate
     * @param y         spawn Y coordinate
     * @param z         spawn Z coordinate
     * @param yaw       spawn yaw
     * @param pitch     spawn pitch
     */
    public WorldSpawn(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = requireWorldName(worldName);
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
        this.x = LocationSnapper.snapHorizontalCoordinate(x);
        this.y = LocationSnapper.snapStandingHeight(y);
        this.z = LocationSnapper.snapHorizontalCoordinate(z);
        this.yaw = LocationSnapper.snapYaw(yaw);
        this.pitch = 0.0F;
    }

    /**
     * Returns the logical world name.
     *
     * @return world name
     */
    public String worldName() {
        return worldName;
    }

    /**
     * Returns the X coordinate.
     *
     * @return X coordinate
     */
    public double x() {
        return x;
    }

    /**
     * Returns the Y coordinate.
     *
     * @return Y coordinate
     */
    public double y() {
        return y;
    }

    /**
     * Returns the Z coordinate.
     *
     * @return Z coordinate
     */
    public double z() {
        return z;
    }

    /**
     * Returns the yaw.
     *
     * @return yaw
     */
    public float yaw() {
        return yaw;
    }

    /**
     * Returns the pitch.
     *
     * @return pitch
     */
    public float pitch() {
        return pitch;
    }

    /**
     * Creates a defensive Bukkit location for the supplied loaded world.
     *
     * @param world loaded Bukkit world
     * @return new mutable location
     */
    public Location toLocation(World world) {
        return new Location(Objects.requireNonNull(world, "world"), x, y, z, yaw, pitch);
    }

    private static String requireWorldName(String value) {
        String name = Objects.requireNonNull(value, "worldName").trim();
        if (name.isEmpty()) throw new IllegalArgumentException("worldName cannot be blank");
        return name;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
