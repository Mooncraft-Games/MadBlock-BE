package org.madblock.social.friends;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class FriendRequestProfile {

    private final Collection<FriendRequest> incomingFriendRequests = ConcurrentHashMap.newKeySet();
    private final Collection<FriendRequest> outgoingFriendRequests = ConcurrentHashMap.newKeySet();

    public FriendRequestProfile () {}

    public FriendRequestProfile (FriendRequestProfile profile) {
        synchronized (profile) {
            for (FriendRequest request : profile.getOutgoingRequests()) {
                addOutgoingFriendRequest(request);
            }
            for (FriendRequest request : profile.getIncomingRequests()) {
                addIncomingFriendRequest(request);
            }
        }
    }

    public Collection<FriendRequest> getIncomingRequests () {
        return Collections.unmodifiableCollection(incomingFriendRequests);
    }

    public Collection<FriendRequest> getOutgoingRequests () {
        return Collections.unmodifiableCollection(outgoingFriendRequests);
    }

    public void addOutgoingFriendRequest (FriendRequest request) {
        outgoingFriendRequests.add(request);
    }

    public void removeOutgoingFriendRequest (FriendRequest request) {
        outgoingFriendRequests.remove(request);
    }

    public void addIncomingFriendRequest (FriendRequest request) {
        incomingFriendRequests.add(request);
    }

    public void removeIncomingFriendRequest (FriendRequest request) {
        incomingFriendRequests.remove(request);
    }

}
