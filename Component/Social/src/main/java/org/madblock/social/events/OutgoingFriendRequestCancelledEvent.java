package org.madblock.social.events;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import org.madblock.social.friends.FriendRequest;

/**
 * Event called when a online player has a outgoing friend request that is cancelled.
 */
public class OutgoingFriendRequestCancelledEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    protected FriendRequest request;

    public OutgoingFriendRequestCancelledEvent(Player player, FriendRequest request) {
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
