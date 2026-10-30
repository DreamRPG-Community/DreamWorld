package cn.mythicland.dreamworldprotect.command;

import cn.mythicland.dreamlib.bootstrap.annotation.CommandComponent;
import cn.mythicland.dreamlib.bootstrap.annotation.CommandHandler;
import cn.mythicland.dreamlib.command.CommandContext;
import cn.mythicland.dreamlib.command.VanillaCommandMessages;
import cn.mythicland.dreamworldprotect.bootstrap.WorldProtectLifecycle;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Toggles temporary building mode for a player.
 */
@CommandComponent("edit")
public final class EditCommand {

    private static final String ONLY_PLAYER_MESSAGE = VanillaCommandMessages.red("该命令只能由玩家执行。");
    private static final String ENABLED_MESSAGE = VanillaCommandMessages.green("建筑模式已开启。");
    private static final String DISABLED_MESSAGE = VanillaCommandMessages.red("建筑模式已关闭。");
    private static final String SAVE_REMINDER_TEMPLATE =
            VanillaCommandMessages.yellow(
                    "检测到您安装了 DreamWorld 插件, 请在重启前保存您的地图改动。 \n"
                            + "使用 /dreamworld save %world% 以保存世界更改。"
            );

    private final WorldProtectLifecycle lifecycle;

    public EditCommand(WorldProtectLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    /**
     * Supplies the configurable edit permission to Lib's command router.
     *
     * @return current edit permission
     */
    public String editPermission() {
        return lifecycle.settings().editPermission();
    }

    @CommandHandler(permissionMethod = "editPermission")
    void edit(CommandContext context) {
        context.requireArguments(0);
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(ONLY_PLAYER_MESSAGE);
            return;
        }

        boolean enabled = lifecycle.editModes().toggle(player.getUniqueId());
        player.sendMessage(enabled ? ENABLED_MESSAGE : DISABLED_MESSAGE);
        if (enabled && lifecycle.worldManager().isEnabled()) {
            String worldName = lifecycle.worldConfigs().logicalName(player.getWorld());
            player.sendMessage(SAVE_REMINDER_TEMPLATE.replace("%world%", worldName));
        }
    }
}
