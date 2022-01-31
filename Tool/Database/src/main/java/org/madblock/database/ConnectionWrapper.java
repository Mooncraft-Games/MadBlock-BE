package org.madblock.database;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@AllArgsConstructor
public class ConnectionWrapper {
    private final Connection connection;

    /**
     * Close the connection
     * @throws SQLException
     */
    public void close() throws SQLException {
        connection.close();
    }

    /**
     * Prepare a statement using this connection
     * @param statement The statement
     * @return a prepared statement given the query and parameters
     * @throws SQLException
     */
    public PreparedStatement prepareStatement(DatabaseStatement statement) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(statement.getSql());
        Object[] params = statement.getParams();
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }

    /** @return the connection wrapped by this object */
    public Connection getConnection() {
        return connection;
    }
}