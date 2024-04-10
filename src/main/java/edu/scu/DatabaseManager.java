package edu.scu;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private String dbPath;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;

    }
    public Connection connect() {
        Connection conn = null;
        try {
            // SQLite connection
            String url = "jdbc:sqlite:" + this.dbPath;
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void initializeDatabase(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS "+ tableName + " (\n"
                + " id integer PRIMARY KEY,\n"
                + " data text NOT NULL\n"
                + ");";
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            // Create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //SELECT statements
    public List<List<String>> executeQuery(String sql) {
        List<List<String>> results = new ArrayList<>();
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return results;
    }

    // INSERT, UPDATE, DELETE, or any SQL statements that modify the database but do not return a result set
    public void executeUpdate(String sql) {
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Transaction handling
    public void startTransaction() {
        try (Connection conn = this.connect()) {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void commitTransaction() {
        try (Connection conn = this.connect()) {
            conn.commit();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void rollbackTransaction() {
        try (Connection conn = this.connect()) {
            conn.rollback();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }




}
