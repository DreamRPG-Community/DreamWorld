package cn.mythicland.dreamworld;

import cn.mythicland.dreamlib.api.DreamLibApi;
import cn.mythicland.framework.bukkit.player.StartupJoinGate;
import cn.mythicland.framework.bukkit.task.PluginTaskScope;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DreamWorldStartupTest {
    @Test
    void admitsPlayersOnlyAfterAllStartupWorldsFinishAndClosesOnUnload() {
        verifyStartup(true);
    }

    @Test
    void failedWorldRestoreKeepsPlayersOut() {
        verifyStartup(false);
    }

    private static void verifyStartup(boolean succeeds) {
        DreamWorldPlugin plugin = mock(DreamWorldPlugin.class);
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getPluginManager()).thenReturn(plugins);
        PluginTaskScope tasks = mock(PluginTaskScope.class);
        DreamWorldConfiguration configuration = mock(DreamWorldConfiguration.class);
        when(configuration.snapshot()).thenReturn(mock(DreamWorldSettings.class));
        CompletableFuture<Void> worlds = new CompletableFuture<>();
        try (var settings = mockStatic(DreamWorldSettingsLoader.class);
             var services = mockConstruction(DreamWorldService.class, (service, ignored) ->
                     when(service.discoverAndLoadAll()).thenReturn(worlds))) {
            DreamWorldLifecycle lifecycle = new DreamWorldLifecycle(plugin, mock(DreamLibApi.class), tasks,
                    configuration, mock(WorldSpawnStore.class));
            lifecycle.load();
            ArgumentCaptor<Listener> listener = ArgumentCaptor.forClass(Listener.class);
            verify(plugins).registerEvents(listener.capture(), eq(plugin));
            StartupJoinGate gate = (StartupJoinGate) listener.getValue();
            assertFalse(gate.isAcceptingJoins());
            ArgumentCaptor<Runnable> start = ArgumentCaptor.forClass(Runnable.class);
            verify(tasks).runLater(eq(1L), start.capture());
            start.getValue().run();
            assertFalse(gate.isAcceptingJoins());
            if (succeeds) worlds.complete(null);
            else worlds.completeExceptionally(new IllegalStateException("World restore failed"));
            assertEquals(succeeds, gate.isAcceptingJoins());
            lifecycle.unload();
            assertFalse(gate.isAcceptingJoins());
            verify(services.constructed().getFirst()).close();
        }
    }
}
