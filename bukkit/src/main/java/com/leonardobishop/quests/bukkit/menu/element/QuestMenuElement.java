package com.leonardobishop.quests.bukkit.menu.element;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassConfig;
import com.leonardobishop.quests.bukkit.battlepass.BattlePassRewardDispatcher;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.menu.CancelQMenu;
import com.leonardobishop.quests.bukkit.menu.ClickResult;
import com.leonardobishop.quests.bukkit.menu.QMenu;
import com.leonardobishop.quests.bukkit.menu.itemstack.QItemStack;
import com.leonardobishop.quests.bukkit.util.FormatUtils;
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
import java.util.concurrent.TimeUnit;

public class QuestMenuElement extends MenuElement {

    private final BukkitQuestsPlugin plugin;
    private final BukkitQuestsConfig config;
    private final QPlayer owner;
    private final Quest quest;
    private final QMenu menu;
    private final boolean dummy;

    private final ClickType startClickType;
    private final ClickType trackClickType;
    private final ClickType cancelClickType;

    public QuestMenuElement(BukkitQuestsPlugin plugin, Quest quest, QMenu menu) {
        this(plugin, quest, menu, false);
    }

    public QuestMenuElement(BukkitQuestsPlugin plugin, Quest quest, QMenu menu, boolean dummy) {
        this.plugin = plugin;
        this.config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        this.menu = menu;
        this.owner = menu.getOwner();
        this.quest = quest;
        this.dummy = dummy;

        this.startClickType = MenuUtils.getClickType(config, "options.gui-actions.start-quest", "LEFT");
        this.trackClickType = MenuUtils.getClickType(config, "options.gui-actions.track-quest", "DROP");
        this.cancelClickType = MenuUtils.getClickType(config, "options.gui-actions.cancel-quest", "RIGHT");
    }

    public QPlayer getOwner() {
        return owner;
    }

    public String getQuestId() {
        return quest.getId();
    }

    public Quest getQuest() {
        return quest;
    }

    @Override
    public ItemStack asItemStack() {
        QuestProgress questProgress = owner.getQuestProgressFile().getQuestProgress(quest);
        QuestStartResult status = owner.canStartQuest(quest);
        long cooldown = owner.getQuestProgressFile().getCooldownFor(quest);
        QItemStack qItemStack = plugin.getQItemStackRegistry().getQuestItemStack(quest);

        Map<String, String> placeholders = new HashMap<>();
        ItemStack display;
        if (status == QuestStartResult.QUEST_LOCKED) {
            List<String> quests = new ArrayList<>();
            for (String requirement : quest.getRequirements()) {
                Quest requirementQuest = plugin.getQuestManager().getQuestById(requirement);
                if (requirementQuest == null) continue;
                if (!owner.getQuestProgressFile().hasQuestProgress(requirementQuest) ||
                        !owner.getQuestProgressFile().getQuestProgress(requirementQuest).isCompletedBefore()) {
                    quests.add(Chat.legacyStrip(plugin.getQItemStackRegistry().getQuestItemStack(requirementQuest).getName()));
                }
            }
            placeholders.put("{quest}", Chat.legacyStrip(qItemStack.getName()));
            placeholders.put("{questcolored}", qItemStack.getName());
            placeholders.put("{questid}", quest.getId());
            if (quests.size() > 1 && plugin.getConfig().getBoolean("options.gui-truncate-requirements", true)) {
                placeholders.put("{requirements}", quests.get(0) + Messages.UI_PLACEHOLDERS_TRUNCATED.getMessageLegacyColor().replace("{amount}", String.valueOf(quests.size() - 1)));
            } else {
                placeholders.put("{requirements}", String.join(", ", quests));
            }
            if (plugin.getQItemStackRegistry().hasQuestLockedItemStack(quest)) {
                display = plugin.getQItemStackRegistry().getQuestLockedItemStack(quest);
            } else {
                display = config.getItem("gui.quest-locked-display");
            }
        } else if (status == QuestStartResult.QUEST_ALREADY_COMPLETED) {
            // Battle-pass quests render a claim icon instead of "Quest Complete" while rewards are
            // still pending, so players can claim from this same GUI rather than a separate menu.
            ItemStack claimIcon = battlePassClaimIcon(questProgress, qItemStack);
            if (claimIcon != null) {
                return claimIcon;
            }
            placeholders.put("{quest}", Chat.legacyStrip(qItemStack.getName()));
            placeholders.put("{questcolored}", qItemStack.getName());
            placeholders.put("{questid}", quest.getId());
            if (plugin.getQItemStackRegistry().hasQuestCompletedItemStack(quest)) {
                display = plugin.getQItemStackRegistry().getQuestCompletedItemStack(quest);
            } else {
                display = config.getItem("gui.quest-completed-display");
            }
        } else if (status == QuestStartResult.QUEST_NO_PERMISSION) {
            placeholders.put("{quest}", Chat.legacyStrip(qItemStack.getName()));
            placeholders.put("{questcolored}", qItemStack.getName());
            placeholders.put("{questid}", quest.getId());
            if (plugin.getQItemStackRegistry().hasQuestPermissionItemStack(quest)) {
                display = plugin.getQItemStackRegistry().getQuestPermissionItemStack(quest);
            } else {
                display = config.getItem("gui.quest-permission-display");
            }
        } else if (cooldown > 0) {
            placeholders.put("{time}", FormatUtils.time(TimeUnit.SECONDS.convert(cooldown, TimeUnit.MILLISECONDS)));
            placeholders.put("{quest}", Chat.legacyStrip(qItemStack.getName()));
            placeholders.put("{questcolored}", qItemStack.getName());
            placeholders.put("{questid}", quest.getId());
            if (plugin.getQItemStackRegistry().hasQuestCooldownItemStack(quest)) {
                display = plugin.getQItemStackRegistry().getQuestCooldownItemStack(quest);
            } else {
                display = config.getItem("gui.quest-cooldown-display");
            }
        } else {
            return MenuUtils.applyPlaceholders(plugin, owner.getPlayerUUID(), qItemStack.toItemStack(quest, owner, questProgress));
        }
        return MenuUtils.applyPlaceholders(plugin, owner.getPlayerUUID(), display, placeholders);
    }

    @Override
    public ClickResult handleClick(Player whoClicked, ClickType clickType) {
        if (dummy) {
            return ClickResult.DO_NOTHING;
        }

        // Battle pass: click a claim-state tile to dispatch the pending reward, then refresh the
        // pane so the icon updates (e.g. CHEST -> ENDER_CHEST after free is claimed, or to the
        // normal "Quest Complete" item once everything is claimed).
        if (BattlePassConfig.isEnabled(config) && BattlePassConfig.isBattlePassQuest(quest)) {
            QuestProgress bpProgress = owner.getQuestProgressFile().getQuestProgressOrNull(quest);
            if (bpProgress != null && bpProgress.isCompletedBefore() && hasAnyClaimablePending(whoClicked, bpProgress)) {
                BattlePassRewardDispatcher.ClaimResult result = plugin.getBattlePassRewardDispatcher().claim(owner, quest);
                announceClaim(whoClicked, result);
                return ClickResult.REFRESH_PANE;
            }
        }

        boolean close = config.getBoolean("options.gui-close-after-accept", true);
        if (!owner.hasStartedQuest(quest) && clickType == startClickType) {
            if (owner.startQuest(quest) == QuestStartResult.QUEST_SUCCESS) {
                return close ? ClickResult.CLOSE_MENU : ClickResult.REFRESH_PANE;
            }

        } else if (clickType == trackClickType) {
            if (owner.hasStartedQuest(quest)) {
                if (!plugin.getQuestsConfig().getBoolean("options.allow-quest-track")) {
                    return ClickResult.DO_NOTHING;
                }

                String tracked = owner.getPlayerPreferences().getTrackedQuestId();

                if (quest.getId().equals(tracked)) {
                    owner.trackQuest(null);
                } else {
                    owner.trackQuest(quest);
                }
                return close ? ClickResult.CLOSE_MENU : ClickResult.REFRESH_PANE;
            }

        } else if (clickType == cancelClickType) {
            if (owner.hasStartedQuest(quest)) {
                if (!plugin.getQuestsConfig().getBoolean("options.allow-quest-cancel")
                        || plugin.getConfig().getBoolean("options.quest-autostart")
                        || quest.isAutoStartEnabled()
                        || !quest.isCancellable()) {
                    return ClickResult.DO_NOTHING;
                }

                if (plugin.getQuestsConfig().getBoolean("options.gui-confirm-cancel", true)) {
                     CancelQMenu cancelQMenu = new CancelQMenu(plugin, menu, owner, quest);
                     plugin.getMenuController().openMenu(owner.getPlayerUUID(), cancelQMenu);
                } else {
                    if (menu.getOwner().cancelQuest(quest)) {
                        return close ? ClickResult.CLOSE_MENU : ClickResult.REFRESH_PANE;
                    }
                }
            }
        }

        return ClickResult.DO_NOTHING;
    }

    /**
     * Returns the icon to render for a battle-pass quest that has been completed but still has
     * pending rewards to claim. Returns null when the quest is not a battle-pass quest, the
     * battle pass is disabled, or all claimable rewards for this player are already claimed.
     */
    private ItemStack battlePassClaimIcon(QuestProgress progress, QItemStack qItemStack) {
        if (!BattlePassConfig.isEnabled(config) || !BattlePassConfig.isBattlePassQuest(quest)) {
            return null;
        }
        Player player = plugin.getServer().getPlayer(owner.getPlayerUUID());
        boolean freeClaimed = progress.isFreeRewardClaimed();
        boolean premiumClaimed = progress.isPremiumRewardClaimed();
        boolean hasPremiumPerm = BattlePassConfig.hasPremium(config, player);
        boolean premiumExists = !quest.getPremiumRewards().isEmpty() || quest.getPremiumVaultReward() != null;

        if (!freeClaimed) {
            return makeChestIcon(false, qItemStack.getName(), progress, hasPremiumPerm, premiumExists);
        }
        if (premiumExists && !premiumClaimed && hasPremiumPerm) {
            return makeChestIcon(true, qItemStack.getName(), progress, hasPremiumPerm, premiumExists);
        }
        // free claimed and (no premium reward, or premium claimed, or no premium perm) — fall
        // through to the regular "Quest Complete" item.
        return null;
    }

    private boolean hasAnyClaimablePending(Player player, QuestProgress progress) {
        boolean freeClaimed = progress.isFreeRewardClaimed();
        boolean premiumClaimed = progress.isPremiumRewardClaimed();
        boolean hasPremiumPerm = BattlePassConfig.hasPremium(config, player);
        boolean premiumExists = !quest.getPremiumRewards().isEmpty() || quest.getPremiumVaultReward() != null;
        if (!freeClaimed) return true;
        return premiumExists && !premiumClaimed && hasPremiumPerm;
    }

    /**
     * Build the claim chest icon. Pulls the base item from a configurable template under
     * `gui.quest-claim-free-display` / `gui.quest-claim-premium-display` so server admins can
     * customize the material, name, and lore. Lore supports per-line placeholders: a line
     * containing only {free-rewards} or {premium-rewards} is expanded to one lore entry per
     * configured reward-string line on the quest, with sensible "(none)" fallbacks.
     */
    private ItemStack makeChestIcon(boolean premium, String questDisplayName, QuestProgress progress, boolean hasPremiumPerm, boolean premiumExists) {
        String configRoot = premium ? "gui.quest-claim-premium-display" : "gui.quest-claim-free-display";
        ItemStack stack;
        if (config.getConfig().isConfigurationSection(configRoot)) {
            stack = plugin.getConfiguredItemStack(configRoot, config.getConfig());
        } else {
            stack = new ItemStack(premium ? safeMaterial("ENDER_CHEST") : safeMaterial("CHEST"));
            ItemMeta tmp = stack.getItemMeta();
            if (tmp != null) {
                tmp.setDisplayName(Chat.legacyColor(premium ? "&d&lClaim Premium Reward" : "&a&lClaim Reward"));
                stack.setItemMeta(tmp);
            }
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            // expand title placeholders
            if (meta.hasDisplayName()) {
                meta.setDisplayName(Chat.legacyColor(applyClaimPlaceholders(meta.getDisplayName(), questDisplayName, progress, hasPremiumPerm, premiumExists, /*expandRewardLines=*/false).getFirst()));
            }
            // expand lore placeholders, with multi-line expansion for {free-rewards}/{premium-rewards}
            List<String> existingLore = meta.hasLore() ? meta.getLore() : defaultLoreTemplate();
            List<String> expanded = new ArrayList<>();
            for (String line : existingLore) {
                expanded.addAll(applyClaimPlaceholders(line, questDisplayName, progress, hasPremiumPerm, premiumExists, /*expandRewardLines=*/true));
            }
            List<String> coloured = new ArrayList<>(expanded.size());
            for (String l : expanded) coloured.add(Chat.legacyColor(l));
            meta.setLore(coloured);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Apply placeholders to one source line. Returns a list because lines containing
     * `{free-rewards}` or `{premium-rewards}` on their own expand to multiple lines.
     */
    private List<String> applyClaimPlaceholders(String source, String questDisplayName, QuestProgress progress, boolean hasPremiumPerm, boolean premiumExists, boolean expandRewardLines) {
        String questStripped = Chat.legacyStrip(questDisplayName);
        boolean freeClaimed = progress.isFreeRewardClaimed();
        boolean premiumClaimed = progress.isPremiumRewardClaimed();

        String freeStatus = freeClaimed ? "&aClaimed" : "&ePending";
        String premiumStatus;
        if (!premiumExists) premiumStatus = "&8None";
        else if (premiumClaimed) premiumStatus = "&aClaimed";
        else if (!hasPremiumPerm) premiumStatus = "&cLocked";
        else premiumStatus = "&ePending";

        // Multi-line expansion for reward placeholders. Each rewardstring line becomes its own
        // lore entry. If the quest has no rewardstring set, fall back to a single "(none)" line.
        if (expandRewardLines && source.trim().equals("{free-rewards}")) {
            return rewardLines(quest.getRewardString());
        }
        if (expandRewardLines && source.trim().equals("{premium-rewards}")) {
            return rewardLines(quest.getPremiumRewardString());
        }

        String out = source
                .replace("{quest}", questStripped)
                .replace("{questcolored}", questDisplayName)
                .replace("{questid}", quest.getId())
                .replace("{free-status}", freeStatus)
                .replace("{premium-status}", premiumStatus);
        return List.of(out);
    }

    private List<String> rewardLines(List<String> rewardString) {
        if (rewardString == null || rewardString.isEmpty()) {
            return List.of("&8  (none)");
        }
        List<String> out = new ArrayList<>(rewardString.size());
        for (String s : rewardString) {
            out.add(s); // user formats their rewardstring how they want — we don't add prefixes
        }
        return out;
    }

    private static List<String> defaultLoreTemplate() {
        return List.of(
                "&7Quest: &f{questcolored}",
                "",
                "&aFree reward &7({free-status})&7:",
                "{free-rewards}",
                "",
                "&dPremium reward &7({premium-status})&7:",
                "{premium-rewards}",
                "",
                "&eClick to claim available rewards."
        );
    }

    private static Material safeMaterial(String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) { }
        }
        return Material.STONE;
    }

    private void announceClaim(Player player, BattlePassRewardDispatcher.ClaimResult result) {
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
}
