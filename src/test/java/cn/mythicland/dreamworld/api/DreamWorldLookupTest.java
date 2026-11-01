package cn.mythicland.dreamworld.api;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DreamWorldLookupTest {
    @Test
    void resolvesLogicalNamesAndLoadsManagedWorldsThroughTheRegisteredService() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        ServicesManager services = mock(ServicesManager.class);
        Plugin manager = mock(Plugin.class);
        DreamWorldApi api = mock(DreamWorldApi.class);
        @SuppressWarnings("unchecked") RegisteredServiceProvider<DreamWorldApi> registration = mock(RegisteredServiceProvider.class);
        World world = mock(World.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(plugins);
        when(server.getServicesManager()).thenReturn(services);
        when(plugins.getPlugin("DreamWorld")).thenReturn(manager);
        when(manager.isEnabled()).thenReturn(true);
        when(services.getRegistration(DreamWorldApi.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(api);
        when(api.findLogicalName(world)).thenReturn(Optional.of("main_city"));
        when(api.find("main_city")).thenReturn(Optional.empty());
        when(api.load("main_city")).thenReturn(CompletableFuture.completedFuture(world));
        DreamWorldLookup lookup = new DreamWorldLookup(plugin);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("main_city")).thenReturn(null);
            assertEquals("main_city", lookup.logicalNameOrBukkitName(world));
            assertSame(world, lookup.resolveWorld("main_city").join());
            when(manager.isEnabled()).thenReturn(false);
            when(world.getName()).thenReturn("world");
            assertEquals("world", lookup.logicalNameOrBukkitName(world));
            assertTrue(lookup.resolveWorld("main_city").isCompletedExceptionally());
        }
    }
}
