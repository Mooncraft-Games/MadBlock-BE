package org.madblock.social.events;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import org.madblock.social.friends.FriendRequest;

/**
 * A event that is called when one of your outgoing friend requests are rejected
 */
public class FriendRequestRejectedEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    protected FriendRequest request;

    public FriendRequestRejectedEvent(Player player, FriendRequest request) {
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
