package com.mycompany.mavenproject1;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Struk {

    // =====================================================
    //  ISI STRUK (teks)
    // =====================================================
    public static String bangun(int idTransaksi) throws SQLException {
        int L = Pengaturan.lebar();
        String garis = "-".repeat(L);
        StringBuilder sb = new StringBuilder();

        String sql = "SELECT t.kode_transaksi, t.tanggal, t.total_harga, t.status, "
                + "m.nama_menu, r.nama_rasa, d.jumlah, d.harga_satuan, d.subtotal, d.catatan, "
                + "p.metode, p.jumlah_bayar, p.kembalian "
                + "FROM transaksi t "
                + "JOIN transaksi_detail d ON d.id_transaksi = t.id_transaksi "
                + "JOIN menu m ON m.id_menu = d.id_menu "
                + "LEFT JOIN varian_rasa r ON r.id_rasa = d.id_rasa "
                + "LEFT JOIN pembayaran p ON p.id_transaksi = t.id_transaksi "
                + "WHERE t.id_transaksi = ? ORDER BY d.id_detail";

        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                boolean awal = true;
                double total = 0, bayar = 0, kembali = 0;
                String metode = "-";
                while (rs.next()) {
                    if (awal) {
                        tambah(sb, tengah(Pengaturan.get("namaToko", "CHIKA CHIKU").toUpperCase(), L), L);
                        String alamat = Pengaturan.get("alamat", "");
                        if (!alamat.isBlank()) tambah(sb, tengah(alamat, L), L);
                        sb.append(garis).append("\n");
                        sb.append(rs.getString("kode_transaksi")).append("\n");
                        sb.append(rs.getTimestamp("tanggal").toString(), 0, 16).append("\n");
                        if ("batal".equals(rs.getString("status"))) sb.append("** DIBATALKAN **\n");
                        sb.append(garis).append("\n");
                        total = rs.getDouble("total_harga");
                        bayar = rs.getDouble("jumlah_bayar");
                        kembali = rs.getDouble("kembalian");
                        if (rs.getString("metode") != null) metode = rs.getString("metode").toUpperCase();
                        awal = false;
                    }
                    String rasa = rs.getString("nama_rasa");
                    tambah(sb, rs.getString("nama_menu") + (rasa != null ? " (" + rasa + ")" : ""), L);
                    sb.append(baris("  " + rs.getInt("jumlah") + " x " + Tema.num(rs.getDouble("harga_satuan")),
                            Tema.num(rs.getDouble("subtotal")), L)).append("\n");
                    String cat = rs.getString("catatan");
                    if (cat != null && !cat.isBlank()) tambah(sb, "  *" + cat, L);
                }
                if (awal) return "Transaksi tidak ditemukan";
                sb.append(garis).append("\n");
                sb.append(baris("TOTAL", Tema.num(total), L)).append("\n");
                sb.append(baris("Bayar (" + metode + ")", Tema.num(bayar), L)).append("\n");
                sb.append(baris("Kembali", Tema.num(kembali), L)).append("\n");
                sb.append(garis).append("\n");
                tambah(sb, tengah(Pengaturan.get("footer", "Terima kasih!"), L), L);
            }
        }
        return sb.toString();
    }

    // =====================================================
    //  TAMPIL & CETAK
    // =====================================================

    /** Menampilkan struk di jendela kecil, ada tombol Cetak. */
    public static void tampil(Component parent, int idTransaksi) {
        String isi;
        try {
            isi = bangun(idTransaksi);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Gagal membuat struk: " + e.getMessage());
            return;
        }
        JTextArea area = new JTextArea(isi);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setMargin(new Insets(10, 10, 10, 10));

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), "Struk", Dialog.ModalityType.APPLICATION_MODAL);
        JButton btnCetak = Tema.tombol("Cetak", Tema.BIRU);
        JButton btnTutup = new JButton("Tutup");
        btnCetak.addActionListener(e -> cetak(d, isi, false));
        btnTutup.addActionListener(e -> d.dispose());

        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bawah.add(btnCetak);
        bawah.add(btnTutup);
        d.add(new JScrollPane(area), BorderLayout.CENTER);
        d.add(bawah, BorderLayout.SOUTH);
        d.setSize(460, 520);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    /** Langsung cetak tanpa menampilkan jendela struk. */
    public static void cetakLangsung(Component parent, int idTransaksi) {
        try {
            cetak(parent, bangun(idTransaksi), false);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Gagal membuat struk: " + e.getMessage());
        }
    }

    /**
     * Mencetak teks struk dengan ukuran kertas yang benar (58mm / 80mm).
     * Kalau nama printer sudah dipilih di Setting, langsung cetak ke printer itu tanpa dialog.
     * Kalau belum, muncul dialog pilih printer.
     */
    public static void cetak(Component parent, String isi, boolean selaluTanya) {
        int L = Pengaturan.lebar();
        List<String> baris = new ArrayList<>();
        for (String b : isi.split("\n", -1)) baris.addAll(bungkus(b, L));

        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService pilihan = cariPrinter(Pengaturan.get("printer", ""));
        try {
            if (pilihan != null) job.setPrintService(pilihan);

            double lebarKertas = Pengaturan.kertasMm() / 25.4 * 72; // mm -> point
            double margin = 6;
            double ukuranFont = (lebarKertas - 2 * margin) / (L * 0.6);
            double tinggi = baris.size() * ukuranFont * 1.3 + 30;

            Paper paper = new Paper();
            paper.setSize(lebarKertas, tinggi);
            paper.setImageableArea(margin, 6, lebarKertas - 2 * margin, tinggi - 12);
            PageFormat pf = new PageFormat();
            pf.setPaper(paper);
            pf.setOrientation(PageFormat.PORTRAIT);

            job.setPrintable(new StrukPrintable(baris, L), pf);
            if (selaluTanya || pilihan == null) {
                if (!job.printDialog()) return;
            }
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent, "Gagal cetak: " + e.getMessage());
        }
    }

    /** Menggambar teks per baris dengan font monospasi yang ukurannya pas dengan lebar kertas. */
    static class StrukPrintable implements Printable {
        final List<String> baris;
        final int lebarKarakter;

        StrukPrintable(List<String> baris, int lebarKarakter) {
            this.baris = baris;
            this.lebarKarakter = lebarKarakter;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int halaman) {
            Graphics2D g2 = (Graphics2D) g;
            double lebar = pf.getImageableWidth();

            // kecilkan font sampai 1 baris penuh (L karakter) muat di lebar kertas
            float ukuran = 14f;
            Font font;
            FontMetrics fm;
            while (true) {
                font = new Font(Font.MONOSPACED, Font.PLAIN, 10).deriveFont(ukuran);
                fm = g2.getFontMetrics(font);
                if (fm.stringWidth("0".repeat(lebarKarakter)) <= lebar * 0.97 || ukuran <= 5f) break;
                ukuran -= 0.5f;
            }
            g2.setFont(font);
            g2.setColor(Color.BLACK);

            int tinggiBaris = fm.getHeight();
            int perHalaman = Math.max(1, (int) (pf.getImageableHeight() / tinggiBaris));
            int mulai = halaman * perHalaman;
            if (mulai >= baris.size()) return NO_SUCH_PAGE;

            g2.translate(pf.getImageableX(), pf.getImageableY());
            int y = fm.getAscent();
            for (int i = mulai; i < Math.min(baris.size(), mulai + perHalaman); i++) {
                g2.drawString(baris.get(i), 0, y);
                y += tinggiBaris;
            }
            return PAGE_EXISTS;
        }
    }

    public static PrintService cariPrinter(String nama) {
        if (nama == null || nama.isBlank()) return null;
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (s.getName().equalsIgnoreCase(nama)) return s;
        }
        return null;
    }

    public static List<String> daftarPrinter() {
        List<String> hasil = new ArrayList<>();
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) hasil.add(s.getName());
        return hasil;
    }

    // =====================================================
    //  FUNGSI BANTU FORMAT TEKS
    // =====================================================

    /** Menambahkan teks ke struk; kalau lebih panjang dari lebar kertas, dipecah ke baris berikutnya. */
    static void tambah(StringBuilder sb, String teks, int L) {
        for (String b : bungkus(teks, L)) sb.append(b).append("\n");
    }

    static List<String> bungkus(String teks, int L) {
        List<String> hasil = new ArrayList<>();
        while (teks.length() > L) {
            int potong = teks.lastIndexOf(' ', L);
            if (potong <= 0) potong = L;
            hasil.add(teks.substring(0, potong));
            teks = "  " + teks.substring(potong).trim();
        }
        hasil.add(teks);
        return hasil;
    }

    static String baris(String kiri, String kanan, int L) {
        int spasi = Math.max(1, L - kiri.length() - kanan.length());
        return kiri + " ".repeat(spasi) + kanan;
    }

    static String tengah(String teks, int L) {
        int spasi = Math.max(0, (L - teks.length()) / 2);
        return " ".repeat(spasi) + teks;
    }
}