package org.madblock.newgamesapi;

import org.madblock.lib.commons.data.store.DefaultKey;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;
import org.madblock.lib.commons.data.store.settings.Settings;
import org.madblock.lib.commons.json.JsonObject;
import org.madblock.lib.commons.json.io.JsonIO;
import org.madblock.lib.commons.json.io.JsonUtil;
import org.madblock.lib.commons.json.io.error.*;

import java.io.*;

public final class ServerConfigProcessor {

    public static final DefaultKey<Float> XP_MULTIPLIER = new DefaultKey<>("xp_multiplier", 1f);
    public static final DefaultKey<Float> COINS_MULTIPLIER = new DefaultKey<>("coins_multiplier", 1f);
    public static final DefaultKey<Float> TOURNEY_MULTIPLIER = new DefaultKey<>("tourney_multiplier", 1f);

    public static final DefaultKey<Boolean> IS_TOURNEY_ENABLED = new DefaultKey<>("is_tourney_enabled", true);

    public static String DEFAULT_CONFIG =
            "{" + "\n" +
                    "    " + formatLine(XP_MULTIPLIER) + "," + "\n" +
                    "    " + formatLine(COINS_MULTIPLIER) + "," + "\n" +
                    "    " + formatLine(TOURNEY_MULTIPLIER) + "," + "\n" +

                    "    " + formatLine(IS_TOURNEY_ENABLED) + "\n" + // When extending, add comma!
            "}";

    protected static int verifyAllDefaultKeys(Settings settings) {
        int replacements = 0;

        if(isSettingNull(settings, XP_MULTIPLIER)) replacements++;
        if(isSettingNull(settings, COINS_MULTIPLIER)) replacements++;
        if(isSettingNull(settings, IS_TOURNEY_ENABLED)) replacements++;

        return replacements;
    }

    public static ControlledSettings loadServerConfiguration(File configFile, boolean fillInDefaults) {
        NewGamesAPI1.getPlgLogger().info("Loading configuration...");

        JsonIO json = new JsonIO();
        ControlledSettings loadedSettings;
        JsonObject root;

        try {
            root = json.read(configFile);

        } catch (FileNotFoundException | JsonEmptyException err) {
            NewGamesAPI1.getPlgLogger().warning("No server configuration found! Creating a copy from default settings.");
            root = json.read(ServerConfigProcessor.DEFAULT_CONFIG);

            try {
                FileWriter writer = new FileWriter(configFile);
                BufferedWriter write = new BufferedWriter(writer);
                write.write(ServerConfigProcessor.DEFAULT_CONFIG);
                write.close();

            } catch (IOException err2) {
                NewGamesAPI1.getPlgLogger().error("Unable to write a new server configuration copy:");
                err2.printStackTrace();
            }

        } catch (JsonFormatException err) {
            NewGamesAPI1.getPlgLogger().error("Unable to parse json configuration! Using default settings: "+err.getMessage());
            root = json.read(ServerConfigProcessor.DEFAULT_CONFIG);

        } catch (JsonParseException err) {
            NewGamesAPI1.getPlgLogger().error("Unable to parse json configuration due to an internal error! Using default settings.");
            err.printStackTrace();
            root = json.read(ServerConfigProcessor.DEFAULT_CONFIG);
        }

        // Locking it as it really shouldn't be messed with.
        // If I add plugin support, I might change this ?   idk
        loadedSettings = JsonUtil.jsonToSettings(root, false);


        if(fillInDefaults) {
            NewGamesAPI1.getPlgLogger().info("Filling in the blanks!");
            int replacements = verifyAllDefaultKeys(loadedSettings); //todo: print count of replacements

            NewGamesAPI1.getPlgLogger().info("Using the defaults for "+replacements+" properties!");
        }

        NewGamesAPI1.getPlgLogger().info("Loaded configuration!");
        return loadedSettings; //.lock();   if we want it to be immutable.
    }

    // Just going to assume settings isn't null as this isn't
    // an important utility method.
    public static <T> boolean isSettingNull(Settings settings, DefaultKey<T> key) {
        if(settings.get(key) == null) {
            settings.set(key, key.getDefaultValue());
            return true; // invalid, replaced.
        }

        return false; // valid
    }

    // A little method to cleanup the default config
    public static <T> String formatLine(DefaultKey<T> key) {

        Object value = key.getDefaultValue();

        // String can't be extended, this is fine.
        if(key.getDefaultValue() instanceof String)
            value = "\"" + key.getDefaultValue() + "\"";

        return String.format("\"%s\": %s", key.get(), value);
    }
}
