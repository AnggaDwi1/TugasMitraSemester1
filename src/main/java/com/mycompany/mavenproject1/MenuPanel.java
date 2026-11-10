package com.mycompany.mavenproject1;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.sql.*;

public class MenuPanel extends JPanel {

    static class Kat {
        int id; String nama;
        Kat(int id, String nama) { this.id = id; this.nama = nama; }
        @Override public String toString() { return nama; }
    }

    JTable tbl = new JTable();
    JTextField txtNama = new JTextField();
    JTextField txtHarga = new JTextField();
    JTextArea txtDesk = new JTextArea(3, 15);
    JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"tersedia", "habis"});
    JComboBox<Kat> cmbKat = new JComboBox<>();
    JLabel lblGambar = new JLabel("Belum ada gambar", SwingConstants.CENTER);
    byte[] gambarTerpilih;
    int idTerpilih = 0;

    public MenuPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        add(Tema.judul("Daftar Menu"), BorderLayout.NORTH);

        // ----- tabel menu (kiri/tengah) -----
        Tema.gayaTabel(tbl);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) isiFormDariTabel();
        });
        Tema.Kartu kartuTabel = new Tema.Kartu();
        kartuTabel.setLayout(new BorderLayout());
        kartuTabel.add(new JScrollPane(tbl), BorderLayout.CENTER);
        add(kartuTabel, BorderLayout.CENTER);

        // ----- form (kanan) -----
        txtDesk.setLineWrap(true);
        txtDesk.setWrapStyleWord(true);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblGambar.setPreferredSize(new Dimension(300, 130));
        lblGambar.setMinimumSize(new Dimension(0, 130));
        lblGambar.setMaximumSize(new Dimension(300, 130));
        lblGambar.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        JButton btnPilihGambar = new JButton("Pilih Gambar...");
        JButton btnHapusGambar = new JButton("Hapus Gambar");
        btnPilihGambar.setPreferredSize(new Dimension(0, 34));
        btnHapusGambar.setPreferredSize(new Dimension(0, 34));
        gayaTombolRingan(btnPilihGambar);
        gayaTombolRingan(btnHapusGambar);
        btnPilihGambar.addActionListener(e -> pilihGambar());
        btnHapusGambar.addActionListener(e -> setGambar(null));
        JPanel aksiGambar = new JPanel(new GridLayout(1, 2, 6, 0));
        aksiGambar.setOpaque(false);
        aksiGambar.add(btnPilihGambar);
        aksiGambar.add(btnHapusGambar);
        aksiGambar.setPreferredSize(new Dimension(300, 36));
        aksiGambar.setMaximumSize(new Dimension(300, 36));
        aksiGambar.setAlignmentX(Component.CENTER_ALIGNMENT);
        tambahKolom(form, "Gambar menu", lblGambar);
        form.add(aksiGambar);
        tambahKolom(form, "Nama menu", txtNama);
        tambahKolom(form, "Harga", txtHarga);
        tambahKolom(form, "Kategori", cmbKat);
        tambahKolom(form, "Status", cmbStatus);
        txtDesk.setPreferredSize(new Dimension(100, 84));
        txtDesk.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        JScrollPane scrollDesk = new JScrollPane(txtDesk);
        scrollDesk.setPreferredSize(new Dimension(300, 84));
        scrollDesk.setMaximumSize(new Dimension(300, 84));
        scrollDesk.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        scrollDesk.getViewport().setBackground(Color.WHITE);
        tambahKolom(form, "Deskripsi", scrollDesk);

        JButton btnSimpan = Tema.tombol("Simpan", Tema.BIRU);
        JButton btnHapus = Tema.tombol("Hapus", Tema.MERAH);
        JButton btnBaru = new JButton("Baru / Kosongkan");
        btnSimpan.addActionListener(e -> simpan());
        btnHapus.addActionListener(e -> hapus());
        btnBaru.addActionListener(e -> kosongkan());

        JPanel tombol = new JPanel(new GridLayout(0, 1, 0, 6));
        tombol.setOpaque(false);
        tombol.add(btnSimpan);
        tombol.add(btnHapus);
        tombol.add(btnBaru);

        Tema.Kartu kartuForm = new Tema.Kartu();
        kartuForm.setLayout(new BorderLayout(0, 6));
        kartuForm.setPreferredSize(new Dimension(330, 0));
        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBorder(null);
        scrollForm.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollForm.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollForm.setOpaque(false);
        scrollForm.getViewport().setOpaque(false);
        scrollForm.getVerticalScrollBar().setUnitIncrement(18);
        kartuForm.add(scrollForm, BorderLayout.CENTER);
        kartuForm.add(tombol, BorderLayout.SOUTH);
        add(kartuForm, BorderLayout.EAST);

        segarkan();
    }

    private void tambahKolom(JPanel panel, String judul, JComponent isi) {
        JLabel label = new JLabel(judul);
        label.setHorizontalAlignment("Gambar menu".equals(judul)
                ? SwingConstants.CENTER : SwingConstants.LEFT);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        isi.setAlignmentX(Component.CENTER_ALIGNMENT);
        int tinggi = isi == lblGambar ? 130 : isi instanceof JScrollPane ? 84 : 36;
        isi.setPreferredSize(new Dimension(300, tinggi));
        isi.setMaximumSize(new Dimension(300, tinggi));
        panel.add(Box.createVerticalStrut(10));
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(isi);
    }

    private void gayaTombolRingan(JButton tombol) {
        tombol.setFocusPainted(false);
        tombol.setBackground(Tema.BG);
        tombol.setForeground(Tema.COKELAT);
        tombol.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
    }

    public void segarkan() {
        // kategori
        cmbKat.removeAllItems();
        try (Connection c = Koneksi.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id_kategori, nama_kategori FROM kategori_menu ORDER BY nama_kategori")) {
            while (rs.next()) cmbKat.addItem(new Kat(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) {
            Db.error(e);
        }
        // tabel
        tbl.setModel(Db.kueri(
                "SELECT m.id_menu, m.nama_menu, COALESCE(k.nama_kategori,'-'), CAST(m.harga AS UNSIGNED), "
              + "m.status, COALESCE(m.deskripsi,'') "
              + "FROM menu m LEFT JOIN kategori_menu k ON k.id_kategori=m.id_kategori ORDER BY m.id_menu",
                new String[]{"ID", "Menu", "Kategori", "Harga", "Status", "Deskripsi"}));
        kosongkan();
    }

    void isiFormDariTabel() {
        int row = tbl.getSelectedRow();
        if (row < 0) return;
        DefaultTableModel m = (DefaultTableModel) tbl.getModel();
        idTerpilih = Integer.parseInt(m.getValueAt(row, 0).toString());
        txtNama.setText(m.getValueAt(row, 1).toString());
        txtHarga.setText(m.getValueAt(row, 3).toString());
        cmbStatus.setSelectedItem(m.getValueAt(row, 4).toString());
        txtDesk.setText(m.getValueAt(row, 5).toString());
        muatGambar(idTerpilih);
        String namaKat = m.getValueAt(row, 2).toString();
        for (int i = 0; i < cmbKat.getItemCount(); i++) {
            if (cmbKat.getItemAt(i).nama.equals(namaKat)) { cmbKat.setSelectedIndex(i); break; }
        }
    }

    void kosongkan() {
        idTerpilih = 0;
        tbl.clearSelection();
        txtNama.setText("");
        txtHarga.setText("");
        txtDesk.setText("");
        setGambar(null);
        cmbStatus.setSelectedIndex(0);
        if (cmbKat.getItemCount() > 0) cmbKat.setSelectedIndex(0);
    }

    void simpan() {
        String nama = txtNama.getText().trim();
        double harga;
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama menu belum diisi");
            return;
        }
        try {
            harga = Double.parseDouble(txtHarga.getText().trim());
            if (harga <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga harus berupa angka lebih dari 0");
            return;
        }
        Kat kat = (Kat) cmbKat.getSelectedItem();
        String desk = txtDesk.getText().trim();

        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                if (idTerpilih == 0) {
                    int idBaru;
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO menu (id_kategori, nama_menu, harga, deskripsi, status, gambar) VALUES (?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
                        setBlob(ps, 6, gambarTerpilih);
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
                            "UPDATE menu SET id_kategori=?, nama_menu=?, harga=?, deskripsi=?, status=?, gambar=? WHERE id_menu=?")) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
                        setBlob(ps, 6, gambarTerpilih);
                        ps.setInt(7, idTerpilih);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            segarkan();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
        }
    }

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
            Image scaled = image.getScaledInstance(240, 130, Image.SCALE_SMOOTH);
            lblGambar.setText("");
            lblGambar.setIcon(new ImageIcon(scaled));
        } catch (IOException ex) {
            lblGambar.setIcon(null);
            lblGambar.setText("Gambar tidak valid");
        }
    }

    private static void setBlob(PreparedStatement ps, int index, byte[] bytes) throws SQLException {
        if (bytes == null) ps.setNull(index, Types.BLOB); else ps.setBytes(index, bytes);
    }

    void hapus() {
        if (idTerpilih == 0) {
            JOptionPane.showMessageDialog(this, "Pilih menu di tabel dulu");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Hapus menu \"" + txtNama.getText() + "\"?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_varian WHERE id_menu=?")) {
                    ps.setInt(1, idTerpilih);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu WHERE id_menu=?")) {
                    ps.setInt(1, idTerpilih);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            segarkan();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Menu tidak bisa dihapus karena sudah pernah terjual.\n"
                  + "Ubah statusnya menjadi \"habis\" saja.");
        }
    }
}
