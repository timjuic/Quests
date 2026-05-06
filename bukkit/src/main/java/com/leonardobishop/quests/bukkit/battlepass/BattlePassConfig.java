package com.leonardobishop.quests.bukkit.battlepass;

import com.leonardobishop.quests.bukkit.config.BukkitQuestsConfig;
import com.leonardobishop.quests.common.quest.Quest;
import org.bukkit.entity.Player;

/**
 * Convenience accessors for battle pass settings under the `battle-pass:` section of config.yml.
 */
public final class BattlePassConfig {

    private static final String ROOT = "battle-pass.";

    private BattlePassConfig() { }

    public static boolean isEnabled(BukkitQuestsConfig config) {
        return config.getBoolean(ROOT + "enabled", false);
    }

    public static String getPremiumPermission(BukkitQuestsConfig config) {
        String perm = config.getString(ROOT + "premium-permission");
        return perm == null || perm.isEmpty() ? "quests.premium" : perm;
    }

    public static int getQuestsPerTier(BukkitQuestsConfig config) {
        return config.getInt(ROOT + "quests-per-tier", 7);
    }

    public static boolean isBattlePassQuest(Quest quest) {
        return quest.getTier() > 0;
    }

    public static boolean hasPremium(BukkitQuestsConfig config, Player player) {
        return player != null && player.hasPermission(getPremiumPermission(config));
    }
}
