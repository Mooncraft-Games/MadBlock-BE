package org.madblock.social;

import cn.nukkit.plugin.PluginBase;
import org.madblock.social.commands.FriendCommand;
import org.madblock.social.friends.FriendsManager;
import org.madblock.social.listeners.FriendNotificationListener;

public class SocialAPI extends PluginBase {

    private FriendsManager friendsManager;

    @Override
    public void onEnable () {
        friendsManager = new FriendsManager(this);
        friendsManager.setAsPrimaryManager();

        getServer().getPluginManager().registerEvents(new FriendNotificationListener(), this);

        getServer().getCommandMap().register("sapi", new FriendCommand(this));
    }

    public static FriendsManager getFriendManager () {
        return FriendsManager.get();
    }

}
