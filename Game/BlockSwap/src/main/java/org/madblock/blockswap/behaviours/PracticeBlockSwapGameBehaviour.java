package org.madblock.blockswap.behaviours;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.team.Team;

public class PracticeBlockSwapGameBehaviour extends BlockSwapGameBehaviour {

    @Override
    public void commonDeathEvent(GamePlayerDeathEvent event) {
        super.commonDeathEvent(event);

        boolean anyAlivePlayers = this.getSessionHandler().getPlayers().stream()
                        .anyMatch(player -> this.getSessionHandler()
                        .getPlayerTeam(player)
                        .filter(Team::isActiveGameTeam)
                        .filter(team -> team.getPlayers().size() > 1)
                        .isPresent());

        if (!anyAlivePlayers) {

            String[] paragraphs = new String[] {
                "The game has ended!",
                "Rounds completed: " + this.getCompletedRounds() + ""
            };

            String endMessage = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);

            for (Player player : this.getSessionHandler().getPlayers()) {
                player.sendMessage(endMessage);
            }
            this.getSessionHandler().endGame(true);
        }
    }

}
