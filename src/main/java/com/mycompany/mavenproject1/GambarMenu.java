package com.mycompany.mavenproject1;

import com.luciad.imageio.webp.WebPWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/** Mengubah gambar menu ke WebP terkompresi dengan ukuran yang sesuai untuk kartu menu. */
final class GambarMenu {
    private static final int BATAS_SISI = 1200;
    private static final float KUALITAS = 0.84f;

    private GambarMenu() { }

    static byte[] keWebp(BufferedImage sumber) throws IOException {
        if (sumber == null) throw new IOException("Gambar tidak dapat dibaca.");

        double skala = Math.min(1.0, (double) BATAS_SISI / Math.max(sumber.getWidth(), sumber.getHeight()));
        int lebar = Math.max(1, (int) Math.round(sumber.getWidth() * skala));
        int tinggi = Math.max(1, (int) Math.round(sumber.getHeight() * skala));
        int tipe = sumber.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage hasil = new BufferedImage(lebar, tinggi, tipe);
        Graphics2D g = hasil.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(sumber, 0, 0, lebar, tinggi, null);
        g.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) throw new IOException("Konverter WebP belum tersedia. Muat ulang dependensi Maven.");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            WebPWriteParam param = new WebPWriteParam(writer.getLocale());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
            param.setCompressionQuality(KUALITAS);
            writer.write(null, new IIOImage(hasil, null, null), param);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
