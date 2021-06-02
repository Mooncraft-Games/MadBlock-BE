package org.madblock.social.events;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import org.madblock.social.friends.FriendRequest;

/**
 * Event called when a online player obtains a incoming friend request.
 */
public class PlayerObtainedFriendRequestEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    protected FriendRequest request;

    public PlayerObtainedFriendRequestEvent(Player player, FriendRequest request) {
        this.player = player;
        this.request = request;
    }

    public FriendRequest getFriendRequest () {
        return request;
    }

    public static HandlerList getHandlers () {
        return handlers;
    }

}
