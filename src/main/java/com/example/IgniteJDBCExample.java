package com.example;

import java.sql.*;

public class IgniteJDBCExample {
    public static void main(String[] args) {
        String url = "jdbc:ignite:thin://127.0.0.1:10800";

        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();

            // stmt.execute("CREATE TABLE IF NOT EXISTS THINGS (id INT PRIMARY KEY, name VARCHAR)");

            // stmt.execute("INSERT INTO THINGS (id, name) VALUES (6, 'THING6')");
            // Insert a Person with a Long key.
            PreparedStatement stmt1 = conn
              .prepareStatement("INSERT INTO THINGS(id, name) VALUES(7, 'THING7')");

            stmt1.execute();
            ResultSet rs = stmt.executeQuery("SELECT * FROM THINGS");
            while (rs.next()) {
                System.out.println("Person [id=" + rs.getInt("id") + ", name=" + rs.getString("name") + "]");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
