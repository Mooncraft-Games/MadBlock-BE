package org.madblock.social.events;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import org.madblock.social.friends.Friend;

/**
 * A event that is called when a online player has a friend come online.
 */
public class FriendOnlineEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    protected Friend friend;

    public FriendOnlineEvent (Player player, Friend friend) {
        this.player = player;
        this.friend = friend;
    }

    public Friend getFriend () {
        return friend;
    }

    public static HandlerList getHandlers () {
        return handlers;
    }

}