package com.leonardobishop.quests.bukkit.battlepass.menu;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassConfig;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassTiers;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.menu.QMenu;
import com.leonardobishop.quests.bukkit.menu.element.MenuElement;
import com.leonardobishop.quests.bukkit.menu.element.SpacerMenuElement;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.quest.Quest;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * The battle pass GUI. One tier per page; quests in that tier are laid out in a single 7-slot row
 * with side padding. Bottom row hosts navigation between tiers and a premium status indicator.
 */
public class BattlePassQMenu extends QMenu {

    private static final int[] QUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int PREV_TIER_SLOT = 45;
    private static final int PREMIUM_INFO_SLOT = 49;
    private static final int NEXT_TIER_SLOT = 53;
    private static final int INVENTORY_SIZE = 54;

    private final BukkitQuestsPlugin plugin;
    private final BukkitQuestsConfig config;
    private int currentTier;
    private final int maxTier;

    public BattlePassQMenu(BukkitQuestsPlugin plugin, QPlayer owner) {
        super(owner);
        this.plugin = plugin;
        this.config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        this.maxTier = Math.max(1, BattlePassTiers.getMaxTier(plugin));
        this.currentTier = Math.max(1, Math.min(maxTier, BattlePassTiers.getHighestUnlockedTier(plugin, owner)));
        rebuildElements();
    }

    public void setCurrentTier(int tier) {
        this.currentTier = Math.max(1, Math.min(maxTier, tier));
        rebuildElements();
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public int getMaxTier() {
        return maxTier;
    }

    private void rebuildElements() {
        menuElements.clear();
        SpacerMenuElement spacer = new SpacerMenuElement();

        // Fill the entire inventory with spacers as the default backdrop, then overlay quest tiles
        // and nav buttons. Server admins can override individual slots via custom config later.
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            menuElements.put(slot, spacer);
        }

        List<Quest> tierQuests = BattlePassTiers.getQuestsInTier(plugin, currentTier);
        for (int i = 0; i < QUEST_SLOTS.length; i++) {
            int slot = QUEST_SLOTS[i];
            if (i < tierQuests.size()) {
                menuElements.put(slot, new BattlePassQuestMenuElement(plugin, owner, tierQuests.get(i)));
            } else {
                menuElements.put(slot, spacer);
            }
        }

        if (currentTier > 1) {
            menuElements.put(PREV_TIER_SLOT, new BattlePassTierNavMenuElement(plugin, this, false));
        }
        if (currentTier < maxTier) {
            menuElements.put(NEXT_TIER_SLOT, new BattlePassTierNavMenuElement(plugin, this, true));
        }
        menuElements.put(PREMIUM_INFO_SLOT, new BattlePassPremiumInfoMenuElement(plugin, owner));
    }

    @Override
    public Inventory draw() {
        String configuredTitle = config.getString("battle-pass.gui.title", "&8Battle Pass — Tier {tier}");
        String title = Chat.legacyColor(configuredTitle.replace("{tier}", String.valueOf(currentTier)));
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            MenuElement element = menuElements.get(slot);
            if (element != null) {
                inventory.setItem(slot, element.asItemStack());
            }
        }
        return inventory;
    }

    public BukkitQuestsPlugin getPlugin() {
        return plugin;
    }

    public boolean playerHasPremium() {
        return BattlePassConfig.hasPremium(config, plugin.getServer().getPlayer(owner.getPlayerUUID()));
    }
}
