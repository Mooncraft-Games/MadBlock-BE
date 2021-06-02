package org.madblock.ranks.api;

import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.ranks.enums.PrimaryRankID;
import org.madblock.ranks.enums.SubRankID;
import org.madblock.ranks.ranks.PrimaryRank;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class RankProfile {

    private final Set<PrimaryRankID> primaryRanks;

    private final Set<SubRankID> subRanks;

    private final String xuid;

    private final RankManager manager;

    public RankProfile (RankManager manager, String xuid, List<PrimaryRankID> primaryRankList, List<SubRankID> subRankList) {
        primaryRanks = new ConcurrentSkipListSet<>(primaryRankList);
        subRanks = new ConcurrentSkipListSet<>(subRankList);
        this.manager = manager;
        this.xuid = xuid;
    }

    public boolean hasPermission (String permission) {
        for (PrimaryRankID rank : primaryRanks) {
            if (this.manager.getPrimaryRank(rank).hasPermission(permission)) {
                return true;
            }
        }
        for (SubRankID rank : subRanks) {
            if (this.manager.getSubRank(rank).hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPrimaryRank (PrimaryRankID rank) {
        return primaryRanks.contains(rank);
    }

    public boolean hasSubRank (SubRankID rank) {
        return subRanks.contains(rank);
    }

    public Set<PrimaryRankID> getPrimaryRanks () {
        return Collections.unmodifiableSet(primaryRanks);
    }

    public Set<SubRankID> getSubRanks () {
        return Collections.unmodifiableSet(subRanks);
    }

    public PrimaryRankID getPrimaryDisplayedRank () {
        PrimaryRank topRank = null;
        for (PrimaryRankID rank : primaryRanks) {
            if (topRank == null || manager.getPrimaryRank(rank).getDisplayIndex() > topRank.getDisplayIndex()) {
                topRank = manager.getPrimaryRank(rank);
            }
        }
        if (topRank != null) {
            return topRank.getRankInfo();
        }
        return null;
    }

    public boolean removePrimaryRank (PrimaryRankID rank) throws SQLException {
        if (hasPrimaryRank(rank)) {
            primaryRanks.remove(rank);
            updatePrimaryRankData();
            return true;
        }
        return false;
    }

    public boolean removeSubRank (SubRankID subRank) throws SQLException {
        if (hasSubRank(subRank)) {
            subRanks.remove(subRank);
            updateSubRankData();
            return true;
        }
        return false;
    }

    public boolean addPrimaryRank (PrimaryRankID rank) throws SQLException {
        if (hasPrimaryRank(rank)) {
            return false;
        }
        int generatedPrimaryRankData = generatePrimaryRankData();
        int generatedSubRankData = generateSubRankData();
        primaryRanks.add(rank);
        if (generatedPrimaryRankData == PrimaryRankID.PLAYER.getId() && generatedSubRankData == 0) {
            // create
            addRankData();
        } else {
            updatePrimaryRankData();
        }
        return true;
    }

    public boolean addSubRank (SubRankID subRank) throws SQLException {
        if (hasSubRank(subRank)) {
            return false;
        }
        int generatedPrimaryRankData = generatePrimaryRankData();
        int generatedSubRankData = generateSubRankData();
        subRanks.add(subRank);
        if (generatedPrimaryRankData == PrimaryRankID.PLAYER.getId() && generatedSubRankData == 0) {
            addRankData();
        } else {
            updateSubRankData();
        }
        return true;
    }

    private int generatePrimaryRankData () {
        int data = 0;
        for (PrimaryRankID rank : primaryRanks) {
            data += rank.getId();
        }
        return data;
    }

    private int generateSubRankData () {
        int data = 0;
        for (SubRankID rank : subRanks) {
            data += rank.getId();
        }
        return data;
    }

    private void addRankData () throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement("INSERT INTO player_ranks (xuid, primary_ranks, sub_ranks) VALUES (?, ?, ?)", new Object[]{ xuid, generatePrimaryRankData(), generateSubRankData() }));
            stmt.execute();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    private void deleteRankData () throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement("DELETE FROM player_ranks WHERE xuid=?", new Object[]{ xuid }));
            stmt.execute();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    private void updateSubRankData () throws SQLException {
        if (generateSubRankData() == 0 && generatePrimaryRankData() == PrimaryRankID.PLAYER.getId()) {
            deleteRankData();
            return;
        }
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement("UPDATE player_ranks SET sub_ranks=? WHERE xuid=?", new Object[]{ generateSubRankData(), xuid }));
            stmt.execute();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    private void updatePrimaryRankData () throws SQLException {
        if (generateSubRankData() == 0 && generatePrimaryRankData() == PrimaryRankID.PLAYER.getId()) {
            deleteRankData();
            return;
        }
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement("UPDATE player_ranks SET primary_ranks=? WHERE xuid=?", new Object[]{ generatePrimaryRankData(), xuid }));
            stmt.execute();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }


}