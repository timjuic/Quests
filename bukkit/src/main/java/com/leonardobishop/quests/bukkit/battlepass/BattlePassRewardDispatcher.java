package com.leonardobishop.quests.bukkit.battlepass;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.bukkit.hook.vault.rewards.VaultReward;
import com.leonardobishop.quests.bukkit.util.DispatchUtils;
import com.leonardobishop.quests.bukkit.util.SoundUtils;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.quest.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Owns reward delivery for battle pass quests. Whereas legacy quests fire rewards immediately on
 * completion, battle pass quests defer reward delivery until the player clicks claim. The
 * {@code freeRewardClaimed} / {@code premiumRewardClaimed} flags on {@link QuestProgress} are the
 * source of truth for which payouts are pending.
 *
 * <p>Premium gating happens at claim time, not completion time. A player who completes a tier
 * before buying premium can later return and claim premium rewards for those quests, because the
 * permission check runs here when they click claim.
 */
public final class BattlePassRewardDispatcher {

    private final BukkitQuestsPlugin plugin;
    private final Map<Quest, VaultReward> freeVaultCache = new WeakHashMap<>();
    private final Map<Quest, VaultReward> premiumVaultCache = new WeakHashMap<>();

    public BattlePassRewardDispatcher(BukkitQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Result of a claim attempt.
     */
    public enum ClaimResult {
        CLAIMED_FREE,
        CLAIMED_PREMIUM,
        CLAIMED_BOTH,
        NOTHING_TO_CLAIM,
        QUEST_NOT_COMPLETED
    }

    /**
     * Attempt to claim all eligible rewards for a quest. Free reward is claimed if unclaimed.
     * Premium reward is claimed if unclaimed AND the player has the premium permission.
     */
    public ClaimResult claim(QPlayer qPlayer, Quest quest) {
        QuestProgress progress = qPlayer.getQuestProgressFile().getQuestProgressOrNull(quest);
        if (progress == null || !progress.isCompletedBefore()) {
            return ClaimResult.QUEST_NOT_COMPLETED;
        }

        Player player = Bukkit.getPlayer(qPlayer.getPlayerUUID());
        if (player == null) {
            return ClaimResult.NOTHING_TO_CLAIM;
        }

        boolean claimedFree = false;
        boolean claimedPremium = false;

        if (!progress.isFreeRewardClaimed()) {
            giveFreeReward(player, quest);
            progress.setFreeRewardClaimed(true);
            claimedFree = true;
        }

        BukkitQuestsConfig config = (BukkitQuestsConfig) plugin.getQuestsConfig();
        if (!progress.isPremiumRewardClaimed() && BattlePassConfig.hasPremium(config, player)) {
            givePremiumReward(player, quest);
            progress.setPremiumRewardClaimed(true);
            claimedPremium = true;
        }

        if (claimedFree && claimedPremium) {
            SoundUtils.playSoundForPlayer(player, plugin.getQuestsConfig().getString("options.sounds.reward-claim"));
            return ClaimResult.CLAIMED_BOTH;
        }
        if (claimedFree) {
            SoundUtils.playSoundForPlayer(player, plugin.getQuestsConfig().getString("options.sounds.reward-claim"));
            return ClaimResult.CLAIMED_FREE;
        }
        if (claimedPremium) {
            SoundUtils.playSoundForPlayer(player, plugin.getQuestsConfig().getString("options.sounds.reward-claim"));
            return ClaimResult.CLAIMED_PREMIUM;
        }
        return ClaimResult.NOTHING_TO_CLAIM;
    }

    private void giveFreeReward(Player player, Quest quest) {
        plugin.getScheduler().doSync(() -> {
            VaultReward vaultReward = freeVaultCache.computeIfAbsent(quest, k -> VaultReward.parse(plugin, k.getVaultReward()));
            vaultReward.give(player);
            for (String s : quest.getRewards()) {
                DispatchUtils.dispatchCommand(player, plugin.applyPlayerAndPAPI(BukkitQuestsPlugin.PAPIType.QUESTS, player, s));
            }
        });
        for (String s : quest.getRewardString()) {
            Chat.send(player, plugin.applyPlayerAndPAPI(BukkitQuestsPlugin.PAPIType.QUESTS, player, s), true);
        }
    }

    private void givePremiumReward(Player player, Quest quest) {
        plugin.getScheduler().doSync(() -> {
            VaultReward vaultReward = premiumVaultCache.computeIfAbsent(quest, k -> VaultReward.parse(plugin, k.getPremiumVaultReward()));
            vaultReward.give(player);
            for (String s : quest.getPremiumRewards()) {
                DispatchUtils.dispatchCommand(player, plugin.applyPlayerAndPAPI(BukkitQuestsPlugin.PAPIType.QUESTS, player, s));
            }
        });
        for (String s : quest.getPremiumRewardString()) {
            Chat.send(player, plugin.applyPlayerAndPAPI(BukkitQuestsPlugin.PAPIType.QUESTS, player, s), true);
        }
    }
}
