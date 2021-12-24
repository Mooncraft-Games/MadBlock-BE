package org.madblock.newgamesapi.rewards;

import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.util.Utility;

public class RewardChunk {

    protected String internalID;
    protected String displayInfo;
    protected int amount; //Purely visual.


    protected int totalExperience;
    protected int totalCoins;
    protected int totalTourneyPoints;

    public RewardChunk(String internalID, String displayInfo, int experience, int coins) { this(internalID, displayInfo, experience, coins, 0); }
    public RewardChunk(String internalID, String displayInfo, int experience, int coins, int totalTourneyPoints) {
        this.internalID = internalID.toLowerCase();
        this.displayInfo = displayInfo;
        this.amount = 1;

        this.totalExperience = experience;
        this.totalCoins = coins;
        this.totalTourneyPoints = totalTourneyPoints;
    }

    public final boolean appendChunk(RewardChunk rewardChunk){
        if(chunkCheck(rewardChunk)){
            mergeChunkData(rewardChunk);
            return true;
        }
        return false;
    }

    protected boolean chunkCheck(RewardChunk rewardChunk){
        return rewardChunk.getInternalID().equals(this.getInternalID());
    }

    protected void mergeChunkData(RewardChunk rewardChunk){
        amount++;
        totalExperience += rewardChunk.getExperience();
        totalCoins += rewardChunk.getCoins();
        totalTourneyPoints += rewardChunk.getTourneyPoints();
    }


    public String getMessage(TextFormat baseColour, TextFormat accent, boolean showTourney){
        String initialMessage = String.format("%s%sx %s%s%s -%s", accent, amount, TextFormat.RESET, baseColour, displayInfo, TextFormat.RESET);
        if(totalCoins > 0) initialMessage = initialMessage.concat(" "+TextFormat.GOLD+TextFormat.BOLD+ String.format("+%s %s", totalCoins, Utility.ResourcePackCharacters.COIN));
        if(totalExperience > 0) initialMessage = initialMessage.concat(" "+TextFormat.GREEN+TextFormat.BOLD+ String.format("+%s XP", totalExperience));
        if((totalTourneyPoints > 0) && showTourney) initialMessage = initialMessage.concat(" "+TextFormat.RED+TextFormat.BOLD+ String.format("+%s %s", totalTourneyPoints, Utility.ResourcePackCharacters.TROPHY));
        return initialMessage;
    }

    public String getInternalID() { return internalID; }
    public String getDisplayInfo() { return displayInfo; }
    public int getQuantity() { return amount; }

    public int getCoins() { return totalCoins; }
    public int getExperience() { return totalExperience; }
    public int getTourneyPoints() { return totalTourneyPoints; }
}
