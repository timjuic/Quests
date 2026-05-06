package com.leonardobishop.quests.bukkit.battlepass.menu;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassConfig;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.menu.ClickResult;
import com.leonardobishop.quests.bukkit.menu.element.MenuElement;
import com.leonardobishop.quests.bukkit.util.MenuUtils;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.common.player.QPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BattlePassPremiumInfoMenuElement extends MenuElement {

    private final BukkitQuestsPlugin plugin;
    private final BukkitQuestsConfig config;
    private final QPlayer owner;

    public BattlePassPremiumInfoMenuElement(BukkitQuestsPlugin plugin, QPlayer owner) {
        this.plugin = plugin;
        this.config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        this.owner = owner;
    }

    @Override
    public ItemStack asItemStack() {
        Player player = plugin.getServer().getPlayer(owner.getPlayerUUID());
        boolean hasPremium = BattlePassConfig.hasPremium(config, player);

        String configRoot = hasPremium ? "battle-pass.gui.premium-active" : "battle-pass.gui.premium-inactive";
        if (config.getConfig().isConfigurationSection(configRoot)) {
            return MenuUtils.applyPlaceholders(plugin, owner.getPlayerUUID(),
                    plugin.getConfiguredItemStack(configRoot, config.getConfig()));
        }

        ItemStack stack = new ItemStack(hasPremium ? Material.NETHER_STAR : Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.legacyColor(hasPremium ? "&d&lPremium Battle Pass" : "&7&lFree Battle Pass"));
            List<String> lore = new ArrayList<>();
            if (hasPremium) {
                lore.add(Chat.legacyColor("&7You have premium access."));
                lore.add(Chat.legacyColor("&7Premium rewards unlocked!"));
            } else {
                lore.add(Chat.legacyColor("&7You are using the free pass."));
                lore.add(Chat.legacyColor("&7Buy premium to unlock extra rewards"));
                lore.add(Chat.legacyColor("&7on every quest."));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public ClickResult handleClick(Player whoClicked, ClickType clickType) {
        return ClickResult.DO_NOTHING;
    }
}
