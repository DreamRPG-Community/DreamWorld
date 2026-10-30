package cn.mythicland.dreamworldprotect.command;

import cn.mythicland.dreamlib.bootstrap.annotation.CommandComponent;
import cn.mythicland.dreamlib.bootstrap.annotation.CommandHandler;
import cn.mythicland.dreamlib.command.CommandContext;
import cn.mythicland.dreamlib.command.VanillaCommandMessages;
import cn.mythicland.dreamworld.DreamWorldPlugin;

import java.util.Objects;

/**
 * Reloads the global and per-world WorldProtect configuration.
 */
@CommandComponent("dreamworldprotect")
public final class ReloadCommand {

    private static final String ADMIN_PERMISSION = "dreamworld.protect.admin";
    private static final String SUCCESS_MESSAGE = VanillaCommandMessages.green(
            "DreamWorld 保护配置已重载。"
    );

    private final DreamWorldPlugin plugin;

    public ReloadCommand(DreamWorldPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @CommandHandler(value = "reload", permission = ADMIN_PERMISSION)
    void reload(CommandContext context) {
        context.requireArguments(0);
        plugin.reloadWorldProtect();
        context.sender().sendMessage(SUCCESS_MESSAGE);
    }
}
