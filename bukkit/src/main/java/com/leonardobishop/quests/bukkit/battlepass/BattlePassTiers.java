package com.leonardobishop.quests.bukkit.battlepass;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgressFile;
import com.leonardobishop.quests.common.quest.Quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tier discovery and unlock-state helpers for battle pass quests.
 *
 * <p>Tier N is unlocked when every quest with {@code tier == N - 1} has been completed at least
 * once by the player ({@link QuestProgress#isCompletedBefore()}). Tier 1 is always unlocked.
 * Unlock is independent of reward claim status — completing the quest unlocks progression even
 * if rewards remain pending in the claim queue.
 */
public final class BattlePassTiers {

    private BattlePassTiers() { }

    /**
     * Get all quests assigned to the given tier, sorted by their natural sort order.
     */
    public static List<Quest> getQuestsInTier(BukkitQuestsPlugin plugin, int tier) {
        if (tier <= 0) return Collections.emptyList();
        List<Quest> result = new ArrayList<>();
        for (Quest quest : plugin.getQuestManager().getQuestMap().values()) {
            if (quest.getTier() == tier) {
                result.add(quest);
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Get the highest tier number for which any quest is configured.
     * Returns 0 if no battle pass quests exist.
     */
    public static int getMaxTier(BukkitQuestsPlugin plugin) {
        int max = 0;
        for (Quest quest : plugin.getQuestManager().getQuestMap().values()) {
            if (quest.getTier() > max) max = quest.getTier();
        }
        return max;
    }

    /**
     * Whether the given tier is unlocked for the player. Tier 1 is always unlocked.
     * For tier N > 1, all quests in tier N-1 must be completed.
     */
    public static boolean isTierUnlocked(BukkitQuestsPlugin plugin, QPlayer qPlayer, int tier) {
        if (tier <= 1) return true;
        List<Quest> previous = getQuestsInTier(plugin, tier - 1);
        if (previous.isEmpty()) {
            // No quests in the previous tier means nothing to gate on; treat as unlocked.
            return true;
        }
        QuestProgressFile progressFile = qPlayer.getQuestProgressFile();
        for (Quest quest : previous) {
            QuestProgress progress = progressFile.getQuestProgressOrNull(quest);
            if (progress == null || !progress.isCompletedBefore()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find the highest tier the player has currently unlocked. Useful as the default page when the
     * battle pass GUI is opened.
     */
    public static int getHighestUnlockedTier(BukkitQuestsPlugin plugin, QPlayer qPlayer) {
        int max = getMaxTier(plugin);
        int unlocked = 1;
        for (int t = 2; t <= max; t++) {
            if (isTierUnlocked(plugin, qPlayer, t)) {
                unlocked = t;
            } else {
                break;
            }
        }
        return unlocked;
    }
}
