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
                List<ItemStruk> items = new ArrayList<>();
                String kode = "", waktu = "", metode = "-";
                boolean batal = false, awal = true;
                double total = 0, bayar = 0, kembali = 0;
                while (rs.next()) {
                    if (awal) {
                        kode = rs.getString("kode_transaksi");
                        waktu = rs.getTimestamp("tanggal").toString().substring(0, 16);
                        batal = "batal".equals(rs.getString("status"));
                        total = rs.getDouble("total_harga");
                        bayar = rs.getDouble("jumlah_bayar");
                        kembali = rs.getDouble("kembalian");
                        if (rs.getString("metode") != null) metode = rs.getString("metode").toUpperCase();
                        awal = false;
                    }
                    String rasa = rs.getString("nama_rasa");
                    items.add(new ItemStruk(
                            rs.getString("nama_menu") + (rasa != null ? " (" + rasa + ")" : ""),
                            rs.getInt("jumlah"), rs.getDouble("harga_satuan"), rs.getDouble("subtotal"),
                            rs.getString("catatan")));
                }
                if (awal) return "Transaksi tidak ditemukan";
                return render(Pengaturan.lebar(),
                        Pengaturan.get("namaToko", "CHIKA CHIKU"),
                        Pengaturan.get("alamat", ""),
                        Pengaturan.get("footer", "Terima kasih!"),
                        kode, waktu, batal, items, total, bayar, kembali, metode);
            }
        }
    }

    /** Contoh struk untuk pratinjau di Setting. Tata letaknya sama persis dengan struk asli (lewat render). */
    public static String contoh(String namaToko, String alamat, String footer, int lebar) {
        List<ItemStruk> items = new ArrayList<>();
        items.add(new ItemStruk("Ayam Pok Pok (Original)", 2, 10000, 20000, null));
        items.add(new ItemStruk("Tahu Walik Chika Chiku (Pedas)", 1, 10000, 10000, "tidak pakai saus"));
        items.add(new ItemStruk("Mie Chikmong", 3, 8000, 24000, null));
        java.time.LocalDateTime sekarang = java.time.LocalDateTime.now();
        String kode = "TRX-" + sekarang.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
        String waktu = sekarang.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return render(lebar, namaToko, alamat, footer, kode, waktu, false, items, 54000, 60000, 6000, "CASH");
    }

    /** Satu baris pesanan di struk. */
    static class ItemStruk {
        final String nama, catatan;
        final int jumlah;
        final double harga, subtotal;

        ItemStruk(String nama, int jumlah, double harga, double subtotal, String catatan) {
            this.nama = nama;
            this.jumlah = jumlah;
            this.harga = harga;
            this.subtotal = subtotal;
            this.catatan = catatan;
        }
    }

    /** Menyusun teks struk. Dipakai oleh struk asli dan oleh pratinjau, supaya bentuknya selalu sama. */
    static String render(int L, String namaToko, String alamat, String footer,
                         String kode, String waktu, boolean batal, List<ItemStruk> items,
                         double total, double bayar, double kembali, String metode) {
        String garis = "-".repeat(L);
        StringBuilder sb = new StringBuilder();

        tambah(sb, tengah(namaToko.toUpperCase(), L), L);
        if (!alamat.isBlank()) tambah(sb, tengah(alamat, L), L);
        sb.append(garis).append("\n");
        sb.append(kode).append("\n");
        sb.append(waktu).append("\n");
        if (batal) sb.append("** DIBATALKAN **\n");
        sb.append(garis).append("\n");

        for (ItemStruk it : items) {
            tambah(sb, it.nama, L);
            sb.append(baris("  " + it.jumlah + " x " + Tema.num(it.harga), Tema.num(it.subtotal), L)).append("\n");
            if (it.catatan != null && !it.catatan.isBlank()) tambah(sb, "  *" + it.catatan, L);
        }

        sb.append(garis).append("\n");
        sb.append(baris("TOTAL", Tema.num(total), L)).append("\n");
        sb.append(baris("Bayar (" + metode + ")", Tema.num(bayar), L)).append("\n");
        sb.append(baris("Kembali", Tema.num(kembali), L)).append("\n");
        sb.append(garis).append("\n");
        tambah(sb, tengah(footer, L), L);
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