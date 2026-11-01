package cn.mythicland.dreamworldprotect.listener;

import cn.mythicland.dreamworld.api.DreamWorldLookup;
import cn.mythicland.dreamworldprotect.policy.WorldProtectionRules;
import cn.mythicland.dreamworldprotect.service.EditModeTracker;
import cn.mythicland.dreamworldprotect.storage.WorldConfigStore;
import cn.mythicland.framework.bukkit.command.VanillaCommandMessages;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Applies the cached policy for each world to Bukkit events.
 *
 * <p>Bukkit discovers the public event methods reflectively through {@link EventHandler}, so
 * IntelliJ cannot see their call sites.</p>
 */
@SuppressWarnings("unused")
public final class WorldProtectListener implements Listener {

    private static final String ACTION_DENIED_MESSAGE = VanillaCommandMessages.red("该世界禁止此操作。");

    private final WorldConfigStore worldConfigs;
    private final EditModeTracker editModes;
    private final DreamWorldLookup worldManager;

    public WorldProtectListener(
            WorldConfigStore worldConfigs,
            EditModeTracker editModes,
            DreamWorldLookup worldManager
    ) {
        this.worldConfigs = worldConfigs;
        this.editModes = editModes;
        this.worldManager = worldManager;
    }

    private static boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        worldConfigs.load(
                event.getWorld(),
                worldManager.logicalNameOrBukkitName(event.getWorld())
        );
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        worldConfigs.unload(event.getWorld());
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockPlace(BlockPlaceEvent event) {
        WorldProtectionRules rules = worldConfigs.get(event.getBlock().getWorld());
        if (!rules.blockPlace() || editModes.isEnabled(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {
        WorldProtectionRules rules = worldConfigs.get(event.getBlock().getWorld());
        if (!rules.blockBreak() || editModes.isEnabled(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onDecorationDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Hanging) && !(event.getEntity() instanceof ArmorStand)) return;
        if (!worldConfigs.get(event.getEntity().getWorld()).blockBreak()) return;
        Player actor = null;
        if (event instanceof EntityDamageByEntityEvent damage) {
            if (damage.getDamager() instanceof Player player) actor = player;
            else if (damage.getDamager() instanceof Projectile projectile &&
                    projectile.getShooter() instanceof Player player)
                actor = player;
        }
        if (actor != null && editModes.isEnabled(actor.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onHangingBreak(HangingBreakEvent event) {
        if (!worldConfigs.get(event.getEntity().getWorld()).blockBreak()) return;
        if (event instanceof HangingBreakByEntityEvent damage && damage.getRemover() instanceof Player player
                && editModes.isEnabled(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onDecorationInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Hanging) && !(event.getRightClicked() instanceof ArmorStand)) return;
        if (!worldConfigs.get(event.getRightClicked().getWorld()).blockBreak() ||
                editModes.isEnabled(event.getPlayer().getUniqueId()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!worldConfigs.get(event.getRightClicked().getWorld()).blockBreak() ||
                editModes.isEnabled(event.getPlayer().getUniqueId()))
            return;
        event.setCancelled(true);
    }

    // Numeric block IDs match the block identifiers accepted by the configuration.
    @SuppressWarnings("deprecation")
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        WorldProtectionRules rules = worldConfigs.get(event.getPlayer().getWorld());
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !rules.blocksInteraction(clickedBlock.getTypeId())) {
            return;
        }
        denyAction(event.getPlayer());
        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketFill(PlayerBucketFillEvent event) {
        denyBucket(event.getPlayer(), event.getPlayer().getWorld(), event);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        denyBucket(event.getPlayer(), event.getPlayer().getWorld(), event);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (worldConfigs.get(event.getBlock().getWorld()).leafDecay()) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockFade(BlockFadeEvent event) {
        if (worldConfigs.get(event.getBlock().getWorld()).blockFade()) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockIgnite(BlockIgniteEvent event) {
        WorldProtectionRules rules = worldConfigs.get(event.getBlock().getWorld());
        boolean protectedCause = event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD
                ? rules.fireSpread()
                : rules.blockIgnite();
        if (protectedCause) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE
                && worldConfigs.get(event.getBlock().getWorld()).fireSpread()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBurn(BlockBurnEvent event) {
        if (worldConfigs.get(event.getBlock().getWorld()).fireSpread()) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityExplode(EntityExplodeEvent event) {
        if (worldConfigs.get(event.getLocation().getWorld()).explosions()) event.blockList().clear();
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockExplode(BlockExplodeEvent event) {
        if (worldConfigs.get(event.getBlock().getWorld()).explosions()) event.blockList().clear();
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isNaturalSpawn(event.getSpawnReason())) return;
        if (worldConfigs.get(event.getLocation().getWorld()).naturalMobSpawning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER
                || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (worldConfigs.get(event.getEntity().getWorld()).fallDamage()) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.ENDER_PEARL
                || !(event.getEntity().getShooter() instanceof Player player)) return;
        if (worldConfigs.get(player.getWorld()).enderPearl()) event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCommand(PlayerCommandPreprocessEvent event) {
        WorldProtectionRules rules = worldConfigs.get(event.getPlayer().getWorld());
        String command = commandName(event.getMessage());
        if (command.isBlank() || command.equals("edit") || !rules.blocksCommand(command)) return;
        denyAction(event.getPlayer());
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        editModes.remove(event.getPlayer().getUniqueId());
    }

    private void denyBucket(Player player, World world, Cancellable event) {
        if (!worldConfigs.get(world).bucket()) return;
        denyAction(player);
        event.setCancelled(true);
    }

    private void denyAction(Player player) {
        player.sendMessage(ACTION_DENIED_MESSAGE);
    }

    private String commandName(String message) {
        if (message == null || !message.startsWith("/")) return "";
        String body = message.substring(1).trim();
        if (body.isBlank()) return "";
        return body.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }
}
