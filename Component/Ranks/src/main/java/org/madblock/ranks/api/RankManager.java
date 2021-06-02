package org.madblock.ranks.api;

import cn.nukkit.Player;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.ranks.enums.PrimaryRankID;
import org.madblock.ranks.enums.SubRankID;
import org.madblock.ranks.ranks.PrimaryRank;
import org.madblock.ranks.ranks.SubRank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankManager {

    private static RankManager instance;

    private static final String GET_RANKS_QUERY = "SELECT primary_ranks, sub_ranks FROM player_ranks WHERE xuid=?;";
    private static final String GET_PURCHASED_RANKS_QUERY = "SELECT PurchasedItemString AS rank_name FROM BuycraftQueue WHERE xuid=? AND PaymentProcessor='prem_ranks'";

    private final Map<String, RankProfile> cache = new ConcurrentHashMap<>();

    private final Map<Integer, PrimaryRank> primaryRanks = new HashMap<>();

    private final Map<Integer, SubRank> subRanks = new HashMap<>();

    public void addPrimaryRank (PrimaryRank rank) {
        primaryRanks.put(rank.getId(), rank);
    }

    public void addSubRank (SubRank rank) {
        subRanks.put(rank.getId(), rank);
    }

    public PrimaryRank getPrimaryRank (PrimaryRankID rank) {
        return primaryRanks.get(rank.getId());
    }

    public SubRank getSubRank (SubRankID rank) {
        return subRanks.get(rank.getId());
    }

    public Optional<RankProfile> getRankProfile (Player player) {
        return getRankProfile(player.getLoginChainData().getXUID());
    }

    public Optional<RankProfile> getRankProfile (String xuid) {
        if (cache.containsKey(xuid)) {
            return Optional.of(cache.get(xuid));
        }
        return Optional.empty();
    }

    public Optional<RankProfile> fetchRankProfile (Player player) throws SQLException {
        return fetchRankProfile(player.getLoginChainData().getXUID());
    }

    public Optional<RankProfile> fetchRankProfile (String xuid) throws SQLException {

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;

        RankProfile profile = null;

        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement(GET_RANKS_QUERY, new Object[]{ xuid }));
            ResultSet results = stmt.executeQuery();
            if (results.first()) {
                profile = new RankProfile(
                        this,
                        xuid,
                        parsePrimaryRanks(results.getInt("primary_ranks")),
                        parseSubRanks(results.getInt("sub_ranks"))
                );
            } else {
                profile = new RankProfile(
                        this,
                        xuid,
                        parsePrimaryRanks(PrimaryRankID.PLAYER.getId()),
                        parseSubRanks(0)
                );
            }
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        cache.put(xuid, profile);
        return Optional.of(profile);

    }

    public Set<PrimaryRankID> fetchRankPurchases (Player player) throws SQLException {
        return fetchRankPurchases(player.getLoginChainData().getXUID());
    }

    public Set<PrimaryRankID> fetchRankPurchases (String xuid) throws SQLException {

        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;

        Set<PrimaryRankID> ranks = new HashSet<>();

        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement(GET_PURCHASED_RANKS_QUERY, new Object[]{ xuid }));
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                switch (results.getString("rank_name").toLowerCase()) {
                    case "recruit":
                        ranks.add(PrimaryRankID.VIP);
                        break;
                    case "chief":
                        ranks.add(PrimaryRankID.VIP_PLUS);
                        break;
                    case "astro":
                        ranks.add(PrimaryRankID.ELITE);
                        break;
                    case "odyssey":
                        ranks.add(PrimaryRankID.OVERLORD);
                        break;
                }
            }
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        return ranks;

    }

    private List<PrimaryRankID> parsePrimaryRanks (int primaryRanksData) {
        List<PrimaryRankID> ranks = new ArrayList<>();
        for (PrimaryRank rank : primaryRanks.values()) {
            if ((primaryRanksData & rank.getId()) != 0) {
                ranks.add(rank.getRankInfo());
            }
        }
        return ranks;
    }

    private List<SubRankID> parseSubRanks (int subRanksData) {
        List<SubRankID> ranks = new ArrayList<>();
        for (SubRank rank : subRanks.values()) {
            if ((subRanksData & rank.getId()) != 0) {
                ranks.add(rank.getRankInfo());
            }
        }
        return ranks;
    }

    public static RankManager getInstance () {
        return instance;
    }

    public static void setInstance (RankManager manager) {
        instance = manager;
    }


}