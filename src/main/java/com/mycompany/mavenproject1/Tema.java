package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

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

    /** Tombol menu sidebar: kapsul kuning muda kalau aktif, cream tipis kalau di-hover. */
    public static class NavTombol extends JButton {
        private boolean aktif;
        private boolean hover;

        public NavTombol(String teks) {
            super(teks);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAktif(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public void setAktif(boolean a) {
            aktif = a;
            setForeground(a ? COKELAT : ABU);
            setFont(new Font("SansSerif", a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (aktif || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(aktif ? KUNING_MUDA : CREAM);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
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