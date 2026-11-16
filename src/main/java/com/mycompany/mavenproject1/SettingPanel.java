package com.mycompany.mavenproject1;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;

/**
 * Pusat pengaturan: info toko & struk, KELOLA MENU (tambah/ubah/hapus, gambar, stok), dan varian rasa.
 * Halaman Menu hanya menampilkan hasilnya.
 */
public class SettingPanel extends JPanel {

    static class Kat {
        int id; String nama;
        Kat(int id, String nama) { this.id = id; this.nama = nama; }
        @Override public String toString() { return nama; }
    }

    // ----- toko & struk -----
    JTextField txtToko = new JTextField();
    JTextField txtAlamat = new JTextField();
    JTextField txtFooter = new JTextField();
    // data toko (terpisah dari struk)
    JTextField txtInfoNama = new JTextField();
    JTextField txtInfoAlamat = new JTextField();
    JTextField txtInfoTelp = new JTextField();
    JComboBox<String> cmbKertas = new JComboBox<>(new String[]{"80 mm", "58 mm"});
    JComboBox<String> cmbUkuran = new JComboBox<>(new String[]{"Kecil", "Sedang", "Besar"});
    JComboBox<String> cmbPrinter = new JComboBox<>();
    JCheckBox chkOtomatis = new JCheckBox("Cetak otomatis setelah Konfirmasi");

    // ----- varian rasa -----
    JTable tblRasa = new JTable();
    JTextField txtRasa = new JTextField();
    JTextField txtKet = new JTextField();

    // ----- kelola menu -----
    JTable tblMenu = new JTable();
    JTextField txtNamaMenu = new JTextField();
    JTextField txtHarga = new JTextField();
    JTextField txtStok = new JTextField();
    JTextArea txtDesk = new JTextArea(3, 15);
    JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"tersedia", "habis"});
    JComboBox<Kat> cmbKat = new JComboBox<>();
    JLabel lblGambar = new JLabel("Belum ada gambar", SwingConstants.CENTER);
    byte[] gambarTerpilih;
    JLabel lblLogo = new JLabel("", SwingConstants.CENTER);
    byte[] logoBaru; // logo yang dipilih tapi belum disimpan
    int idMenu = 0;

    public SettingPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        add(Tema.judul("Setting"), BorderLayout.NORTH);

        JTabbedPane tab = new JTabbedPane();
        tab.setFont(new Font("SansSerif", Font.BOLD, 13));
        tab.setOpaque(false);
        tab.addTab("Toko", sisiKiri(buatKartuToko(), 520));
        tab.addTab("Struk", buatTabStruk());
        tab.addTab("Kelola Menu", buatTabMenu());
        tab.addTab("Varian Rasa", sisiKiri(buatKartuRasa(), 640));
        tab.addTab("Logo Aplikasi", sisiKiri(buatKartuLogo(), 520));
        add(tab, BorderLayout.CENTER);

        segarkan();
    }

    /** Kartu dengan lebar tetap di kiri, sisa ruang dibiarkan kosong. */
    private JPanel sisiKiri(Tema.Kartu kartu, int lebar) {
        kartu.setPreferredSize(new Dimension(lebar, 0));
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        p.add(kartu, BorderLayout.WEST);
        return p;
    }

    // =====================================================
    //  TAB 1: INFO TOKO (tidak terkait struk)
    // =====================================================
    Tema.Kartu buatKartuToko() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.setOpaque(false);
        form.add(new JLabel("Nama toko"));  form.add(txtInfoNama);
        form.add(new JLabel("Alamat"));     form.add(txtInfoAlamat);
        form.add(new JLabel("No. telepon")); form.add(txtInfoTelp);

        JButton btnSimpan = Tema.tombol("Simpan Info Toko", Tema.BIRU);
        btnSimpan.addActionListener(e -> simpanInfoToko());

        JLabel info = new JLabel(infoDatabase());
        info.setForeground(Tema.ABU);

        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 10));
        k.add(new JLabel("<html><b>Info Toko</b></html>"), BorderLayout.NORTH);
        JPanel tengah = new JPanel(new BorderLayout());
        tengah.setOpaque(false);
        tengah.add(form, BorderLayout.NORTH);
        k.add(tengah, BorderLayout.CENTER);
        JPanel bawah = new JPanel(new GridLayout(0, 1, 0, 8));
        bawah.setOpaque(false);
        bawah.add(btnSimpan);
        bawah.add(info);
        k.add(bawah, BorderLayout.SOUTH);
        return k;
    }

    // =====================================================
    //  TAB 2: STRUK (nama/alamat/pesan struk, pengaturan + pratinjau langsung)
    // =====================================================
    private final JTextArea areaStruk = new JTextArea();
    private final JPanel kertasStruk = new JPanel(new BorderLayout());

    JPanel buatTabStruk() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.setOpaque(false);
        form.add(new JLabel("Nama toko"));           form.add(txtToko);
        form.add(new JLabel("Alamat"));              form.add(txtAlamat);
        form.add(new JLabel("Pesan bawah struk"));   form.add(txtFooter);
        form.add(new JLabel("Lebar kertas"));        form.add(cmbKertas);
        form.add(new JLabel("Ukuran huruf struk"));  form.add(cmbUkuran);
        form.add(new JLabel("Printer struk"));       form.add(cmbPrinter);
        form.add(chkOtomatis);

        JLabel petunjuk = new JLabel("<html>Pratinjau di kanan berubah langsung saat pengaturan diubah, "
                + "tanpa mencetak.</html>");
        petunjuk.setForeground(Tema.ABU);

        JButton btnSimpan = Tema.tombol("Simpan Pengaturan", Tema.BIRU);
        btnSimpan.addActionListener(e -> simpanToko());

        Tema.Kartu kiri = new Tema.Kartu();
        kiri.setLayout(new BorderLayout(0, 10));
        kiri.setPreferredSize(new Dimension(380, 0));
        kiri.add(new JLabel("<html><b>Pengaturan Struk</b></html>"), BorderLayout.NORTH);
        JPanel tengah = new JPanel(new BorderLayout(0, 10));
        tengah.setOpaque(false);
        tengah.add(form, BorderLayout.NORTH);
        tengah.add(petunjuk, BorderLayout.CENTER);
        kiri.add(tengah, BorderLayout.CENTER);
        kiri.add(btnSimpan, BorderLayout.SOUTH);

        // ----- pratinjau: kertas putih di atas latar abu muda -----
        areaStruk.setEditable(false);
        areaStruk.setOpaque(false);
        areaStruk.setForeground(Color.BLACK);
        areaStruk.setMargin(new Insets(0, 0, 0, 0));
        kertasStruk.setBackground(Color.WHITE);
        kertasStruk.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.GARIS),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        kertasStruk.add(areaStruk, BorderLayout.CENTER);

        JPanel meja = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        meja.setBackground(new Color(0xF3EBDD));
        meja.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        meja.add(kertasStruk);
        JScrollPane scroll = new JScrollPane(meja);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        Tema.Kartu kanan = new Tema.Kartu();
        kanan.setLayout(new BorderLayout(0, 10));
        kanan.add(new JLabel("<html><b>Pratinjau Struk</b></html>"), BorderLayout.NORTH);
        kanan.add(scroll, BorderLayout.CENTER);

        // pratinjau ikut berubah saat nama/alamat/footer/kertas/ukuran diubah
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { perbaruiPratinjau(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { perbaruiPratinjau(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { perbaruiPratinjau(); }
        };
        txtToko.getDocument().addDocumentListener(dl);
        txtAlamat.getDocument().addDocumentListener(dl);
        txtFooter.getDocument().addDocumentListener(dl);
        cmbKertas.addActionListener(e -> perbaruiPratinjau());
        cmbUkuran.addActionListener(e -> perbaruiPratinjau());

        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        p.add(kiri, BorderLayout.WEST);
        p.add(kanan, BorderLayout.CENTER);
        return p;
    }

    /** Menyusun ulang teks struk contoh dari isi form saat ini (belum perlu disimpan). */
    void perbaruiPratinjau() {
        int mm = cmbKertas.getSelectedIndex() == 1 ? 58 : 80;
        int lebar = Pengaturan.lebar(mm, cmbUkuran.getSelectedIndex());
        areaStruk.setText(Struk.contoh(txtToko.getText().trim(), txtAlamat.getText().trim(),
                txtFooter.getText().trim(), lebar));

        // lebar kertas di layar mengikuti 80 mm / 58 mm; huruf dikecilkan sampai 1 baris penuh muat
        int kertasPx = mm == 58 ? 250 : 340;
        int tersedia = kertasPx - 2 * 14 - 2;
        int ukuran = 16;
        Font f = new Font(Font.MONOSPACED, Font.PLAIN, ukuran);
        while (ukuran > 6) {
            f = new Font(Font.MONOSPACED, Font.PLAIN, ukuran);
            if (areaStruk.getFontMetrics(f).charWidth('0') * lebar <= tersedia) break;
            ukuran--;
        }
        areaStruk.setFont(f);
        int tinggi = areaStruk.getPreferredSize().height + 2 * 14 + 2;
        kertasStruk.setPreferredSize(new Dimension(kertasPx, tinggi));
        kertasStruk.revalidate();
        kertasStruk.repaint();
    }

    String infoDatabase() {
        try (Connection c = Koneksi.get()) {
            return "Database terhubung: " + c.getCatalog();
        } catch (SQLException e) {
            return "Database TIDAK terhubung";
        }
    }

    void simpanInfoToko() {
        Pengaturan.put("tokoNama", txtInfoNama.getText().trim());
        Pengaturan.put("tokoAlamat", txtInfoAlamat.getText().trim());
        Pengaturan.put("tokoTelp", txtInfoTelp.getText().trim());
        JOptionPane.showMessageDialog(this, "Info toko tersimpan");
    }

    void simpanToko() {
        Pengaturan.put("namaToko", txtToko.getText().trim());
        Pengaturan.put("alamat", txtAlamat.getText().trim());
        Pengaturan.put("footer", txtFooter.getText().trim());
        Pengaturan.put("kertas", cmbKertas.getSelectedIndex() == 0 ? "80" : "58");
        Pengaturan.put("ukuran", String.valueOf(cmbUkuran.getSelectedIndex()));
        Pengaturan.put("printer", cmbPrinter.getSelectedIndex() <= 0 ? "" : (String) cmbPrinter.getSelectedItem());
        Pengaturan.put("otomatis", chkOtomatis.isSelected() ? "1" : "0");
        JOptionPane.showMessageDialog(this, "Pengaturan tersimpan");
    }

    // =====================================================
    //  TAB 2: KELOLA MENU
    // =====================================================
    JPanel buatTabMenu() {
        // ----- tabel menu -----
        Tema.gayaTabel(tblMenu);
        tblMenu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMenu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) isiFormMenu();
        });
        Tema.Kartu kartuTabel = new Tema.Kartu();
        kartuTabel.setLayout(new BorderLayout(0, 8));
        JLabel petunjuk = new JLabel("Pilih menu di tabel untuk mengubah, mengganti gambar, atau menambah stok.");
        petunjuk.setForeground(Tema.ABU);
        kartuTabel.add(petunjuk, BorderLayout.NORTH);
        kartuTabel.add(new JScrollPane(tblMenu), BorderLayout.CENTER);

        // ----- form -----
        txtDesk.setLineWrap(true);
        txtDesk.setWrapStyleWord(true);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        lblGambar.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        lblGambar.setForeground(Tema.ABU);
        JButton btnPilihGambar = new JButton("Pilih Gambar...");
        JButton btnHapusGambar = new JButton("Hapus Gambar");
        gayaTombolRingan(btnPilihGambar);
        gayaTombolRingan(btnHapusGambar);
        btnPilihGambar.addActionListener(e -> pilihGambar());
        btnHapusGambar.addActionListener(e -> setGambar(null));
        JPanel aksiGambar = new JPanel(new GridLayout(1, 2, 6, 0));
        aksiGambar.setOpaque(false);
        aksiGambar.add(btnPilihGambar);
        aksiGambar.add(btnHapusGambar);
        aksiGambar.setPreferredSize(new Dimension(300, 34));
        aksiGambar.setMaximumSize(new Dimension(300, 34));

        tambahKolom(form, "Gambar menu", lblGambar);
        form.add(Box.createVerticalStrut(6));
        aksiGambar.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(aksiGambar);
        tambahKolom(form, "Nama menu", txtNamaMenu);
        tambahKolom(form, "Harga", txtHarga);
        tambahKolom(form, "Stok", txtStok);
        tambahKolom(form, "Kategori", cmbKat);
        tambahKolom(form, "Status", cmbStatus);
        JScrollPane scrollDesk = new JScrollPane(txtDesk);
        scrollDesk.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        scrollDesk.getViewport().setBackground(Color.WHITE);
        tambahKolom(form, "Deskripsi", scrollDesk);

        JButton btnTambahStok = Tema.tombol("+ Tambah Stok", Tema.HIJAU);
        JButton btnSimpan = Tema.tombol("Simpan", Tema.BIRU);
        JButton btnHapus = Tema.tombol("Hapus", Tema.MERAH);
        JButton btnBaru = new JButton("Baru / Kosongkan");
        btnTambahStok.addActionListener(e -> tambahStok());
        btnSimpan.addActionListener(e -> simpanMenu());
        btnHapus.addActionListener(e -> hapusMenu());
        btnBaru.addActionListener(e -> kosongkanMenu());

        JPanel tombol = new JPanel(new GridLayout(0, 2, 6, 6));
        tombol.setOpaque(false);
        tombol.add(btnTambahStok);
        tombol.add(btnSimpan);
        tombol.add(btnHapus);
        tombol.add(btnBaru);

        Tema.Kartu kartuForm = new Tema.Kartu();
        kartuForm.setLayout(new BorderLayout(0, 8));
        kartuForm.setPreferredSize(new Dimension(350, 0));
        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBorder(null);
        scrollForm.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollForm.setOpaque(false);
        scrollForm.getViewport().setOpaque(false);
        scrollForm.getVerticalScrollBar().setUnitIncrement(18);
        kartuForm.add(new JLabel("<html><b>Form Menu</b></html>"), BorderLayout.NORTH);
        kartuForm.add(scrollForm, BorderLayout.CENTER);
        kartuForm.add(tombol, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        p.add(kartuTabel, BorderLayout.CENTER);
        p.add(kartuForm, BorderLayout.EAST);
        return p;
    }

    private void tambahKolom(JPanel panel, String judul, JComponent isi) {
        JLabel label = new JLabel(judul);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        isi.setAlignmentX(Component.CENTER_ALIGNMENT);
        int tinggi = isi == lblGambar ? 140 : isi instanceof JScrollPane ? 84 : 36;
        isi.setPreferredSize(new Dimension(300, tinggi));
        isi.setMaximumSize(new Dimension(300, tinggi));
        panel.add(Box.createVerticalStrut(10));
        // label dibungkus agar rata kiri sejajar dengan kolom 300px
        JPanel baris = new JPanel(new BorderLayout());
        baris.setOpaque(false);
        baris.setMaximumSize(new Dimension(300, 20));
        baris.setAlignmentX(Component.CENTER_ALIGNMENT);
        baris.add(label, BorderLayout.WEST);
        panel.add(baris);
        panel.add(Box.createVerticalStrut(5));
        panel.add(isi);
    }

    private void gayaTombolRingan(JButton tombol) {
        tombol.setFocusPainted(false);
        tombol.setBackground(Tema.BG);
        tombol.setForeground(Tema.COKELAT);
        tombol.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
    }

    void muatMenu() {
        cmbKat.removeAllItems();
        try (Connection c = Koneksi.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id_kategori, nama_kategori FROM kategori_menu ORDER BY nama_kategori")) {
            while (rs.next()) cmbKat.addItem(new Kat(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) {
            Db.error(e);
        }
        tblMenu.setModel(Db.kueri(
                "SELECT m.id_menu, m.nama_menu, COALESCE(k.nama_kategori,'-'), CAST(m.harga AS UNSIGNED), "
              + "m.status, COALESCE(m.deskripsi,''), m.stok "
              + "FROM menu m LEFT JOIN kategori_menu k ON k.id_kategori=m.id_kategori WHERE m.dihapus=0 ORDER BY m.id_menu",
                new String[]{"ID", "Menu", "Kategori", "Harga", "Status", "Deskripsi", "Stok"}));
        kosongkanMenu();
    }

    void isiFormMenu() {
        int row = tblMenu.getSelectedRow();
        if (row < 0) return;
        DefaultTableModel m = (DefaultTableModel) tblMenu.getModel();
        idMenu = Integer.parseInt(m.getValueAt(row, 0).toString());
        txtNamaMenu.setText(m.getValueAt(row, 1).toString());
        txtHarga.setText(m.getValueAt(row, 3).toString());
        cmbStatus.setSelectedItem(m.getValueAt(row, 4).toString());
        txtDesk.setText(m.getValueAt(row, 5).toString());
        txtStok.setText(m.getValueAt(row, 6).toString());
        muatGambar(idMenu);
        String namaKat = m.getValueAt(row, 2).toString();
        for (int i = 0; i < cmbKat.getItemCount(); i++) {
            if (cmbKat.getItemAt(i).nama.equals(namaKat)) { cmbKat.setSelectedIndex(i); break; }
        }
    }

    void kosongkanMenu() {
        idMenu = 0;
        tblMenu.clearSelection();
        txtNamaMenu.setText("");
        txtHarga.setText("");
        txtStok.setText("0");
        txtDesk.setText("");
        setGambar(null);
        cmbStatus.setSelectedIndex(0);
        if (cmbKat.getItemCount() > 0) cmbKat.setSelectedIndex(0);
    }

    /** Pilih ulang baris menu di tabel setelah data dimuat ulang. */
    private void pilihBaris(int id) {
        for (int i = 0; i < tblMenu.getRowCount(); i++) {
            if (Integer.parseInt(tblMenu.getValueAt(i, 0).toString()) == id) {
                tblMenu.setRowSelectionInterval(i, i);
                tblMenu.scrollRectToVisible(tblMenu.getCellRect(i, 0, true));
                return;
            }
        }
    }

    void simpanMenu() {
        String nama = txtNamaMenu.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama menu belum diisi");
            return;
        }
        double harga;
        try {
            harga = Double.parseDouble(txtHarga.getText().trim());
            if (harga <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga harus berupa angka lebih dari 0");
            return;
        }
        int stok;
        try {
            stok = Integer.parseInt(txtStok.getText().trim());
            if (stok < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Stok harus berupa angka bulat 0 atau lebih");
            return;
        }
        Kat kat = (Kat) cmbKat.getSelectedItem();
        String desk = txtDesk.getText().trim();
        final boolean baru = idMenu == 0;
        final int idLama = idMenu;

        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                if (baru) {
                    int idBaru;
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO menu (id_kategori, nama_menu, harga, deskripsi, status, gambar, stok) VALUES (?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
                        setBlob(ps, 6, gambarTerpilih);
                        ps.setInt(7, stok);
                        ps.executeUpdate();
                        try (ResultSet k = ps.getGeneratedKeys()) { k.next(); idBaru = k.getInt(1); }
                    }
                    // menu baru otomatis boleh pakai semua rasa
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO menu_varian (id_menu, id_rasa) SELECT ?, id_rasa FROM varian_rasa")) {
                        ps.setInt(1, idBaru);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE menu SET id_kategori=?, nama_menu=?, harga=?, deskripsi=?, status=?, gambar=?, stok=? WHERE id_menu=?")) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
                        setBlob(ps, 6, gambarTerpilih);
                        ps.setInt(7, stok);
                        ps.setInt(8, idLama);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            muatMenu();
            if (!baru) pilihBaris(idLama);
            JOptionPane.showMessageDialog(this, baru ? "Menu baru ditambahkan" : "Perubahan menu tersimpan");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
        }
    }

    void tambahStok() {
        if (idMenu == 0) {
            JOptionPane.showMessageDialog(this, "Pilih menu di tabel dulu");
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "Tambah stok \"" + txtNamaMenu.getText() + "\" sebanyak:", "Tambah Stok", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        int tambah;
        try {
            tambah = Integer.parseInt(input.trim());
            if (tambah <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Masukkan angka bulat lebih dari 0");
            return;
        }
        final int id = idMenu;
        try (Connection c = Koneksi.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE menu SET stok = stok + ?, status='tersedia' WHERE id_menu=?")) {
            ps.setInt(1, tambah);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Db.error(e);
            return;
        }
        muatMenu();
        pilihBaris(id);
    }

    void hapusMenu() {
        if (idMenu == 0) {
            JOptionPane.showMessageDialog(this, "Pilih menu di tabel dulu");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Hapus menu \"" + txtNamaMenu.getText() + "\"?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        final int id = idMenu;
        String info = null;
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_varian WHERE id_menu=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu WHERE id_menu=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                // 1451 = masih dipakai tabel lain (menu sudah pernah terjual)
                if (e.getErrorCode() != 1451 && !(e instanceof java.sql.SQLIntegrityConstraintViolationException)) throw e;
                // Riwayat penjualan harus tetap utuh, jadi menu disembunyikan dari semua daftar.
                try (PreparedStatement ps = c.prepareStatement("UPDATE menu SET dihapus=1 WHERE id_menu=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
                info = "Menu ini sudah pernah terjual, jadi dihapus dari daftar menu.\n"
                     + "Riwayat transaksi dan laporan penjualan tetap tersimpan.";
            }
        } catch (SQLException e) {
            Db.error(e);
            return;
        }
        muatMenu();
        if (info != null) JOptionPane.showMessageDialog(this, info);
    }

    // ----- gambar menu -----
    private void pilihGambar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih gambar menu");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Gambar (JPG, PNG, GIF, WebP)", "jpg", "jpeg", "png", "gif", "webp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage image = ImageIO.read(chooser.getSelectedFile());
            if (image == null) throw new IOException("File bukan gambar yang didukung.");
            setGambar(GambarMenu.keWebp(image));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal membaca gambar: " + ex.getMessage());
        }
    }

    private void muatGambar(int id) {
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement("SELECT gambar FROM menu WHERE id_menu=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { setGambar(null); return; }
                byte[] bytes = rs.getBytes(1);
                if (bytes == null) { setGambar(null); return; }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                setGambar(image == null ? bytes : GambarMenu.keWebp(image));
            }
        } catch (SQLException | IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal memuat gambar: " + ex.getMessage());
        }
    }

    private void setGambar(byte[] bytes) {
        gambarTerpilih = bytes;
        if (bytes == null || bytes.length == 0) {
            lblGambar.setIcon(null);
            lblGambar.setText("Belum ada gambar");
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IOException("Data gambar tidak valid.");
            // muat dalam kotak 296x136 tanpa merusak proporsi
            double s = Math.min(296.0 / image.getWidth(), 136.0 / image.getHeight());
            int w = Math.max(1, (int) Math.round(image.getWidth() * s));
            int h = Math.max(1, (int) Math.round(image.getHeight() * s));
            lblGambar.setText("");
            lblGambar.setIcon(new ImageIcon(image.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
        } catch (IOException ex) {
            lblGambar.setIcon(null);
            lblGambar.setText("Gambar tidak valid");
        }
    }

    private static void setBlob(PreparedStatement ps, int index, byte[] bytes) throws SQLException {
        if (bytes == null) ps.setNull(index, Types.BLOB); else ps.setBytes(index, bytes);
    }

    // =====================================================
    //  TAB 3: VARIAN RASA
    // =====================================================
    Tema.Kartu buatKartuRasa() {
        Tema.gayaTabel(tblRasa);
        tblRasa.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnTambah = Tema.tombol("Tambah Rasa", Tema.BIRU);
        JButton btnHapus = Tema.tombol("Hapus Rasa Terpilih", Tema.MERAH);
        btnTambah.addActionListener(e -> tambahRasa());
        btnHapus.addActionListener(e -> hapusRasa());

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setOpaque(false);
        form.add(new JLabel("Nama rasa"));  form.add(txtRasa);
        form.add(new JLabel("Keterangan")); form.add(txtKet);
        form.add(btnTambah);                form.add(btnHapus);

        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 10));
        k.add(new JLabel("<html><b>Varian Rasa</b></html>"), BorderLayout.NORTH);
        k.add(new JScrollPane(tblRasa), BorderLayout.CENTER);
        k.add(form, BorderLayout.SOUTH);
        return k;
    }

    // =====================================================
    //  TAB 4: LOGO APLIKASI
    // =====================================================
    Tema.Kartu buatKartuLogo() {
        lblLogo.setOpaque(true);
        lblLogo.setBackground(Color.WHITE);
        lblLogo.setForeground(Tema.ABU);
        lblLogo.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        lblLogo.setPreferredSize(new Dimension(0, 210));

        JLabel petunjuk = new JLabel("<html>Logo tampil di halaman login dan jadi ikon aplikasi. "
                + "Pakai gambar landscape (lebar) supaya hasilnya paling bagus. "
                + "Perubahan terlihat di halaman login berikutnya (setelah logout).</html>");
        petunjuk.setForeground(Tema.ABU);

        JButton btnPilih = new JButton("Pilih Gambar...");
        btnPilih.setFocusPainted(false);
        btnPilih.addActionListener(e -> pilihLogo());
        JButton btnSimpan = Tema.tombol("Simpan Logo", Tema.KUNING);
        btnSimpan.addActionListener(e -> simpanLogo());
        JButton btnBawaan = new JButton("Kembalikan Bawaan");
        btnBawaan.setFocusPainted(false);
        btnBawaan.addActionListener(e -> kembalikanLogoBawaan());

        JPanel tombol = new JPanel(new GridLayout(1, 3, 8, 0));
        tombol.setOpaque(false);
        tombol.add(btnPilih);
        tombol.add(btnSimpan);
        tombol.add(btnBawaan);

        JPanel bawah = new JPanel(new BorderLayout(0, 10));
        bawah.setOpaque(false);
        bawah.add(petunjuk, BorderLayout.NORTH);
        bawah.add(tombol, BorderLayout.SOUTH);

        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 12));
        k.add(new JLabel("<html><b>Logo Aplikasi</b></html>"), BorderLayout.NORTH);
        JPanel tengah = new JPanel(new BorderLayout(0, 12));
        tengah.setOpaque(false);
        tengah.add(lblLogo, BorderLayout.NORTH);
        tengah.add(bawah, BorderLayout.CENTER);
        k.add(tengah, BorderLayout.CENTER);
        return k;
    }

    private void pilihLogo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih logo aplikasi");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Gambar (JPG, PNG, GIF, WebP)", "jpg", "jpeg", "png", "gif", "webp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage image = ImageIO.read(chooser.getSelectedFile());
            if (image == null) throw new IOException("File bukan gambar yang didukung.");
            logoBaru = LogoToko.keBytes(image);
            tampilkanLogo(ImageIO.read(new ByteArrayInputStream(logoBaru)));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal membaca gambar: " + ex.getMessage());
        }
    }

    private void simpanLogo() {
        if (logoBaru == null) {
            JOptionPane.showMessageDialog(this, "Pilih gambar dulu dengan tombol \"Pilih Gambar...\".");
            return;
        }
        try {
            LogoToko.simpan(logoBaru);
            logoBaru = null;
            JOptionPane.showMessageDialog(this, "Logo tersimpan. Akan tampil di halaman login berikutnya.");
        } catch (SQLException ex) {
            Db.error(ex);
        }
    }

    private void kembalikanLogoBawaan() {
        int ok = JOptionPane.showConfirmDialog(this, "Kembalikan logo ke bawaan?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            LogoToko.hapus();
            logoBaru = null;
            tampilkanLogo(LogoToko.muat());
        } catch (SQLException ex) {
            Db.error(ex);
        }
    }

    /** Menampilkan pratinjau logo di dalam kotak, proporsi tetap. */
    private void tampilkanLogo(BufferedImage image) {
        if (image == null) {
            lblLogo.setIcon(null);
            lblLogo.setText("Logo tidak tersedia");
            return;
        }
        double s = Math.min(440.0 / image.getWidth(), 190.0 / image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * s));
        int h = Math.max(1, (int) Math.round(image.getHeight() * s));
        lblLogo.setText("");
        lblLogo.setIcon(new ImageIcon(LogoToko.skala(image, w, h)));
    }

    public void segarkan() {
        txtInfoNama.setText(Pengaturan.get("tokoNama", Pengaturan.get("namaToko", "Tahu Walik")));
        txtInfoAlamat.setText(Pengaturan.get("tokoAlamat", Pengaturan.get("alamat", "")));
        txtInfoTelp.setText(Pengaturan.get("tokoTelp", ""));
        txtToko.setText(Pengaturan.get("namaToko", "Tahu Walik"));
        txtAlamat.setText(Pengaturan.get("alamat", ""));
        txtFooter.setText(Pengaturan.get("footer", "Terima kasih!"));
        cmbKertas.setSelectedIndex(Pengaturan.kertasMm() == 80 ? 0 : 1);
        cmbUkuran.setSelectedIndex(Pengaturan.ukuran());
        cmbPrinter.removeAllItems();
        cmbPrinter.addItem("(tanya saat cetak)");
        for (String nama : Struk.daftarPrinter()) cmbPrinter.addItem(nama);
        cmbPrinter.setSelectedItem(Pengaturan.get("printer", ""));
        if (cmbPrinter.getSelectedIndex() < 0) cmbPrinter.setSelectedIndex(0);
        chkOtomatis.setSelected("1".equals(Pengaturan.get("otomatis", "0")));
        perbaruiPratinjau();
        muatRasa();
        muatMenu();
        logoBaru = null;
        tampilkanLogo(LogoToko.muat());
    }

    void muatRasa() {
        tblRasa.setModel(Db.kueri(
                "SELECT id_rasa, nama_rasa, COALESCE(keterangan,'') FROM varian_rasa ORDER BY id_rasa",
                new String[]{"ID", "Rasa", "Keterangan"}));
    }

    void tambahRasa() {
        String nama = txtRasa.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama rasa belum diisi");
            return;
        }
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO varian_rasa (nama_rasa, keterangan) VALUES (?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nama);
                    String ket = txtKet.getText().trim();
                    ps.setString(2, ket.isEmpty() ? null : ket);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                // rasa baru otomatis berlaku untuk semua menu
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO menu_varian (id_menu, id_rasa) SELECT id_menu, ? FROM menu")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            txtRasa.setText("");
            txtKet.setText("");
            muatRasa();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menambah rasa: " + e.getMessage());
        }
    }

    void hapusRasa() {
        int r = tblRasa.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih rasa di tabel dulu");
            return;
        }
        int id = Integer.parseInt(tblRasa.getValueAt(r, 0).toString());
        int ok = JOptionPane.showConfirmDialog(this, "Hapus rasa \"" + tblRasa.getValueAt(r, 1) + "\"?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_varian WHERE id_rasa=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM varian_rasa WHERE id_rasa=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            muatRasa();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Rasa ini sudah pernah dipakai di transaksi, jadi tidak bisa dihapus.");
        }
    }
}