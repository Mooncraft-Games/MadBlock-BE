package org.madblock.newgamesapi.rewards;

import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.util.Utility;

public class AchievementProgressChunk extends RewardChunk {

    private int achievementProgressMaximum;
    private int achievementProgressMeasurement;

    public AchievementProgressChunk(String internalID, String info, int experience, int coins, int progress, int maxProgress) {
        super(internalID, info, experience, coins);
        this.achievementProgressMeasurement = progress;
        this.achievementProgressMaximum = maxProgress;
    }

    @Override
    protected boolean chunkCheck(RewardChunk rewardChunk) {
        return (super.chunkCheck(rewardChunk) && (rewardChunk instanceof AchievementProgressChunk));
    }

    @Override
    protected void mergeChunkData(RewardChunk rewardChunk) {
        super.mergeChunkData(rewardChunk);
        AchievementProgressChunk chunk = (AchievementProgressChunk) rewardChunk;
        this.achievementProgressMeasurement = Math.min(chunk.getAchievementProgressMeasurement()+this.achievementProgressMeasurement, this.achievementProgressMaximum);
    }

    @Override
    public String getMessage(TextFormat baseColour, TextFormat accent, boolean showTourney){
        if(achievementProgressMeasurement >= achievementProgressMaximum){
            String initialMessage = String.format("%s%s%s %s%s%s -%s", TextFormat.LIGHT_PURPLE, TextFormat.BOLD, "ACHIEVEMENT GET!", TextFormat.RESET, baseColour, displayInfo,  TextFormat.RESET);
            if(totalCoins > 0) initialMessage = initialMessage.concat(" "+TextFormat.GOLD+TextFormat.BOLD+ String.format("+%s %s", totalCoins, Utility.ResourcePackCharacters.COIN));
            if(totalExperience > 0) initialMessage = initialMessage.concat(" "+TextFormat.GREEN+TextFormat.BOLD+ String.format("+%s XP", totalExperience));
            return initialMessage;
        } else {
            String initialMessage = String.format("%s%s%s%s%s%s (%s/%s) -%s", accent, TextFormat.BOLD, "Achievement: ", TextFormat.RESET, baseColour, displayInfo, achievementProgressMeasurement, achievementProgressMaximum, TextFormat.RESET);
            if(totalCoins > 0) initialMessage = initialMessage.concat(" "+TextFormat.GOLD+TextFormat.BOLD+ String.format("+%s %s", totalCoins, Utility.ResourcePackCharacters.COIN));
            if(totalExperience > 0) initialMessage = initialMessage.concat(" "+TextFormat.GREEN+TextFormat.BOLD+ String.format("+%s XP", totalExperience));
            return initialMessage;
        }
    }

    public int getAchievementProgressMeasurement() { return achievementProgressMeasurement; }
    public int getAchievementProgressMaximum() { return achievementProgressMaximum; }
}
