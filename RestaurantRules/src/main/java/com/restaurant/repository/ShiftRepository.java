package com.restaurant.repository;

import com.restaurant.model.MenuItem;
import com.restaurant.model.Shift;
import com.restaurant.model.Waiter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShiftRepository {
    private final Connection connection;

    public ShiftRepository(Connection connection) throws SQLException {
        this.connection = connection;

        try (var stmt = this.connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS shift(" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  date TEXT NOT NULL," +
                    "  guest_count INTEGER NOT NULL," +
                    "  total_earnings REAL NOT NULL" +
                    ");");
        }

        try (var stmt = this.connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS shift_waiter_link(" +
                    "  waiter_id INTEGER NOT NULL," +
                    "  shift_id INTEGER NOT NULL," +
                    "  PRIMARY KEY (waiter_id, shift_id)," +
                    "  FOREIGN KEY (waiter_id) REFERENCES waiter(id) ON DELETE CASCADE," +
                    "  FOREIGN KEY (shift_id) REFERENCES shift(id) ON DELETE CASCADE" +
                    ");");
        }
    }

    /**
     * Inserts the shift row and writes the generated id back onto `shift`.
     * Does NOT persist waiter links - call connectWaiter(...) separately
     * for each waiter on the shift (needs shift.getId() to be set first,
     * which this method takes care of).
     */
    public void save(Shift shift) throws SQLException {
        try (var stmt = this.connection.prepareStatement(
                "INSERT INTO shift(date, guest_count, total_earnings) VALUES(?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, shift.getDate().toString());
            stmt.setInt(2, shift.getGuestCount());
            stmt.setDouble(3, shift.getTotalEarnings());
            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    shift.setId(keys.getInt(1));
                }
            }
        }
    }

    public void connectWaiter(int shift_id, int waiter_id) throws SQLException {
        try (var stmt = this.connection
                .prepareStatement("INSERT INTO shift_waiter_link" +
                        "(shift_id, waiter_id) " +
                        "VALUES(?, ?);")) {
            stmt.setInt(1, shift_id);
            stmt.setInt(2, waiter_id);
            stmt.executeUpdate();
        }
    }

    public void connectWaiter(Shift shift, Waiter waiter) throws SQLException {
        if (shift.getId() == 0 || waiter.getId() == 0) { return; }
        this.connectWaiter(shift.getId(), waiter.getId());
    }

    public List<Integer> getWaiterIds(int shiftId) throws SQLException {
        try (var stmt = this.connection.prepareStatement(
                "SELECT waiter_id FROM shift_waiter_link WHERE shift_id = ?;")) {
            stmt.setInt(1, shiftId);
            try (var rows = stmt.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rows.next()) {
                    ids.add(rows.getInt("waiter_id"));
                }
                return ids;
            }
        }
    }

    public List<Shift> getAll() throws SQLException {
        try (var stmt = this.connection.createStatement()) {
            var rows = stmt.executeQuery("SELECT * FROM shift;");

            List<Shift> items = new ArrayList<Shift>();
            while (rows.next()) {
                int id = rows.getInt("id");
                items.add(new Shift(
                        id,
                        LocalDate.parse(rows.getString("date")),
                        this.getWaiterIds(id),
                        rows.getInt("guest_count"),
                        rows.getDouble("total_earnings")
                ));
            }
            return items;
        }
    }

    public void delete(int id) throws SQLException {
        try (var stmt = this.connection.prepareStatement("DELETE FROM shift WHERE id = ?;")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void delete(Shift shift) throws SQLException {
        delete(shift.getId());
    }
}
