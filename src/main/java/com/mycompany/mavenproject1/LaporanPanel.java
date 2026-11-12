package com.mycompany.mavenproject1;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Ringkasan transaksi dan penjualan untuk rentang tanggal yang dipilih. */
public class LaporanPanel extends JPanel {
    private final JComboBox<String> modeTanggal = new JComboBox<>(new String[]{"Harian", "Rentang tanggal"});
    private final PemilihTanggal tanggalHarian = new PemilihTanggal(LocalDate.now());
    private final PemilihTanggal dari = new PemilihTanggal(LocalDate.now().withDayOfMonth(1));
    private final PemilihTanggal sampai = new PemilihTanggal(LocalDate.now());
    private final JLabel lblTransaksi = new JLabel("0");
    private final JLabel lblOmzet = new JLabel("Rp 0");
    private final JLabel lblCash = new JLabel("Rp 0");
    private final JLabel lblQris = new JLabel("Rp 0");
    private final JTable tblTransaksi = new JTable();
    private final JTable tblMenu = new JTable();
    private final JTabbedPane tab = new JTabbedPane();

    public LaporanPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JButton btnTerapkan = Tema.tombol("Tampilkan", Tema.KUNING);
        btnTerapkan.addActionListener(e -> segarkan());
        JButton btnEkspor = Tema.tombol("Ekspor CSV", Tema.KUNING);
        btnEkspor.addActionListener(e -> eksporCsv());

        JPanel harian = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        harian.setOpaque(false);
        harian.add(new JLabel("Tanggal:")); harian.add(tanggalHarian);
        JPanel rentang = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rentang.setOpaque(false);
        rentang.add(new JLabel("Dari:")); rentang.add(dari);
        rentang.add(new JLabel("Sampai:")); rentang.add(sampai);
        JPanel pilihanTanggal = new JPanel(new CardLayout());
        pilihanTanggal.setOpaque(false);
        pilihanTanggal.add(harian, "Harian");
        pilihanTanggal.add(rentang, "Rentang tanggal");
        modeTanggal.addActionListener(e -> {
            ((CardLayout) pilihanTanggal.getLayout()).show(pilihanTanggal, (String) modeTanggal.getSelectedItem());
            segarkan();
        });
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filter.setOpaque(false);
        filter.add(modeTanggal);
        filter.add(pilihanTanggal);
        filter.add(btnTerapkan); filter.add(btnEkspor);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(Tema.judul("Laporan"), BorderLayout.NORTH);
        header.add(filter, BorderLayout.CENTER);

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(Tema.kartuStat("Transaksi Selesai", lblTransaksi));
        stats.add(Tema.kartuStat("Total Omzet", lblOmzet));
        stats.add(Tema.kartuStat("Pembayaran Cash", lblCash));
        stats.add(Tema.kartuStat("Pembayaran QRIS", lblQris));

        Tema.gayaTabel(tblTransaksi);
        Tema.gayaTabel(tblMenu);
        tab.addTab("Transaksi", new JScrollPane(tblTransaksi));
        tab.addTab("Penjualan Menu", new JScrollPane(tblMenu));

        JPanel atas = new JPanel(new BorderLayout(0, 14));
        atas.setOpaque(false);
        atas.add(header, BorderLayout.NORTH);
        atas.add(stats, BorderLayout.CENTER);
        add(atas, BorderLayout.NORTH);
        add(tab, BorderLayout.CENTER);
        segarkan();
    }

    public void segarkan() {
        LocalDate awal = "Harian".equals(modeTanggal.getSelectedItem()) ? tanggalHarian.getTanggal() : dari.getTanggal();
        LocalDate akhir = "Harian".equals(modeTanggal.getSelectedItem()) ? awal : sampai.getTanggal();
        if (akhir.isBefore(awal)) {
            JOptionPane.showMessageDialog(this, "Tanggal akhir tidak boleh sebelum tanggal awal.", "Rentang tanggal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Nilai tanggal sudah diparse LocalDate, sehingga aman dimasukkan ke filter SQL.
        String rentang = "t.tanggal >= '" + awal + "' AND t.tanggal < '" + akhir.plusDays(1) + "'";
        String selesai = rentang + " AND t.status='selesai'";
        lblTransaksi.setText(String.valueOf(Db.angka("SELECT COUNT(*) FROM transaksi t WHERE " + selesai)));
        lblOmzet.setText(Tema.rp(Db.angka("SELECT COALESCE(SUM(t.total_harga),0) FROM transaksi t WHERE " + selesai)));
        lblCash.setText(Tema.rp(Db.angka("SELECT COALESCE(SUM(p.jumlah_bayar-p.kembalian),0) FROM pembayaran p JOIN transaksi t ON t.id_transaksi=p.id_transaksi WHERE p.metode='cash' AND " + selesai)));
        lblQris.setText(Tema.rp(Db.angka("SELECT COALESCE(SUM(p.jumlah_bayar),0) FROM pembayaran p JOIN transaksi t ON t.id_transaksi=p.id_transaksi WHERE p.metode='qris' AND " + selesai)));
        tblTransaksi.setModel(Db.kueri(
            "SELECT t.kode_transaksi, DATE_FORMAT(t.tanggal,'%d/%m/%Y %H:%i'), COALESCE(k.nama_kasir,'-'), CAST(t.total_harga AS UNSIGNED), COALESCE(p.metode,'-'), t.status " +
            "FROM transaksi t LEFT JOIN kasir k ON k.id_kasir=t.id_kasir LEFT JOIN pembayaran p ON p.id_transaksi=t.id_transaksi WHERE " + rentang + " ORDER BY t.tanggal DESC",
            new String[]{"Kode", "Waktu", "Kasir", "Total", "Metode", "Status"}));
        tblMenu.setModel(Db.kueri(
            "SELECT m.nama_menu, COALESCE(SUM(d.jumlah),0), CAST(COALESCE(SUM(d.subtotal),0) AS UNSIGNED) " +
            "FROM transaksi_detail d JOIN transaksi t ON t.id_transaksi=d.id_transaksi JOIN menu m ON m.id_menu=d.id_menu WHERE " + selesai +
            " GROUP BY m.id_menu,m.nama_menu ORDER BY SUM(d.jumlah) DESC",
            new String[]{"Menu", "Jumlah Terjual", "Pendapatan"}));
    }

    private void eksporCsv() {
        JTable tabel = tab.getSelectedIndex() == 1 ? tblMenu : tblTransaksi;
        JFileChooser chooser = new JFileChooser();
        boolean laporanMenu = tabel == tblMenu;
        chooser.setDialogTitle("Simpan laporan");
        chooser.setSelectedFile(new File(laporanMenu ? "laporan-penjualan-menu.csv" : "laporan-transaksi.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("\uFEFF");
            TableModel model = tabel.getModel();
            for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) w.write(','); csv(w, model.getColumnName(c)); }
            w.write("\r\n");
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) w.write(','); csv(w, String.valueOf(model.getValueAt(r, c))); }
                w.write("\r\n");
            }
            JOptionPane.showMessageDialog(this, "Laporan disimpan: " + file.getAbsolutePath());
        } catch (IOException e) { JOptionPane.showMessageDialog(this, "Gagal menyimpan CSV: " + e.getMessage(), "Ekspor", JOptionPane.ERROR_MESSAGE); }
    }

    private void csv(Writer w, String value) throws IOException {
        w.write('"'); w.write(value.replace("\"", "\"\"")); w.write('"');
    }
}
