package org.madblock.social.events;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import org.madblock.social.friends.Friend;

/**
 * Event called when a online player obtains a friend.
 */
public class PlayerObtainedFriendEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    protected Friend friend;

    public PlayerObtainedFriendEvent (Player player, Friend friend) {
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
