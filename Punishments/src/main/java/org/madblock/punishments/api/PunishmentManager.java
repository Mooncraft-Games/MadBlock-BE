package org.madblock.punishments.api;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.builders.PunishmentEntryBuilder;
import org.madblock.punishments.enums.PunishmentType;
import org.madblock.punishments.forms.PunishmentFormManager;
import org.madblock.punishments.list.PunishmentCategory;
import org.madblock.punishments.list.PunishmentOffense;
import org.madblock.punishments.list.SubPunishmentOffense;
import org.madblock.punishments.logic.BanPunishmentLogic;
import org.madblock.punishments.logic.MutePunishmentLogic;
import org.madblock.punishments.logic.PunishmentLogic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PunishmentManager {

    private static final String GET_PUNISHMENTS_QUERY = "SELECT id, (SELECT username FROM player_lookup WHERE xuid=punishments.author) as author, expires, created, offense, (SELECT username FROM player_lookup WHERE xuid=punishments.remover) as remover, removed_reason, code FROM punishments WHERE offender=? AND platform=?";
    private static final String REMOVE_PUNISHMENT_VIA_PLAYER_QUERY = "UPDATE punishments SET remover=?, removed_reason=?, expires=? WHERE id=?";
    private static final String REMOVE_PUNISHMENT_VIA_CONSOLE_QUERY = "UPDATE punishments SET removed_reason=?, expires=? WHERE id=?";
    private static final String ADD_PUNISHMENT_VIA_PLAYER_QUERY = "INSERT INTO punishments (offender, author, platform, offense, code, created, expires) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String ADD_PUNISHMENT_VIA_CONSOLE_QUERY = "INSERT INTO punishments (offender, platform, offense, code, created, expires) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String GET_LATEST_PUNISHMENT_OF_PLAYER_QUERY = "SELECT id, (SELECT username FROM player_lookup WHERE xuid=punishments.author) as author, expires, created, offense, (SELECT username FROM player_lookup WHERE xuid=punishments.remover) as remover, removed_reason, code FROM punishments WHERE offender=? AND platform=? ORDER BY id DESC LIMIT 1";

    private static PunishmentManager instance;

    private final PunishmentsPlugin plugin;

    private final PunishmentFormManager formManager = new PunishmentFormManager();

    private final Map<String, PunishmentCategory> categories = new HashMap<>();

    private final Map<PunishmentType, PunishmentLogic> punishmentLogic = new HashMap<>();

    private final Map<Integer, PunishmentEntry> punishmentCache = new ConcurrentHashMap<>();

    private final Map<String, List<Integer>> playerPunishmentsCache = new ConcurrentHashMap<>();

    public PunishmentManager (PunishmentsPlugin plugin) {
        instance = this;
        this.plugin = plugin;

        this.punishmentLogic.put(PunishmentType.BAN, new BanPunishmentLogic());
        this.punishmentLogic.put(PunishmentType.MUTE, new MutePunishmentLogic());

        this.categories.put(
                "chat1",
                new PunishmentCategory("chat", 1,  60000L * 60 * 4, PunishmentType.MUTE)
                    .addOffense(
                            new PunishmentOffense("Spam")
                                    .addSubReason(new SubPunishmentOffense("Spam [Character Spam]"))
                                    .addSubReason(new SubPunishmentOffense("Spam [Message Repetition]"))
                                    .addSubReason(new SubPunishmentOffense("Spam [General Spam]", PunishmentOffense.AdditionalFunction.REASON_PROMPT))
                    )
                    .addOffense(new PunishmentOffense("General Rudeness"))
                    .addOffense(new PunishmentOffense("Chat Trolling"))
                    .addOffense(new PunishmentOffense("Light Advertisement"))
                    .addOffense(new PunishmentOffense("Filter Bypass"))
                    .addOffense(new PunishmentOffense("Inappropriate Behavior"))
                    .addOffense(new PunishmentOffense("Rioting"))
        );

        this.categories.put(
                "chat2",
                new PunishmentCategory("chat", 2, 60000L * 60 * 24, PunishmentType.MUTE)
                    .addOffense(new PunishmentOffense("Abusive Behaviour"))
                    .addOffense(new PunishmentOffense("Filter Bypass"))
        );

        this.categories.put(
                "chat3",
                new PunishmentCategory("chat", 3, 60000L * 60 * 24 * 30, PunishmentType.MUTE)
                    .addOffense(new PunishmentOffense("Discrimination"))
                    .addOffense(new PunishmentOffense("General Threats"))
        );

        this.categories.put(
                "chat4",
                new PunishmentCategory("chat", 4, -1, PunishmentType.MUTE)
                    .addOffense(new PunishmentOffense("Revealing Personal Information"))
                    .addOffense(new PunishmentOffense("Unapproved Links"))
                    .addOffense(new PunishmentOffense("Death Threats"))
        );

        this.categories.put(
                "general1",
                new PunishmentCategory("general", 1, 60000L * 60 * 4, PunishmentType.BAN)
                    .addOffense(new PunishmentOffense("Glitch Abuse"))
                    .addOffense(new PunishmentOffense("Punishment Evading"))
        );

        this.categories.put(
                "general2",
                new PunishmentCategory("general", 2, 60000L * 60 * 24 * 30, PunishmentType.BAN)
                    .addOffense(
                            new PunishmentOffense("Inappropriate Gameplay")
                                .addSubReason(new SubPunishmentOffense("Inappropriate Gameplay [Build]", PunishmentOffense.AdditionalFunction.GAME_PROMPT))
                                .addSubReason(new SubPunishmentOffense("Inappropriate Gameplay [Item]", PunishmentOffense.AdditionalFunction.GAME_PROMPT))
                                .addSubReason(new SubPunishmentOffense("Inappropriate Gameplay", PunishmentOffense.AdditionalFunction.GAME_PROMPT))
                    )
        );

        this.categories.put(
                "general3",
                new PunishmentCategory("general", 3, -1, PunishmentType.BAN)
                    .addOffense(
                            new PunishmentOffense("Inappropriate Username", PunishmentOffense.AdditionalFunction.CHANGE_AND_APPEAL)
                    )
                    .addOffense(
                            new PunishmentOffense("Unapproved Skin")
                                    .addSubReason(new SubPunishmentOffense("Unapproved Skin [Non Alex/Steve Model]", PunishmentOffense.AdditionalFunction.CHANGE_AND_APPEAL))
                                    .addSubReason(new SubPunishmentOffense("Unapproved Skin [Inappropriate Content]", PunishmentOffense.AdditionalFunction.CHANGE_AND_APPEAL))
                    )
        );

        this.categories.put(
                "hack1",
                new PunishmentCategory("hack", 1, 60000L * 60 * 24 * 15, PunishmentType.BAN)
                    .addOffense(
                            new PunishmentOffense("Client-side Modifications")
                                    .addSubReason(new SubPunishmentOffense("XRay"))
                                    .addSubReason(new SubPunishmentOffense("Tracers"))
                                    .addSubReason(new SubPunishmentOffense("ESP"))
                    )
                    .addOffense(new PunishmentOffense("Aimbot"))
                    .addOffense(new PunishmentOffense("Anti-Knockback"))
                    .addOffense(new PunishmentOffense("FastBreak"))
                    .addOffense(new PunishmentOffense("Killaura"))
                    .addOffense(new PunishmentOffense("NoFall"))
                    .addOffense(new PunishmentOffense("Nuker"))
                    .addOffense(new PunishmentOffense("Reach"))
        );

        this.categories.put(
                "hack2",
                new PunishmentCategory("hack", 2, -1, PunishmentType.BAN)
                    .addOffense(new PunishmentOffense("Fly Hacking"))
                    .addOffense(new PunishmentOffense("Speed Hacking"))
                    .addOffense(new PunishmentOffense("Glide Hacking"))
                    .addOffense(new PunishmentOffense("High Jump Hacking"))
                    .addOffense(new PunishmentOffense("Bunny Hopping"))
                    .addOffense(new PunishmentOffense("Spider Hacking"))
                    .addOffense(new PunishmentOffense("No-Slow Hacking"))
                    .addOffense(new PunishmentOffense("Jesus Hacking"))
                    .addOffense(new PunishmentOffense("Anti-AFK"))
        );

        this.categories.put(
                "ntb",
                new PunishmentCategory("ntb", -1, PunishmentType.BAN, "punishments.issue.ntb")
                    .addOffense(new PunishmentOffense("Network Ban [Excessive Punishments]"))
                    .addOffense(new PunishmentOffense("Network Ban [Constant Toxicity]"))
                    .addOffense(new PunishmentOffense("Network Ban [Leaking Information]"))
                    .addOffense(new PunishmentOffense("Network Ban [Major Server Damage]"))
                    .addOffense(new PunishmentOffense("Network Ban", PunishmentOffense.AdditionalFunction.REASON_PROMPT))
        );

    }

    public Optional<PunishmentCategory> getCategory (String code) {
        String id = code.toLowerCase();
        if (categories.containsKey(id)) {
            return Optional.of(categories.get(id));
        } else {
            return Optional.empty();
        }
    }

    public Optional<PunishmentLogic> getLogic (PunishmentType type) {
        if (punishmentLogic.containsKey(type)) {
            return Optional.of(punishmentLogic.get(type));
        } else {
            return Optional.empty();
        }
    }

    public List<PunishmentEntry> getPunishments (Player player) throws SQLException {
        return getPunishments(player.getLoginChainData().getXUID());
    }

    public List<PunishmentEntry> getPunishments (String xuid) throws SQLException {

        List<PunishmentEntry> punishments = new ArrayList<>();
        if (playerPunishmentsCache.containsKey(xuid)) {
            List<Integer> punishmentIds = playerPunishmentsCache.get(xuid);
            for (Integer id : punishmentIds) {
                punishments.add(punishmentCache.get(id));
            }
            return punishments;
        }

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        ResultSet results;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement(GET_PUNISHMENTS_QUERY, new Object[]{ xuid, "server" }));
            results = stmt.executeQuery();
            List<Integer> playerPunishmentIds = new ArrayList<>();
            while (results.next()) {
                Optional<PunishmentCategory> category = this.getCategory(results.getString("code"));
                if (category.isPresent()) {
                    PunishmentEntry entry = new PunishmentEntryBuilder()
                            .setId(results.getInt("id"))
                            .setCode(results.getString("code"))
                            .setExpiresAt(results.getLong("expires"))
                            .setIssuedAt(results.getLong("created"))
                            .setIssuedBy(results.getString("author"))
                            .setReason(results.getString("offense"))
                            .setRemovedBy(results.getString("remover"))
                            .setRemovedReason(results.getString("removed_reason"))
                            .setType(category.get().getType())
                            .build();

                    punishments.add(
                            entry
                    );
                    playerPunishmentIds.add(entry.getId());
                    punishmentCache.put(results.getInt("id"), entry);
                }
            }
            this.playerPunishmentsCache.put(xuid, playerPunishmentIds);
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        return punishments;
    }

    public PunishmentFormManager getFormManager () {
        return this.formManager;
    }

    public Optional<PunishmentEntry> getCachedPunishment (int id) {
        return Optional.ofNullable(punishmentCache.getOrDefault(id, null));
    }

    public List<PunishmentEntry> getActivePunishments (Player player) throws SQLException {
        return getActivePunishments(player.getLoginChainData().getXUID());
    }

    public List<PunishmentEntry> getActivePunishments (String xuid) throws SQLException {
        return getPunishments(xuid).stream().filter(p -> !p.isExpired()).collect(Collectors.toList());
    }

    public boolean removePunishment (int id, String removalReason, CommandSender staffMember) throws SQLException {

        if (!punishmentCache.containsKey(id) || punishmentCache.get(id).isRemoved()) {
            return false;
        }

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        int rowsAffected;

        try {
            if (staffMember.isPlayer()) {
                stmt = wrapper.prepareStatement(new DatabaseStatement(REMOVE_PUNISHMENT_VIA_PLAYER_QUERY, new Object[]{ ((Player)staffMember).getLoginChainData().getXUID(), removalReason, -1, id }));
            } else {
                stmt = wrapper.prepareStatement(new DatabaseStatement(REMOVE_PUNISHMENT_VIA_CONSOLE_QUERY, new Object[]{ removalReason, -1, id }));
            }
            rowsAffected = stmt.executeUpdate();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        if (staffMember.isPlayer()) {
            punishmentCache.get(id).remove(((Player)staffMember).getLoginChainData().getUsername(), removalReason);
        } else {
            punishmentCache.get(id).remove(null, removalReason);
        }

        return rowsAffected > 0;
    }

    public void punish (String xuid, PunishmentCategory offense, String reason, CommandSender staffMember) throws SQLException {

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        Date currentDate = new Date();

        try {
            if (staffMember.isPlayer()) {
                Object[] args = new Object[]{ xuid, ((Player)staffMember).getLoginChainData().getXUID(), "server", reason ,offense.getCode(), currentDate.getTime(), null };
                if (offense.getDuration() == -1) {
                    args[6] = null;
                } else {
                    args[6] = currentDate.getTime() + offense.getDuration();
                }
                stmt = wrapper.prepareStatement(new DatabaseStatement(ADD_PUNISHMENT_VIA_PLAYER_QUERY, args));
            } else {
                Object[] args = new Object[]{ xuid, "server", reason, offense.getCode(), currentDate.getTime(), null };
                if (offense.getDuration() == -1) {
                    args[5] = null;
                } else {
                    args[5] = currentDate.getTime() + offense.getDuration();
                }
                stmt = wrapper.prepareStatement(new DatabaseStatement(ADD_PUNISHMENT_VIA_CONSOLE_QUERY, args));
            }
            stmt.execute();
            stmt.close();


            stmt = wrapper.prepareStatement(new DatabaseStatement(GET_LATEST_PUNISHMENT_OF_PLAYER_QUERY, new Object[]{ xuid, "server" }));
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                PunishmentEntry newestEntry = new PunishmentEntryBuilder()
                        .setId(results.getInt("id"))
                        .setCode(results.getString("code"))
                        .setExpiresAt(results.getLong("expires"))
                        .setIssuedAt(results.getLong("created"))
                        .setIssuedBy(results.getString("author"))
                        .setReason(results.getString("offense"))
                        .setRemovedBy(results.getString("remover"))
                        .setRemovedReason(results.getString("removed_reason"))
                        .setType(offense.getType())
                        .build();
                this.punishmentCache.put(newestEntry.getId(), newestEntry);
                List<Integer> playerPunishmentIds = this.playerPunishmentsCache.getOrDefault(xuid, new ArrayList<>());
                playerPunishmentIds.add(newestEntry.getId());
                this.playerPunishmentsCache.put(xuid, playerPunishmentIds);

                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                    // Is their player online? Call onPunish for the punishment.
                    for (Player player : this.plugin.getServer().getOnlinePlayers().values()) {
                        if (player.getLoginChainData().getXUID().equals(xuid)) {
                            Optional<PunishmentLogic> logic = this.getLogic(offense.getType());
                            if (logic.isPresent()) {
                                this.getLogic(newestEntry.getType()).get().onPunish(player, newestEntry);
                            }
                        }
                    }
                });

            }

        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    public static void setInstance (PunishmentManager inst) {
        instance = inst;
    }

    public static PunishmentManager getInstance() {
        return instance;
    }

}
