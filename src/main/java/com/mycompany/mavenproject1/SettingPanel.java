package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class SettingPanel extends JPanel {

    JTextField txtToko = new JTextField();
    JTextField txtAlamat = new JTextField();
    JTextField txtFooter = new JTextField();
    JComboBox<String> cmbKertas = new JComboBox<>(new String[]{"80 mm", "58 mm"});
    JComboBox<String> cmbUkuran = new JComboBox<>(new String[]{"Kecil", "Sedang", "Besar"});
    JComboBox<String> cmbPrinter = new JComboBox<>();
    JCheckBox chkOtomatis = new JCheckBox("Cetak otomatis setelah Konfirmasi");

    JTable tblRasa = new JTable();
    JTextField txtRasa = new JTextField();
    JTextField txtKet = new JTextField();

    public SettingPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        add(Tema.judul("Setting"), BorderLayout.NORTH);

        JPanel isi = new JPanel(new GridLayout(1, 2, 14, 0));
        isi.setOpaque(false);
        isi.add(buatKartuToko());
        isi.add(buatKartuRasa());
        add(isi, BorderLayout.CENTER);

        segarkan();
    }

    // ---------- kartu kiri: info toko untuk struk ----------
    Tema.Kartu buatKartuToko() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.setOpaque(false);
        form.add(new JLabel("Nama toko (di struk)"));  form.add(txtToko);
        form.add(new JLabel("Alamat (di struk)"));     form.add(txtAlamat);
        form.add(new JLabel("Pesan bawah struk"));     form.add(txtFooter);
        form.add(new JLabel("Lebar kertas"));          form.add(cmbKertas);
        form.add(new JLabel("Ukuran huruf struk"));    form.add(cmbUkuran);
        form.add(new JLabel("Printer struk"));         form.add(cmbPrinter);
        form.add(chkOtomatis);

        JButton btnSimpan = Tema.tombol("Simpan Pengaturan", Tema.BIRU);
        btnSimpan.addActionListener(e -> simpanToko());

        JLabel info = new JLabel(infoDatabase());
        info.setForeground(Tema.ABU);

        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 10));
        k.add(new JLabel("<html><b>Info Toko &amp; Struk</b></html>"), BorderLayout.NORTH);
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

    String infoDatabase() {
        try (Connection c = Koneksi.get()) {
            return "Database terhubung: " + c.getCatalog();
        } catch (SQLException e) {
            return "Database TIDAK terhubung";
        }
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

    // ---------- kartu kanan: kelola varian rasa ----------
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

    public void segarkan() {
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
        muatRasa();
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