package com.leonardobishop.quests.bukkit.battlepass.menu;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassConfig;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassRewardDispatcher;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassTiers;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.menu.ClickResult;
import com.leonardobishop.quests.bukkit.menu.element.MenuElement;
import com.leonardobishop.quests.bukkit.menu.itemstack.QItemStack;
import com.leonardobishop.quests.bukkit.util.MenuUtils;
import com.leonardobishop.quests.bukkit.util.Messages;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.common.enums.QuestStartResult;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.quest.Quest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One quest tile in the battle pass tier row. Renders a status-specific icon and dispatches the
 * appropriate action on click: start the quest, claim rewards, or show a status message.
 */
public class BattlePassQuestMenuElement extends MenuElement {

    private final BukkitQuestsPlugin plugin;
    private final BukkitQuestsConfig config;
    private final QPlayer owner;
    private final Quest quest;

    public BattlePassQuestMenuElement(BukkitQuestsPlugin plugin, QPlayer owner, Quest quest) {
        this.plugin = plugin;
        this.config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        this.owner = owner;
        this.quest = quest;
    }

    public Quest getQuest() {
        return quest;
    }

    private boolean hasPremiumReward() {
        return !quest.getPremiumRewards().isEmpty() || quest.getPremiumVaultReward() != null;
    }

    private State currentState(Player player) {
        if (!BattlePassTiers.isTierUnlocked(plugin, owner, quest.getTier())) {
            return State.TIER_LOCKED;
        }
        QuestProgress progress = owner.getQuestProgressFile().getQuestProgressOrNull(quest);
        boolean completed = progress != null && progress.isCompletedBefore();
        if (!completed) {
            QuestStartResult result = owner.canStartQuest(quest);
            if (result == QuestStartResult.QUEST_ALREADY_STARTED || (progress != null && progress.isStarted())) {
                return State.IN_PROGRESS;
            }
            return State.NOT_STARTED;
        }
        boolean freeClaimed = progress.isFreeRewardClaimed();
        boolean premiumClaimed = progress.isPremiumRewardClaimed();
        boolean hasPremium = BattlePassConfig.hasPremium(config, player);
        boolean premiumExists = hasPremiumReward();

        boolean premiumDone = premiumClaimed || !premiumExists;
        if (freeClaimed && premiumDone) {
            return State.FULLY_CLAIMED;
        }
        if (!freeClaimed) {
            // Free is the priority claim; once claimed, the tile may flip to premium-claim.
            return State.CLAIMABLE_FREE;
        }
        // free is claimed, premium exists and is not claimed
        return hasPremium ? State.CLAIMABLE_PREMIUM : State.PREMIUM_LOCKED;
    }

    @Override
    public ItemStack asItemStack() {
        Player player = plugin.getServer().getPlayer(owner.getPlayerUUID());
        State state = currentState(player);

        Map<String, String> placeholders = new HashMap<>();
        QItemStack questItem = plugin.getQItemStackRegistry().getQuestItemStack(quest);
        String displayName = questItem.getName();
        placeholders.put("{quest}", Chat.legacyStrip(displayName));
        placeholders.put("{questcolored}", displayName);
        placeholders.put("{questid}", quest.getId());
        placeholders.put("{tier}", String.valueOf(quest.getTier()));

        String configRoot = "battle-pass.tile-states." + state.configKey();
        ItemStack base;
        if (config.getConfig().isConfigurationSection(configRoot)) {
            base = plugin.getConfiguredItemStack(configRoot, config.getConfig());
        } else {
            base = fallbackItem(state, displayName);
        }
        return MenuUtils.applyPlaceholders(plugin, owner.getPlayerUUID(), base, placeholders);
    }

    private ItemStack fallbackItem(State state, String questDisplayName) {
        // Material names changed across MC versions (the 1.13 "flatten"). Each branch tries the
        // modern name first and falls back to the legacy 1.8 name so this works on both.
        Material material = switch (state) {
            case TIER_LOCKED -> safeMaterial("BARRIER");
            case NOT_STARTED -> safeMaterial("BOOK");
            case IN_PROGRESS -> safeMaterial("WRITABLE_BOOK", "BOOK_AND_QUILL");
            case CLAIMABLE_FREE -> safeMaterial("CHEST");
            case CLAIMABLE_PREMIUM -> safeMaterial("ENDER_CHEST");
            case PREMIUM_LOCKED -> safeMaterial("IRON_BARS");
            case FULLY_CLAIMED -> safeMaterial("LIME_DYE", "EMERALD");
        };
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.legacyColor(state.fallbackName()) + " " + questDisplayName);
            List<String> lore = new ArrayList<>();
            for (String line : state.fallbackLore()) {
                lore.add(Chat.legacyColor(line));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public ClickResult handleClick(Player whoClicked, ClickType clickType) {
        State state = currentState(whoClicked);
        switch (state) {
            case TIER_LOCKED -> {
                Messages.BATTLEPASS_TIER_LOCKED.send(whoClicked, "{tier}", String.valueOf(quest.getTier()));
                return ClickResult.DO_NOTHING;
            }
            case NOT_STARTED -> {
                owner.startQuest(quest);
                return ClickResult.REFRESH_PANE;
            }
            case IN_PROGRESS -> {
                return ClickResult.DO_NOTHING;
            }
            case PREMIUM_LOCKED -> {
                Messages.BATTLEPASS_CLAIM_NEED_PREMIUM.send(whoClicked);
                return ClickResult.DO_NOTHING;
            }
            case CLAIMABLE_FREE, CLAIMABLE_PREMIUM -> {
                BattlePassRewardDispatcher.ClaimResult result = plugin.getBattlePassRewardDispatcher().claim(owner, quest);
                announce(whoClicked, result);
                return ClickResult.REFRESH_PANE;
            }
            case FULLY_CLAIMED -> {
                return ClickResult.DO_NOTHING;
            }
        }
        return ClickResult.DO_NOTHING;
    }

    private void announce(Player player, BattlePassRewardDispatcher.ClaimResult result) {
        QItemStack questItem = plugin.getQItemStackRegistry().getQuestItemStack(quest);
        String displayName = questItem.getName();
        String stripped = Chat.legacyStrip(displayName);
        switch (result) {
            case CLAIMED_FREE -> Messages.BATTLEPASS_CLAIM_FREE.send(player, "{quest}", stripped, "{questcolored}", displayName);
            case CLAIMED_PREMIUM -> Messages.BATTLEPASS_CLAIM_PREMIUM.send(player, "{quest}", stripped, "{questcolored}", displayName);
            case CLAIMED_BOTH -> Messages.BATTLEPASS_CLAIM_BOTH.send(player, "{quest}", stripped, "{questcolored}", displayName);
            case NOTHING_TO_CLAIM, QUEST_NOT_COMPLETED -> { /* silent */ }
        }
    }

    private static Material safeMaterial(String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) { }
        }
        return Material.STONE;
    }

    private enum State {
        TIER_LOCKED("tier-locked", "&c&lTier Locked", List.of("&7Complete the previous tier first.")),
        NOT_STARTED("not-started", "&e&lAvailable", List.of("&7Click to start this quest.")),
        IN_PROGRESS("in-progress", "&e&lIn Progress", List.of("&7You're working on this quest.")),
        CLAIMABLE_FREE("claimable-free", "&a&lClaim", List.of("&7Click to claim the free reward.")),
        CLAIMABLE_PREMIUM("claimable-premium", "&d&lClaim Premium", List.of("&7Click to claim the premium reward.")),
        PREMIUM_LOCKED("premium-locked", "&8&lPremium Locked", List.of("&7Buy the premium battle pass", "&7to claim this reward.")),
        FULLY_CLAIMED("fully-claimed", "&a&lClaimed", List.of("&7All rewards have been claimed."));

        private final String key;
        private final String fallbackName;
        private final List<String> fallbackLore;

        State(String key, String fallbackName, List<String> fallbackLore) {
            this.key = key;
            this.fallbackName = fallbackName;
            this.fallbackLore = fallbackLore;
        }

        String configKey() { return key; }
        String fallbackName() { return fallbackName; }
        List<String> fallbackLore() { return fallbackLore; }
    }
}
