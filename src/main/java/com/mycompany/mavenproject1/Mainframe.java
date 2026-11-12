package com.mycompany.mavenproject1;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Mainframe extends JFrame {

    CardLayout kartu = new CardLayout();
    JPanel isi = new JPanel(kartu);

    DashboardPanel dashboard = new DashboardPanel();
    TransaksiPanel transaksi = new TransaksiPanel();
    MenuPanel menu = new MenuPanel();
    SettingPanel setting = new SettingPanel();
    LaporanPanel laporan = new LaporanPanel();

    List<Tema.NavTombol> navTombol = new ArrayList<>();

    StringBuilder rfidBuffer = new StringBuilder();
    long lastKeyTime = 0;
    KeyEventDispatcher rfidDispatcher;

    public Mainframe() {
        setTitle("Kasir Tahu Walik");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        isi.add(dashboard, "dashboard");
        isi.add(menu, "menu");
        isi.add(transaksi, "transaksi");
        isi.add(setting, "setting");
        isi.add(laporan, "laporan");

        add(buatSidebar(), BorderLayout.WEST);
        add(isi, BorderLayout.CENTER);

        pasangListenerRFID();

        buka("OWNER".equalsIgnoreCase(Sesi.roleKasir) ? "dashboard" : "transaksi", 0);
    }
    
void pasangListenerRFID() {
        rfidDispatcher = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // Menggunakan KEY_PRESSED agar tombol Enter selalu terdeteksi dengan pasti
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    long waktuSekarang = System.currentTimeMillis();
                    
                    // Batas waktu diperbesar jadi 500ms agar alat yang agak lambat tetap terbaca
                    if (waktuSekarang - lastKeyTime > 500) { 
                        rfidBuffer.setLength(0);
                    }
                    lastKeyTime = waktuSekarang;

                    // Deteksi tombol Enter (kode tombol 10 atau VK_ENTER)
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (rfidBuffer.length() >= 5) {
                            String uid = rfidBuffer.toString();
                            rfidBuffer.setLength(0); // Kosongkan memori ketikan
                            prosesTapKartu(uid); // Jalankan fungsi logout
                            return true; // Cegah tombol Enter menekan menu lain
                        }
                        rfidBuffer.setLength(0);
                    } else {
                        // Kumpulkan angka yang diketik alat
                        char c = e.getKeyChar();
                        if (Character.isDigit(c)) {
                            rfidBuffer.append(c);
                        }
                    }
                }
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(rfidDispatcher);
    }

    void prosesTapKartu(String uid) {
        // Mencegah double tap dalam waktu kurang dari 2 detik (2000 milidetik)
        if (System.currentTimeMillis() - Sesi.waktuTerakhirTap < 2000) {
            return;
        }

        String sql = "SELECT id_kasir FROM kasir WHERE uid_kartu = ? AND status = 'aktif'";
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sesi.waktuTerakhirTap = System.currentTimeMillis(); 
                    
                    // Memberikan jeda sangat kecil (100ms) agar ketikan alat benar-benar selesai
                    // sebelum pindah kembali ke layar Login.
                    SwingUtilities.invokeLater(() -> {
                        try { Thread.sleep(100); } catch (Exception ex) {}
                        logoutOtomatis();
                    });
                }
            }
        } catch (Exception e) {
            Db.error(e);
        }
    }

void logoutOtomatis() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(rfidDispatcher);
        
        // --- MENCATAT JAM KELUAR (LOGOUT) ---
        if (Sesi.idLog > 0) {
            String sqlLog = "UPDATE log_kasir SET waktu_logout = CURRENT_TIMESTAMP WHERE id_log = ?";
            try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sqlLog)) {
                ps.setInt(1, Sesi.idLog);
                ps.executeUpdate();
            } catch (Exception e) {
                Db.error(e);
            }
        }
        // ------------------------------------
        
        Sesi.idKasir = 0;
        Sesi.namaKasir = "";
        Sesi.roleKasir = "KARYAWAN";
        Sesi.idLog = 0;
        
        // KUNCI PENTING: Reset timer tepat saat logout selesai diproses
        Sesi.waktuTerakhirTap = System.currentTimeMillis();
        
        new LoginFrame().setVisible(true);
        this.dispose();
    }

    String inisial(String nama) {
        String[] p = nama.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return "?";
        String s = p[0].substring(0, 1);
        if (p.length > 1 && !p[1].isEmpty()) s += p[1].substring(0, 1);
        return s.toUpperCase();
    }

    JPanel buatSidebar() {
        // ----- kartu logo -----
        Tema.Kartu logo = new Tema.Kartu().warna(Tema.KUNING);
        logo.setLayout(new GridLayout(2, 1, 0, 0));
        logo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel l1 = new JLabel("TAHU WALIK", SwingConstants.CENTER);
        l1.setFont(new Font("SansSerif", Font.BOLD, 20));
        l1.setForeground(new Color(0x2B1A0E));
        JLabel l2 = new JLabel("CHIKA CHIKU", SwingConstants.CENTER);
        l2.setFont(new Font("SansSerif", Font.BOLD, 11));
        l2.setForeground(Tema.COKELAT);
        logo.add(l1);
        logo.add(l2);

        // ----- menu navigasi -----
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6));
        nav.setOpaque(false);
        String[] nama = {"Dashboard", "Menu", "Transaksi", "Laporan", "Setting"};
        String[] kunci = {"dashboard", "menu", "transaksi", "laporan", "setting"};
        for (int i = 0; i < nama.length; i++) {
            if (!"OWNER".equalsIgnoreCase(Sesi.roleKasir) && !"transaksi".equals(kunci[i])) continue;
            final int idx = i;
            Tema.NavTombol b = new Tema.NavTombol(nama[i]);
            b.addActionListener(e -> buka(kunci[idx], navTombol.indexOf(b)));
            navTombol.add(b);
            nav.add(b);
        }
        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.setOpaque(false);
        navWrap.add(nav, BorderLayout.NORTH);

        JPanel atas = new JPanel(new BorderLayout(0, 20));
        atas.setOpaque(false);
        atas.add(logo, BorderLayout.NORTH);
        atas.add(navWrap, BorderLayout.CENTER);

        // ----- profil kasir + logout -----
        Tema.Kartu profil = new Tema.Kartu().warna(Tema.CREAM);
        profil.setLayout(new BorderLayout(10, 0));
        profil.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel info = new JLabel("<html><b>" + Tema.esc(Sesi.namaKasir) + "</b><br>"
                + "<span style='color:#1B9E6B;font-size:10px'>&#9679; "
                + ("OWNER".equalsIgnoreCase(Sesi.roleKasir) ? "Owner" : "Karyawan") + "</span></html>");
        profil.add(Tema.avatar(inisial(Sesi.namaKasir), 38), BorderLayout.WEST);
        profil.add(info, BorderLayout.CENTER);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setForeground(Tema.MERAH);
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logoutManual());

        JPanel bawah = new JPanel(new BorderLayout(0, 8));
        bawah.setOpaque(false);
        bawah.add(profil, BorderLayout.NORTH);
        bawah.add(btnLogout, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setPreferredSize(new Dimension(230, 0));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.GARIS),
                BorderFactory.createEmptyBorder(18, 14, 18, 14)));
        wrap.add(atas, BorderLayout.NORTH);
        wrap.add(bawah, BorderLayout.SOUTH);
        return wrap;
    }

    void logoutManual() {
        int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin logout?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (konfirmasi == JOptionPane.YES_OPTION) {
            Sesi.waktuTerakhirTap = System.currentTimeMillis();
            logoutOtomatis();
        }
    }

    void buka(String nama, int aktif) {
        if (!"OWNER".equalsIgnoreCase(Sesi.roleKasir) && !"transaksi".equals(nama)) return;
        switch (nama) {
            case "dashboard": dashboard.segarkan(); break;
            case "menu": menu.segarkan(); break;
            case "transaksi": transaksi.segarkan(); break;
            case "setting": setting.segarkan(); break;
            case "laporan": laporan.segarkan(); break;
        }
        kartu.show(isi, nama);
        for (int i = 0; i < navTombol.size(); i++) {
            navTombol.get(i).setAktif(i == aktif);
        }
    }
}
