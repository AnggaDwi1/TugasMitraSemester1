package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;

public class LoginFrame extends JFrame {

    private JTextField txtRfid;
    private JTextField txtNama;
    private JPasswordField txtPin;

    public LoginFrame() {
        setTitle("Login Kasir - Tahu Walik");
        setSize(900, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // ===== PANEL UTAMA: kiri (form) + kanan (banner) =====
        JPanel utama = new JPanel(new GridLayout(1, 2));
        utama.setBackground(Color.WHITE);
        add(utama, BorderLayout.CENTER);

        utama.add(buatPanelForm());
        utama.add(buatPanelBanner());
    }

    // ─── SISI KIRI: FORM LOGIN ────────────────────────────────
    private JPanel buatPanelForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(320, 420));

        // Logo / Brand
        JLabel lblBrand = new JLabel("TAHU WALIK");
        lblBrand.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblBrand.setForeground(Tema.KUNING);
        lblBrand.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblBrand);

        form.add(Box.createVerticalStrut(30));

        // Greeting
        JLabel lblHello = new JLabel("Selamat Datang!");
        lblHello.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblHello.setForeground(Tema.COKELAT);
        lblHello.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblHello);

        form.add(Box.createVerticalStrut(6));

        JLabel lblSub = new JLabel("Silakan masuk untuk melanjutkan.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(Tema.ABU);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblSub);

        form.add(Box.createVerticalStrut(28));

        // ── Field: Tap kartu RFID ──
        form.add(buatLabel("Tap Kartu RFID"));
        form.add(Box.createVerticalStrut(6));
        txtRfid = buatTextField("Tempelkan kartu di reader...");
        txtRfid.addActionListener(e -> prosesLogin(txtRfid.getText().trim()));
        form.add(txtRfid);

        form.add(Box.createVerticalStrut(16));

        // ── Separator ──
        JPanel sep = new JPanel(new BorderLayout(10, 0));
        sep.setOpaque(false);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        JSeparator s1 = new JSeparator();
        s1.setForeground(Tema.GARIS);
        JSeparator s2 = new JSeparator();
        s2.setForeground(Tema.GARIS);
        JLabel lblAtau = new JLabel("atau", SwingConstants.CENTER);
        lblAtau.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblAtau.setForeground(Tema.ABU);
        sep.add(s1, BorderLayout.WEST);
        // Use a trick: put separators in a box
        JPanel sepLine = new JPanel(new GridLayout(1, 3, 8, 0));
        sepLine.setOpaque(false);
        sepLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        sepLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel lineLeft = new JPanel(new BorderLayout());
        lineLeft.setOpaque(false);
        lineLeft.add(new JSeparator() {{ setForeground(Tema.GARIS); }}, BorderLayout.CENTER);
        JPanel lineRight = new JPanel(new BorderLayout());
        lineRight.setOpaque(false);
        lineRight.add(new JSeparator() {{ setForeground(Tema.GARIS); }}, BorderLayout.CENTER);
        sepLine.add(lineLeft);
        sepLine.add(lblAtau);
        sepLine.add(lineRight);
        form.add(sepLine);

        form.add(Box.createVerticalStrut(16));

        // ── Field: Nama Kasir ──
        form.add(buatLabel("Nama Kasir"));
        form.add(Box.createVerticalStrut(6));
        txtNama = buatTextField("Masukkan nama kasir");
        form.add(txtNama);

        form.add(Box.createVerticalStrut(12));

        // ── Field: PIN ──
        form.add(buatLabel("PIN"));
        form.add(Box.createVerticalStrut(6));
        txtPin = new JPasswordField();
        txtPin.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtPin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        txtPin.setPreferredSize(new Dimension(300, 42));
        txtPin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.GARIS, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        txtPin.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPin.addActionListener(e -> prosesLoginPin(txtNama.getText().trim(), txtPin.getPassword()));
        form.add(txtPin);

        form.add(Box.createVerticalStrut(24));

        // ── Tombol Masuk ──
        JButton btnMasuk = new JButton("Masuk dengan PIN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(Tema.KUNING.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(Tema.KUNING.brighter());
                } else {
                    g2.setColor(Tema.KUNING);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnMasuk.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnMasuk.setForeground(Tema.COKELAT);
        btnMasuk.setContentAreaFilled(false);
        btnMasuk.setBorderPainted(false);
        btnMasuk.setFocusPainted(false);
        btnMasuk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMasuk.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnMasuk.setPreferredSize(new Dimension(300, 46));
        btnMasuk.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnMasuk.addActionListener(e -> prosesLoginPin(txtNama.getText().trim(), txtPin.getPassword()));
        form.add(btnMasuk);

        form.add(Box.createVerticalGlue());

        // Footer
        JLabel lblFooter = new JLabel("© 2025 Tahu Walik • Kasir Point of Sale");
        lblFooter.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(0xC0B09A));
        lblFooter.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblFooter);

        wrapper.add(form);
        return wrapper;
    }

    // ─── SISI KANAN: BANNER DEKORATIF ─────────────────────────
    private JPanel buatPanelBanner() {
        JPanel banner = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, Tema.KUNING, w, h, new Color(0xFFE082));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Lingkaran dekoratif transparan
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g2.setColor(Color.WHITE);
                g2.fillOval(w - 180, -60, 300, 300);
                g2.fillOval(-80, h - 200, 250, 250);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
                g2.fillOval(w / 2 - 100, h / 2 - 80, 200, 200);

                // Garis dekoratif
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.setStroke(new BasicStroke(1.5f));
                for (int i = 0; i < 6; i++) {
                    int y = 80 + i * 70;
                    g2.drawLine(30, y, w - 30, y + 20);
                }

                g2.dispose();
            }
        };
        banner.setOpaque(false);

        // Konten di dalam banner
        JPanel konten = new JPanel();
        konten.setLayout(new BoxLayout(konten, BoxLayout.Y_AXIS));
        konten.setOpaque(false);

        // Ikon tahu walik (emoji placeholder)
        JLabel lblIcon = new JLabel("🍽");
        lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 56));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        konten.add(lblIcon);

        konten.add(Box.createVerticalStrut(20));

        JLabel lblTitle = new JLabel("Tahu Walik");
        lblTitle.setFont(new Font("Serif", Font.BOLD, 34));
        lblTitle.setForeground(Tema.COKELAT);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        konten.add(lblTitle);

        konten.add(Box.createVerticalStrut(8));

        JLabel lblTagline = new JLabel("Point of Sale System");
        lblTagline.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblTagline.setForeground(new Color(0x6B4226));
        lblTagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        konten.add(lblTagline);

        konten.add(Box.createVerticalStrut(30));

        // Info card di dalam banner
        JPanel infoCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(Tema.COKELAT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        infoCard.setOpaque(false);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        infoCard.setMaximumSize(new Dimension(280, 120));
        infoCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblQuote = new JLabel("<html><div style='text-align:center;color:white;'>"
                + "Renyah di luar,<br>lembut di dalam.<br>"
                + "<i>Tahu Walik khas Batu.</i></div></html>");
        lblQuote.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblQuote.setForeground(Color.WHITE);
        lblQuote.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoCard.add(lblQuote);

        konten.add(infoCard);

        banner.add(konten);
        return banner;
    }

    // ─── HELPER: label & text field ───────────────────────────
    private JLabel buatLabel(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Tema.COKELAT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField buatTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0xC0B09A));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }
        };
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        tf.setPreferredSize(new Dimension(300, 42));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.GARIS, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        // repaint on focus to show/hide placeholder
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { tf.repaint(); }
            @Override public void focusLost(FocusEvent e) { tf.repaint(); }
        });
        return tf;
    }

    // ─── PROSES LOGIN RFID ────────────────────────────────────
    private void prosesLogin(String uidCard) {
        txtRfid.setText("");
        if (uidCard.isEmpty()) return;
        if (System.currentTimeMillis() - Sesi.waktuTerakhirTap < 4000) return;

        String sql = "SELECT id_kasir, nama_kasir, role FROM kasir WHERE uid_kartu = ? AND status = 'aktif'";
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uidCard);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sesi.idKasir = rs.getInt("id_kasir");
                    Sesi.namaKasir = rs.getString("nama_kasir");
                    Sesi.roleKasir = rs.getString("role");
                    Sesi.waktuTerakhirTap = System.currentTimeMillis();
                    String sqlLog = "INSERT INTO log_kasir (id_kasir) VALUES (?)";
                    try (PreparedStatement psLog = c.prepareStatement(sqlLog, Statement.RETURN_GENERATED_KEYS)) {
                        psLog.setInt(1, Sesi.idKasir);
                        psLog.executeUpdate();
                        try (ResultSet rsLog = psLog.getGeneratedKeys()) {
                            if (rsLog.next()) Sesi.idLog = rsLog.getInt(1);
                        }
                    }
                    new Mainframe().setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Akses Ditolak! Kartu tidak terdaftar atau nonaktif.",
                            "Gagal Login", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            Db.error(e);
        }
    }

    // ─── PROSES LOGIN PIN ─────────────────────────────────────
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
                    JOptionPane.showMessageDialog(this, "Nama atau PIN salah.",
                            "Login gagal", JOptionPane.ERROR_MESSAGE);
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
                    try (ResultSet log = psLog.getGeneratedKeys()) {
                        if (log.next()) Sesi.idLog = log.getInt(1);
                    }
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
