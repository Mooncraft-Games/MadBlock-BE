package org.madblock.newgamesapi.rewards;

import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.ServerConfigProcessor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerRewardsProfile {

    // Note: This only works since we're using MariaDB
    // This can definitely be shortened in the amount of parameters we pass it in the future.
    private static final String ADD_REWARDS_QUERY = "INSERT INTO player_rewards (xuid, experience, coins, tourney) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE experience=experience + ?, coins=coins + ?, tourney=tourney + ?";

    private String xuid;
    private AtomicInteger coins;
    private AtomicInteger experience;
    private AtomicInteger tourney;
    private AtomicInteger level;
    private AtomicInteger xpRequiredToLevelUp;
    public PlayerRewardsProfile(String xuid, int experience, int coins, int tourney) {
        this.xuid = xuid;
        this.experience = new AtomicInteger(experience);
        this.coins = new AtomicInteger(coins);
        this.tourney = new AtomicInteger(tourney);
        this.level = new AtomicInteger(0);
        this.xpRequiredToLevelUp = new AtomicInteger(0);
        this.recalculateLevel();
    }

    public int getCoins () {
        return coins.get();
    }

    public int getExperience () {
        return experience.get();
    }

    public int getTourneyPoints() { return tourney.get(); }

    public int getLevel () {
        return level.get();
    }

    public int getXPRequiredToLevelUp () {
        return xpRequiredToLevelUp.get();
    }

    public boolean addRewards (RewardChunk rewards) throws SQLException {


        experience.getAndAdd(rewards.getExperience());
        coins.getAndAdd(rewards.getCoins());
        tourney.getAndAdd(rewards.getTourneyPoints());

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        this.recalculateLevel();
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement(ADD_REWARDS_QUERY, new Object[]{
                    xuid,
                    rewards.getExperience(), rewards.getCoins(), rewards.getTourneyPoints(),
                    rewards.getExperience(), rewards.getCoins(), rewards.getTourneyPoints()
            }));
            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    /**
     * Recalculte the level of the player.
     * Rather weird XP system, ask Guardian for xp doc.
     */
    private void recalculateLevel () {
        int xpLeft = experience.get();
        int recalculatedLevel = 0;

        // 0-10
        for (int i = 0; i < 10; i++) {
            if (xpLeft - 500 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 500 * (i + 1)));
                return;
            }
            xpLeft -= 500 * (i + 1);
            recalculatedLevel++;
        }

        // 10-20 (5000 can be calculate by 500 * 10)
        for (int i = 0; i < 10; i++) {
            if (xpLeft - 5000 - 1000 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 5000 - 1000 * (i + 1)));
                return;
            }
            xpLeft -= 5000 + 1000 * (i + 1);
            recalculatedLevel++;
        }

        // 20-40 (15000 can be calculated by 500 * 10 + 1000 * 10)
        for (int i = 0; i < 20; i++) {
            if (xpLeft - 15000 - 2000 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 15000 - 2000 * (i + 1)));
                return;
            }
            xpLeft -= 15000 + 2000 * (i + 1);
            recalculatedLevel++;
        }

        // 40-60
        for (int i = 0; i < 20; i++) {
            if (xpLeft - 55000 - 3000 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 55000 - 3000 * (i + 1)));
                return;
            }
            xpLeft -= 55000 + 3000 * (i + 1);
            recalculatedLevel++;
        }

        // 60-80
        for (int i = 0; i < 20; i++) {
            if (xpLeft - 115000 - 4000 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 115000 - 4000 * (i + 1)));
                return;
            }
            xpLeft -= 115000 + 4000 * (i + 1);
            recalculatedLevel++;
        }

        // 80-100
        for (int i = 0; i < 20; i++) {
            if (xpLeft - 195000 - 5000 * (i + 1) < 0) {
                level.set(recalculatedLevel);
                xpRequiredToLevelUp.set(Math.abs(xpLeft - 195000 - 5000 * (i + 1)));
                return;
            }
            xpLeft -= 195000 + 5000 * (i + 1);
            recalculatedLevel++;
        }

        xpRequiredToLevelUp.set(0);
        level.set(100);
    }

    /**
     * Sets the tourney points without a database update.
     */
    public void quietlyResetTourney() {
        this.tourney.set(0);
    }

    // Hi dap, pls don't kill me for this monstrosity of an implementation <3


}
