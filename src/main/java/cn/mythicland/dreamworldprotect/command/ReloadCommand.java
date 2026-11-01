package cn.mythicland.dreamworldprotect.command;

import cn.mythicland.dreamworld.DreamWorldPlugin;
import cn.mythicland.framework.bukkit.command.CommandBinding;
import cn.mythicland.framework.bukkit.command.CommandContext;
import cn.mythicland.framework.bukkit.command.CommandRoute;
import cn.mythicland.framework.bukkit.command.VanillaCommandMessages;

import java.util.Objects;

/**
 * Reloads the global and per-world WorldProtect configuration.
 */
@CommandBinding("dreamworldprotect")
public final class ReloadCommand {

    private static final String ADMIN_PERMISSION = "dreamworld.protect.admin";
    private static final String SUCCESS_MESSAGE = VanillaCommandMessages.green(
            "DreamWorld 保护配置已重载。"
    );

    private final DreamWorldPlugin plugin;

    public ReloadCommand(DreamWorldPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @CommandRoute(path = {"dreamworldprotect", "reload"}, maxArguments = Integer.MAX_VALUE, permission = ADMIN_PERMISSION)
    void reload(CommandContext context) {
        context.requireArguments(0);
        plugin.reloadWorldProtect();
        context.sender().sendMessage(SUCCESS_MESSAGE);
    }
}
