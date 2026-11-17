package com.mycompany.mavenproject1;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Logo aplikasi. Disimpan di database (seperti gambar menu) supaya bisa diganti dari Setting.
 * Kalau belum pernah diganti, dipakai logo bawaan dari {@link LogoData}.
 */
public final class LogoToko {

    private static final int LEBAR_MAKS = 900;
    private static final int TINGGI_MAKS = 450;

    private static BufferedImage cache;
    private static boolean sudahDimuat;

    private LogoToko() { }

    /** Logo yang sedang dipakai: dari database kalau ada, kalau tidak logo bawaan. Null kalau semuanya gagal. */
    public static synchronized BufferedImage muat() {
        if (!sudahDimuat) {
            BufferedImage img = dariDatabase();
            cache = img != null ? img : bawaan();
            sudahDimuat = true;
        }
        return cache;
    }

    /** Logo bawaan yang tertanam di kode. */
    public static BufferedImage bawaan() {
        try {
            return ImageIO.read(new ByteArrayInputStream(LogoData.bytes()));
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Menyimpan logo baru (byte PNG) ke database. */
    public static synchronized void simpan(byte[] png) throws SQLException {
        try (Connection c = Koneksi.get()) {
            buatTabel(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO pengaturan_gambar (kunci, data) VALUES ('logo', ?) "
                  + "ON DUPLICATE KEY UPDATE data = VALUES(data)")) {
                ps.setBytes(1, png);
                ps.executeUpdate();
            }
        }
        sudahDimuat = false;
    }

    /** Menghapus logo kustom, sehingga kembali ke logo bawaan. */
    public static synchronized void hapus() throws SQLException {
        try (Connection c = Koneksi.get()) {
            buatTabel(c);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM pengaturan_gambar WHERE kunci='logo'")) {
                ps.executeUpdate();
            }
        }
        sudahDimuat = false;
    }

    /** Mengubah gambar apa pun jadi PNG yang ukurannya wajar (maks 900 x 450 px, proporsi tetap). */
    public static byte[] keBytes(BufferedImage sumber) throws IOException {
        if (sumber == null) throw new IOException("Gambar tidak dapat dibaca.");
        BufferedImage kecil = skala(sumber, LEBAR_MAKS, TINGGI_MAKS);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(kecil, "png", out)) throw new IOException("Gagal mengubah gambar ke PNG.");
        return out.toByteArray();
    }

    /** Memperkecil gambar muat dalam kotak lebarMaks x tinggiMaks tanpa memperbesar dan tanpa merusak proporsi. */
    public static BufferedImage skala(BufferedImage src, int lebarMaks, int tinggiMaks) {
        double s = Math.min(1.0, Math.min((double) lebarMaks / src.getWidth(), (double) tinggiMaks / src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));

        BufferedImage cur = ubah(src, src.getWidth(), src.getHeight());
        int cw = cur.getWidth(), ch = cur.getHeight();
        // perkecil bertahap (separuh-separuh) supaya hasilnya halus
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = ubah(cur, cw, ch);
        }
        if (cw != w || ch != h) cur = ubah(cur, w, h);
        return cur;
    }

    private static BufferedImage ubah(BufferedImage src, int w, int h) {
        int tipe = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(w, h, tipe);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage dariDatabase() {
        try (Connection c = Koneksi.get()) {
            buatTabel(c);
            try (PreparedStatement ps = c.prepareStatement("SELECT data FROM pengaturan_gambar WHERE kunci='logo'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes(1);
                    if (bytes != null && bytes.length > 0) return ImageIO.read(new ByteArrayInputStream(bytes));
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace(); // diam-diam: pakai logo bawaan
        }
        return null;
    }

    private static void buatTabel(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS pengaturan_gambar ("
                    + "kunci VARCHAR(50) NOT NULL PRIMARY KEY, "
                    + "data MEDIUMBLOB NOT NULL)");
        }
    }
}
    

