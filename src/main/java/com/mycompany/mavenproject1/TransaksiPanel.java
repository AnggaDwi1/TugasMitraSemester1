package com.mycompany.mavenproject1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TransaksiPanel extends JPanel {

    static class Rasa {
        int id; String nama;
        Rasa(int id, String nama) { this.id = id; this.nama = nama; }
        @Override public String toString() { return nama; }
    }

    static class Item {
        int idMenu; String nama; int idRasa; String rasa; double harga; int qty; String catatan;
        double subtotal() { return harga * qty; }
    }

    /** Kotak "foto" menu: kotak kuning muda dengan huruf depan nama menu. */
    static class Foto extends JPanel {
        final String huruf;
        final BufferedImage gambar;
        Foto(String nama, byte[] bytes) {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 84));
            huruf = nama.isEmpty() ? "?" : nama.substring(0, 1).toUpperCase();
            BufferedImage loaded = null;
            if (bytes != null) try {
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
                if (original != null) loaded = kecilkan(original, 480);
            } catch (IOException ignored) { }
            gambar = loaded;
        }
        /** Kecilkan gambar (rasio tetap) supaya hemat memori. */
        static BufferedImage kecilkan(BufferedImage src, int lebarMaks) {
            if (src.getWidth() <= lebarMaks) return src;
            int h = Math.max(1, Math.round(src.getHeight() * (float) lebarMaks / src.getWidth()));
            BufferedImage out = new BufferedImage(lebarMaks, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src.getScaledInstance(lebarMaks, h, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
            return out;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setColor(Tema.KUNING_MUDA);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (gambar != null) {
                Shape oldClip = g2.getClip();
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                // "cover": gambar memenuhi kotak, rasio asli dijaga, kelebihan dipotong di tengah
                double s = Math.max((double) getWidth() / gambar.getWidth(), (double) getHeight() / gambar.getHeight());
                int dw = (int) Math.ceil(gambar.getWidth() * s);
                int dh = (int) Math.ceil(gambar.getHeight() * s);
                g2.drawImage(gambar, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);
                g2.setClip(oldClip);
            } else {
                g2.setColor(Tema.COKELAT);
                g2.setFont(new Font("SansSerif", Font.BOLD, 38));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(huruf, (getWidth() - fm.stringWidth(huruf)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // ---------- tab kasir ----------
    JPanel gridMenu = new JPanel(new GridLayout(0, 3, 12, 12));
    JPanel barKategori = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JTextField txtCari = new JTextField();
    int idKategoriAktif = 0; // 0 = semua kategori

    JPanel listKeranjang = new JPanel();
    JLabel lblJumlah = new JLabel("0 item");
    JLabel lblTotal = new JLabel("Rp 0");
    JToggleButton rbCash = new JToggleButton("Cash", true);
    JToggleButton rbQris = new JToggleButton("QRIS");
    JTextField txtBayar = new JTextField(10);
    JLabel lblKembali = new JLabel("Rp 0");
    List<Item> keranjang = new ArrayList<>();

    // ---------- tab riwayat ----------
    JTable tblRiwayat = new JTable();

    public TransaksiPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        add(Tema.judul("Transaksi"), BorderLayout.NORTH);

        JTabbedPane tab = new JTabbedPane();
        tab.setFont(new Font("SansSerif", Font.BOLD, 13));
        tab.addTab("Kasir", buatTabKasir());
        if ("OWNER".equalsIgnoreCase(Sesi.roleKasir)) {
            tab.addTab("Riwayat", buatTabRiwayat());
            tab.addChangeListener(e -> { if (tab.getSelectedIndex() == 1) muatRiwayat(); });
        }
        add(tab, BorderLayout.CENTER);

        segarkan();
    }

    // =====================================================
    //  TAB KASIR
    // =====================================================
    JPanel buatTabKasir() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(Tema.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // ----- kiri: pencarian, kategori, kartu menu -----
        txtCari.putClientProperty("JTextField.placeholderText", "Cari menu...");
        txtCari.setPreferredSize(new Dimension(0, 38));
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { muatMenu(); }
            @Override public void removeUpdate(DocumentEvent e) { muatMenu(); }
            @Override public void changedUpdate(DocumentEvent e) { muatMenu(); }
        });
        barKategori.setOpaque(false);

        JLabel lblKat = new JLabel("Kategori");
        lblKat.setFont(new Font("SansSerif", Font.BOLD, 15));
        JPanel kat = new JPanel(new BorderLayout(0, 8));
        kat.setOpaque(false);
        kat.add(lblKat, BorderLayout.NORTH);
        kat.add(barKategori, BorderLayout.CENTER);

        JPanel atasKiri = new JPanel(new BorderLayout(0, 14));
        atasKiri.setOpaque(false);
        atasKiri.add(txtCari, BorderLayout.NORTH);
        atasKiri.add(kat, BorderLayout.CENTER);

        gridMenu.setBackground(Tema.BG);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Tema.BG);
        wrap.add(gridMenu, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(Tema.BG);

        JPanel kiri = new JPanel(new BorderLayout(0, 12));
        kiri.setOpaque(false);
        kiri.add(atasKiri, BorderLayout.NORTH);
        kiri.add(sp, BorderLayout.CENTER);
        p.add(kiri, BorderLayout.CENTER);

        // ----- kanan: detail pesanan + pembayaran -----
        JLabel judulKeranjang = new JLabel("Detail Pesanan");
        judulKeranjang.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblJumlah.setForeground(Tema.ABU);
        JButton btnKosong = new JButton("Hapus Semua");
        btnKosong.setForeground(Tema.MERAH);
        btnKosong.setContentAreaFilled(false);
        btnKosong.setBorderPainted(false);
        btnKosong.setFocusPainted(false);
        btnKosong.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnKosong.addActionListener(e -> { keranjang.clear(); refreshKeranjang(); });

        JPanel judulKiri = new JPanel(new GridLayout(2, 1));
        judulKiri.setOpaque(false);
        judulKiri.add(judulKeranjang);
        judulKiri.add(lblJumlah);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(judulKiri, BorderLayout.WEST);
        header.add(btnKosong, BorderLayout.EAST);

        listKeranjang.setLayout(new BoxLayout(listKeranjang, BoxLayout.Y_AXIS));
        listKeranjang.setOpaque(false);
        JPanel listWrap = new JPanel(new BorderLayout());
        listWrap.setOpaque(false);
        listWrap.add(listKeranjang, BorderLayout.NORTH);
        JScrollPane spKeranjang = new JScrollPane(listWrap);
        spKeranjang.setBorder(null);
        spKeranjang.setOpaque(false);
        spKeranjang.getViewport().setOpaque(false);
        spKeranjang.getVerticalScrollBar().setUnitIncrement(16);

        // pembayaran
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTotal.setForeground(Tema.COKELAT);
        lblKembali.setFont(new Font("SansSerif", Font.BOLD, 14));

        ButtonGroup grup = new ButtonGroup();
        grup.add(rbCash);
        grup.add(rbQris);
        rbCash.setFocusPainted(false);
        rbQris.setFocusPainted(false);
        rbCash.addActionListener(e -> aturMetode());
        rbQris.addActionListener(e -> aturMetode());
        JPanel metode = new JPanel(new GridLayout(1, 2, 8, 0));
        metode.setOpaque(false);
        metode.add(rbCash);
        metode.add(rbQris);

        txtBayar.putClientProperty("JTextField.placeholderText", "Uang diterima");
        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { hitungKembali(); }
        });
        JButton btnPas = new JButton("Uang Pas");
        btnPas.setFocusPainted(false);
        btnPas.addActionListener(e -> {
            if (rbCash.isSelected()) {
                txtBayar.setText(String.valueOf(Math.round(total())));
                hitungKembali();
            }
        });
        JPanel bayar = new JPanel(new BorderLayout(8, 0));
        bayar.setOpaque(false);
        bayar.add(txtBayar, BorderLayout.CENTER);
        bayar.add(btnPas, BorderLayout.EAST);

        JButton btnBayar = Tema.tombol("Bayar", Tema.KUNING);
        btnBayar.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnBayar.setPreferredSize(new Dimension(0, 46));
        JButton btnBatal = new JButton("Batal");
        btnBatal.setPreferredSize(new Dimension(80, 46));
        btnBayar.addActionListener(e -> konfirmasi());
        btnBatal.addActionListener(e -> reset());
        JPanel tombol = new JPanel(new BorderLayout(8, 0));
        tombol.setOpaque(false);
        tombol.add(btnBatal, BorderLayout.WEST);
        tombol.add(btnBayar, BorderLayout.CENTER);

        JPanel ringkas = new JPanel();
        ringkas.setLayout(new BoxLayout(ringkas, BoxLayout.Y_AXIS));
        ringkas.setOpaque(false);
        ringkas.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.GARIS));
        ringkas.add(Box.createVerticalStrut(12));
        tambahV(ringkas, baris("Total", lblTotal), 10);
        tambahV(ringkas, metode, 10);
        tambahV(ringkas, bayar, 8);
        tambahV(ringkas, baris("Kembali", lblKembali), 12);
        tambahV(ringkas, tombol, 0);

        Tema.Kartu kanan = new Tema.Kartu();
        kanan.setLayout(new BorderLayout(0, 10));
        kanan.setPreferredSize(new Dimension(370, 0));
        kanan.add(header, BorderLayout.NORTH);
        kanan.add(spKeranjang, BorderLayout.CENTER);
        kanan.add(ringkas, BorderLayout.SOUTH);
        p.add(kanan, BorderLayout.EAST);

        refreshKeranjang();
        return p;
    }

    /** Menambah komponen ke panel vertikal (rata kiri) + jarak di bawahnya. */
    void tambahV(JPanel box, JComponent c, int jarak) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(c);
        if (jarak > 0) box.add(Box.createVerticalStrut(jarak));
    }

    JPanel baris(String kiri, JLabel kanan) {
        JLabel l = new JLabel(kiri);
        l.setForeground(Tema.ABU);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(l, BorderLayout.WEST);
        p.add(kanan, BorderLayout.EAST);
        return p;
    }

    public void segarkan() {
        muatKategori();
        muatMenu();
        if ("OWNER".equalsIgnoreCase(Sesi.roleKasir)) muatRiwayat();
    }

    // ---------- kategori (chip) ----------
    void muatKategori() {
        barKategori.removeAll();
        barKategori.add(chip("Semua", 0));
        try (Connection c = Koneksi.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id_kategori, nama_kategori FROM kategori_menu ORDER BY nama_kategori")) {
            while (rs.next()) barKategori.add(chip(rs.getString(2), rs.getInt(1)));
        } catch (SQLException e) {
            Db.error(e);
        }
        barKategori.revalidate();
        barKategori.repaint();
    }

    JButton chip(String teks, int id) {
        boolean aktif = id == idKategoriAktif;
        JButton b = new JButton(teks);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(aktif ? Tema.KUNING : Color.WHITE);
        b.setForeground(Tema.COKELAT);
        b.setFont(new Font("SansSerif", aktif ? Font.BOLD : Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            idKategoriAktif = id;
            muatKategori();
            muatMenu();
        });
        return b;
    }

    // ---------- kartu menu ----------
    void muatMenu() {
        gridMenu.removeAll();
        StringBuilder sql = new StringBuilder("SELECT id_menu, nama_menu, harga, gambar, stok FROM menu WHERE status='tersedia' AND dihapus=0");
        if (idKategoriAktif > 0) sql.append(" AND id_kategori=?");
        String cari = txtCari.getText().trim().toLowerCase();
        if (!cari.isEmpty()) sql.append(" AND LOWER(nama_menu) LIKE ?");
        sql.append(" ORDER BY id_menu");

        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (idKategoriAktif > 0) ps.setInt(i++, idKategoriAktif);
            if (!cari.isEmpty()) ps.setString(i++, "%" + cari + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) gridMenu.add(kartuMenu(rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getBytes(4), rs.getInt(5)));
            }
        } catch (SQLException e) {
            Db.error(e);
        }
        if (gridMenu.getComponentCount() == 0) {
            JLabel kosong = new JLabel("Menu tidak ditemukan");
            kosong.setForeground(Tema.ABU);
            gridMenu.add(kosong);
        }
        gridMenu.revalidate();
        gridMenu.repaint();
    }

    Tema.Kartu kartuMenu(int id, String nama, double harga, byte[] gambar, int stok) {
        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 8));
        k.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        k.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel n = new JLabel("<html><body style='width:110px'><b>" + Tema.esc(nama) + "</b></body></html>");
        JLabel h = new JLabel(Tema.rp(harga));
        h.setFont(new Font("SansSerif", Font.BOLD, 15));
        h.setForeground(Tema.COKELAT);

        // Label stok
        JLabel lblStok = new JLabel("Stok: " + stok);
        lblStok.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStok.setForeground(stok <= 10 ? Tema.MERAH : stok <= 30 ? Tema.ORANYE : Tema.ABU);

        JPanel info = new JPanel(new BorderLayout(0, 2));
        info.setOpaque(false);
        info.add(n, BorderLayout.NORTH);
        info.add(h, BorderLayout.CENTER);
        info.add(lblStok, BorderLayout.SOUTH);

        JButton b;
        if (stok <= 0) {
            b = Tema.tombol("Habis", Tema.MERAH);
            b.setEnabled(false);
        } else {
            b = Tema.tombol("+ Tambah", Tema.KUNING);
            b.addActionListener(e -> dialogTambah(id, nama, harga, stok));
        }

        k.add(new Foto(nama, gambar), BorderLayout.NORTH);
        k.add(info, BorderLayout.CENTER);
        k.add(b, BorderLayout.SOUTH);
        k.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (stok > 0) dialogTambah(id, nama, harga, stok); }
        });
        return k;
    }

    /** Dialog tambah pesanan: pilih rasa (chip), jumlah (stepper), dan catatan. */
    void dialogTambah(int idMenu, String nama, double harga, int stok) {
        // hitung qty menu ini yang sudah ada di keranjang
        int sudahDiKeranjang = 0;
        for (Item it : keranjang) {
            if (it.idMenu == idMenu) sudahDiKeranjang += it.qty;
        }
        final int sisaStok = stok - sudahDiKeranjang;
        if (sisaStok <= 0) {
            JOptionPane.showMessageDialog(this, "Stok " + nama + " sudah habis di keranjang.");
            return;
        }

        // ambil daftar rasa dari database
        List<Rasa> daftarRasa = new ArrayList<>();
        try (Connection c = Koneksi.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT r.id_rasa, r.nama_rasa FROM menu_varian mv "
                   + "JOIN varian_rasa r ON r.id_rasa=mv.id_rasa WHERE mv.id_menu=? ORDER BY r.id_rasa")) {
            ps.setInt(1, idMenu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) daftarRasa.add(new Rasa(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            Db.error(e);
            return;
        }

        final Rasa[] pilihRasa = { daftarRasa.isEmpty() ? new Rasa(0, "Tanpa rasa") : daftarRasa.get(0) };
        final int[] qty = { 1 };
        final boolean[] ok = { false };

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Tambah Pesanan",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Tema.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ----- header: nama, harga, stok -----
        JLabel lblNama = new JLabel("<html><body style='width:280px'>" + Tema.esc(nama) + "</body></html>");
        lblNama.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblNama.setForeground(Tema.COKELAT);
        JLabel lblHarga = new JLabel(Tema.rp(harga));
        lblHarga.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblHarga.setForeground(Tema.COKELAT);
        JLabel lblSisa = new JLabel("Tersedia " + sisaStok + " porsi");
        lblSisa.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSisa.setForeground(Tema.ABU);

        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        JPanel headBawah = new JPanel(new GridLayout(2, 1, 0, 2));
        headBawah.setOpaque(false);
        headBawah.add(lblHarga);
        headBawah.add(lblSisa);
        head.add(lblNama, BorderLayout.NORTH);
        head.add(headBawah, BorderLayout.CENTER);

        JPanel isi = new JPanel();
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        isi.setOpaque(false);
        tambahV(isi, head, 14);

        // ----- rasa (chip, grid 2 kolom supaya tidak melebar) -----
        if (!daftarRasa.isEmpty()) {
            JPanel chips = new JPanel(new GridLayout(0, 2, 8, 8));
            chips.setOpaque(false);
            ButtonGroup grup = new ButtonGroup();
            boolean pertama = true;
            for (Rasa r : daftarRasa) {
                JToggleButton t = new JToggleButton(r.nama, pertama);
                pertama = false;
                t.putClientProperty("JButton.buttonType", "roundRect");
                t.putClientProperty("JButton.selectedBackground", Tema.KUNING);
                t.putClientProperty("JButton.selectedForeground", Tema.COKELAT);
                t.setBackground(Color.WHITE);
                t.setForeground(Tema.COKELAT);
                t.setFont(new Font("SansSerif", Font.BOLD, 13));
                t.setFocusPainted(false);
                t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                t.setMargin(new Insets(8, 12, 8, 12));
                t.addActionListener(e -> pilihRasa[0] = r);
                grup.add(t);
                chips.add(t);
            }
            tambahV(isi, seksi("Pilih Rasa"), 6);
            tambahV(isi, chips, 16);
        }

        // ----- jumlah (stepper) -----
        JLabel lblQty = new JLabel("1", SwingConstants.CENTER);
        lblQty.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblQty.setForeground(Tema.COKELAT);
        lblQty.setPreferredSize(new Dimension(48, 38));

        JLabel lblSubtotal = new JLabel(Tema.rp(harga), SwingConstants.RIGHT);
        lblSubtotal.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblSubtotal.setForeground(Tema.COKELAT);

        Runnable updateQty = () -> {
            lblQty.setText(String.valueOf(qty[0]));
            lblSubtotal.setText(Tema.rp(harga * qty[0]));
        };

        JButton btnMin = tombolQty("−");
        JButton btnPlus = tombolQty("+");
        btnMin.setPreferredSize(new Dimension(38, 38));
        btnPlus.setPreferredSize(new Dimension(38, 38));
        btnMin.addActionListener(e -> { if (qty[0] > 1) { qty[0]--; updateQty.run(); } });
        btnPlus.addActionListener(e -> { if (qty[0] < sisaStok) { qty[0]++; updateQty.run(); } });

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        stepper.setOpaque(false);
        stepper.add(btnMin);
        stepper.add(lblQty);
        stepper.add(btnPlus);

        JPanel barisQty = new JPanel(new BorderLayout());
        barisQty.setOpaque(false);
        barisQty.add(stepper, BorderLayout.WEST);
        barisQty.add(lblSubtotal, BorderLayout.EAST);

        tambahV(isi, seksi("Jumlah"), 6);
        tambahV(isi, barisQty, 16);

        // ----- catatan -----
        JTextField txtCatatan = new JTextField();
        txtCatatan.putClientProperty("JTextField.placeholderText", "Contoh: tidak pedas");
        txtCatatan.setPreferredSize(new Dimension(0, 40));
        txtCatatan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtCatatan.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tambahV(isi, seksi("Catatan (opsional)"), 6);
        tambahV(isi, txtCatatan, 0);

        // ----- tombol -----
        JButton btnBatal = new JButton("Batal");
        btnBatal.setPreferredSize(new Dimension(90, 44));
        btnBatal.setFocusPainted(false);
        btnBatal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBatal.addActionListener(e -> d.dispose());

        JButton btnOk = Tema.tombol("Tambah ke Pesanan", Tema.KUNING);
        btnOk.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnOk.setPreferredSize(new Dimension(0, 44));
        btnOk.addActionListener(e -> { ok[0] = true; d.dispose(); });

        JPanel tombol = new JPanel(new BorderLayout(10, 0));
        tombol.setOpaque(false);
        tombol.add(btnBatal, BorderLayout.WEST);
        tombol.add(btnOk, BorderLayout.CENTER);

        JPanel bawah = new JPanel(new BorderLayout());
        bawah.setOpaque(false);
        bawah.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.GARIS),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)));
        bawah.add(tombol, BorderLayout.CENTER);

        root.add(isi, BorderLayout.CENTER);
        root.add(bawah, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.getRootPane().setDefaultButton(btnOk);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();
        d.setSize(380, d.getHeight()); // lebar dikunci supaya ramping
        d.setLocationRelativeTo(this);
        d.setVisible(true); // menunggu sampai dialog ditutup

        if (!ok[0]) return;

        Rasa rasa = pilihRasa[0];
        int jumlah = qty[0];
        String cat = txtCatatan.getText().trim();

        // item yang sama (menu + rasa + catatan) digabung jumlahnya
        for (Item it : keranjang) {
            if (it.idMenu == idMenu && it.idRasa == rasa.id && it.catatan.equals(cat)) {
                it.qty += jumlah;
                refreshKeranjang();
                return;
            }
        }
        Item it = new Item();
        it.idMenu = idMenu;
        it.nama = nama;
        it.idRasa = rasa.id;
        it.rasa = rasa.id == 0 ? "-" : rasa.nama;
        it.harga = harga;
        it.qty = jumlah;
        it.catatan = cat;
        keranjang.add(it);
        refreshKeranjang();
    }

    /** Label judul kecil untuk tiap bagian dialog. */
    JLabel seksi(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(Tema.ABU);
        return l;
    }

    // ---------- keranjang ----------
    void refreshKeranjang() {
        listKeranjang.removeAll();
        if (keranjang.isEmpty()) {
            JLabel kosong = new JLabel("Keranjang masih kosong", SwingConstants.CENTER);
            kosong.setForeground(Tema.ABU);
            kosong.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            kosong.setAlignmentX(Component.LEFT_ALIGNMENT);
            listKeranjang.add(kosong);
        }
        int jumlah = 0;
        for (Item it : keranjang) {
            JPanel b = barisItem(it);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            listKeranjang.add(b);
            listKeranjang.add(Box.createVerticalStrut(8));
            jumlah += it.qty;
        }
        lblJumlah.setText(jumlah + " item");
        lblTotal.setText(Tema.rp(total()));
        hitungKembali();
        listKeranjang.revalidate();
        listKeranjang.repaint();
    }

    JPanel barisItem(Item it) {
        Tema.Kartu k = new Tema.Kartu().warna(Tema.CREAM);
        k.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        k.setLayout(new BorderLayout(8, 0));

        String rasa = it.idRasa == 0 ? "" : " (" + Tema.esc(it.rasa) + ")";
        String cat = it.catatan.isEmpty() ? "" : "<br>*" + Tema.esc(it.catatan);
        JLabel info = new JLabel("<html><body style='width:105px'><b>" + Tema.esc(it.nama) + "</b>" + rasa
                + "<br><span style='color:#9A7B63'>@ " + Tema.rp(it.harga) + "</span>" + cat + "</body></html>");

        JButton min = tombolQty("-");
        JButton plus = tombolQty("+");
        min.addActionListener(e -> {
            it.qty--;
            if (it.qty <= 0) keranjang.remove(it);
            refreshKeranjang();
        });
        plus.addActionListener(e -> { it.qty++; refreshKeranjang(); });
        JLabel qty = new JLabel(String.valueOf(it.qty), SwingConstants.CENTER);
        qty.setFont(new Font("SansSerif", Font.BOLD, 14));
        qty.setPreferredSize(new Dimension(24, 28));

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        kontrol.setOpaque(false);
        kontrol.add(min);
        kontrol.add(qty);
        kontrol.add(plus);

        JLabel sub = new JLabel(Tema.rp(it.subtotal()), SwingConstants.RIGHT);
        sub.setFont(new Font("SansSerif", Font.BOLD, 13));
        sub.setForeground(Tema.COKELAT);

        JPanel kanan = new JPanel(new BorderLayout(0, 6));
        kanan.setOpaque(false);
        kanan.add(kontrol, BorderLayout.NORTH);
        kanan.add(sub, BorderLayout.SOUTH);

        k.add(info, BorderLayout.CENTER);
        k.add(kanan, BorderLayout.EAST);
        return k;
    }

    JButton tombolQty(String teks) {
        JButton b = new JButton(teks);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(28, 28));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(Tema.COKELAT);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        return b;
    }

    double total() {
        double t = 0;
        for (Item it : keranjang) t += it.subtotal();
        return t;
    }

    void aturMetode() {
        boolean cash = rbCash.isSelected();
        txtBayar.setEnabled(cash);
        if (!cash) txtBayar.setText("");
        hitungKembali();
    }

    void hitungKembali() {
        if (rbQris.isSelected()) { lblKembali.setText("Rp 0"); return; }
        try {
            double bayar = Double.parseDouble(txtBayar.getText().trim());
            lblKembali.setText(Tema.rp(Math.max(0, bayar - total())));
        } catch (NumberFormatException e) {
            lblKembali.setText("Rp 0");
        }
    }

    void reset() {
        keranjang.clear();
        txtBayar.setText("");
        rbCash.setSelected(true);
        aturMetode();
        refreshKeranjang();
    }

    void konfirmasi() {
        if (keranjang.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang masih kosong");
            return;
        }
        double total = total();
        double bayar = total, kembali = 0;
        String metode = rbQris.isSelected() ? "qris" : "cash";

        if (rbCash.isSelected()) {
            try {
                bayar = Double.parseDouble(txtBayar.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Isi uang bayar dengan angka");
                return;
            }
            if (bayar < total) {
                JOptionPane.showMessageDialog(this, "Uang bayar kurang");
                return;
            }
            kembali = bayar - total;
        } else {
            // QRIS statis: pastikan kasir sudah melihat notifikasi uang masuk
            int ok = JOptionPane.showConfirmDialog(this,
                    "Apakah pembayaran QRIS " + Tema.rp(total) + " sudah masuk\n(cek notifikasi / soundbox)?",
                    "Konfirmasi QRIS", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        int idTrx;
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                // 1. kode transaksi TRX-YYYYMMDD-XXX
                int urut = 1;
                try (Statement s = c.createStatement();
                     ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM transaksi WHERE DATE(tanggal)=CURDATE()")) {
                    if (rs.next()) urut = rs.getInt(1) + 1;
                }
                String kode = String.format("TRX-%s-%03d",
                        new SimpleDateFormat("yyyyMMdd").format(new java.util.Date()), urut);

                // 2. header
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transaksi (kode_transaksi, total_harga, status) VALUES (?,?,'selesai')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, kode);
                    ps.setDouble(2, total);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); idTrx = k.getInt(1); }
                }

                // 3. detail (subtotal dihitung otomatis oleh MySQL, jadi tidak diisi)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transaksi_detail (id_transaksi, id_menu, id_rasa, jumlah, harga_satuan, catatan) "
                      + "VALUES (?,?,?,?,?,?)")) {
                    for (Item it : keranjang) {
                        ps.setInt(1, idTrx);
                        ps.setInt(2, it.idMenu);
                        if (it.idRasa == 0) ps.setNull(3, Types.INTEGER); else ps.setInt(3, it.idRasa);
                        ps.setInt(4, it.qty);
                        ps.setDouble(5, it.harga);
                        ps.setString(6, it.catatan.isEmpty() ? null : it.catatan);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 3b. kurangi stok menu
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE menu SET stok = stok - ? WHERE id_menu = ?")) {
                    for (Item it : keranjang) {
                        ps.setInt(1, it.qty);
                        ps.setInt(2, it.idMenu);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 4. pembayaran
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO pembayaran (id_transaksi, metode, jumlah_bayar, kembalian) VALUES (?,?,?,?)")) {
                    ps.setInt(1, idTrx);
                    ps.setString(2, metode);
                    ps.setDouble(3, bayar);
                    ps.setDouble(4, kembali);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
            return;
        }

        reset();
        muatMenu();
        muatRiwayat();
        // struk dicetak SETELAH data tersimpan
        if ("1".equals(Pengaturan.get("otomatis", "0"))) Struk.cetakLangsung(this, idTrx);
        else Struk.tampil(this, idTrx);
    }

    // =====================================================
    //  TAB RIWAYAT
    // =====================================================
    JPanel buatTabRiwayat() {
        Tema.gayaTabel(tblRiwayat);
        tblRiwayat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnRefresh = new JButton("Refresh");
        JButton btnStruk = Tema.tombol("Lihat / Cetak Struk", Tema.BIRU);
        JButton btnBatalTrx = Tema.tombol("Batalkan Transaksi", Tema.MERAH);
        btnRefresh.addActionListener(e -> muatRiwayat());
        btnStruk.addActionListener(e -> {
            int id = idRiwayatTerpilih();
            if (id > 0) Struk.tampil(this, id);
        });
        btnBatalTrx.addActionListener(e -> batalkan());

        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        atas.setOpaque(false);
        atas.add(btnRefresh);
        atas.add(btnStruk);
        atas.add(btnBatalTrx);

        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 10));
        k.add(atas, BorderLayout.NORTH);
        k.add(new JScrollPane(tblRiwayat), BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        p.add(k, BorderLayout.CENTER);
        return p;
    }

    void muatRiwayat() {
        tblRiwayat.setModel(Db.kueri(
                "SELECT t.id_transaksi, t.kode_transaksi, DATE_FORMAT(t.tanggal,'%d/%m/%Y %H:%i'), "
              + "CAST(t.total_harga AS UNSIGNED), COALESCE(p.metode,'-'), t.status "
              + "FROM transaksi t LEFT JOIN pembayaran p ON p.id_transaksi=t.id_transaksi "
              + "ORDER BY t.id_transaksi DESC LIMIT 200",
                new String[]{"ID", "Kode", "Waktu", "Total", "Metode", "Status"}));
    }

    int idRiwayatTerpilih() {
        int r = tblRiwayat.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi di tabel dulu");
            return -1;
        }
        return Integer.parseInt(tblRiwayat.getValueAt(r, 0).toString());
    }

    void batalkan() {
        int id = idRiwayatTerpilih();
        if (id < 0) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Batalkan transaksi ini? Transaksi batal tidak dihitung di omzet.",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                int berubah;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE transaksi SET status='batal' WHERE id_transaksi=? AND status='selesai'")) {
                    ps.setInt(1, id);
                    berubah = ps.executeUpdate();
                }
                if (berubah > 0) { // kembalikan stok hanya jika transaksi baru saja dibatalkan
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE menu m JOIN transaksi_detail d ON d.id_menu=m.id_menu "
                          + "SET m.stok = m.stok + d.jumlah WHERE d.id_transaksi=?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Db.error(e);
        }
        muatRiwayat();
        muatMenu();
    }
}