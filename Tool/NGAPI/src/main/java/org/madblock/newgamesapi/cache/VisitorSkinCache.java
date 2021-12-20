package org.madblock.newgamesapi.cache;

import cn.nukkit.Player;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChangeSkinEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class VisitorSkinCache implements Listener {

    private static VisitorSkinCache instance;

    public static final String SKIN_CACHE_PATH = NewGamesAPI1.get().getServer().getDataPath()+"/cache/skins/";

    protected HashMap<String, Skin> skinCache;


    public VisitorSkinCache() {
        this.skinCache = new HashMap<>();
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
    }

    public void createCache() throws IOException {
        NewGamesAPI1.getPlgLogger().info("== Checking VisitorSkinCache ==");
        File skinCacheFolder = new File(SKIN_CACHE_PATH);
        if (!skinCacheFolder.exists()) {
            NewGamesAPI1.getPlgLogger().info("   !! Missing directory! Creating directories.");
            boolean results = skinCacheFolder.mkdirs();
            if (!results) {
                throw new FileNotFoundException("Could not create directory " + SKIN_CACHE_PATH);
            }
        }
        NewGamesAPI1.getPlgLogger().info("== Completed VisitorSkinCache structure integrity.  ==");
    }

    // Saves occur on server end
    // This should be replaced if the server scales up.
    public void save() {
        NewGamesAPI1.getPlgLogger().info("== Saving Skins ==");
        for(Map.Entry<String, Skin> e: this.skinCache.entrySet()) {
            try {
                File file = new File(SKIN_CACHE_PATH + e.getKey() + ".dat");
                boolean success = file.exists() || file.createNewFile();
                if (success) {

                    Skin skin = e.getValue();
                    NBTIO.write(Utility.parseNBTFromSkin(skin), file);

                } else {
                    throw new FileNotFoundException("Could not create file " + file.getAbsolutePath());
                }

            } catch (Exception err) {
                NewGamesAPI1.getPlgLogger().warning("Error writing to skin cache: "+e.getKey());
                err.printStackTrace();
            }
        }
        this.skinCache.clear();
        NewGamesAPI1.getPlgLogger().info("== Skins Saved ==");
    }

    public Optional<Skin> getPlayerSkin(String xuid) {
        Optional<Player> targetPlayer = NewGamesAPI1.get().getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
        if (targetPlayer.isPresent()) {
            return Optional.of(targetPlayer.get().getSkin());
        } else if (skinCache.containsKey(getSkinId("XUID", xuid))) {
            return Optional.of(skinCache.get(getSkinId("XUID", xuid)));
        } else {
            return getSkin("XUID", xuid);
        }
    }

    public Optional<Skin> getCustomSkin(String id) {
        return getSkin("CUSTOM", id);
    }

    public Optional<Skin> getSkin(String type, String identifier) {
        String key = getSkinId(type, identifier);
        if (skinCache.containsKey(key)) {
            return Optional.of(skinCache.get(key));
        } else {
            try {

                File file = new File(SKIN_CACHE_PATH + key + ".dat");
                if (file.exists()) {

                    CompoundTag tag = NBTIO.read(file);
                    return Optional.of(Utility.parseSkinFromNBT(tag));

                } else {
                    return Optional.empty();
                }

            } catch (Exception exception) {
                NewGamesAPI1.getPlgLogger().warning("Error loading from skin cache: "+key);
                exception.printStackTrace();
                return Optional.empty();
            }
        }
    }

    public String addSkin(String id, Skin skin) {
        return addSkin("CUSTOM", id, skin);
    }

    public String addSkin(Player skinOwner) {
        return addSkin("XUID", skinOwner.getLoginChainData().getXUID(), skinOwner.getSkin());
    }

    public String addSkin(String type, String identifier, Skin skin) {
        String fullID = getSkinId(type, identifier);
        this.skinCache.put(fullID, skin);
        return fullID;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getLoginChainData().isXboxAuthed()) {
            addSkin(event.getPlayer());
        }
    }

    @EventHandler
    public void onSkinChange(PlayerChangeSkinEvent event) {
        addSkin(event.getPlayer());
    }

    protected String getSkinId(String type, String identifier) {
        return type + "#" + identifier;
    }

    public void setAsPrimaryManager() {
        instance = this;
    }

    public static VisitorSkinCache get() { return instance; }


}
