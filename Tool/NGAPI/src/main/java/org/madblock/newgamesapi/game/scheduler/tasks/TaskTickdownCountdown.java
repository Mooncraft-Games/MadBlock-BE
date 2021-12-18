package org.madblock.newgamesapi.game.scheduler.tasks;

import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.HashMap;

public class TaskTickdownCountdown extends Task {

    private int maximumTimer;
    private int currentTimer;

    private String access_token;
    private GameHandler handler;

    private HashMap<Player, DummyBossBar> timerBossbars;

    public TaskTickdownCountdown(String access_token, GameHandler handler, int maximumTimer){
        this.access_token = access_token;
        this.handler = handler;
        this.maximumTimer = maximumTimer;
        this.currentTimer = maximumTimer; // Set to full.
        this.timerBossbars = new HashMap<>();
    }

    @Override
    public void onRun(int currentTick) {
        if(handler.getGameState() == GameHandler.GameState.COUNTDOWN){
            currentTimer--;
            double percent = Math.floor((double)currentTimer / (double)maximumTimer * 100);
            int val = (int) percent;
            if (maximumTimer > 0) {
                for(Player player: handler.getPlayers()){
                    // If onGameBegins returns 0, this could run before the player is actually on the map
                    if (player.getLevel().getId() == handler.getPrimaryMap().getId()) {
                        DummyBossBar bossBar = getPlayerBossbar(player);
                        bossBar.setLength(val);
                        bossBar.setText(String.format("%s%sCountdown: %s%s%s", TextFormat.GOLD, TextFormat.BOLD, TextFormat.RESET, TextFormat.RED, currentTimer));
                    }
                }
            }
            if(currentTimer <= 10 && currentTimer > 0){
                TextFormat textColour = currentTimer <= 3 ? TextFormat.RED : TextFormat.GOLD;
                String subtitle = currentTimer <= 5 ? TextFormat.YELLOW+" - Get ready! - " : "";
                Sound tickSound = currentTimer <= 3 ? Sound.NOTE_BANJO : Sound.MOB_SHEEP_SHEAR;
                for (Player player: handler.getPlayers()) {
                    player.getLevel().addSound(player.getPosition(), tickSound, 1f, 0.8f, player);
                    player.sendTitle(String.format("%s%s%s", textColour, TextFormat.BOLD, currentTimer), subtitle, 6,8,6);
                }
            }

            if (currentTimer <= 0){
                if(maximumTimer > 0) {
                    for (Player player : handler.getPlayers()) {
                        player.getLevel().addSound(player.getPosition().add(0, 2, 0), Sound.BLOCK_BELL_HIT, 0.12f, 0.3f, player);
                        player.sendTitle("" + TextFormat.GREEN + TextFormat.BOLD + "START!", "", 6, 20, 10);
                    }
                }
                handler.setGameState(access_token, GameHandler.GameState.PRE_MAIN_LOOP);
                handler.getGameBehaviors().onInitialCountdownEnd();
                NewGamesAPI1.get().getServer().getScheduler().scheduleTask(new TaskStartSessionLoops(access_token, handler));
                this.cancel();
            }

        } else {
            this.cancel();
        }
    }

    protected DummyBossBar getPlayerBossbar(Player player) {
        if(!timerBossbars.containsKey(player)) {
            DummyBossBar bossBar = new DummyBossBar.Builder(player)
                    .color(BossBarColor.BLUE)
                    .length(100)
                    .text(String.format("%s%sCountdown: %s%s", TextFormat.GOLD, TextFormat.BOLD, TextFormat.RED, "..."))
                    .build();
            handler.getBossbars().get(player).add(bossBar);
            timerBossbars.put(player, bossBar);
            player.createBossBar(bossBar);
            return bossBar;
        }
        return timerBossbars.get(player);
    }
}
