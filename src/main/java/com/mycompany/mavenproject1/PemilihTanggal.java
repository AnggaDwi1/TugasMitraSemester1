package com.mycompany.mavenproject1;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pemilih tanggal ringan berbasis Swing, tanpa dependensi tambahan. */
public class PemilihTanggal extends JPanel {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("id", "ID"));
    private LocalDate tanggal;
    private final JButton tombol = new JButton();

    public PemilihTanggal(LocalDate tanggalAwal) {
        super(new BorderLayout());
        setOpaque(false);
        setTanggal(tanggalAwal);
        tombol.setHorizontalAlignment(SwingConstants.LEFT);
        tombol.setFocusPainted(false);
        tombol.setBackground(Color.WHITE);
        tombol.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.GARIS), BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.addActionListener(e -> bukaKalender());
        add(tombol, BorderLayout.CENTER);
    }

    public LocalDate getTanggal() { return tanggal; }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
        tombol.setText(tanggal.format(FORMAT) + "  ▾");
    }

    private void bukaKalender() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Pilih tanggal", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        JPanel isi = new JPanel(new BorderLayout(8, 8));
        isi.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        isi.setBackground(Color.WHITE);
        JPanel kalender = new JPanel(new BorderLayout(4, 8));
        kalender.setOpaque(false);
        JPanel judul = new JPanel(new BorderLayout(8, 0));
        judul.setOpaque(false);
        JButton sebelumnya = new JButton("‹");
        JButton berikutnya = new JButton("›");
        JLabel bulan = new JLabel("", SwingConstants.CENTER);
        bulan.setFont(bulan.getFont().deriveFont(Font.BOLD, 14f));
        judul.add(sebelumnya, BorderLayout.WEST); judul.add(bulan, BorderLayout.CENTER); judul.add(berikutnya, BorderLayout.EAST);
        JPanel hari = new JPanel(new GridLayout(0, 7, 3, 3));
        hari.setOpaque(false);
        kalender.add(judul, BorderLayout.NORTH); kalender.add(hari, BorderLayout.CENTER);
        isi.add(kalender, BorderLayout.CENTER);

        final YearMonth[] tampil = {YearMonth.from(tanggal)};
        Runnable gambar = () -> {
            bulan.setText(tampil[0].getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("id", "ID")) + " " + tampil[0].getYear());
            hari.removeAll();
            String[] namaHari = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            for (String nama : namaHari) {
                JLabel label = new JLabel(nama, SwingConstants.CENTER);
                label.setForeground(Tema.ABU); hari.add(label);
            }
            int offset = tampil[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) hari.add(new JLabel(""));
            for (int d = 1; d <= tampil[0].lengthOfMonth(); d++) {
                LocalDate pilih = tampil[0].atDay(d);
                JButton tanggalBtn = new JButton(String.valueOf(d));
                tanggalBtn.setFocusPainted(false);
                tanggalBtn.setMargin(new Insets(5, 8, 5, 8));
                if (pilih.equals(tanggal)) tanggalBtn.setBackground(Tema.KUNING);
                tanggalBtn.addActionListener(e -> { setTanggal(pilih); dialog.dispose(); });
                hari.add(tanggalBtn);
            }
            hari.revalidate(); hari.repaint();
        };
        sebelumnya.addActionListener(e -> { tampil[0] = tampil[0].minusMonths(1); gambar.run(); });
        berikutnya.addActionListener(e -> { tampil[0] = tampil[0].plusMonths(1); gambar.run(); });
        gambar.run();
        dialog.setContentPane(isi);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
