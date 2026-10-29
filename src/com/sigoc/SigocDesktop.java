package com.sigoc;

import com.sigoc.infrastructure.in.ui.FrmLogin;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class SigocDesktop {
    private SigocDesktop() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { 
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // --- MEJORAS VISUALES GLOBALES ---
                java.awt.Font fuente = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
                java.awt.Font fuenteNegrita = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13);
                
                // Aplicar Segoe UI a todo
                java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) {
                        UIManager.put(key, new javax.swing.plaf.FontUIResource(fuente));
                    }
                }
                
                // Estilos modernos para Tablas
                UIManager.put("Table.rowHeight", 28);
                UIManager.put("Table.showVerticalLines", false);
                UIManager.put("Table.gridColor", new java.awt.Color(230, 230, 230));
                UIManager.put("Table.selectionBackground", new java.awt.Color(0, 120, 215)); // Azul Windows
                UIManager.put("Table.selectionForeground", java.awt.Color.WHITE);
                UIManager.put("TableHeader.font", new javax.swing.plaf.FontUIResource(fuenteNegrita));
                UIManager.put("TableHeader.background", new java.awt.Color(245, 245, 245));
                UIManager.put("TableHeader.foreground", java.awt.Color.DARK_GRAY);
                
                // Botones y Paneles
                UIManager.put("Button.font", new javax.swing.plaf.FontUIResource(fuenteNegrita));
                UIManager.put("Panel.background", java.awt.Color.WHITE);
                UIManager.put("ScrollPane.background", java.awt.Color.WHITE);
                UIManager.put("SplitPane.background", java.awt.Color.WHITE);
                UIManager.put("Label.foreground", new java.awt.Color(40, 40, 40));
                
            } catch (Exception ignored) { }
            new FrmLogin().setVisible(true);
        });
    }
}
