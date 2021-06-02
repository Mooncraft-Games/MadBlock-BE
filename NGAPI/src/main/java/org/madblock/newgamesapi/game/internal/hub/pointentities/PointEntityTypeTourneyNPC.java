package org.madblock.newgamesapi.game.internal.hub.pointentities;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.network.protocol.EmotePacket;
import cn.nukkit.utils.TextFormat;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.cache.VisitorSkinCache;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeNPC;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.enums.PrimaryRankID;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PointEntityTypeTourneyNPC extends PointEntityTypeNPC {

    private static final String FETCH_STANDINGS_STATEMENT = "SELECT player_lookup.username, player_lookup.xuid, player_rewards.tourney FROM `player_rewards` INNER JOIN `player_lookup` ON player_rewards.xuid = player_lookup.xuid WHERE player_rewards.tourney > 0 ORDER BY player_rewards.tourney DESC;";

    // POINT ENTITY PROPERTIES

    // 0 = first place, 1 = second place, 2 = third place
    private static final String TOURNEY_STANDING_KEY = "tourney_standing";

    // This comes in handy for figuring out the emote ids
    // https://github.com/xStrixU/PocketMine-EmoteSlapper/blob/master/src/emoteslapper/manager/EmoteManager.php
    private static final String EMOTE_KEY = "emote_id";

    // END POINT ENTITY PROPERTIES

    private final Map<PointEntity, TourneyPointEntityData> cache;

    public PointEntityTypeTourneyNPC(GameHandler gameHandler) {
        super("tourney_npc", gameHandler);
        this.cache = new HashMap<>();
    }

    @Override
    public void onRegister() {
        super.onRegister();
        gameHandler.getGameScheduler().registerGameTask(() -> NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), this::updateStandings, true), 20 * 5, 20 * 20);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        this.cache.put(entity, new TourneyPointEntityData());
        addFunction("update", this::updateTourneyPointEntity);
        addFunction("emote", this::doEmote);

        super.onAddPointEntity(entity); // NPCHuman

        this.gameHandler.getGameScheduler().registerGameTask(() -> this.executeFunction("emote", entity, this.manager.getLevelLookup().get(entity), new HashMap<>()), (int)(Math.random() * 10) + 20);
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
        this.cache.remove(entity);
    }

    @Override
    public String getPersistentUuidNbtLocation() {
        return "ngapi_tourney_npc_identifier";
    }

    @Override
    protected Optional<Skin> getSkin(PointEntity entity) {
        Optional<TourneyStanding> standing = this.cache.get(entity).getCurrentStanding();
        Optional<Skin> returnedSkin = Optional.empty();
        if (standing.isPresent()) {
            returnedSkin = VisitorSkinCache.get().getPlayerSkin(standing.get().getXuid());
        }

        if (!returnedSkin.isPresent()) {
            // Try and retrieve default skin.
            Skin skin = new Skin();
            File skinFile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/invisible.png");
            try {
                BufferedImage image = ImageIO.read(skinFile);
                skin.setSkinData(image);
                skin.setTrusted(true);
                skin.setArmSize(entity.getStringProperties().getOrDefault("arm_size", "wide"));
                skin.generateSkinId(entity.getId());
                return skin.isValid() ? Optional.of(skin) : Optional.empty();
            }  catch (Exception err) {
                return Optional.empty();
            }
        } else {
            return returnedSkin;
        }

    }

    @Override
    protected String getDisplayName(PointEntity entity) {
        if (this.cache.containsKey(entity)) {

            Optional<TourneyStanding> standing = this.cache.get(entity).getCurrentStanding();
            if (standing.isPresent()) {

                RankManager manager = RankManager.getInstance();
                Optional<RankProfile> rankProfile = manager.getRankProfile(standing.get().getXuid());
                StringBuilder displayNameBuilder = new StringBuilder();

                if (rankProfile.isPresent()) {
                    PrimaryRankID primaryRankID = rankProfile.get().getPrimaryDisplayedRank();
                    if (primaryRankID.getName().isPresent()) {
                        displayNameBuilder.append(TextFormat.BOLD).append(primaryRankID.getColor().orElse(TextFormat.WHITE)).append(primaryRankID.getName().get()).append(TextFormat.RESET).append(" ");
                    }
                } else {
                    // Fetch ranks and update entity again
                    NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                        try {
                            manager.fetchRankProfile(standing.get().getXuid());
                            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> this.executeFunction("update", entity, this.manager.getLevelLookup().get(entity), new HashMap<>()));
                        } catch (SQLException exception) {
                            NewGamesAPI1.get().getLogger().error("An error occurred while fetching ranks in the tourney NPC.");
                            exception.printStackTrace();
                        }
                    }, true);
                }

                displayNameBuilder.append(standing.get().getDisplayName())
                        .append("\n")
                        .append(Utility.ResourcePackCharacters.TROPHY).append(" ").append(standing.get().getPoints());

                return displayNameBuilder.toString();

            } else {
                return "";
            }

        } else {

            return "";

        }
    }

    private void updateStandings() {
        ConnectionWrapper wrapper = null;
        PreparedStatement stmt = null;
        List<List<TourneyStanding>> standings = new ArrayList<>();
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
            stmt = wrapper.prepareStatement(new DatabaseStatement(FETCH_STANDINGS_STATEMENT));
            ResultSet results = stmt.executeQuery();

            // First we need to figure out the current standings.
            int lastPoints = -1;
            while (results.next()) {

                String displayName = results.getString("username");
                String xuid = results.getString("xuid");
                int points = results.getInt("tourney");

                if (lastPoints != points) {
                    standings.add(new ArrayList<>());
                }
                standings.get(standings.size() - 1).add(
                        new TourneyStanding(displayName, xuid, points)
                );
                lastPoints = points;
            }

        } catch (SQLException exception) {
            NewGamesAPI1.get().getLogger().error("An error occurred when fetching tourney standings.");
            exception.printStackTrace();
        } finally {
            if (wrapper != null) {
                DatabaseUtility.closeQuietly(wrapper);
            }
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
        }

        // Update point entity data and then request a skin update
        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
            for (Map.Entry<PointEntity, TourneyPointEntityData> entry : this.cache.entrySet()) {
                int standing = entry.getKey().getIntegerProperties().getOrDefault(TOURNEY_STANDING_KEY, 0);
                if (standing >= standings.size()) {
                    entry.getValue().setStandings(Collections.unmodifiableList(new ArrayList<>()));
                } else {
                    entry.getValue().setStandings(Collections.unmodifiableList(standings.get(standing)));
                }
            }
            this.executeFunctionForAll("update", new HashMap<>());

        });

    }


    // POINT ENTITY FUNCTIONS

    // "emote"
    private void doEmote(PointEntityCallData callData) {
        for (Player player : lastInstances.get(callData.getPointEntity()).getViewers().values()) {
            EmotePacket emotePacket = new EmotePacket();
            emotePacket.emoteID = callData.getPointEntity().getStringProperties().getOrDefault(EMOTE_KEY, "4c8ae710-df2e-47cd-814d-cc7bf21a3d67");
            emotePacket.runtimeId = lastInstances.get(callData.getPointEntity()).getId();
            player.dataPacket(emotePacket);
        }

        int seconds = (int)(Math.random() * 10) + 20;
        this.gameHandler.getGameScheduler().registerGameTask(() -> this.executeFunction("emote", callData.getPointEntity(), callData.getLevel(), new HashMap<>()), seconds * 20);

    }

    // "update"
    private void updateTourneyPointEntity(PointEntityCallData callData) {
        if (this.cache.containsKey(callData.getPointEntity())) {

            this.cache.get(callData.getPointEntity()).next();
            Optional<Skin> skin = getSkin(callData.getPointEntity());
            EntityHuman entity = this.lastInstances.get(callData.getPointEntity());
            Optional<TourneyStanding> standing = this.cache.get(callData.getPointEntity()).getCurrentStanding();

            if (standing.isPresent() && skin.isPresent()) {
                entity.despawnFromAll();
                entity.setSkin(skin.get());
                entity.namedTag.put("Skin", Utility.parseNBTFromSkin(skin.get()));
                entity.setScale(1.25f);
                entity.setNameTag(getDisplayName(callData.getPointEntity()));
                entity.spawnToAll();
            } else {
                entity.despawnFromAll();
            }

        } else {
            NewGamesAPI1.get().getLogger().error("Somehow attempted to update skin of non-registered tourney point entity.");
        }
    }

    // END POINT ENTITY FUNCTIONS

    private class TourneyPointEntityData {

        private int index;
        private List<TourneyStanding> standings;

        public TourneyPointEntityData() {
            this.standings = new ArrayList<>();
        }

        public void setStandings(List<TourneyStanding> standings) {
            this.standings = standings;
        }

        public Optional<TourneyStanding> getCurrentStanding() {
            if (this.standings.size() > 0) {
                TourneyStanding standing = standings.get(index);
                return Optional.of(standing);
            } else {
                return Optional.empty();
            }
        }

        public Optional<TourneyStanding> next() {
            if (this.standings.size() > 0) {
                index++;
                if (index >= standings.size()) {
                    index = 0;
                }
                TourneyStanding standing = standings.get(index);
                return Optional.of(standing);
            } else {
                return Optional.empty();
            }
        }

    }

    private class TourneyStanding {

        private String displayName;
        private String xuid;
        private int points;

        public TourneyStanding(String displayName, String xuid, int points) {
            this.displayName = displayName;
            this.xuid = xuid;
            this.points = points;
        }

        public int getPoints() {
            return this.points;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getXuid() {
            return this.xuid;
        }

    }
}
