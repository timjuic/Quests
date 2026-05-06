package com.leonardobishop.quests.bukkit.battlepass.menu;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.menu.ClickResult;
import com.leonardobishop.quests.bukkit.menu.element.MenuElement;
import com.leonardobishop.quests.bukkit.util.MenuUtils;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattlePassTierNavMenuElement extends MenuElement {

    private final BukkitQuestsPlugin plugin;
    private final BukkitQuestsConfig config;
    private final BattlePassQMenu menu;
    private final boolean next;

    public BattlePassTierNavMenuElement(BukkitQuestsPlugin plugin, BattlePassQMenu menu, boolean next) {
        this.plugin = plugin;
        this.config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        this.menu = menu;
        this.next = next;
    }

    @Override
    public ItemStack asItemStack() {
        int targetTier = next ? menu.getCurrentTier() + 1 : menu.getCurrentTier() - 1;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{tier}", String.valueOf(targetTier));

        String configRoot = next ? "battle-pass.gui.next-tier-button" : "battle-pass.gui.prev-tier-button";
        ItemStack stack;
        if (config.getConfig().isConfigurationSection(configRoot)) {
            stack = plugin.getConfiguredItemStack(configRoot, config.getConfig());
        } else {
            stack = new ItemStack(Material.ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Chat.legacyColor(next ? "&aNext Tier &7({tier})" : "&aPrevious Tier &7({tier})").replace("{tier}", String.valueOf(targetTier)));
                meta.setLore(List.of(Chat.legacyColor("&7Click to view tier &c" + targetTier + "&7.")));
                stack.setItemMeta(meta);
            }
        }
        return MenuUtils.applyPlaceholders(plugin, menu.getOwner().getPlayerUUID(), stack, placeholders);
    }

    @Override
    public ClickResult handleClick(Player whoClicked, ClickType clickType) {
        menu.setCurrentTier(next ? menu.getCurrentTier() + 1 : menu.getCurrentTier() - 1);
        return ClickResult.REFRESH_PANE;
    }
}
