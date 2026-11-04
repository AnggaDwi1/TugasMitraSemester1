package com.mycompany.mavenproject1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {

    private static final String DB   = "mitrasemester1"; // lihat di panel kiri phpMyAdmin
    private static final String URL  = "jdbc:mysql://localhost:3306/" + DB
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";
    private static final String USER = "root";
    private static final String PASS = ""; // XAMPP default kosong

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

