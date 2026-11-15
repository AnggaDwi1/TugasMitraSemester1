package com.mycompany.mavenproject1;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Halaman Menu: HANYA tampilan katalog (kartu bergambar, cari, filter kategori).
 * Tambah/ubah/hapus menu, ganti gambar, dan tambah stok ada di Setting > Kelola Menu.
 */
public class MenuPanel extends JPanel {

    private static final int LEBAR_KARTU = 240;
    private static final int STOK_MENIPIS = 5;

    /** Satu menu beserta gambar mini yang sudah siap digambar. */
    static class Item {
        int id;
        String nama, kategori, status, deskripsi;
        double harga;
        int stok;
        BufferedImage gambar;

        boolean habis() { return "habis".equalsIgnoreCase(status) || stok <= 0; }
        boolean menipis() { return !habis() && stok <= STOK_MENIPIS; }
    }

    private final List<Item> semua = new ArrayList<>();
    private final JPanel grid = new JPanel();
    private final JScrollPane scroll;
    private final JTextField txtCari = new JTextField();
    private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel lblRingkas = new JLabel(" ");

    private String kategoriAktif = null; // null = semua kategori
    private int kolom = 4;
    private int versiMuat = 0;

    public MenuPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Tema.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // ----- header: judul + ringkasan -----
        JLabel hint = new JLabel("Untuk menambah, mengubah, atau mengisi stok menu, buka Setting › Kelola Menu");
        hint.setForeground(Tema.ABU);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel judul = new JPanel(new GridLayout(0, 1, 0, 2));
        judul.setOpaque(false);
        judul.add(Tema.judul("Daftar Menu"));
        judul.add(hint);

        lblRingkas.setForeground(Tema.ABU);
        lblRingkas.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel baris1 = new JPanel(new BorderLayout());
        baris1.setOpaque(false);
        baris1.add(judul, BorderLayout.WEST);
        baris1.add(lblRingkas, BorderLayout.EAST);

        // ----- filter: chip kategori + kolom cari -----
        chipPanel.setOpaque(false);
        txtCari.putClientProperty("JTextField.placeholderText", "Cari menu...");
        txtCari.setPreferredSize(new Dimension(240, 34));
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tampilkan(); }
            public void removeUpdate(DocumentEvent e) { tampilkan(); }
            public void changedUpdate(DocumentEvent e) { tampilkan(); }
        });
        JPanel baris2 = new JPanel(new BorderLayout(12, 0));
        baris2.setOpaque(false);
        baris2.add(chipPanel, BorderLayout.CENTER);
        baris2.add(txtCari, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(baris1, BorderLayout.NORTH);
        header.add(baris2, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ----- grid kartu -----
        grid.setOpaque(false);
        JPanel pembungkus = new JPanel(new BorderLayout());
        pembungkus.setOpaque(false);
        pembungkus.add(grid, BorderLayout.NORTH);
        scroll = new JScrollPane(pembungkus);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int baru = Math.max(1, scroll.getViewport().getWidth() / LEBAR_KARTU);
                if (baru != kolom) {
                    kolom = baru;
                    if (!semua.isEmpty()) tampilkan();
                }
            }
        });
        add(scroll, BorderLayout.CENTER);

        segarkan();
    }

    // =====================================================
    //  MUAT DATA (di thread latar supaya aplikasi tidak macet)
    // =====================================================
    public void segarkan() {
        final int versi = ++versiMuat;
        pesan("Memuat menu...");
        new SwingWorker<List<Item>, Void>() {
            @Override protected List<Item> doInBackground() throws Exception {
                List<Item> hasil = new ArrayList<>();
                String sql = "SELECT m.id_menu, m.nama_menu, COALESCE(k.nama_kategori,'Lainnya'), m.harga, "
                        + "m.status, COALESCE(m.deskripsi,''), m.stok, m.gambar "
                        + "FROM menu m LEFT JOIN kategori_menu k ON k.id_kategori=m.id_kategori "
                        + "ORDER BY k.nama_kategori, m.nama_menu";
                try (Connection c = Koneksi.get();
                     Statement s = c.createStatement();
                     ResultSet rs = s.executeQuery(sql)) {
                    while (rs.next()) {
                        Item it = new Item();
                        it.id = rs.getInt(1);
                        it.nama = rs.getString(2);
                        it.kategori = rs.getString(3);
                        it.harga = rs.getDouble(4);
                        it.status = rs.getString(5);
                        it.deskripsi = rs.getString(6);
                        it.stok = rs.getInt(7);
                        it.gambar = mini(rs.getBytes(8));
                        hasil.add(it);
                    }
                }
                return hasil;
            }

            @Override protected void done() {
                if (versi != versiMuat) return; // ada pemuatan yang lebih baru
                try {
                    semua.clear();
                    semua.addAll(get());
                    if (kategoriAktif != null && semua.stream().noneMatch(i -> kategoriAktif.equals(i.kategori))) {
                        kategoriAktif = null;
                    }
                    bangunChip();
                    ringkasan();
                    tampilkan();
                } catch (Exception e) {
                    Throwable t = e.getCause() != null ? e.getCause() : e;
                    pesan("Gagal memuat menu");
                    Db.error(t instanceof Exception ? (Exception) t : e);
                }
            }
        }.execute();
    }

    /** Decode gambar dari database lalu kecilkan supaya hemat memori. */
    private static BufferedImage mini(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null || src.getWidth() <= 480) return src;
            int w = 480;
            int h = Math.max(1, Math.round(src.getHeight() * 480f / src.getWidth()));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    // =====================================================
    //  TAMPILAN
    // =====================================================
    private void ringkasan() {
        long habis = semua.stream().filter(Item::habis).count();
        lblRingkas.setText(semua.size() + " menu   •   " + (semua.size() - habis) + " tersedia   •   " + habis + " habis");
    }

    private void bangunChip() {
        chipPanel.removeAll();
        Set<String> kategori = new LinkedHashSet<>();
        for (Item it : semua) kategori.add(it.kategori);
        ButtonGroup grup = new ButtonGroup();
        chipPanel.add(chip("Semua", null, grup));
        for (String k : kategori) chipPanel.add(chip(k, k, grup));
        chipPanel.revalidate();
        chipPanel.repaint();
    }

    private JToggleButton chip(String teks, String nilai, ButtonGroup grup) {
        boolean aktif = (nilai == null && kategoriAktif == null) || (nilai != null && nilai.equals(kategoriAktif));
        JToggleButton b = new JToggleButton(teks, aktif);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(Color.WHITE);
        b.setForeground(Tema.COKELAT);
        b.setPreferredSize(new Dimension(Math.max(64, b.getPreferredSize().width + 8), 34));
        b.addActionListener(e -> { kategoriAktif = nilai; tampilkan(); });
        grup.add(b);
        return b;
    }

    private void pesan(String teks) {
        grid.removeAll();
        grid.setLayout(new BorderLayout());
        JLabel l = new JLabel(teks, SwingConstants.CENTER);
        l.setForeground(Tema.ABU);
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        l.setBorder(BorderFactory.createEmptyBorder(70, 0, 70, 0));
        grid.add(l, BorderLayout.CENTER);
        grid.revalidate();
        grid.repaint();
    }

    private void tampilkan() {
        String q = txtCari.getText().trim().toLowerCase();
        List<Item> hasil = new ArrayList<>();
        for (Item it : semua) {
            if (kategoriAktif != null && !kategoriAktif.equals(it.kategori)) continue;
            if (!q.isEmpty() && !(it.nama.toLowerCase().contains(q)
                    || it.deskripsi.toLowerCase().contains(q)
                    || it.kategori.toLowerCase().contains(q))) continue;
            hasil.add(it);
        }
        if (hasil.isEmpty()) {
            pesan(semua.isEmpty() ? "Belum ada menu. Tambahkan lewat Setting › Kelola Menu." : "Menu tidak ditemukan");
            return;
        }
        grid.removeAll();
        grid.setLayout(new GridLayout(0, Math.max(1, kolom), 14, 14));
        for (Item it : hasil) grid.add(kartu(it));
        grid.revalidate();
        grid.repaint();
    }

    private Tema.Kartu kartu(Item it) {
        Tema.Kartu k = new Tema.Kartu();
        k.setLayout(new BorderLayout(0, 10));
        k.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        k.setPreferredSize(new Dimension(LEBAR_KARTU - 14, 292));
        if (it.deskripsi != null && !it.deskripsi.isBlank()) k.setToolTipText(it.deskripsi);

        GambarKartu foto = new GambarKartu(it.gambar, it.habis());
        foto.setPreferredSize(new Dimension(100, 150));
        k.add(foto, BorderLayout.NORTH);

        JLabel nama = new JLabel(it.nama);
        nama.setFont(new Font("SansSerif", Font.BOLD, 15));
        nama.setForeground(Tema.COKELAT);

        JLabel kat = new JLabel(it.kategori.toUpperCase());
        kat.setFont(new Font("SansSerif", Font.BOLD, 10));
        kat.setForeground(Tema.ABU);

        JLabel desk = new JLabel(potong(it.deskripsi, 34));
        desk.setFont(new Font("SansSerif", Font.PLAIN, 11));
        desk.setForeground(Tema.ABU);

        JLabel harga = new JLabel(Tema.rp(it.harga));
        harga.setFont(new Font("SansSerif", Font.BOLD, 17));
        harga.setForeground(it.habis() ? Tema.ABU : Tema.COKELAT);

        Lencana badge = it.habis() ? new Lencana("Habis", Tema.MERAH)
                : it.menipis() ? new Lencana("Menipis", Tema.ORANYE)
                : new Lencana("Tersedia", Tema.HIJAU);

        JLabel stok = new JLabel("Stok: " + Math.max(0, it.stok));
        stok.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stok.setForeground(it.menipis() ? Tema.ORANYE : Tema.ABU);

        JPanel barisHarga = new JPanel(new BorderLayout(6, 0));
        barisHarga.setOpaque(false);
        barisHarga.add(harga, BorderLayout.CENTER);
        barisHarga.add(badge, BorderLayout.EAST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{kat, nama, desk, barisHarga, stok}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        info.add(kat);
        info.add(Box.createVerticalStrut(2));
        info.add(nama);
        info.add(Box.createVerticalStrut(2));
        info.add(desk);
        info.add(Box.createVerticalGlue());
        info.add(barisHarga);
        info.add(Box.createVerticalStrut(4));
        info.add(stok);
        k.add(info, BorderLayout.CENTER);
        return k;
    }

    private static String potong(String s, int maks) {
        if (s == null || s.isBlank()) return " ";
        s = s.trim().replaceAll("\\s+", " ");
        return s.length() <= maks ? s : s.substring(0, maks - 1) + "…";
    }

    // =====================================================
    //  KOMPONEN KECIL
    // =====================================================

    /** Foto menu: dipotong rapi (cover) dengan sudut atas membulat. */
    static class GambarKartu extends JComponent {
        private final BufferedImage img;
        private final boolean redup;

        GambarKartu(BufferedImage img, boolean redup) {
            this.img = img;
            this.redup = redup;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setClip(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            if (img == null) {
                g.setColor(Tema.KUNING_MUDA);
                g.fillRect(0, 0, w, h);
                g.setColor(Tema.COKELAT);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                String t = "Belum ada gambar";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(t, (w - fm.stringWidth(t)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            } else {
                double s = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
                int dw = (int) Math.ceil(img.getWidth() * s);
                int dh = (int) Math.ceil(img.getHeight() * s);
                g.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            }
            if (redup) {
                g.setColor(new Color(255, 248, 232, 170));
                g.fillRect(0, 0, w, h);
            }
            g.dispose();
        }
    }

    /** Label status berbentuk kapsul. */
    static class Lencana extends JLabel {
        private final Color warna;

        Lencana(String teks, Color warna) {
            super(teks, SwingConstants.CENTER);
            this.warna = warna;
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(warna);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}