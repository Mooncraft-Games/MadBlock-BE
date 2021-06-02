package org.madblock.ranks.ranks;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor @Getter
public abstract class Rank {
    private final int id;
    private final List<String> permissions;

    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }
}