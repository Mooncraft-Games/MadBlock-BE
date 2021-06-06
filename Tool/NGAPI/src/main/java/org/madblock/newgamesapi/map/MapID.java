package org.madblock.newgamesapi.map;

import cn.nukkit.utils.TextFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.madblock.lib.stattrack.statistic.ITrackedEntityID;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.exception.MapIDParseException;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.map.types.RotatablePosition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class MapID implements ITrackedEntityID {

    private transient static final List<Integer> API_SUPPORTED_MAPID_VERSIONS = Arrays.asList(1);

    private transient String levelPath;
    private transient String mapInfoMessage;
    private transient String[] mapInfoMessageParagraphs;

    private String id;
    private Integer mapid_version;
    private String[] supported_gamemodes;

    private String display_name;
    private String description;
    private String[] authors;

    private HashMap<String, RotatablePosition[]> spawns;
    private HashMap<String, MapRegion> regions;
    private HashMap<String, PointEntity> point_entities;
    private HashMap<String, String> strings;
    private HashMap<String, String[]> death_messages;
    private HashMap<String, Integer> integers;
    private HashMap<String, Float> floats;
    private HashMap<String, Boolean> switches;

    private void verifyIntegrity(){
        if(this.id == null) throw new MapIDParseException("MapID did not contain a map identifier.");
        if(this.mapid_version == null || !API_SUPPORTED_MAPID_VERSIONS.contains(this.mapid_version)) throw new MapIDParseException("MapID for map %s is not a supported version.");
        if(this.supported_gamemodes == null || this.supported_gamemodes.length == 0){
            this.supported_gamemodes = new String[]{"unknown"};
            NewGamesAPI1.getPlgLogger().warning(String.format("Map [id: %s] does not specify any intended gamemodes. This map is not testing ready!", this.id));
        }

        if(this.display_name == null){
            this.display_name = "";
            NewGamesAPI1.getPlgLogger().warning(String.format("Map [id: %s] does not specify a display name. This map is not production ready!", this.id));
        }
        if(this.description == null){
            this.description = "This is a map! No description here!";
            NewGamesAPI1.getPlgLogger().warning(String.format("Map [id: %s] does not specify a description. This map may not be production ready!", this.id));
        }
        if (this.authors == null || this.authors.length == 0){
            this.authors = new String[]{"Mooncraft Games"};
            NewGamesAPI1.getPlgLogger().warning(String.format("Map [id: %s] does not specify any authors. This map may not be production ready!", this.id));
        }
        for(String author: this.authors){
            if(author == null){
                this.authors = new String[]{"Mooncraft Games"};
                NewGamesAPI1.getPlgLogger().warning(String.format("Map [id: %s] has a minor schema error. An author is null! Skipping authors.", this.id));
            }
        }

        if(this.spawns == null) this.spawns = new HashMap<>();
        if(this.regions == null) this.regions = new HashMap<>();
        if(this.point_entities == null) this.point_entities = new HashMap<>();
        if(this.strings == null) this.strings = new HashMap<>();
        if(this.death_messages == null) this.death_messages = new HashMap<>();
        if(this.integers == null) this.integers = new HashMap<>();
        if(this.floats == null) this.floats = new HashMap<>();
        if(this.switches == null) this.switches = new HashMap<>();

        for(Map.Entry<String, MapRegion> r: regions.entrySet()){ r.getValue().verifyIntegrityFromJson(r.getKey()); }
        for(Map.Entry<String, PointEntity> r: new ArrayList<>(point_entities.entrySet())){
            PointEntity val = r.getValue();
            String newID = val.verifyIntegrityFromJson(r.getKey());
            point_entities.remove(r.getKey());
            point_entities.put(newID, val);
        }

        this.id = this.id.toLowerCase();
        for(int i = 0; i < supported_gamemodes.length; i++){
            this.supported_gamemodes[i] = this.supported_gamemodes[i].toLowerCase();
        }

        for(String deathMsgCategory: new ArrayList<>(death_messages.keySet())){
            String key = deathMsgCategory.toLowerCase();
            String[] val = death_messages.get(deathMsgCategory);
            this.death_messages.remove(deathMsgCategory);
            this.death_messages.put(key, val);
        }

        updateMapIDMessage();
    }

    public void updateMapIDMessage() {
        StringBuilder authorBuilder = new StringBuilder();
        for(int i = 0; i < getAuthors().length; i++){
            authorBuilder.append(getAuthors()[i]);
            if(i+1 < getAuthors().length){
                authorBuilder.append(", ");
            }
        }

        mapInfoMessageParagraphs = new String[]{
                ""+ TextFormat.BLUE+TextFormat.BOLD+"Map: "+TextFormat.RESET+TextFormat.DARK_AQUA+getDisplayName()+"\n",
                "",
                getDescription(),
                ""+TextFormat.BLUE+TextFormat.BOLD+"By: "+TextFormat.RESET+TextFormat.DARK_AQUA+authorBuilder.toString()
        };

        mapInfoMessage = Utility.generateParagraph(mapInfoMessageParagraphs, TextFormat.BLUE, TextFormat.DARK_AQUA, 35);
    }

    private void setLevelPath(String path){ this.levelPath = path; }

    public String getLevelPath() { return levelPath; }
    public String getMapInfoMessage() { return mapInfoMessage; }
    public String[] getMapInfoMessageParagraphs() { return mapInfoMessageParagraphs; }

    public String getId() { return id; }
    public String[] getGamemodes() { return supported_gamemodes; }
    public String getDisplayName() { return display_name; }
    public String getDescription() { return description; }
    public String[] getAuthors() { return authors; }

    /**
     * Marks spawnpoints under the team assigned to them.
     *
     * Key: Spawnqueues for the gamemode. Usually team names but custom
     * spawnqueues can be used if a game supports them.
     * Value: Spawns within the spawnqueue.
     * @return Map of Spawnqueues and their spawns.
     */
    public HashMap<String, RotatablePosition[]> getSpawns() { return new HashMap<>(spawns); }
    /**
     * Marks identified regions within a map.
     * Key: RegionID.
     * Value: A region with 2 points and a tag array.
     * @return Map of the Regions tied to their respective IDs
     */
    public HashMap<String, MapRegion> getRegions() { return new HashMap<>(regions); }
    /**
     * Identifies all the point entities available in a map.
     * Key: Unique ID of the point entity.
     * Value: The point entity tied to the ID.
     * @return Map of the Point Entities tied to their respective IDs
     */
    public HashMap<String, PointEntity> getPointEntities() { return new HashMap<>(point_entities); }
    /**
     * Gamemode specific modifications to a map. Could be used to
     * mark points.
     * @return A map of strings tied to IDs
     */
    public HashMap<String, String> getStrings() { return new HashMap<>(strings); }
    /**
     * Gamemode specific deathmessages sorted into categories based off the death type.
     * @return A map of String arrays tied to their death categories.
     */
    public HashMap<String, String[]> getDeathMessages() { return new HashMap<>(death_messages); }
    /**
     * Gamemode specific modifications to a map. Could be used to
     * modify round times.
     * @return A map of Integers tied to IDs
     */
    public HashMap<String, Integer> getIntegers() { return new HashMap<>(integers); }
    /**
     * Gamemode specific modifications to a map. Could be used to
     * rebalance cooldowns.
     * @return A map of Floats tied to IDs
     */
    public HashMap<String, Float> getFloats() { return new HashMap<>(floats); }
    /**
     * Gamemode specific modifications to a map. Can be used to
     * enable/disable features.
     * @return A map of booleans tied to IDs
     */
    public HashMap<String, Boolean> getSwitches() { return new HashMap<>(switches); }

    public static MapID generateMapID(String pathToLevelTemplate) throws MapIDParseException, IllegalArgumentException, IllegalStateException{
        File levelFolder = new File(pathToLevelTemplate);
        if(!levelFolder.isDirectory()) throw new IllegalArgumentException(String.format("Path to level template must point to a directory (%s)", levelFolder.getAbsolutePath()));

        File[] levelfiles = levelFolder.listFiles();
        File mapIDFile = null;
        for(int i = 0; i < levelfiles.length; i++){
            File file = levelfiles[i];
            if(file.isFile() && (file.getName().toLowerCase().equals("mapid.json"))){
                mapIDFile = file;
            }
        }
        if(mapIDFile == null) throw new IllegalArgumentException(String.format("Level template does not contain a mapid.json (%s)", levelFolder.getAbsolutePath()));

        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        try {
            FileReader reader = new FileReader(mapIDFile);
            BufferedReader r = new BufferedReader(reader);
            MapID mapID = gson.fromJson(r, MapID.class);
            mapID.verifyIntegrity();
            mapID.setLevelPath(levelFolder.getAbsolutePath());
            return mapID;
        } catch (FileNotFoundException fileNotFoundException){
            throw new IllegalStateException("Somehow a file disappeared while parsing a MapID. Oops?");
        }
    }

    @Override
    public String getEntityType() {
        return "map";
    }

    @Override
    public String getStoredID() {
        return id;
    }
}
