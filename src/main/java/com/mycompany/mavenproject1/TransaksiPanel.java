package com.mycompany.mavenproject1;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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
        Foto(String nama) {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 84));
            huruf = nama.isEmpty() ? "?" : nama.substring(0, 1).toUpperCase();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Tema.KUNING_MUDA);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(Tema.COKELAT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 38));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(huruf, (getWidth() - fm.stringWidth(huruf)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
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
        StringBuilder sql = new StringBuilder("SELECT id_menu, nama_menu, harga FROM menu WHERE status='tersedia'");
        if (idKategoriAktif > 0) sql.append(" AND id_kategori=?");
        String cari = txtCari.getText().trim().toLowerCase();
        if (!cari.isEmpty()) sql.append(" AND LOWER(nama_menu) LIKE ?");
        sql.append(" ORDER BY id_menu");

        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (idKategoriAktif > 0) ps.setInt(i++, idKategoriAktif);
            if (!cari.isEmpty()) ps.setString(i++, "%" + cari + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) gridMenu.add(kartuMenu(rs.getInt(1), rs.getString(2), rs.getDouble(3)));
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

    Tema.Kartu kartuMenu(int id, String nama, double harga) {
        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 8));
        k.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        k.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel n = new JLabel("<html><body style='width:110px'><b>" + Tema.esc(nama) + "</b></body></html>");
        JLabel h = new JLabel(Tema.rp(harga));
        h.setFont(new Font("SansSerif", Font.BOLD, 15));
        h.setForeground(Tema.COKELAT);
        JPanel info = new JPanel(new BorderLayout(0, 2));
        info.setOpaque(false);
        info.add(n, BorderLayout.NORTH);
        info.add(h, BorderLayout.CENTER);

        JButton b = Tema.tombol("+ Tambah", Tema.KUNING);
        b.addActionListener(e -> dialogTambah(id, nama, harga));

        k.add(new Foto(nama), BorderLayout.NORTH);
        k.add(info, BorderLayout.CENTER);
        k.add(b, BorderLayout.SOUTH);
        k.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { dialogTambah(id, nama, harga); }
        });
        return k;
    }

    /** Dialog kecil untuk pilih rasa, jumlah, dan catatan. */
    void dialogTambah(int idMenu, String nama, double harga) {
        JComboBox<Rasa> cmbRasa = new JComboBox<>();
        cmbRasa.addItem(new Rasa(0, "Tanpa rasa"));
        try (Connection c = Koneksi.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT r.id_rasa, r.nama_rasa FROM menu_varian mv "
                   + "JOIN varian_rasa r ON r.id_rasa=mv.id_rasa WHERE mv.id_menu=? ORDER BY r.id_rasa")) {
            ps.setInt(1, idMenu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cmbRasa.addItem(new Rasa(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            Db.error(e);
            return;
        }
        if (cmbRasa.getItemCount() > 1) cmbRasa.setSelectedIndex(1);

        JSpinner spn = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JTextField txtCatatan = new JTextField(15);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Rasa:"));    form.add(cmbRasa);
        form.add(new JLabel("Jumlah:"));  form.add(spn);
        form.add(new JLabel("Catatan:")); form.add(txtCatatan);

        int ok = JOptionPane.showConfirmDialog(this, form, nama + " - " + Tema.rp(harga),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        Rasa rasa = (Rasa) cmbRasa.getSelectedItem();
        int qty = (Integer) spn.getValue();
        String cat = txtCatatan.getText().trim();

        // item yang sama (menu + rasa + catatan) digabung jumlahnya
        for (Item it : keranjang) {
            if (it.idMenu == idMenu && it.idRasa == rasa.id && it.catatan.equals(cat)) {
                it.qty += qty;
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
        it.qty = qty;
        it.catatan = cat;
        keranjang.add(it);
        refreshKeranjang();
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
        try (Connection c = Koneksi.get();
             PreparedStatement ps = c.prepareStatement("UPDATE transaksi SET status='batal' WHERE id_transaksi=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Db.error(e);
        }
        muatRiwayat();
    }
}
