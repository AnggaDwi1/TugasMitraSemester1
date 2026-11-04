package com.mycompany.mavenproject1;

import java.util.prefs.Preferences;

/** Menyimpan pengaturan toko di komputer (tidak perlu tabel baru di database). */
public class Pengaturan {

    private static final Preferences P = Preferences.userNodeForPackage(Pengaturan.class);

    // jumlah karakter per baris untuk ukuran huruf: 0 = kecil, 1 = sedang, 2 = besar
    private static final int[] LEBAR_80MM = {48, 40, 34};
    private static final int[] LEBAR_58MM = {32, 27, 23};

    public static String get(String kunci, String bawaan) {
        return P.get(kunci, bawaan);
    }

    public static void put(String kunci, String nilai) {
        P.put(kunci, nilai);
    }

    /** Lebar kertas dalam mm: 80 atau 58. */
    public static int kertasMm() {
        return "58".equals(P.get("kertas", "80")) ? 58 : 80;
    }

    /** 0 = kecil, 1 = sedang, 2 = besar. */
    public static int ukuran() {
        try {
            int u = Integer.parseInt(P.get("ukuran", "1"));
            return Math.max(0, Math.min(2, u));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Jumlah karakter per baris. Makin sedikit karakter, makin besar hurufnya. */
    public static int lebar() {
        return (kertasMm() == 58 ? LEBAR_58MM : LEBAR_80MM)[ukuran()];
    }
}