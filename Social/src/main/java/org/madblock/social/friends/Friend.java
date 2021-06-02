package org.madblock.social.friends;

import org.madblock.playerregistry.PlayerServerLocation;

import java.util.Optional;

public class Friend {

    private final String xuid;
    private final String username;

    private volatile Optional<PlayerServerLocation> location;

    public Friend (String xuid, String username) {
        this.xuid = xuid;
        this.username = username;
        location = Optional.empty();
    }

    public Friend (String xuid, String username, Optional<PlayerServerLocation> location) {
        this(xuid, username);
        this.location = location;
    }

    public String getUsername() {
        return username;
    }

    public String getXuid () { return xuid; }

    public Optional<PlayerServerLocation> getLocation () {
        return location;
    }

    /**
     * Returns whether or not the player was online at the time of creating this object
     * This method should only be used when retrieving the friends list via getFriends
     * @return
     */
    public boolean isOnline () {
        return location.isPresent();
    }

    public void setLocation (Optional<PlayerServerLocation> location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Friend) {
            return ((Friend)obj).xuid.equals(xuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return xuid.hashCode() + username.hashCode();
    }
}
