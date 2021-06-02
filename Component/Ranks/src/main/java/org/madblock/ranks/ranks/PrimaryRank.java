package org.madblock.ranks.ranks;

import lombok.Getter;
import org.madblock.ranks.enums.PrimaryRankID;

import java.util.List;

@Getter
public class PrimaryRank extends Rank {
    private final PrimaryRankID rankInfo;

    private final int displayIndex;

    public PrimaryRank(PrimaryRankID rank, int displayIndex, List<String> permissions) {
        super(rank.getId(), permissions);
        rankInfo = rank;
        this.displayIndex = displayIndex;
    }
}