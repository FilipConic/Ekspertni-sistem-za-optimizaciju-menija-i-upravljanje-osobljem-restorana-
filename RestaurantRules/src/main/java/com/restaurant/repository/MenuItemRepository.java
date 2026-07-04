package com.restaurant.repository;

import com.restaurant.model.MenuItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MenuItemRepository {
    private final Connection connection;

    public MenuItemRepository(Connection connection) throws SQLException {
        this.connection = connection;

        try (var stmt = this.connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS menu_item ("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  name TEXT NOT NULL,"
                    + "  category TEXT NOT NULL,"
                    + "  price REAL NOT NULL,"
                    + "  preparation_cost REAL NOT NULL,"
                    + "  weekly_sales INTEGER NOT NULL"
                    + ");"
            );
        }
    }

    public void save(MenuItem menuItem) throws SQLException {
        try (var stmt = this.connection.prepareStatement(
                "INSERT INTO menu_item"
                        + "(name, category, price, preparation_cost, weekly_sales)"
                        + "VALUES(?, ?, ?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, menuItem.getName());
            stmt.setString(2, menuItem.getCategory());
            stmt.setDouble(3, menuItem.getPrice());
            stmt.setDouble(4, menuItem.getPreparationCost());
            stmt.setInt(5, menuItem.getWeeklySales());
            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    menuItem.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<MenuItem> getAll() throws SQLException {
        try (var stmt = this.connection.createStatement()) {
            var rows = stmt.executeQuery("SELECT * FROM menu_item;");
            List<MenuItem> items = new ArrayList<>();

            while (rows.next()) {
                items.add(new MenuItem(
                        rows.getInt("id"),
                        rows.getString("name"),
                        rows.getString("category"),
                        rows.getDouble("price"),
                        rows.getDouble("preparation_cost"),
                        rows.getInt("weekly_sales")
                ));
            }

            return items;
        }
    }

    public void delete(int id) throws SQLException {
        try (var stmt = this.connection.prepareStatement("DELETE FROM menu_item WHERE id = ?;")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void delete(MenuItem item) throws SQLException {
        delete(item.getId());
    }
}
