package com.sigoc.presentacion;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

/** Utilidad para ejecutar operaciones de BD fuera del EDT, evitando congelamiento de la UI. */
public final class ClsSwingUtil {
    private ClsSwingUtil() {}

    /** Ejecuta {@code operacion} en segundo plano; al terminar invoca {@code alTerminar} en el EDT. */
    public static <T> void ejecutarAsync(JFrame owner, Callable<T> operacion, Consumer<T> alTerminar, Consumer<Exception> alFallar) {
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        habilitarBotones(owner.getContentPane(), false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                owner.setCursor(Cursor.getDefaultCursor());
                habilitarBotones(owner.getContentPane(), true);
                try { alTerminar.accept(get()); }
                catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    alFallar.accept(c instanceof Exception ex ? ex : new Exception(c));
                }
            }
        }.execute();
    }

    public interface TareaVoid {
        void ejecutar() throws Exception;
    }

    /** Variante sin valor de retorno. */
    public static void ejecutarVoid(JFrame owner, TareaVoid operacion, Runnable alTerminar, Consumer<Exception> alFallar) {
        ejecutarAsync(owner, () -> { operacion.ejecutar(); return null; }, v -> alTerminar.run(), alFallar);
    }

    private static void habilitarBotones(Container c, boolean enabled) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton) comp.setEnabled(enabled);
            if (comp instanceof Container ct) habilitarBotones(ct, enabled);
        }
    }

    /** Ajusta el ancho de las columnas de una JTable al tamaño del encabezado y contenido con márgenes legibles. */
    public static void autoAjustarColumnas(JTable tabla) {
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        var colModel = tabla.getColumnModel();
        for (int col = 0; col < tabla.getColumnCount(); col++) {
            var tableCol = colModel.getColumn(col);
            int width = 80;
            Object headerVal = tableCol.getHeaderValue();
            if (headerVal != null) {
                java.awt.FontMetrics fm = tabla.getFontMetrics(tabla.getTableHeader().getFont());
                width = Math.max(width, fm.stringWidth(headerVal.toString()) + 35);
            }
            java.awt.FontMetrics fm = tabla.getFontMetrics(tabla.getFont());
            int maxRows = Math.min(tabla.getRowCount(), 50);
            for (int row = 0; row < maxRows; row++) {
                Object val = tabla.getValueAt(row, col);
                if (val != null) {
                    width = Math.max(width, fm.stringWidth(val.toString()) + 25);
                }
            }
            tableCol.setPreferredWidth(Math.min(width, 380));
        }
    }
}
