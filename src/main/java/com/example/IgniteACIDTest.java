package com.example;

import java.sql.*;

public class IgniteACIDTest {

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1/";

    public static void main(String[] args) {
        try (
            Connection conn = DriverManager.getConnection(JDBC_URL);
            Statement stmt = conn.createStatement()
        ) {
            // Clean slate
            stmt.executeUpdate("DROP TABLE IF EXISTS Things");

            // CREATE TABLE with TRANSACTIONAL atomicity
            stmt.executeUpdate(
                "CREATE TABLE Things (" +
                "  id INT PRIMARY KEY, " +
                "  name VARCHAR" +
                ") WITH \"atomicity=TRANSACTIONAL\""
            );

            // Start transaction (Atomicity, Isolation)
            conn.setAutoCommit(false);

            // Insert rows
            stmt.executeUpdate("INSERT INTO Things (id, name) VALUES (1, 'Alpha')");
            stmt.executeUpdate("INSERT INTO Things (id, name) VALUES (2, 'Beta')");

            // Simulate a failure
            try {
                stmt.executeUpdate("INSERT INTO Things (id, name) VALUES (1, 'Conflict')"); // duplicate key
                conn.commit(); // should not reach here
            } catch (SQLException e) {
                System.out.println("Rolling back due to failure: " + e.getMessage());
                conn.rollback(); // Atomicity test: all operations must rollback
            }

            // Verify rollback: nothing should be committed
            ResultSet rs = stmt.executeQuery("SELECT * FROM Things");
            System.out.println("Data after failed transaction (should be empty):");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ", " + rs.getString("name"));
            }

            // Try again with valid data
            stmt.executeUpdate("INSERT INTO Things (id, name) VALUES (3, 'Gamma')");
            stmt.executeUpdate("INSERT INTO Things (id, name) VALUES (4, 'Delta')");
            conn.commit(); // Durability test: commit successfully

            // Check committed data
            rs = stmt.executeQuery("SELECT * FROM Things");
            System.out.println("Data after successful commit:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ", " + rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
