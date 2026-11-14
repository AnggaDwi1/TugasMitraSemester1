package com.mycompany.mavenproject1;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

/** Dashboard: ringkasan hari ini dibanding kemarin, grafik penjualan, menu terlaris, dan metode bayar. */
public class DashboardPanel extends JPanel {

    private static final Locale ID = new Locale("id", "ID");
    private static final String[] HARI = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};

    private final JLabel lblSub = new JLabel(" ");
    private final KartuAngka kTrx = new KartuAngka("Transaksi Hari Ini");
    private final KartuAngka kOmzet = new KartuAngka("Omzet Hari Ini");
    private final KartuAngka kRata = new KartuAngka("Rata-rata per Transaksi");
    private final KartuAngka kItem = new KartuAngka("Item Terjual");

    private final GrafikGaris grafikJam = new GrafikGaris();
    private final GrafikBatang grafikMinggu = new GrafikBatang();
    private final GrafikPeringkat peringkat = new GrafikPeringkat();
    private final Donut donut = new Donut();

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // ----- header -----
        JButton btnRefresh = Tema.tombol("Refresh", Tema.KUNING);
        btnRefresh.addActionListener(e -> segarkan());
        lblSub.setForeground(Tema.ABU);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel teks = new JPanel(new GridLayout(2, 1, 0, 2));
        teks.setOpaque(false);
        teks.add(Tema.judul("Dashboard"));
        teks.add(lblSub);

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        kanan.setOpaque(false);
        kanan.add(btnRefresh);

        JPanel judul = new JPanel(new BorderLayout());
        judul.setOpaque(false);
        judul.add(teks, BorderLayout.WEST);
        judul.add(kanan, BorderLayout.EAST);

        // ----- kartu angka -----
        JPanel stat = new JPanel(new GridLayout(1, 4, 12, 0));
        stat.setOpaque(false);
        stat.add(kTrx);
        stat.add(kOmzet);
        stat.add(kRata);
        stat.add(kItem);

        JPanel atas = new JPanel(new BorderLayout(0, 14));
        atas.setOpaque(false);
        atas.add(judul, BorderLayout.NORTH);
        atas.add(stat, BorderLayout.CENTER);
        add(atas, BorderLayout.NORTH);

        // ----- grafik -----
        JPanel baris1 = baris(
                kartuGrafik("Omzet Hari Ini vs Kemarin", "Akumulasi omzet per jam, arahkan kursor untuk detail", grafikJam),
                kartuGrafik("7 Hari Terakhir", "Omzet harian", grafikMinggu), 0.62);
        JPanel baris2 = baris(
                kartuGrafik("Menu Terlaris Hari Ini", "5 menu dengan penjualan terbanyak", peringkat),
                kartuGrafik("Metode Pembayaran", "Komposisi omzet hari ini", donut), 0.58);

        JPanel tengah = new JPanel(new GridLayout(2, 1, 0, 14));
        tengah.setOpaque(false);
        tengah.add(baris1);
        tengah.add(baris2);
        add(tengah, BorderLayout.CENTER);

        segarkan();
    }

    // =====================================================
    //  LAYOUT BANTU
    // =====================================================
    private static JPanel baris(JComponent kiri, JComponent kanan, double lebarKiri) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = lebarKiri;
        gc.insets = new Insets(0, 0, 0, 7);
        p.add(kiri, gc);
        gc.gridx = 1;
        gc.weightx = 1 - lebarKiri;
        gc.insets = new Insets(0, 7, 0, 0);
        p.add(kanan, gc);
        return p;
    }

    private static Tema.Kartu kartuGrafik(String judul, String sub, JComponent isi) {
        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 8));
        JLabel j = new JLabel("<html><b>" + Tema.esc(judul) + "</b></html>");
        j.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JLabel s = new JLabel(sub);
        s.setForeground(Tema.ABU);
        s.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 1));
        head.setOpaque(false);
        head.add(j);
        head.add(s);
        k.add(head, BorderLayout.NORTH);
        k.add(isi, BorderLayout.CENTER);
        return k;
    }

    // =====================================================
    //  DATA
    // =====================================================
    private static double angka(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    public void segarkan() {
        String selesai = "t.status='selesai'";

        // 1) ringkasan hari ini (index 0) dan kemarin (index 1)
        double[] trx = new double[2], omzet = new double[2], item = new double[2];
        DefaultTableModel m = Db.kueri(
                "SELECT DATEDIFF(CURDATE(), DATE(t.tanggal)), COUNT(*), COALESCE(SUM(t.total_harga),0) "
              + "FROM transaksi t WHERE DATE(t.tanggal) >= CURDATE() - INTERVAL 1 DAY AND " + selesai
              + " GROUP BY 1", new String[]{"h", "n", "omzet"});
        for (int r = 0; r < m.getRowCount(); r++) {
            int h = (int) angka(m.getValueAt(r, 0));
            if (h == 0 || h == 1) {
                trx[h] = angka(m.getValueAt(r, 1));
                omzet[h] = angka(m.getValueAt(r, 2));
            }
        }
        m = Db.kueri(
                "SELECT DATEDIFF(CURDATE(), DATE(t.tanggal)), COALESCE(SUM(d.jumlah),0) "
              + "FROM transaksi_detail d JOIN transaksi t ON t.id_transaksi=d.id_transaksi "
              + "WHERE DATE(t.tanggal) >= CURDATE() - INTERVAL 1 DAY AND " + selesai + " GROUP BY 1",
                new String[]{"h", "item"});
        for (int r = 0; r < m.getRowCount(); r++) {
            int h = (int) angka(m.getValueAt(r, 0));
            if (h == 0 || h == 1) item[h] = angka(m.getValueAt(r, 1));
        }
        double rataIni = trx[0] > 0 ? omzet[0] / trx[0] : 0;
        double rataLalu = trx[1] > 0 ? omzet[1] / trx[1] : 0;

        kTrx.set(String.valueOf((long) trx[0]), trx[0], trx[1], String.valueOf((long) trx[1]));
        kOmzet.set(Tema.rp(omzet[0]), omzet[0], omzet[1], Tema.rp(omzet[1]));
        kRata.set(Tema.rp(rataIni), rataIni, rataLalu, Tema.rp(rataLalu));
        kItem.set(String.valueOf((long) item[0]), item[0], item[1], String.valueOf((long) item[1]));

        // 2) omzet per jam: hari ini vs kemarin (akumulasi)
        double[][] jam = new double[2][24];
        int minJam = 8, maxJam = 20, jamTerakhirIni = -1;
        m = Db.kueri(
                "SELECT DATEDIFF(CURDATE(), DATE(t.tanggal)), HOUR(t.tanggal), COALESCE(SUM(t.total_harga),0) "
              + "FROM transaksi t WHERE DATE(t.tanggal) >= CURDATE() - INTERVAL 1 DAY AND " + selesai
              + " GROUP BY 1, 2", new String[]{"h", "jam", "omzet"});
        for (int r = 0; r < m.getRowCount(); r++) {
            int h = (int) angka(m.getValueAt(r, 0));
            int j = (int) angka(m.getValueAt(r, 1));
            if ((h != 0 && h != 1) || j < 0 || j > 23) continue;
            jam[h][j] = angka(m.getValueAt(r, 2));
            minJam = Math.min(minJam, j);
            maxJam = Math.max(maxJam, j);
            if (h == 0) jamTerakhirIni = Math.max(jamTerakhirIni, j);
        }
        int sekarang = Math.max(LocalDateTime.now().getHour(), jamTerakhirIni);
        int n = maxJam - minJam + 1;
        String[] label = new String[n];
        double[] kini = new double[n], lalu = new double[n];
        double cumIni = 0, cumLalu = 0;
        for (int i = 0; i < n; i++) {
            int j = minJam + i;
            label[i] = String.format("%02d", j);
            cumIni += jam[0][j];
            cumLalu += jam[1][j];
            kini[i] = j <= sekarang ? cumIni : Double.NaN;
            lalu[i] = cumLalu;
        }
        grafikJam.setData(label, kini, lalu);

        // 3) 7 hari terakhir
        double[] minggu = new double[7];
        String[] labelMinggu = new String[7];
        LocalDate hariIni = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            labelMinggu[i] = HARI[hariIni.minusDays(6 - i).getDayOfWeek().getValue() - 1];
        }
        m = Db.kueri(
                "SELECT DATEDIFF(CURDATE(), DATE(t.tanggal)), COALESCE(SUM(t.total_harga),0) "
              + "FROM transaksi t WHERE DATE(t.tanggal) >= CURDATE() - INTERVAL 6 DAY AND " + selesai
              + " GROUP BY 1", new String[]{"h", "omzet"});
        for (int r = 0; r < m.getRowCount(); r++) {
            int h = (int) angka(m.getValueAt(r, 0));
            if (h >= 0 && h <= 6) minggu[6 - h] = angka(m.getValueAt(r, 1));
        }
        grafikMinggu.setData(labelMinggu, minggu);

        // 4) menu terlaris hari ini
        m = Db.kueri(
                "SELECT m.nama_menu, SUM(d.jumlah) FROM transaksi_detail d "
              + "JOIN transaksi t ON t.id_transaksi=d.id_transaksi "
              + "JOIN menu m ON m.id_menu=d.id_menu "
              + "WHERE DATE(t.tanggal)=CURDATE() AND " + selesai
              + " GROUP BY m.id_menu, m.nama_menu ORDER BY 2 DESC LIMIT 5",
                new String[]{"menu", "terjual"});
        String[] nama = new String[m.getRowCount()];
        double[] terjual = new double[m.getRowCount()];
        for (int r = 0; r < nama.length; r++) {
            nama[r] = String.valueOf(m.getValueAt(r, 0));
            terjual[r] = angka(m.getValueAt(r, 1));
        }
        peringkat.setData(nama, terjual);

        // 5) metode pembayaran hari ini
        double cash = 0, qris = 0;
        m = Db.kueri(
                "SELECT p.metode, COALESCE(SUM(CASE WHEN p.metode='cash' THEN p.jumlah_bayar - p.kembalian "
              + "ELSE p.jumlah_bayar END),0) FROM pembayaran p JOIN transaksi t ON t.id_transaksi=p.id_transaksi "
              + "WHERE DATE(t.tanggal)=CURDATE() AND " + selesai + " GROUP BY p.metode",
                new String[]{"metode", "total"});
        for (int r = 0; r < m.getRowCount(); r++) {
            String metode = String.valueOf(m.getValueAt(r, 0));
            if ("cash".equalsIgnoreCase(metode)) cash += angka(m.getValueAt(r, 1));
            else if ("qris".equalsIgnoreCase(metode)) qris += angka(m.getValueAt(r, 1));
        }
        donut.setData(cash, qris);

        lblSub.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID))
                + "   \u2022   diperbarui " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    // =====================================================
    //  FUNGSI BANTU GAMBAR
    // =====================================================
    static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static void kosong(Graphics2D g2, int w, int h, String teks) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(Tema.ABU);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(teks, (w - fm.stringWidth(teks)) / 2, h / 2 + fm.getAscent() / 2 - 2);
    }

    static String hex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    static String trimNol(String s) {
        return s.endsWith(",0") ? s.substring(0, s.length() - 2) : s;
    }

    /** 1.250.000 -> "1,3 jt", 250.000 -> "250 rb". */
    static String ringkas(double v) {
        if (v >= 1_000_000) return trimNol(String.format(ID, "%.1f", v / 1_000_000)) + " jt";
        if (v >= 1_000) return Math.round(v / 1_000) + " rb";
        return String.valueOf(Math.round(v));
    }

    static String potong(String s, FontMetrics fm, int lebar) {
        if (fm.stringWidth(s) <= lebar) return s;
        while (s.length() > 1 && fm.stringWidth(s + "\u2026") > lebar) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }

    static Color alfa(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // =====================================================
    //  KARTU ANGKA + PERBANDINGAN KEMARIN
    // =====================================================
    static class KartuAngka extends Tema.Kartu {
        private final JLabel nilai = new JLabel("0");
        private final JLabel delta = new JLabel(" ");

        KartuAngka(String label) {
            setLayout(new BorderLayout());
            JLabel l = new JLabel(label);
            l.setForeground(Tema.ABU);
            l.setFont(new Font("SansSerif", Font.PLAIN, 12));
            nilai.setFont(new Font("SansSerif", Font.BOLD, 26));
            nilai.setForeground(Tema.COKELAT);
            delta.setFont(new Font("SansSerif", Font.PLAIN, 11));
            JPanel isi = new JPanel(new GridLayout(3, 1, 0, 2));
            isi.setOpaque(false);
            isi.add(l);
            isi.add(nilai);
            isi.add(delta);
            add(isi, BorderLayout.CENTER);
        }

        void set(String teks, double kini, double lalu, String laluTeks) {
            nilai.setText(teks);
            if (kini == 0 && lalu == 0) {
                delta.setText("<html><span style='color:" + hex(Tema.ABU) + "'>Belum ada transaksi</span></html>");
                return;
            }
            String simbol, persen;
            Color warna;
            if (lalu == 0) {
                simbol = "\u25B2"; persen = "Baru"; warna = Tema.HIJAU;
            } else {
                double p = (kini - lalu) / lalu * 100;
                if (Math.abs(p) < 0.05) {
                    simbol = "\u25CF"; persen = "Sama"; warna = Tema.ABU;
                } else {
                    simbol = p > 0 ? "\u25B2" : "\u25BC";
                    persen = String.format(ID, "%.1f%%", Math.abs(p));
                    warna = p > 0 ? Tema.HIJAU : Tema.MERAH;
                }
            }
            delta.setText("<html><span style='color:" + hex(warna) + "'><b>" + simbol + " " + persen + "</b></span> "
                    + "<span style='color:" + hex(Tema.ABU) + "'>vs kemarin " + Tema.esc(laluTeks) + "</span></html>");
        }
    }

    // =====================================================
    //  GRAFIK GARIS: hari ini vs kemarin
    // =====================================================
    static class GrafikGaris extends JComponent {
        private static final int KIRI = 58, KANAN = 14, ATAS = 30, BAWAH = 26;
        private String[] label = new String[0];
        private double[] kini = new double[0], lalu = new double[0];
        private int hover = -1;

        GrafikGaris() {
            setPreferredSize(new Dimension(300, 180));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int n = label.length;
                    int lebar = getWidth() - KIRI - KANAN;
                    if (n < 2 || lebar <= 0 || e.getX() < KIRI - 10 || e.getX() > getWidth() - 4) {
                        hover = -1;
                    } else {
                        double rel = (e.getX() - KIRI) / (double) lebar;
                        hover = Math.max(0, Math.min(n - 1, (int) Math.round(rel * (n - 1))));
                    }
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
            };
            addMouseMotionListener(ma);
            addMouseListener(ma);
        }

        void setData(String[] label, double[] kini, double[] lalu) {
            this.label = label;
            this.kini = kini;
            this.lalu = lalu;
            this.hover = -1;
            repaint();
        }

        private double xAt(int i, int n) {
            return KIRI + (getWidth() - KIRI - KANAN) * i / (n - 1.0);
        }

        private static double langkah(double maks) {
            double mentah = maks / 4;
            double mag = Math.pow(10, Math.floor(Math.log10(mentah)));
            double n = mentah / mag;
            return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * mag;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            int n = label.length;
            double maks = 0;
            for (int i = 0; i < n; i++) {
                if (!Double.isNaN(kini[i])) maks = Math.max(maks, kini[i]);
                maks = Math.max(maks, lalu[i]);
            }
            if (n < 2 || maks <= 0) {
                kosong(g2, w, h, "Belum ada data penjualan");
                g2.dispose();
                return;
            }
            double step = langkah(maks);
            double puncak = step * Math.ceil(maks / step);
            int bawah = h - BAWAH;

            // garis bantu + label sumbu Y
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            for (int k = 0; k * step <= puncak + 1e-6; k++) {
                double v = k * step;
                int y = (int) Math.round(bawah - v / puncak * (bawah - ATAS));
                g2.setColor(Tema.GARIS);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(KIRI, y, w - KANAN, y);
                g2.setColor(Tema.ABU);
                String s = v == 0 ? "0" : ringkas(v);
                g2.drawString(s, KIRI - 8 - fm.stringWidth(s), y + 4);
            }
            // label sumbu X
            int lompat = n > 14 ? 2 : 1;
            g2.setColor(Tema.ABU);
            for (int i = 0; i < n; i += lompat) {
                int x = (int) xAt(i, n);
                g2.drawString(label[i], x - fm.stringWidth(label[i]) / 2, h - 8);
            }

            // kemarin (putus-putus)
            g2.setColor(Tema.ABU);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f));
            GeneralPath pl = new GeneralPath();
            for (int i = 0; i < n; i++) {
                double x = xAt(i, n), y = bawah - lalu[i] / puncak * (bawah - ATAS);
                if (i == 0) pl.moveTo(x, y); else pl.lineTo(x, y);
            }
            g2.draw(pl);

            // hari ini (garis + area)
            int akhir = -1;
            GeneralPath pk = new GeneralPath();
            for (int i = 0; i < n && !Double.isNaN(kini[i]); i++) {
                double x = xAt(i, n), y = bawah - kini[i] / puncak * (bawah - ATAS);
                if (i == 0) pk.moveTo(x, y); else pk.lineTo(x, y);
                akhir = i;
            }
            if (akhir >= 0) {
                GeneralPath area = new GeneralPath(pk);
                area.lineTo(xAt(akhir, n), bawah);
                area.lineTo(xAt(0, n), bawah);
                area.closePath();
                g2.setPaint(new GradientPaint(0, ATAS, alfa(Tema.ORANYE, 80), 0, bawah, alfa(Tema.ORANYE, 0)));
                g2.fill(area);
                g2.setPaint(Tema.ORANYE);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(pk);
                double xe = xAt(akhir, n), ye = bawah - kini[akhir] / puncak * (bawah - ATAS);
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(xe - 5, ye - 5, 10, 10));
                g2.setColor(Tema.ORANYE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new Ellipse2D.Double(xe - 5, ye - 5, 10, 10));
            }

            // legenda
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            int lx = KIRI, ly = 12;
            g2.setColor(Tema.ORANYE);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(lx, ly, lx + 18, ly);
            g2.setColor(Tema.COKELAT);
            g2.drawString("Hari ini", lx + 24, ly + 4);
            lx += 24 + fm.stringWidth("Hari ini") + 18;
            g2.setColor(Tema.ABU);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f));
            g2.drawLine(lx, ly, lx + 18, ly);
            g2.setColor(Tema.COKELAT);
            g2.drawString("Kemarin", lx + 24, ly + 4);

            // hover
            if (hover >= 0 && hover < n) {
                double x = xAt(hover, n);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(0xD9C9A8));
                g2.drawLine((int) x, ATAS, (int) x, bawah);
                double yl = bawah - lalu[hover] / puncak * (bawah - ATAS);
                g2.setColor(Tema.ABU);
                g2.fill(new Ellipse2D.Double(x - 4, yl - 4, 8, 8));
                if (!Double.isNaN(kini[hover])) {
                    double yk = bawah - kini[hover] / puncak * (bawah - ATAS);
                    g2.setColor(Tema.ORANYE);
                    g2.fill(new Ellipse2D.Double(x - 4, yk - 4, 8, 8));
                }
                String t0 = "Pukul " + label[hover] + ":00";
                String t1 = "Hari ini   " + (Double.isNaN(kini[hover]) ? "-" : Tema.rp(kini[hover]));
                String t2 = "Kemarin   " + Tema.rp(lalu[hover]);
                Font fB = new Font("SansSerif", Font.BOLD, 11), fN = new Font("SansSerif", Font.PLAIN, 11);
                int bw = Math.max(g2.getFontMetrics(fB).stringWidth(t0),
                        Math.max(g2.getFontMetrics(fN).stringWidth(t1), g2.getFontMetrics(fN).stringWidth(t2))) + 20;
                int bh = 62;
                int bx = (int) x + 12;
                if (bx + bw > w - 4) bx = (int) x - 12 - bw;
                int by = ATAS + 4;
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 10, 10));
                g2.setColor(new Color(0xD9C9A8));
                g2.draw(new RoundRectangle2D.Double(bx, by, bw, bh, 10, 10));
                g2.setColor(Tema.COKELAT);
                g2.setFont(fB);
                g2.drawString(t0, bx + 10, by + 18);
                g2.setFont(fN);
                g2.setColor(Tema.ORANYE);
                g2.drawString(t1, bx + 10, by + 35);
                g2.setColor(Tema.ABU);
                g2.drawString(t2, bx + 10, by + 51);
            }
            g2.dispose();
        }
    }

    // =====================================================
    //  GRAFIK BATANG: 7 hari terakhir
    // =====================================================
    static class GrafikBatang extends JComponent {
        private String[] label = new String[0];
        private double[] nilai = new double[0];

        GrafikBatang() {
            setPreferredSize(new Dimension(220, 180));
        }

        void setData(String[] label, double[] nilai) {
            this.label = label;
            this.nilai = nilai;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            int n = label.length;
            double maks = 0;
            for (double v : nilai) maks = Math.max(maks, v);
            if (n == 0 || maks <= 0) {
                kosong(g2, w, h, "Belum ada data penjualan");
                g2.dispose();
                return;
            }
            int atas = 22, bawah = h - 24, area = bawah - atas;
            int slot = w / n;
            int barW = Math.min(46, (int) (slot * 0.58));
            int geser = (w - slot * n) / 2;

            g2.setColor(Tema.GARIS);
            g2.drawLine(0, bawah, w, bawah);

            for (int i = 0; i < n; i++) {
                boolean ini = i == n - 1;
                int cx = geser + slot * i + slot / 2;
                double v = nilai[i];
                int bh = v <= 0 ? 0 : Math.max(4, (int) Math.round(v / maks * area));
                if (bh > 0) {
                    g2.setColor(ini ? Tema.KUNING : Tema.KUNING_MUDA);
                    g2.fill(new RoundRectangle2D.Double(cx - barW / 2.0, bawah - bh, barW, bh, 10, 10));
                    g2.setFont(new Font("SansSerif", ini ? Font.BOLD : Font.PLAIN, 10));
                    g2.setColor(ini ? Tema.COKELAT : Tema.ABU);
                    String s = ringkas(v);
                    g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, bawah - bh - 6);
                }
                g2.setFont(new Font("SansSerif", ini ? Font.BOLD : Font.PLAIN, 11));
                g2.setColor(ini ? Tema.COKELAT : Tema.ABU);
                g2.drawString(label[i], cx - g2.getFontMetrics().stringWidth(label[i]) / 2, h - 7);
            }
            g2.dispose();
        }
    }

    // =====================================================
    //  PERINGKAT MENU TERLARIS
    // =====================================================
    static class GrafikPeringkat extends JComponent {
        private String[] nama = new String[0];
        private double[] jumlah = new double[0];

        GrafikPeringkat() {
            setPreferredSize(new Dimension(300, 180));
        }

        void setData(String[] nama, double[] jumlah) {
            this.nama = nama;
            this.jumlah = jumlah;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            int n = Math.min(nama.length, 5);
            if (n == 0) {
                kosong(g2, w, h, "Belum ada penjualan hari ini");
                g2.dispose();
                return;
            }
            double maks = 0;
            for (int i = 0; i < n; i++) maks = Math.max(maks, jumlah[i]);
            if (maks <= 0) maks = 1;
            int rowH = Math.min(46, Math.max(34, h / n));
            Font fNama = new Font("SansSerif", Font.BOLD, 13);
            Font fQty = new Font("SansSerif", Font.PLAIN, 12);

            for (int i = 0; i < n; i++) {
                int y = i * rowH;
                if (y + rowH > h + 6) break;
                int bd = 24;
                g2.setColor(i == 0 ? Tema.KUNING : Tema.KUNING_MUDA);
                g2.fillOval(0, y + 3, bd, bd);
                g2.setColor(Tema.COKELAT);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                String no = String.valueOf(i + 1);
                FontMetrics fmNo = g2.getFontMetrics();
                g2.drawString(no, (bd - fmNo.stringWidth(no)) / 2, y + 3 + (bd - fmNo.getHeight()) / 2 + fmNo.getAscent());

                int x = bd + 12;
                String qty = (long) jumlah[i] + " terjual";
                g2.setFont(fQty);
                int qw = g2.getFontMetrics().stringWidth(qty);
                g2.setColor(Tema.ABU);
                g2.drawString(qty, w - qw, y + 15);

                g2.setFont(fNama);
                g2.setColor(Tema.COKELAT);
                g2.drawString(potong(nama[i], g2.getFontMetrics(), w - x - qw - 12), x, y + 15);

                int lebarTrack = w - x - 2;
                g2.setColor(Tema.GARIS);
                g2.fill(new RoundRectangle2D.Double(x, y + 22, lebarTrack, 8, 8, 8));
                int isi = Math.max(8, (int) Math.round(lebarTrack * jumlah[i] / maks));
                g2.setColor(i == 0 ? Tema.ORANYE : Tema.KUNING);
                g2.fill(new RoundRectangle2D.Double(x, y + 22, isi, 8, 8, 8));
            }
            g2.dispose();
        }
    }

    // =====================================================
    //  DONUT: cash vs QRIS
    // =====================================================
    static class Donut extends JComponent {
        private double cash, qris;

        Donut() {
            setPreferredSize(new Dimension(240, 180));
        }

        void setData(double cash, double qris) {
            this.cash = cash;
            this.qris = qris;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            double total = cash + qris;
            int d = Math.max(70, Math.min(h - 12, (int) (w * 0.46)));
            int x0 = 6, y0 = (h - d) / 2;
            float sw = Math.max(12f, d * 0.17f);
            double ax = x0 + sw / 2, ay = y0 + sw / 2, ad = d - sw;

            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            if (total <= 0) {
                g2.setColor(Tema.GARIS);
                g2.draw(new Ellipse2D.Double(ax, ay, ad, ad));
            } else {
                double sudutCash = 360 * cash / total;
                double sudutQris = 360 * qris / total;
                if (cash > 0) {
                    g2.setColor(Tema.KUNING);
                    g2.draw(new Arc2D.Double(ax, ay, ad, ad, 90, -sudutCash, Arc2D.OPEN));
                }
                if (qris > 0) {
                    g2.setColor(Tema.COKELAT);
                    g2.draw(new Arc2D.Double(ax, ay, ad, ad, 90 - sudutCash, -sudutQris, Arc2D.OPEN));
                }
            }

            // teks tengah
            int cx = x0 + d / 2, cy = y0 + d / 2;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(Tema.ABU);
            String t1 = total > 0 ? "Total" : "Belum ada";
            g2.drawString(t1, cx - g2.getFontMetrics().stringWidth(t1) / 2, cy - 2);
            if (total > 0) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                g2.setColor(Tema.COKELAT);
                String t2 = ringkas(total);
                g2.drawString(t2, cx - g2.getFontMetrics().stringWidth(t2) / 2, cy + 16);
            }

            // legenda
            int lx = x0 + d + 22;
            int ly = h / 2 - 46;
            legenda(g2, lx, ly, Tema.KUNING, "Cash", cash, total);
            legenda(g2, lx, ly + 54, Tema.COKELAT, "QRIS", qris, total);
            g2.dispose();
        }

        private void legenda(Graphics2D g2, int x, int y, Color warna, String nama, double nilai, double total) {
            g2.setColor(warna);
            g2.fill(new RoundRectangle2D.Double(x, y + 3, 12, 12, 4, 4));
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(Tema.COKELAT);
            g2.drawString(nama, x + 20, y + 14);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Tema.ABU);
            String persen = total > 0 ? Math.round(nilai / total * 100) + "%" : "0%";
            g2.drawString(Tema.rp(nilai) + "  \u2022  " + persen, x + 20, y + 32);
        }
    }
}