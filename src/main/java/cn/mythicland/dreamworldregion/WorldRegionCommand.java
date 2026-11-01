package cn.mythicland.dreamworldregion;

import cn.mythicland.dreamlib.api.DreamLibApi;
import cn.mythicland.dreamworld.DreamWorldPlugin;
import cn.mythicland.dreamworldregion.api.DreamWorldRegionApi;
import cn.mythicland.dreamworldregion.api.LandmarkDefinition;
import cn.mythicland.dreamworldregion.api.RegionBounds;
import cn.mythicland.dreamworldregion.api.RegionDefinition;
import cn.mythicland.framework.bukkit.command.CommandBinding;
import cn.mythicland.framework.bukkit.command.CommandCompleter;
import cn.mythicland.framework.bukkit.command.CommandContext;
import cn.mythicland.framework.bukkit.command.CommandRoute;
import cn.mythicland.framework.bukkit.command.CommandUsageException;
import cn.mythicland.framework.bukkit.location.LocationSnapper;
import cn.mythicland.framework.bukkit.text.LegacyText;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles WorldRegion's administrative command tree.
 */
@CommandBinding(value = "dreamworldregion", permission = "dreamworld.region.admin")
final class WorldRegionCommand {

    private static final String ROOT = "/dreamworld region";
    private static final String LANDMARK_USAGE = ROOT + " landmark set <id> <显示名>\n"
            + ROOT + " landmark delete <id>";

    private final DreamWorldPlugin plugin;
    private final DreamLibApi lib;
    private final DreamWorldRegionApi service;
    private final WorldRegionDataStore dataStore;
    private final RegionSelectionService selections;
    private final DreamWorldIntegration worldManager;

    WorldRegionCommand(
            DreamWorldPlugin plugin,
            DreamLibApi lib,
            DreamWorldRegionApi service,
            WorldRegionDataStore dataStore,
            RegionSelectionService selections,
            DreamWorldIntegration worldManager
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lib = Objects.requireNonNull(lib, "lib");
        this.service = Objects.requireNonNull(service, "service");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.selections = Objects.requireNonNull(selections, "selections");
        this.worldManager = Objects.requireNonNull(worldManager, "worldManager");
    }

    private static ParsedDefinition parse(List<String> arguments) {
        String id = arguments.getFirst();
        List<String> nameParts = new ArrayList<>(arguments.subList(1, arguments.size()));
        int priority = 0;
        if (nameParts.size() > 1) {
            String last = nameParts.getLast();
            if (isIntegerLiteral(last)) {
                try {
                    priority = Integer.parseInt(last);
                    nameParts.removeLast();
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("区域优先级超出整数范围: " + last, exception);
                }
            }
        }
        String displayName = String.join(" ", nameParts).trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "区域显示名不能为空。\n" + ROOT + " create <id> <显示名> [priority]"
            );
        }
        return new ParsedDefinition(id, displayName, priority);
    }

    private static boolean isIntegerLiteral(String value) {
        return value.matches("[+-]?\\d+");
    }

    private static String format(Location location) {
        return location.getWorld().getName()
                + " ("
                + location.getBlockX()
                + ", "
                + location.getBlockY()
                + ", "
                + location.getBlockZ()
                + ")";
    }

    @CommandRoute(
        path = {"dreamworldregion", "pos1"},
        maxArguments = Integer.MAX_VALUE
    )
    void positionOne(CommandContext context) {
        setPosition(context, true);
    }

    @CommandRoute(
        path = {"dreamworldregion", "pos2"},
        maxArguments = Integer.MAX_VALUE
    )
    void positionTwo(CommandContext context) {
        setPosition(context, false);
    }

    @CommandRoute(
        path = {"dreamworldregion", "clear"},
        maxArguments = Integer.MAX_VALUE
    )
    void clear(CommandContext context) {
        context.requireArguments(0);
        if (context.sender() instanceof Player player) selections.clear(player.getUniqueId());
        context.sender().sendMessage(LegacyText.colorize("&a已清除当前区域选区。"));
    }

    @CommandRoute(
        path = {"dreamworldregion", "create"},
        maxArguments = Integer.MAX_VALUE,
        usages = ROOT + " create <id> <显示名> [priority]"
    )
    void create(CommandContext context) {
        context.requireAtLeast(2);
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(LegacyText.colorize("&c该命令只能由玩家执行。"));
            return;
        }
        RegionSelectionService.Selection selection = selections.find(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("请先使用木铲设置两个区域角点。"));
        Location first = selection.first();
        Location second = selection.second();
        if (first == null || second == null) {
            throw new IllegalStateException("请先使用木铲设置两个区域角点。\n" + context.usage());
        }
        if (!Objects.requireNonNull(first.getWorld(), "first.world")
                .equals(Objects.requireNonNull(second.getWorld(), "second.world"))) {
            throw new IllegalStateException("区域两个角点必须位于同一世界。");
        }
        ParsedDefinition parsed = parse(context.arguments());
        RegionDefinition definition = new RegionDefinition(
                parsed.id(),
                parsed.displayName(),
                parsed.priority(),
                worldManager.logicalNameOrBukkitName(first.getWorld()),
                RegionBounds.between(first, second)
        );
        dataStore.createRegion(definition);
        selections.clear(player.getUniqueId());
        context.sender().sendMessage(LegacyText.colorize("&a区域已保存: " + definition.id()));
    }

    @CommandRoute(
        path = {"dreamworldregion", "delete"},
        maxArguments = Integer.MAX_VALUE,
        usages = ROOT + " delete <id>"
    )
    void delete(CommandContext context) {
        context.requireArguments(1);
        String id = context.argument(0);
        dataStore.deleteRegion(id);
        context.sender().sendMessage(LegacyText.colorize("&a区域已删除: " + id));
    }

    @CommandRoute(
        path = {"dreamworldregion", "list"},
        maxArguments = Integer.MAX_VALUE
    )
    void list(CommandContext context) {
        context.requireArguments(0);
        Collection<RegionDefinition> regions = service.regions();
        if (regions.isEmpty()) {
            context.sender().sendMessage(LegacyText.colorize("&c当前没有保存区域。"));
            return;
        }
        for (RegionDefinition region : regions) {
            context.sender().sendMessage(LegacyText.colorize(
                    "&a" + region.id() + " - " + region.displayName()
                            + " &7(priority=" + region.priority() + ")"
            ));
        }
    }

    @CommandRoute(
        path = {"dreamworldregion", "landmark"},
        maxArguments = Integer.MAX_VALUE,
        usages = LANDMARK_USAGE
    )
    void landmark(CommandContext context) {
        context.requireAtLeast(1);
        switch (context.argument(0).toLowerCase(Locale.ROOT)) {
            case "set" -> setLandmark(context, context.arguments().subList(1, context.arguments().size()));
            case "delete" -> deleteLandmark(context, context.arguments().subList(1, context.arguments().size()));
            default -> throw context.invalidUsage();
        }
    }

    @CommandCompleter("landmark")
    List<String> completeLandmark(CommandContext context) {
        if (context.arguments().size() == 1) {
            String prefix = context.argument(0).toLowerCase(Locale.ROOT);
            return java.util.stream.Stream.of("set", "delete")
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }
        if (context.arguments().size() == 2 && context.argument(0).equalsIgnoreCase("delete")) {
            String prefix = context.argument(1).toLowerCase(Locale.ROOT);
            return service.landmarks().stream()
                    .map(LandmarkDefinition::id)
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    @CommandRoute(
            path = {"dreamworldregion", "teleport"},
            maxArguments = Integer.MAX_VALUE,
            aliases = {"tp"},
            usages = ROOT + " teleport <地标id>",
            permission = "",
            inheritPermission = false
    )
    void teleport(CommandContext context) {
        context.requireArguments(1);
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(LegacyText.colorize("&c该命令只能由玩家执行。"));
            return;
        }
        String id = context.argument(0);
        context.sender().sendMessage(LegacyText.colorize("&a正在传送到地标: " + id));
        service.teleport(player, id).whenComplete((success, error) ->
                lib.runOnMain(() -> {
                    if (error != null) {
                        context.sender().sendMessage(LegacyText.colorize(
                                "&c地标传送失败: " + DreamLibApi.rootCauseMessage(error)
                        ));
                        return;
                    }
                    context.sender().sendMessage(Boolean.TRUE.equals(success)
                            ? LegacyText.colorize("&a已传送到地标: " + id)
                            : LegacyText.colorize("&c地标传送被 Bukkit 拒绝: " + id));
                }).whenComplete((ignored, notificationFailure) -> {
                    if (notificationFailure != null) {
                        plugin.getLogger().log(
                                Level.SEVERE,
                                "Failed to report DreamWorld region landmark teleport result: " + id,
                                notificationFailure
                        );
                    }
                })
        );
    }

    @CommandCompleter("teleport")
    List<String> completeTeleport(CommandContext context) {
        if (context.arguments().size() != 1) return List.of();
        String prefix = context.argument(0).toLowerCase(Locale.ROOT);
        return service.landmarks().stream()
                .map(LandmarkDefinition::id)
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    @CommandRoute(
        path = {"dreamworldregion", "reload"},
        maxArguments = Integer.MAX_VALUE
    )
    void reload(CommandContext context) {
        context.requireArguments(0);
        plugin.reloadWorldRegion();
        context.sender().sendMessage(LegacyText.colorize("&aDreamWorld 区域配置已重载。"));
    }

    private void setPosition(CommandContext context, boolean first) {
        context.requireArguments(0);
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(LegacyText.colorize("&c该命令只能由玩家执行。"));
            return;
        }
        Location location = player.getLocation().getBlock().getLocation();
        if (first) selections.setFirst(player.getUniqueId(), location);
        else selections.setSecond(player.getUniqueId(), location);
        player.sendMessage(LegacyText.colorize(
                "&a已设置区域" + (first ? "第一" : "第二") + "角: " + format(location)
        ));
    }

    private void setLandmark(CommandContext context, List<String> arguments) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(LegacyText.colorize("&c该命令只能由玩家执行。"));
            return;
        }
        if (arguments.size() < 2) {
            throw new CommandUsageException(ROOT + " landmark set <id> <显示名>");
        }
        String displayName = String.join(" ", arguments.subList(1, arguments.size())).trim();
        Location location = LocationSnapper.snapBlockAndHorizontalView(player.getLocation());
        LandmarkDefinition definition = new LandmarkDefinition(
                arguments.getFirst(),
                displayName,
                worldManager.logicalNameOrBukkitName(Objects.requireNonNull(location.getWorld(), "player.world")),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        dataStore.createLandmark(definition);
        context.sender().sendMessage(LegacyText.colorize("&a地标已保存: " + definition.id()));
    }

    private void deleteLandmark(CommandContext context, List<String> arguments) {
        if (arguments.size() != 1) {
            throw new CommandUsageException(ROOT + " landmark delete <id>");
        }
        String id = arguments.getFirst();
        dataStore.deleteLandmark(id);
        context.sender().sendMessage(LegacyText.colorize("&a地标已删除: " + id));
    }

    private record ParsedDefinition(String id, String displayName, int priority) {
    }
}
