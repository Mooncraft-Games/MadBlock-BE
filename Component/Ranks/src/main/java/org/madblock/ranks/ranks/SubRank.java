package org.madblock.ranks.ranks;

import lombok.Getter;
import org.madblock.ranks.enums.SubRankID;

import java.util.List;

@Getter
public class SubRank extends Rank {
    private final SubRankID rankInfo;

    public SubRank(SubRankID rank, List<String> permissions) {
        super(rank.getId(), permissions);
        rankInfo = rank;
    }
}