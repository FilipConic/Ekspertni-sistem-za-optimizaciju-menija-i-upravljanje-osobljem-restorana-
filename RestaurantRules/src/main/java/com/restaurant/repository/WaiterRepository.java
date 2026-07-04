package com.restaurant.repository;

import com.restaurant.model.MenuItem;
import com.restaurant.model.Waiter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WaiterRepository {
    private final Connection connection;

    public WaiterRepository(Connection connection) throws SQLException {
        this.connection = connection;

        try (var stmt = connection
                .createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS waiter (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  name TEXT NOT NULL," +
                    "  avg_tables_per_shift REAL NOT NULL," +
                    "  avg_order_value REAL NOT NULL," +
                    "  guest_rating REAL NOT NULL" +
                    ");");
        }
    }

    /**
     * Inserts the waiter row and writes the generated id back onto
     * `waiter` - needed so it can be linked to a shift immediately
     * after being added, without a round-trip SELECT.
     */
    public void save(Waiter waiter) throws SQLException {
        try (var stmt = this.connection
                .prepareStatement("INSERT INTO waiter" +
                                "(name, avg_tables_per_shift, avg_order_value, guest_rating)" +
                                "VALUES(?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, waiter.getName());
            stmt.setDouble(2, waiter.getAvgTablesPerShift());
            stmt.setDouble(3, waiter.getAvgOrderValue());
            stmt.setDouble(4, waiter.getGuestRating());
            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    waiter.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Waiter> getAll() throws SQLException {
        try (var stmt = this.connection.createStatement()) {
            var rows = stmt.executeQuery("SELECT * FROM waiter;");

            List<Waiter> items = new ArrayList<Waiter>();
            while (rows.next()) {
                items.add(new Waiter(
                        rows.getInt("id"),
                        rows.getString("name"),
                        rows.getDouble("avg_tables_per_shift"),
                        rows.getDouble("avg_order_value"),
                        rows.getDouble("guest_rating")
                ));
            }

            return items;
        }
    }

    public void delete(int id) throws SQLException {
        try (var stmt = this.connection.prepareStatement("DELETE FROM waiter WHERE id = ?;")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void delete(Waiter waiter) throws SQLException {
        delete(waiter.getId());
    }
}
