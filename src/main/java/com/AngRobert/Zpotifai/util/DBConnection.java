package com.AngRobert.Zpotifai.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// simple utility class that helps with the database connection. initialized once and the connection is used with DBConnection.get();
public class DBConnection {
    private static Connection conn;

    public static void init() {
        try {
            String url  = System.getenv("DB_URL");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");
            conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static Connection get() { return conn; }

    public static void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            e.getMessage();
        }
    }
}
