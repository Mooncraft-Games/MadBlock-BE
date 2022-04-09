package org.madblock.newgamesapi.kits;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.item.Item;
import org.madblock.lib.stattrack.statistic.ITrackedEntityID;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.HashMap;
import java.util.Optional;

public abstract class Kit implements ITrackedEntityID {

    private HashMap<Player, ExtendedKit> extendedKitVariants;
    private HashMap<String, String> properties;

    public Kit() {
        this.extendedKitVariants = new HashMap<>();
        this.properties = new HashMap<>();
    }

    public void onRegister() { }

    public abstract String getKitID();

    public abstract String getKitDisplayName();
    public abstract String getKitDescription();

    @Override public String getEntityType() { return "kit"; }
    @Override public String getStoredID() { return getKitID(); }

    /** @return the items a player should recieve at the start of a game, only in the hotbar.*/
    public Item[] getHotbarItems(){ return new Item[0]; }

    /** @return the items a player should recieve at the start of a game.*/
    public Item[] getKitItems(){ return new Item[0]; }

    /** @return the cost of the kit */
    public int getCost () { return 0; }

    public boolean isVisibleInKitSelector() { return true; }

    /** @return the item a player should recieve in their head armour slot at the start of a game.*/
    public Optional<Item> getKitHelmet() { return Optional.empty(); }
    /** @return the item a player should recieve in their body armour slot at the start of a game.*/
    public Optional<Item> getKitChestplate() { return Optional.empty(); }
    /** @return the item a player should recieve in their leg armour slot at the start of a game.*/
    public Optional<Item> getKitLeggings() { return Optional.empty(); }
    /** @return the item a player should recieve in their feet armour slot at the start of a game.*/
    public Optional<Item> getKitBoots() { return Optional.empty(); }

    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() { return Optional.empty(); }


    public HashMap<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    public Optional<String> getProperty(String key){
        return Optional.ofNullable(properties.get(key.toLowerCase()));
    }

    protected void registerProperty(String key, String value){
        properties.put(key.toLowerCase(), value);
    }

    /**
     * Adds further effects and such for the player when the game starts.
     * <b>IT SHOULD NOT</b> give players items from #getKitItems();
     * @param player the player with the kit applied.
     */
    public void onKitEquip(Player player){ }

    /**
     * Handles any clean up not processed by the gamemode.
     * This could include kit potion effects.
     * @param player the player with the kit applied.
     */
    public void onKitUnequip(Player player){ }

    public final void applyKit(Player player, GameHandler handler, boolean clearPreviousInventory) {
        if (handler.getAppliedSessionKits().containsKey(player)){
            handler.getAppliedSessionKits().get(player).removeKit(player, handler, clearPreviousInventory);
        }

        handler.getAppliedSessionKits().put(player, this);

        for(int i = 0; i < 9 && i < getHotbarItems().length; i++){
            Item item = getHotbarItems()[i].clone();
            if(item != null && item.getId() != 0) {
                player.getInventory().setItem(i, item);
            }
        }

        for(Item item: getKitItems()){ player.getInventory().addItem(item).clone(); }

        player.getInventory().setArmorContents(new Item[]{
                        getKitHelmet().orElse(new BlockAir().toItem()).clone(),
                        getKitChestplate().orElse(new BlockAir().toItem()).clone(),
                        getKitLeggings().orElse(new BlockAir().toItem()).clone(),
                        getKitBoots().orElse(new BlockAir().toItem()).clone()
        });

        getExtendedKitFeatures().ifPresent(extendedKitClass -> {
            try {
                ExtendedKit ekit = extendedKitClass.newInstance();
                extendedKitVariants.put(player, ekit);
                ekit.prepareExtendedKit(player, handler);
            } catch (Exception err){
                NewGamesAPI1.getPlgLogger().warning("Broken ExtendedKit behaviors for kit: "+getKitID());
                err.printStackTrace();
            }
        });
        player.getInventory().sendContents(player);
        onKitEquip(player);
        handler.getGameBehaviors().onKitEquip(player, this);
    }

    public final void removeKit(Player player, GameHandler handler, boolean clearWholeInventory) {
        handler.getAppliedSessionKits().remove(player);
        if(extendedKitVariants.containsKey(player)){
            extendedKitVariants.get(player).removeExtendedKit();
            extendedKitVariants.remove(player);
        }
        if(clearWholeInventory){
            player.getInventory().clearAll();
            player.getCursorInventory().clearAll();
        } else {
            for (Item item : getKitItems()) {
                player.getInventory().remove(item);
                player.getCursorInventory().remove(item);
            }
            getKitHelmet().ifPresent(item -> {
                player.getInventory().remove(item);
                player.getCursorInventory().remove(item);
            });
            getKitChestplate().ifPresent(item -> {
                player.getInventory().remove(item);
                player.getCursorInventory().remove(item);
            });
            getKitLeggings().ifPresent(item -> {
                player.getInventory().remove(item);
                player.getCursorInventory().remove(item);
            });
            getKitBoots().ifPresent(item -> {
                player.getInventory().remove(item);
                player.getCursorInventory().remove(item);
            });
        }
        player.getInventory().sendContents(player);
        player.getCursorInventory().sendContents(player);
        onKitUnequip(player);
    }
}
