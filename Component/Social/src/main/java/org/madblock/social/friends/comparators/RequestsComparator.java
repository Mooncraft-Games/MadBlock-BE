package org.madblock.social.friends.comparators;

import cn.nukkit.plugin.Plugin;
import org.madblock.social.friends.FriendRequest;

import java.util.Comparator;

public class RequestsComparator implements Comparator<FriendRequest> {

    private Plugin plugin;

    public RequestsComparator (Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int compare(FriendRequest requestA, FriendRequest requestB) {
        return requestB.getUsername().compareTo(requestA.getUsername()); // Sort by name
    }
}
