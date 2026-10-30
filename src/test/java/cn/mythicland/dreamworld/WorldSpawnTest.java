package cn.mythicland.dreamworld;

import cn.mythicland.dreamworld.api.WorldSpawn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldSpawnTest {

    @Test
    void keepsAnImmutableCoordinateSnapshot() {
        WorldSpawn spawn = new WorldSpawn("dungeon", 1.5D, 64.0D, -2.5D, 90.0F, 0.0F);

        assertEquals("dungeon", spawn.worldName());
        assertEquals(1.5D, spawn.x());
        assertEquals(64.0D, spawn.y());
        assertEquals(-2.5D, spawn.z());
        assertEquals(90.0F, spawn.yaw());
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorldSpawn("dungeon", Double.NaN, 64.0D, 0.0D, 0.0F, 0.0F)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorldSpawn(" ", 0.0D, 64.0D, 0.0D, 0.0F, 0.0F)
        );
    }

    @Test
    void configurationAndApiValuesAlwaysUseCenteredHorizontalSpawns() {
        WorldSpawn spawn = new WorldSpawn("dungeon", -1.2D, 64.75D, 2.8D, 67.0F, -68.0F);
        assertEquals(-1.5D, spawn.x());
        assertEquals(64.0D, spawn.y());
        assertEquals(2.5D, spawn.z());
        assertEquals(45.0F, spawn.yaw());
        assertEquals(0.0F, spawn.pitch());
    }
}
