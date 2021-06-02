package org.madblock.database;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.ConfigSection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseAPI extends PluginBase {
    private static final Map<String, HikariDataSource> SOURCES = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        for (String key : getConfig().getKeys(false)) {
            if (getConfig().isSection(key)) {
                ConfigSection section = getConfig().getSection(key);
                if (
                        section.exists("username") &&
                                section.exists("password") &&
                                section.exists("host") &&
                                section.exists("port") &&
                                section.exists("database")
                ) {
                    // valid config
                    HikariConfig dbConfig = new HikariConfig();
                    dbConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    dbConfig.setJdbcUrl(
                            String.format(
                                    "jdbc:mysql://%s:%s/%s?serverTimezone=UTC",
                                    section.getString("host"),
                                    section.getString("port"),
                                    section.getString("database")
                            )
                    );
                    dbConfig.setUsername(section.getString("username"));
                    dbConfig.setPassword(section.getString("password"));
                    SOURCES.put(key, new HikariDataSource(dbConfig));
                    getLogger().info(String.format("Created data source for: %s", key));
                } else {
                    getLogger().warning(String.format("Invalid database configuration format for key: %s", key));
                }
            } else {
                getLogger().warning(String.format("Invalid database configuration format for key: %s", key));
            }
        }
    }

    @Override
    public void onDisable() {
        SOURCES.forEach((key, source) -> source.close());
        SOURCES.clear();
    }

    /**
     * Retrieve a database source.
     * You are expected to close the connection after you are finished using it.
     * @param key The key listed in the config.yml file
     * @return db source
     */
    public static ConnectionWrapper getConnection(String key) throws SQLException {
        if (SOURCES.containsKey(key)) {
            ConnectionWrapper wrapper = new ConnectionWrapper(SOURCES.get(key).getConnection());
            return wrapper;
        }
        throw new SQLException("Failed to connect to a database that has no configuration");
    }
}