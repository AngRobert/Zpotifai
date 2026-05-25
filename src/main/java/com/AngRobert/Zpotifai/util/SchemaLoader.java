package com.AngRobert.Zpotifai.util;

import java.io.InputStream;
import java.sql.Connection;

public class SchemaLoader {
    public static void run() {
        try {
            InputStream is = SchemaLoader.class.getClassLoader().getResourceAsStream("schema.sql");
            if (is == null) {
                System.out.println("schema.sql not found");
                return;
            }
            String sql = new String(is.readAllBytes());
            Connection conn = DBConnection.get();
            if (conn != null) {
                conn.createStatement().execute(sql);
                

                System.out.println("Schema initialized successfully.");
            } else {
                System.out.println("Database connection is null. Cannot initialize schema.");
            }
        } catch (Exception e) {
            System.out.println("Schema init failed: " + e.getMessage());
        }
    }
}
