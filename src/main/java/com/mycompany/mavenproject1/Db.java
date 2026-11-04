package com.mycompany.mavenproject1;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;


public class Db {

    /** Query yang hasilnya 1 angka, misal SELECT COUNT(*) ... */
    public static long angka(String sql) {
        try (Connection c = Koneksi.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            error(e);
        }
        return 0;
    }

    /** Query untuk isi JTable. Jumlah kolom SELECT harus sama dengan panjang array kolom. */
    public static DefaultTableModel kueri(String sql, String[] kolom) {
        DefaultTableModel m = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        try (Connection c = Koneksi.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[kolom.length];
                for (int i = 0; i < kolom.length; i++) row[i] = rs.getObject(i + 1);
                m.addRow(row);
            }
        } catch (SQLException e) {
            error(e);
        }
        return m;
    }

    public static void error(Exception e) {
        JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
    }
}