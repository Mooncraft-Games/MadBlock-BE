package org.madblock.newgamesapi.registry;

import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.defaultgroup.KitArcher;
import org.madblock.newgamesapi.kits.defaultgroup.KitChef;
import org.madblock.newgamesapi.kits.defaultgroup.KitSwordsman;
import org.madblock.newgamesapi.kits.defaultgroup.KitTank;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class KitRegistry {

    private static KitRegistry registryInstance;

    public static final KitGroup DEFAULT = new KitGroup("default", "Default", false, new KitSwordsman(), new KitTank(), new KitArcher(), new KitChef());

    private HashMap<String, KitGroup> kitgroups;

    public KitRegistry (){
        this.kitgroups = new HashMap<>();
        kitgroups.put("default", DEFAULT);
    }

    /**
     * Makes the registry the result provided from KitRegistry#get() and
     * finalizes the instance to an extent.
     */
    public void setAsPrimaryRegistry(){
        if(registryInstance == null) registryInstance = this;
    }

    /**
     * Registers a kit group. Does not accept already registered groups. Case-
     * insensitive.
     * @return true if group was registered.
     */
    public boolean registerKitGroup(KitGroup group) {
        String id = group.getGroupID().toLowerCase();
        if(!kitgroups.containsKey(id)){
            kitgroups.put(id, group);
            for(Kit kit: group.getGroupKits().values()){
                kit.onRegister();
            }
            return true;
        }
        return false;
    }

    /** @return the primary instance of the Registry. */
    public static KitRegistry get(){
        return registryInstance;
    }

    /**
     * Gets a registered kit group.
     * @param id - The ID of the group
     * @return an optional. Check presence with Optional#ifPresent()
     */
    public Optional<KitGroup> getKitGroup(String id) {
        return Optional.ofNullable(kitgroups.get(id.toLowerCase()));
    }

    public Set<String> getAllKitGroups() { return kitgroups.keySet(); }
}
