package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    JLabel lblTrx = new JLabel("0");
    JLabel lblOmzet = new JLabel("Rp 0");
    JLabel lblCash = new JLabel("Rp 0");
    JLabel lblQris = new JLabel("Rp 0");
    JTable tblTerakhir = new JTable();
    JTable tblTerlaris = new JTable();

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // ----- atas: judul + tombol refresh + kartu statistik -----
        JButton btnRefresh = Tema.tombol("Refresh", Tema.KUNING);
        btnRefresh.addActionListener(e -> segarkan());
        JPanel judul = new JPanel(new BorderLayout());
        judul.setOpaque(false);
        judul.add(Tema.judul("Dashboard"), BorderLayout.WEST);
        judul.add(btnRefresh, BorderLayout.EAST);

        JPanel stat = new JPanel(new GridLayout(1, 4, 12, 0));
        stat.setOpaque(false);
        stat.add(Tema.kartuStat("Transaksi Hari Ini", lblTrx));
        stat.add(Tema.kartuStat("Omzet Hari Ini", lblOmzet));
        stat.add(Tema.kartuStat("Total Cash", lblCash));
        stat.add(Tema.kartuStat("Total QRIS", lblQris));

        JPanel atas = new JPanel(new BorderLayout(0, 14));
        atas.setOpaque(false);
        atas.add(judul, BorderLayout.NORTH);
        atas.add(stat, BorderLayout.CENTER);
        add(atas, BorderLayout.NORTH);

        // ----- bawah: dua tabel -----
        Tema.gayaTabel(tblTerakhir);
        Tema.gayaTabel(tblTerlaris);

        Tema.Kartu kiri = new Tema.Kartu();
        kiri.setLayout(new BorderLayout(0, 8));
        kiri.add(new JLabel("<html><b>Transaksi Terakhir</b></html>"), BorderLayout.NORTH);
        kiri.add(new JScrollPane(tblTerakhir), BorderLayout.CENTER);

        Tema.Kartu kanan = new Tema.Kartu();
        kanan.setLayout(new BorderLayout(0, 8));
        kanan.add(new JLabel("<html><b>Menu Terlaris Hari Ini</b></html>"), BorderLayout.NORTH);
        kanan.add(new JScrollPane(tblTerlaris), BorderLayout.CENTER);

        JPanel tengah = new JPanel(new GridLayout(1, 2, 14, 0));
        tengah.setOpaque(false);
        tengah.add(kiri);
        tengah.add(kanan);
        add(tengah, BorderLayout.CENTER);

        segarkan();
    }

    public void segarkan() {
        String hariIni = "DATE(t.tanggal)=CURDATE() AND t.status='selesai'";

        lblTrx.setText(String.valueOf(Db.angka(
                "SELECT COUNT(*) FROM transaksi t WHERE " + hariIni)));
        lblOmzet.setText(Tema.rp(Db.angka(
                "SELECT COALESCE(SUM(t.total_harga),0) FROM transaksi t WHERE " + hariIni)));
        lblCash.setText(Tema.rp(Db.angka(
                "SELECT COALESCE(SUM(p.jumlah_bayar - p.kembalian),0) FROM pembayaran p "
              + "JOIN transaksi t ON t.id_transaksi=p.id_transaksi WHERE p.metode='cash' AND " + hariIni)));
        lblQris.setText(Tema.rp(Db.angka(
                "SELECT COALESCE(SUM(p.jumlah_bayar),0) FROM pembayaran p "
              + "JOIN transaksi t ON t.id_transaksi=p.id_transaksi WHERE p.metode='qris' AND " + hariIni)));

        tblTerakhir.setModel(Db.kueri(
                "SELECT t.kode_transaksi, DATE_FORMAT(t.tanggal,'%d/%m %H:%i'), CAST(t.total_harga AS UNSIGNED), "
              + "COALESCE(p.metode,'-'), t.status "
              + "FROM transaksi t LEFT JOIN pembayaran p ON p.id_transaksi=t.id_transaksi "
              + "ORDER BY t.id_transaksi DESC LIMIT 10",
                new String[]{"Kode", "Waktu", "Total", "Metode", "Status"}));

        tblTerlaris.setModel(Db.kueri(
                "SELECT m.nama_menu, SUM(d.jumlah) AS terjual, CAST(SUM(d.subtotal) AS UNSIGNED) "
              + "FROM transaksi_detail d JOIN transaksi t ON t.id_transaksi=d.id_transaksi "
              + "JOIN menu m ON m.id_menu=d.id_menu WHERE " + hariIni
              + " GROUP BY m.id_menu, m.nama_menu ORDER BY terjual DESC LIMIT 10",
                new String[]{"Menu", "Terjual", "Pendapatan"}));
    }
}