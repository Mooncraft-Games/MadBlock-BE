package org.madblock.newgamesapi.kits;

import java.util.HashMap;

public final class KitGroup {

    private String groupID;
    private String displayName;
    private String defaultKitID;
    private boolean visibleInKitGroupSelector;
    private HashMap<String, Kit> groupkits;

    public KitGroup(String groupID, String displayName, boolean visibleInKitGroupSelector, Kit defaultKit, Kit... kits){
        this.groupID = groupID.toLowerCase();
        this.displayName = displayName;
        this.visibleInKitGroupSelector = visibleInKitGroupSelector;
        this.groupkits = new HashMap<>();
        this.defaultKitID = defaultKit.getKitID().toLowerCase();

        groupkits.put(defaultKit.getKitID().toLowerCase(), defaultKit);
        for(Kit kit: kits){
            groupkits.put(kit.getKitID().toLowerCase(), kit);
        }
    }

    public String getGroupID() {
        return groupID;
    }
    public String getDisplayName () { return displayName; }
    public String getDefaultKitID() { return defaultKitID; }
    public boolean isVisibleInKitGroupSelector () { return visibleInKitGroupSelector; }

    public Kit getDefaultKit() { return groupkits.get(defaultKitID); }

    public HashMap<String, Kit> getGroupKits() {
        return new HashMap<>(groupkits);
    }
}
