package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;

public class LoginFrame extends JFrame {

    private JTextField txtRfid;

    public LoginFrame() {
        setTitle("Login Kasir - Tahu Walik");
        setSize(430, 330);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Tema.BG);

        JPanel pnlTengah = new JPanel(new GridLayout(0, 1, 0, 10));
        pnlTengah.setOpaque(false);
        pnlTengah.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JLabel lblJudul = Tema.judul("Login Kasir");
        lblJudul.setHorizontalAlignment(SwingConstants.CENTER);
        txtRfid = new JTextField();
        txtRfid.setToolTipText("Tap kartu RFID di sini");
        txtRfid.setHorizontalAlignment(SwingConstants.CENTER);
        txtRfid.setFont(new Font("SansSerif", Font.BOLD, 16));
        txtRfid.addActionListener(e -> prosesLogin(txtRfid.getText().trim()));
        JTextField txtNama = new JTextField();
        JPasswordField txtPin = new JPasswordField();
        JButton btnPin = Tema.tombol("Masuk dengan PIN", Tema.BIRU);
        btnPin.addActionListener(e -> prosesLoginPin(txtNama.getText().trim(), txtPin.getPassword()));
        txtPin.addActionListener(e -> prosesLoginPin(txtNama.getText().trim(), txtPin.getPassword()));

        pnlTengah.add(lblJudul);
        pnlTengah.add(new JLabel("Tap kartu RFID"));
        pnlTengah.add(txtRfid);
        pnlTengah.add(new JLabel("Atau masuk dengan nama dan PIN"));
        pnlTengah.add(txtNama);
        pnlTengah.add(txtPin);
        pnlTengah.add(btnPin);

        add(pnlTengah, BorderLayout.CENTER);
    }

private void prosesLogin(String uidCard) {
        txtRfid.setText(""); // Langsung bersihkan form
        
        if (uidCard.isEmpty()) {
            return;
        }

        // UBAH JADI 4000 (4 detik) agar kartu yang telat diangkat tidak memicu login otomatis
        if (System.currentTimeMillis() - Sesi.waktuTerakhirTap < 4000) {
            return; 
        }

        String sql = "SELECT id_kasir, nama_kasir, role FROM kasir WHERE uid_kartu = ? AND status = 'aktif'";
        
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uidCard);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sesi.idKasir = rs.getInt("id_kasir");
                    Sesi.namaKasir = rs.getString("nama_kasir");
                    Sesi.roleKasir = rs.getString("role");
                    
                    // Update timer saat login sukses
                    Sesi.waktuTerakhirTap = System.currentTimeMillis();
                    
                    // --- MENCATAT JAM MASUK (LOGIN) ---
                    String sqlLog = "INSERT INTO log_kasir (id_kasir) VALUES (?)";
                    try (PreparedStatement psLog = c.prepareStatement(sqlLog, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        psLog.setInt(1, Sesi.idKasir);
                        psLog.executeUpdate();
                        try (ResultSet rsLog = psLog.getGeneratedKeys()) {
                            if (rsLog.next()) {
                                Sesi.idLog = rsLog.getInt(1); 
                            }
                        }
                    }
                    // ----------------------------------
                    
                    new Mainframe().setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Akses Ditolak! Kartu tidak terdaftar atau nonaktif.", "Gagal Login", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            Db.error(e);
        }
    }

    private void prosesLoginPin(String nama, char[] pin) {
        if (nama.isEmpty() || pin.length == 0) {
            JOptionPane.showMessageDialog(this, "Isi nama kasir dan PIN.");
            return;
        }
        String sql = "SELECT id_kasir, nama_kasir, role, pin_hash FROM kasir WHERE nama_kasir = ? AND status = 'aktif'";
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nama);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !verifikasiPin(pin, rs.getString("pin_hash"))) {
                    JOptionPane.showMessageDialog(this, "Nama atau PIN salah.", "Login gagal", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Sesi.idKasir = rs.getInt("id_kasir");
                Sesi.namaKasir = rs.getString("nama_kasir");
                Sesi.roleKasir = rs.getString("role");
                Sesi.waktuTerakhirTap = System.currentTimeMillis();
                try (PreparedStatement psLog = c.prepareStatement(
                        "INSERT INTO log_kasir (id_kasir) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    psLog.setInt(1, Sesi.idKasir);
                    psLog.executeUpdate();
                    try (ResultSet log = psLog.getGeneratedKeys()) { if (log.next()) Sesi.idLog = log.getInt(1); }
                }
                new Mainframe().setVisible(true);
                dispose();
            }
        } catch (SQLException e) {
            Db.error(e);
        } finally {
            java.util.Arrays.fill(pin, '\0');
        }
    }

    private boolean verifikasiPin(char[] pin, String encoded) {
        if (encoded == null || encoded.isEmpty()) return false;
        try {
            String[] parts = encoded.split("\\$", 2);
            if (parts.length != 2) return false;
            byte[] salt = java.util.Base64.getDecoder().decode(parts[0]);
            byte[] expected = java.util.Base64.getDecoder().decode(parts[1]);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(pin, salt, 120000, expected.length * 8);
            byte[] actual = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }
}
