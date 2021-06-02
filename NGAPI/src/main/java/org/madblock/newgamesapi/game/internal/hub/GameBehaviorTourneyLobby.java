package org.madblock.newgamesapi.game.internal.hub;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.internal.hub.pointentities.PointEntityTypeTourneyNPC;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardsManager;

import java.util.Optional;

public class GameBehaviorTourneyLobby extends GameBehaviorLobby {

    @Override
    public void onInitialCountdownEnd() {
        super.onInitialCountdownEnd();
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new PointEntityTypeTourneyNPC(getSessionHandler()));
    }

    protected void updateScoreboards(Player player){
        super.updateScoreboards(player);
        Optional<PlayerRewardsProfile> rewards = RewardsManager.get().getRewards(player);

        String points = "???";

        if (rewards.isPresent()) {
            PlayerRewardsProfile p = rewards.get();
            points = String.valueOf(p.getTourneyPoints());
        } else {
            // Should be handled by the hub. Change when that's no longer the case <3
        }

        getSessionHandler().getScoreboardManager().setLine(player, -1, String.format("%s %s%s", Utility.ResourcePackCharacters.TROPHY, TextFormat.RED, points));
    }

}
