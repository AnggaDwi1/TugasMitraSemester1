package com.mycompany.mavenproject1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {

    private static final String DB   = System.getProperty("kasir.db", "mitrasemester1");
    private static final String HOST = System.getProperty("kasir.db.host", "localhost");
    private static final String PORT = System.getProperty("kasir.db.port", "3306");
    private static final String URL  = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";
    private static final String USER = System.getProperty("kasir.db.user", "root");
    private static final String PASS = System.getProperty("kasir.db.password", "");

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Tes koneksi: klik kanan file ini, lalu Run File
    public static void main(String[] args) {
        try (Connection c = get()) {
            System.out.println("Koneksi BERHASIL ke database: " + c.getCatalog());
        } catch (SQLException e) {
            System.out.println("Koneksi GAGAL: " + e.getMessage());
        }
    }
}

