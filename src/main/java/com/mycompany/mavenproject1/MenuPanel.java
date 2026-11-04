package com.mycompany.mavenproject1;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.setOpaque(false);
        form.add(new JLabel("Nama menu"));  form.add(txtNama);
        form.add(new JLabel("Harga"));      form.add(txtHarga);
        form.add(new JLabel("Kategori"));   form.add(cmbKat);
        form.add(new JLabel("Status"));     form.add(cmbStatus);
        form.add(new JLabel("Deskripsi"));

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
        kartuForm.setPreferredSize(new Dimension(300, 0));
        JPanel atasForm = new JPanel(new BorderLayout());
        atasForm.setOpaque(false);
        atasForm.add(form, BorderLayout.NORTH);
        atasForm.add(new JScrollPane(txtDesk), BorderLayout.CENTER);
        kartuForm.add(atasForm, BorderLayout.CENTER);
        kartuForm.add(tombol, BorderLayout.SOUTH);
        add(kartuForm, BorderLayout.EAST);

        segarkan();
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
                            "INSERT INTO menu (id_kategori, nama_menu, harga, deskripsi, status) VALUES (?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
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
                            "UPDATE menu SET id_kategori=?, nama_menu=?, harga=?, deskripsi=?, status=? WHERE id_menu=?")) {
                        if (kat == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, kat.id);
                        ps.setString(2, nama);
                        ps.setDouble(3, harga);
                        ps.setString(4, desk.isEmpty() ? null : desk);
                        ps.setString(5, (String) cmbStatus.getSelectedItem());
                        ps.setInt(6, idTerpilih);
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