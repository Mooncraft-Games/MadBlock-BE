package org.madblock.social.friends.comparators;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import org.madblock.social.friends.Friend;

import java.util.Comparator;

public class FriendComparator implements Comparator<Friend> {

    private Plugin plugin;

    public FriendComparator (Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int compare(Friend friendA, Friend friendB) {
        if (friendA.isOnline() && !friendB.isOnline()) {
            return -1;
        } else if (friendB.isOnline() && !friendA.isOnline()) {
            return 1;
        }
        Player playerA = plugin.getServer().getPlayerExact(friendA.getUsername());
        Player playerB = plugin.getServer().getPlayerExact(friendB.getUsername());
        if (playerA != null && playerB != null) {
            return playerB.getLoginChainData().getUsername().compareTo(playerA.getLoginChainData().getUsername());
        } else if (playerA != null) {
            return -1;
        } else if (playerB != null) {
            return 1;
        }
        return friendB.getUsername().compareTo(friendA.getUsername()); // Sort by name
    }
}
