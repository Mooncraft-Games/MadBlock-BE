package org.madblock.blockswap.behaviours;

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
            this.getSessionHandler().declareVictoryForPlayer(event.getDeathCause().getVictim());
        }
    }

}
