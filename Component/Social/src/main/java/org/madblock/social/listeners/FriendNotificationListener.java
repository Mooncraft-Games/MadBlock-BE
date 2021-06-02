package org.madblock.social.listeners;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.utils.TextFormat;
import org.madblock.social.Utility;
import org.madblock.social.events.*;

public class FriendNotificationListener implements Listener {

    @EventHandler
    public void onFriendObtained (PlayerObtainedFriendEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("You are now friends with %s!", event.getFriend().getUsername()), TextFormat.GREEN));
    }

    @EventHandler
    public void onFriendLost (PlayerLostFriendEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("You are no longer friends with %s.", event.getFriend().getUsername()), TextFormat.RED));
    }

    @EventHandler
    public void onIncomingFriendRequest (PlayerObtainedFriendRequestEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("You received a friend request from %s! /friend accept %s", event.getFriendRequest().getUsername(), event.getFriendRequest().getUsername()), TextFormat.GREEN));
    }

    @EventHandler
    public void onIncomingFriendRequestCancelled (IncomingFriendRequestCancelledEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("%s cancelled their friend request.", event.getFriendRequest().getUsername()), TextFormat.RED));
    }

    @EventHandler
    public void onOutgoingFriendRequestCancelled (OutgoingFriendRequestCancelledEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("Your friend request with %s was cancelled.", event.getFriendRequest().getUsername()), TextFormat.RED));
    }

    @EventHandler
    public void onPlayerRejectedFriendRequest (FriendRequestRejectedEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("Your friend request with %s was rejected.", event.getFriendRequest().getUsername()), TextFormat.RED));
    }

    @EventHandler
    public void onFriendOnlineEvent (FriendOnlineEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("%s is logged in!", event.getFriend().getUsername()), TextFormat.GREEN));
    }

    @EventHandler
    public void onFriendOfflineEvent (FriendOfflineEvent event) {
        event.getPlayer().sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("%s logged off.", event.getFriend().getUsername()), TextFormat.RED));
    }

}
