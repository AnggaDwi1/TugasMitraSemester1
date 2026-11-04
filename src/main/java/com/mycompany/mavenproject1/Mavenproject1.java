package com.mycompany.mavenproject1;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Mavenproject1 {

    public static void main(String[] args) {
        // 1. Pasang tema desain UI
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);

        // 2. Warna & gaya Yellow & Cream
        UIManager.put("Label.foreground", Tema.COKELAT);
        UIManager.put("CheckBox.foreground", Tema.COKELAT);
        UIManager.put("TextField.foreground", Tema.COKELAT);
        UIManager.put("Component.focusColor", Tema.KUNING);
        UIManager.put("Component.focusedBorderColor", Tema.KUNING);
        UIManager.put("Component.borderColor", Tema.GARIS);
        UIManager.put("ToggleButton.selectedBackground", Tema.KUNING);
        UIManager.put("ToggleButton.selectedForeground", Tema.COKELAT);
        UIManager.put("ToggleButton.foreground", Tema.COKELAT);
        UIManager.put("TabbedPane.underlineColor", Tema.KUNING);
        UIManager.put("TabbedPane.selectedForeground", Tema.COKELAT);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());

        // 3. Buka halaman Login Frame sebagai tampilan pertama
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}