package org.madblock.newgamesapi.map;

import cn.nukkit.Server;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class MapManager {

    public static final String API_LEVEL_SHORTID;
    public static final String API_LEVEL_VERSION;
    public static final String API_LEVEL_ID_SERIES_TAG;

    static {
        API_LEVEL_SHORTID = "ngapi";
        API_LEVEL_VERSION = "uramaki";

        API_LEVEL_ID_SERIES_TAG = String.format("%s.%s", API_LEVEL_SHORTID, API_LEVEL_VERSION);
    }

    public final String API_TEMPLATES_PATH;
    public final String API_WORLDS_PATH;

    private static MapManager managerInstance;

    private HashMap<String, HashMap<String, MapID>> mapTemplateCategories;

    public MapManager (){
        Server server = NewGamesAPI1.get().getServer();
        API_TEMPLATES_PATH = server.getDataPath()+"/templates/";
        API_WORLDS_PATH = server.getDataPath()+"/worlds/";

        this.mapTemplateCategories = new HashMap<>();
    }

    /**
     * Makes the manager the result provided from MapManger#get() and
     * finalizes the instance to an extent.
     */
    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
    }

    /**
     * Begins server map system checkup ensuring the correct directories exist. If
     * a directory was missing, the server will attempt to create them.
     */
    public void beginServerStartChecks() throws IOException {
        NewGamesAPI1.getPlgLogger().info("== Starting MapManager preparation checks. ==");
        NewGamesAPI1.getPlgLogger().info(": (Check 1/2) Checking Template directory status.");
        File templatesFolder = new File(API_TEMPLATES_PATH);

        if(!templatesFolder.exists()){
            NewGamesAPI1.getPlgLogger().info(":   !! Missing directory! Creating directories.");
            boolean result = templatesFolder.mkdirs();

            if(!result) throw new FileNotFoundException("Could not create directory at: "+API_TEMPLATES_PATH);
        } else if(!templatesFolder.isDirectory()){
            throw new FileNotFoundException("File found in directory's place at: "+API_TEMPLATES_PATH);
        }

        NewGamesAPI1.getPlgLogger().info(": > PASS");
        NewGamesAPI1.getPlgLogger().info(": (Check 2/2) Checking API World directory status.");
        File worldsFolder = new File(API_WORLDS_PATH);

        if(worldsFolder.exists() && worldsFolder.isDirectory()){

            for(File foundFolder : worldsFolder.listFiles()) {

                if(foundFolder.isDirectory() && foundFolder.getName().startsWith(API_LEVEL_ID_SERIES_TAG)) {
                    NewGamesAPI1.getPlgLogger().info(": !! Existing directory! Removing.");
                    FileUtils.deleteDirectory(foundFolder);
                }
            }
        }

        NewGamesAPI1.getPlgLogger().info(": > PASS");
        NewGamesAPI1.getPlgLogger().info("== -- -- -- -- -- ==");
    }

    public void loadMapDatabase(){
        NewGamesAPI1.getPlgLogger().info("== Starting MapManager MapID Scanning. ==");
        ArrayList<File> collectedFiles = scanDirectoryAndChildren(new File(API_TEMPLATES_PATH));
        int loadedMapCount = 0;
        int errorCount = 0;

        for(File mapidfile: collectedFiles){

            try {
                MapID mapid = MapID.generateMapID(mapidfile.getParentFile().getAbsolutePath());
                String[] gamemodetags = mapid.getGamemodes();

                for(String tag: gamemodetags) {

                    if (!mapTemplateCategories.containsKey(tag)) {
                        mapTemplateCategories.put(tag, new HashMap<>());
                    }
                    mapTemplateCategories.get(tag).put(mapid.getId(), mapid);
                }
                loadedMapCount++;

            } catch (JsonSyntaxException jse) {
                NewGamesAPI1.getPlgLogger().warning("JSON Syntax error in map at: "+mapidfile.getAbsolutePath());
                errorCount++;
            } catch (JsonParseException jpe) {
                NewGamesAPI1.getPlgLogger().warning("Parsing error in map at: "+mapidfile.getAbsolutePath());
                errorCount++;
            } catch (Exception error){
                error.printStackTrace();
                NewGamesAPI1.getPlgLogger().warning("Failure loading broken mapid...");
                errorCount++;
            }
        }
        NewGamesAPI1.getPlgLogger().info(String.format(": > PASS (%s / %s detected maps loaded)", loadedMapCount, loadedMapCount+errorCount));
        NewGamesAPI1.getPlgLogger().info("== -- -- -- -- -- ==");
    }

    private ArrayList<File> scanDirectoryAndChildren(File directory){
        ArrayList<File> collectedFiles = new ArrayList<>();

        File[] files = directory.listFiles();
        if(files != null) {
            for (File file : files) {
                if(file.isDirectory()){
                    collectedFiles.addAll(scanDirectoryAndChildren(file));
                    continue;
                }
                if(file.getName().toLowerCase().equals("mapid.json")){
                    collectedFiles.add(file);
                }
            }
        }

        return collectedFiles;
    }

    public Optional<Level> loadTemplateMapFromMapID(String sessionID, MapID id) throws IOException {

        File directory = new File(id.getLevelPath());
        if(directory.isDirectory()){
            String levelName = parseLevelNameStringFromSeries(sessionID, "map");
            String fullPathToLevel = API_WORLDS_PATH+levelName+"/";
            File worldsDir = new File(fullPathToLevel);
            if(worldsDir.exists()){
                FileUtils.deleteDirectory(worldsDir);
            }
            worldsDir.mkdirs();
            FileUtils.copyDirectory(directory, worldsDir);
            if(NewGamesAPI1.get().getServer().loadLevel(levelName)){
                Level level = NewGamesAPI1.get().getServer().getLevelByName(levelName);
                level.getProvider().updateLevelName(levelName);
                level.getGameRules().setGameRule(GameRule.DO_FIRE_TICK, false);
                level.getGameRules().setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
                applyLevelProperties(id, level);
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    protected void applyLevelProperties(MapID mapID, Level level){
        applyTimeProperty(mapID, level);
        applyWeatherProperty(mapID, level);

        level.getGameRules().setGameRule(GameRule.SHOW_COORDINATES, mapID.getSwitches().getOrDefault("showCoordinates", false));
    }

    protected void applyTimeProperty(MapID mapID, Level level){
        level.getGameRules().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, mapID.getSwitches().getOrDefault("do_daylight_cycle", false));
        int time = mapID.getIntegers().getOrDefault("time", 8000);
        // Perhaps add a way to set it with words like "day" or "midnight"
        level.setTime(time);
    }

    protected void applyWeatherProperty(MapID mapID, Level level){
        level.getGameRules().setGameRule(GameRule.DO_WEATHER_CYCLE, mapID.getSwitches().getOrDefault("do_weather", false));
        level.setRaining(mapID.getSwitches().getOrDefault("raining", false));
        level.setThundering(mapID.getSwitches().getOrDefault("thundering", false));
        level.setRainTime(mapID.getIntegers().getOrDefault("rain_time", 6000));
        level.setRainTime(mapID.getIntegers().getOrDefault("thunder_time", 6000));
    }

    /** Returns all the gamemode tags maps are registered under. */
    public ArrayList<String> getMapTemplateCategories() {
        return new ArrayList<>(mapTemplateCategories.keySet());
    }

    public Optional<ArrayList<String>> getMapIdentifiersFromCategory(String category){
        String lowerCategory = category.toLowerCase();
        if(mapTemplateCategories.containsKey(lowerCategory)){
            return Optional.of(new ArrayList<>(mapTemplateCategories.get(lowerCategory).keySet()));
        }
        return Optional.empty();
    }

    public Optional<ArrayList<MapID>> getMapsFromCategory(String category){
        String lowerCategory = category.toLowerCase();
        if(mapTemplateCategories.containsKey(lowerCategory)){
            return Optional.of(new ArrayList<>(mapTemplateCategories.get(lowerCategory).values()));
        }
        return Optional.empty();
    }

    public Optional<MapID> getSpecificMap(String category, String mapIdentifier){
        String lowerCategory = category.toLowerCase();
        String lowerMapId = mapIdentifier.toLowerCase();
        if(mapTemplateCategories.containsKey(lowerCategory)){
            if(mapTemplateCategories.get(lowerCategory).containsKey(lowerMapId)){
                return Optional.of(mapTemplateCategories.get(lowerCategory).get(lowerMapId));
            }
        }
        return Optional.empty();
    }

    public static String parseLevelNameStringFromSeries(String... tags){
        StringBuilder stringBuilder = new StringBuilder(API_LEVEL_ID_SERIES_TAG);
        for(String tag: tags){
            stringBuilder.append(".").append(tag);
        }
        return stringBuilder.toString();
    }

    /** @return the primary instance of the Manager. */
    public static MapManager get(){
        return managerInstance;
    }

}
