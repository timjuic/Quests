package com.leonardobishop.quests.bukkit.command;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassConfig;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassTiers;
import com.leonardobishop.quests.bukkit.battlepass.menu.BattlePassQMenu;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.util.Messages;
import com.leonardobishop.quests.common.player.QPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BattlePassCommandHandler implements CommandHandler {

    private final BukkitQuestsPlugin plugin;

    public BattlePassCommandHandler(BukkitQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }
        if (!BattlePassConfig.isEnabled((BukkitQuestsConfig) plugin.getQuestsConfig())) {
            Messages.COMMAND_BATTLEPASS_DISABLED.send(sender);
            return;
        }
        if (BattlePassTiers.getMaxTier(plugin) <= 0) {
            Messages.COMMAND_BATTLEPASS_NO_QUESTS.send(sender);
            return;
        }
        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            Messages.COMMAND_DATA_NOT_LOADED.send(player);
            return;
        }
        BattlePassQMenu menu = new BattlePassQMenu(plugin, qPlayer);
        plugin.getMenuController().openMenu(player.getUniqueId(), menu);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable String getPermission() {
        return "quests.command.battlepass";
    }
}
