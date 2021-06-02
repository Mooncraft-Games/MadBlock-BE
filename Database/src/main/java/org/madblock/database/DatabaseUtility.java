package org.madblock.database;

import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtility {
    /**
     * Close a statement without throwing any errors.
     * @param stmt The {@link Statement} being closed.
     */
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Close a connection without throwing any errors
     * @param wrapper The {@link ConnectionWrapper} being closed.
     */
    public static void closeQuietly(ConnectionWrapper wrapper) {
        if (wrapper != null) {
            try {
                wrapper.close();
            } catch (SQLException ignored) {}
        }
    }
}