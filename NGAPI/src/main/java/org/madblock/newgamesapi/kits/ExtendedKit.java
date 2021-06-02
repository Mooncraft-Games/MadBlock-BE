package org.madblock.newgamesapi.kits;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;

public abstract class ExtendedKit implements Listener {

    public Player target;
    public GameHandler gameHandler;

    public final void prepareExtendedKit(Player player, GameHandler gameHandler){
        this.target = player;
        this.gameHandler = gameHandler;
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        onPrepareExtendedKit();
    }

    public final void removeExtendedKit(){
        HandlerList.unregisterAll(this);
        onRemoveExtendedKit();
    }

    protected void onPrepareExtendedKit(){ }
    protected void onRemoveExtendedKit(){ }

    protected boolean checkEventIsForTargetPlayer(Entity entity){
        return (entity instanceof Player && target == entity);
    }

    /** @return the player this Extended kit tracks.*/
    public Player getTarget() {
        return target;
    }
    /** @return the game the player is participating in.*/
    public GameHandler getGameHandler() { return gameHandler; }
}
