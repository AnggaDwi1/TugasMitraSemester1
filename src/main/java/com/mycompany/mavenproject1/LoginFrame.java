package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginFrame extends JFrame {

    private JTextField txtRfid;

    public LoginFrame() {
        setTitle("Login Kasir - Tahu Walik");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Tema.BG);

        JPanel pnlTengah = new JPanel(new GridLayout(2, 1, 0, 15));
        pnlTengah.setOpaque(false);
        pnlTengah.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 40));

        JLabel lblJudul = Tema.judul("Silakan Tap Kartu Kasir");
        lblJudul.setHorizontalAlignment(SwingConstants.CENTER);
        
        txtRfid = new JTextField();
        txtRfid.setHorizontalAlignment(SwingConstants.CENTER);
        txtRfid.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        txtRfid.addActionListener(e -> prosesLogin(txtRfid.getText().trim()));

        pnlTengah.add(lblJudul);
        pnlTengah.add(txtRfid);

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

        String sql = "SELECT id_kasir, nama_kasir FROM kasir WHERE uid_kartu = ? AND status = 'aktif'";
        
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uidCard);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sesi.idKasir = rs.getInt("id_kasir");
                    Sesi.namaKasir = rs.getString("nama_kasir");
                    
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
}