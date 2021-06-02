package org.madblock.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class DatabaseStatement {
    private final String sql;
    private final Object[] params;

    public DatabaseStatement(String sql) {
        this.sql = sql;
        params = new Object[0];
    }
}