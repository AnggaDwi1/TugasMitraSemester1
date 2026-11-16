package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Tema {

    // ===== Palet: Yellow & Cream (Warm Food Theme) =====
    public static final Color KUNING      = new Color(0xFFC928); // tombol & highlight
    public static final Color KUNING_MUDA = new Color(0xFFE9A8); // menu aktif, thumbnail
    public static final Color CREAM       = new Color(0xFFF8E8); // background
    public static final Color COKELAT     = new Color(0x8B451F); // teks & aksen

    public static final Color BG = CREAM;
    public static final Color ABU = new Color(0x9A7B63);   // teks sekunder
    public static final Color GARIS = new Color(0xF0E3C8); // border kartu
    public static final Color HIJAU = new Color(0x1B9E6B);
    public static final Color MERAH = new Color(0xD9534F);
    public static final Color ORANYE = new Color(0xE8630A);

    /** Alias lama supaya file lain tetap jalan tanpa diubah. */
    public static final Color BIRU = KUNING;

    public static String num(double v) {
        return NumberFormat.getInstance(Locale.forLanguageTag("id-ID")).format(Math.round(v));
    }

    public static String rp(double v) {
        return "Rp " + num(v);
    }

    /** Escape teks untuk dipakai di dalam JLabel HTML. */
    public static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================
    //  FUNGSI BANTU TAMPILAN
    // =====================================================

    /** Campur dua warna. t = 0 hasilnya a, t = 1 hasilnya b. */
    public static Color campur(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Memberi jarak antar huruf (tracking), cocok untuk label kecil huruf kapital. */
    public static Font renggang(Font f, float tracking) {
        Map<TextAttribute, Object> m = new HashMap<>();
        m.put(TextAttribute.TRACKING, tracking);
        return f.deriveFont(m);
    }

    /**
     * Menggambar ikon garis sederhana (tanpa file gambar).
     * Nama ikon: dashboard, menu, transaksi, laporan, setting, keluar.
     * Ikon dirancang di kotak 20 x 20 lalu diskalakan ke ukuran s.
     */
    public static void gambarIkon(Graphics2D g, String nama, double x, double y, double s, Color warna) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(s / 20.0, s / 20.0);
        g2.setColor(warna);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (nama) {
            case "dashboard":
                g2.draw(new RoundRectangle2D.Double(2.5, 2.5, 6, 6, 2.4, 2.4));
                g2.draw(new RoundRectangle2D.Double(11.5, 2.5, 6, 6, 2.4, 2.4));
                g2.draw(new RoundRectangle2D.Double(2.5, 11.5, 6, 6, 2.4, 2.4));
                g2.draw(new RoundRectangle2D.Double(11.5, 11.5, 6, 6, 2.4, 2.4));
                break;
            case "menu": // mangkuk dengan uap
                g2.draw(new Arc2D.Double(2, 4, 16, 14, 180, 180, Arc2D.OPEN));
                g2.drawLine(2, 11, 18, 11);
                g2.drawLine(7, 3, 7, 7);
                g2.drawLine(13, 3, 13, 7);
                break;
            case "transaksi": { // struk
                Path2D p = new Path2D.Double();
                p.moveTo(4, 2);
                p.lineTo(16, 2);
                p.lineTo(16, 17.5);
                p.lineTo(14, 16);
                p.lineTo(12, 17.5);
                p.lineTo(10, 16);
                p.lineTo(8, 17.5);
                p.lineTo(6, 16);
                p.lineTo(4, 17.5);
                p.closePath();
                g2.draw(p);
                g2.drawLine(7, 6, 13, 6);
                g2.draw(new java.awt.geom.Line2D.Double(7, 9.5, 13, 9.5));
                break;
            }
            case "laporan": // grafik batang
                g2.fill(new RoundRectangle2D.Double(3.5, 10.5, 3.6, 7, 1.4, 1.4));
                g2.fill(new RoundRectangle2D.Double(8.2, 4, 3.6, 13.5, 1.4, 1.4));
                g2.fill(new RoundRectangle2D.Double(12.9, 7.5, 3.6, 10, 1.4, 1.4));
                break;
            case "setting": { // roda gigi
                g2.draw(new Ellipse2D.Double(4.8, 4.8, 10.4, 10.4));
                g2.draw(new Ellipse2D.Double(7.9, 7.9, 4.2, 4.2));
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    g2.draw(new java.awt.geom.Line2D.Double(
                            10 + Math.cos(a) * 6.3, 10 + Math.sin(a) * 6.3,
                            10 + Math.cos(a) * 8.6, 10 + Math.sin(a) * 8.6));
                }
                break;
            }
            case "keluar": { // pintu + panah
                Path2D p = new Path2D.Double();
                p.moveTo(8.5, 3);
                p.lineTo(4, 3);
                p.lineTo(4, 17);
                p.lineTo(8.5, 17);
                g2.draw(p);
                g2.drawLine(8, 10, 17, 10);
                Path2D h = new Path2D.Double();
                h.moveTo(13.5, 6.5);
                h.lineTo(17, 10);
                h.lineTo(13.5, 13.5);
                g2.draw(h);
                break;
            }
            default:
                break;
        }
        g2.dispose();
    }

    /** Panel sudut membulat + border tipis (dipakai untuk semua "kartu"). */
    public static class Kartu extends JPanel {
        private Color warna = Color.WHITE;

        public Kartu() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        }

        public Kartu warna(Color c) {
            warna = c;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(warna);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(GARIS);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Tombol menu sidebar. Kapsul kuning muda dengan penanda kecil kalau aktif,
     * dan sorotan lembut yang memudar halus kalau di-hover. Ikon opsional di sebelah kiri.
     */
    public static class NavTombol extends JButton {
        private static final int IKON_X = 22;
        private static final int IKON_UKURAN = 20;

        private final String ikon;
        private boolean aktif;
        private boolean masuk;
        private float hover; // 0 = tidak di-hover, 1 = penuh
        private final Timer anim;

        public NavTombol(String teks) {
            this(teks, null);
        }

        public NavTombol(String teks, String ikon) {
            super(teks);
            this.ikon = ikon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(12, ikon != null ? 54 : 18, 12, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            anim = new Timer(15, e -> {
                float target = masuk ? 1f : 0f;
                if (hover < target) hover = Math.min(target, hover + 0.2f);
                else if (hover > target) hover = Math.max(target, hover - 0.2f);
                repaint();
                if (hover == target) ((Timer) e.getSource()).stop();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { masuk = true; anim.start(); }
                @Override public void mouseExited(MouseEvent e) { masuk = false; anim.start(); }
            });
            setAktif(false);
        }

        public void setAktif(boolean a) {
            aktif = a;
            setForeground(a ? COKELAT : ABU);
            setFont(new Font("SansSerif", a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (aktif) {
                g2.setColor(KUNING_MUDA);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(COKELAT);
                g2.fillRoundRect(7, (h - 18) / 2, 4, 18, 4, 4); // penanda menu aktif
            } else if (hover > 0f) {
                g2.setColor(new Color(KUNING_MUDA.getRed(), KUNING_MUDA.getGreen(), KUNING_MUDA.getBlue(), Math.round(hover * 105)));
                g2.fillRoundRect(0, 0, w, h, 16, 16);
            }

            if (ikon != null) {
                Color warna = aktif ? COKELAT : campur(ABU, COKELAT, hover);
                gambarIkon(g2, ikon, IKON_X, (h - IKON_UKURAN) / 2.0, IKON_UKURAN, warna);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Tombol logout: teks merah, sorotan merah muda tipis saat di-hover. */
    public static class TombolKeluar extends JButton {
        private boolean hover;

        public TombolKeluar(String teks) {
            super(teks);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(12, 54, 12, 16));
            setForeground(MERAH);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(new Color(0xFDECEA));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
            gambarIkon(g2, "keluar", 22, (getHeight() - 20) / 2.0, 20, MERAH);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Label kecil berbentuk kapsul, misalnya untuk menandai peran (Owner / Karyawan). */
    public static class Chip extends JLabel {
        private final Color latar;

        public Chip(String teks, Color latar, Color huruf) {
            super(teks);
            this.latar = latar;
            setForeground(huruf);
            setFont(new Font("SansSerif", Font.BOLD, 10));
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(latar);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Lingkaran kuning berisi inisial (foto profil kasir). */
    public static JComponent avatar(String inisial, int d) {
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(KUNING);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(COKELAT);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(inisial, (getWidth() - fm.stringWidth(inisial)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(d, d));
        return c;
    }

    /** Lingkaran berisi teks dengan cincin tipis di luarnya (untuk logo dan profil). */
    public static JComponent avatarCincin(String teks, int d, Color isi, Color huruf) {
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(KUNING_MUDA);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, w - 3, h - 3);
                g2.setColor(isi);
                g2.fillOval(4, 4, w - 8, h - 8);
                g2.setColor(huruf);
                g2.setFont(new Font("SansSerif", Font.BOLD, Math.round(d * 0.32f)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(teks, (w - fm.stringWidth(teks)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(d, d));
        return c;
    }

    public static Kartu kartuStat(String label, JLabel nilai) {
        Kartu k = new Kartu();
        k.setLayout(new GridLayout(2, 1));
        JLabel l = new JLabel(label);
        l.setForeground(ABU);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nilai.setFont(new Font("SansSerif", Font.BOLD, 22));
        nilai.setForeground(COKELAT);
        k.add(l);
        k.add(nilai);
        return k;
    }

    public static JLabel judul(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("SansSerif", Font.BOLD, 22));
        l.setForeground(COKELAT);
        return l;
    }

    /** Tombol berwarna. Di atas kuning, teks dibuat cokelat supaya tetap terbaca. */
    public static JButton tombol(String teks, Color bg) {
        JButton b = new JButton(teks);
        b.setBackground(bg);
        b.setForeground(KUNING.equals(bg) ? COKELAT : Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static void gayaTabel(JTable t) {
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        t.setForeground(COKELAT);
        t.setGridColor(GARIS);
        t.setSelectionBackground(KUNING_MUDA);
        t.setSelectionForeground(COKELAT);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(CREAM);
        t.getTableHeader().setForeground(COKELAT);
    }
}