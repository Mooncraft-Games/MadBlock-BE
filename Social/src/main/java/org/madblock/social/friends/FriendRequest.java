package org.madblock.social.friends;

public class FriendRequest {

    private final String xuid;
    private final String name;

    public FriendRequest (String xuid, String name) {
        this.xuid = xuid;
        this.name = name;
    }

    public String getUsername() {
        return name;
    }

    public String getXuid () {
        return xuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FriendRequest) {
            return ((FriendRequest)obj).xuid.equals(xuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + xuid.hashCode();
    }

}
