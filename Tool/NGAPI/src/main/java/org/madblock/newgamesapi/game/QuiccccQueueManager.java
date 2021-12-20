package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.registry.GameRegistry;

import java.util.*;

public class QuiccccQueueManager implements Listener {

    private static QuiccccQueueManager managerInstance;

    protected HashMap<String, ArrayList<GameHandler>> startingLobbies;
    protected HashMap<String, ArrayList<Player>> queues;
    protected HashMap<Player, String> reverseQueueMap;

    public QuiccccQueueManager(){
        this.startingLobbies = new HashMap<>();
        this.queues = new HashMap<>();
        this.reverseQueueMap = new HashMap<>();
    }

    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
        NewGamesAPI1.get().getServer().getScheduler().scheduleRepeatingTask(NewGamesAPI1.get(), this::updatePlayerStatusTask, 20);
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
    }

    public void updatePlayerStatusTask(){
        for(Map.Entry<String, ArrayList<Player>> entry: queues.entrySet()){
            if(GameRegistry.get().getGameID(entry.getKey()).isPresent()) {
                GameID game = GameRegistry.get().getGameID(entry.getKey()).get();

                for(Player player: entry.getValue()){
                    player.sendActionBar(
                            Utility.generateServerMessage("QUEUE", TextFormat.GOLD,
                            String.format("There are %s%s/%s %splayers waiting in the queue. Starting soon.",
                                   TextFormat.GOLD, entry.getValue().size(), game.getGameProperties().getGuidelinePlayers(), TextFormat.YELLOW), TextFormat.YELLOW),
                            0, 30, 0);
                }
            }
        }
    }

    public void matchmakePlayer(Player player, String gameID){
        String gid = gameID.toLowerCase();
        try {
            leaveQueue(player);
            if (GameRegistry.get().getGameID(gid).isPresent()) {
                if(startingLobbies.containsKey(gid)){
                    ArrayList<GameHandler> fl = startingLobbies.get(gid);
                    for(GameHandler handler: new ArrayList<>(fl)){
                        if (handler.getPlayers().size() > handler.getGameID().getGameProperties().getMaximumPlayers()) {
                            // If the game hasn't started yet, people can still leave.
                            if (handler.getGameState() != GameHandler.GameState.PRE_COUNTDOWN && handler.getGameState() != GameHandler.GameState.COUNTDOWN) {
                                fl.remove(handler);
                            }
                        } else if(handler.getGameState() == GameHandler.GameState.PRE_COUNTDOWN || handler.getGameState() == GameHandler.GameState.COUNTDOWN){
                            if(handler.addPlayerToGame(player)){
                                return;
                            }
                        } else {
                            fl.remove(handler);
                        }
                    }
                    startingLobbies.put(gid, fl);
                }

                GameID game = GameRegistry.get().getGameID(gid).get();

                if(!queues.containsKey(gid)){
                    queues.put(gid, new ArrayList<>());
                }

                ArrayList<Player> q = queues.get(gid);
                for(Player p: q) player.getLevel().addSound(player.getPosition(), Sound.BLOCK_BEEHIVE_ENTER, 1, 0.8f, player);

                q.add(player);
                reverseQueueMap.put(player, gid);
                player.getLevel().addSound(player.getPosition(), Sound.RANDOM_LEVELUP, 1, 1f, player);
                player.sendMessage(Utility.generateServerMessage("QUEUE", TextFormat.GOLD, TextFormat.GRAY + "You have queued for " +
                        TextFormat.GOLD + game.getGameDisplayName() + TextFormat.GRAY + ". If you would like to leave the queue, type " +
                        TextFormat.RED + "/leavequeue" + TextFormat.GRAY + "."));

                if(q.size() >= game.getGameProperties().getGuidelinePlayers()){
                    String sessionID = GameManager.get().createGameSession(gameID, 500);
                    Optional<GameHandler> session = GameManager.get().getSession(sessionID);
                    if(session.isPresent()){
                        ArrayList<GameHandler> handlers = startingLobbies.getOrDefault(gid, new ArrayList<>());
                        handlers.add(session.get());
                        startingLobbies.put(gid, handlers);
                        ArrayList<Player> rejects = new ArrayList<>(Arrays.asList(session.get().prepare(q.toArray(new Player[0]))));
                        q.removeIf(p -> !rejects.contains(p));
                    }
                }
            } else {
                player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The npc is misconfigured. Tell a developer pls. <3", TextFormat.RED));
            }
        } catch (Exception err){
            player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Something went wrong when connecting you. Tell a developer pls. <3", TextFormat.RED));
            err.printStackTrace();
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        leaveQueue(event.getPlayer());
    }

    public void leaveQueue(Player player) {
        if (isInQueue(player)) {
            String queueID = reverseQueueMap.remove(player);
            queues.get(queueID).remove(player);
        }
    }

    public boolean isInQueue(Player player) {
        return reverseQueueMap.containsKey(player);
    }

    /** @return the primary instance of the Manager. */
    public static QuiccccQueueManager get() { return managerInstance; }

}
