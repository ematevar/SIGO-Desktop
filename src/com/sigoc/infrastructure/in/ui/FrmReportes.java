package com.sigoc.infrastructure.in.ui;

import com.sigoc.application.service.ClsReporteService;
import com.sigoc.infrastructure.in.ui.ClsPanelGrafico.TipoGrafico;
import com.sigoc.infrastructure.in.ui.ClsSwingUtil;
import com.sigoc.application.port.in.ReporteUseCase;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.*;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FrmReportes extends JFrame {

    private final ReporteUseCase service = new ClsReporteService(new com.sigoc.infrastructure.out.persistence.ClsReporteDAO());
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Avance físico por fecha", "Consumo acumulado y stock", "Personal", "Finanzas por obra"});
    private final DefaultTableModel modelo = new DefaultTableModel();
    private ClsReporteService.Reporte actual;
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    public FrmReportes() {
        super("Reportes gerenciales");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(tipo);
        JButton ver = new JButton("Generar"), exportar = new JButton("Exportar CSV para Excel");
        ver.addActionListener(e -> cargar());
        exportar.addActionListener(e -> exportar());
        top.add(ver);
        top.add(exportar);
        add(top, BorderLayout.NORTH);
        JTable tbl = new JTable(modelo);
        split.setTopComponent(new JScrollPane(tbl));
        split.setBottomComponent(new JPanel());
        split.setDividerLocation(200);
        add(split, BorderLayout.CENTER);
        cargar();
    }

    private void cargar() {
        int selectedIndex = tipo.getSelectedIndex();
        ClsSwingUtil.ejecutarAsync(this,
            () -> service.generar(selectedIndex),
            reporte -> {
                actual = reporte;
                modelo.setRowCount(0);
                modelo.setColumnCount(0);
                actual.columnas().forEach(modelo::addColumn);
                for (Object[] r : actual.filas()) {
                    modelo.addRow(r);
                }
                actualizarGrafico();
            },
            ex -> JOptionPane.showMessageDialog(this, "No se pudo generar el reporte.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void actualizarGrafico() {
        if (actual == null || actual.filas().isEmpty()) {
            split.setBottomComponent(new JPanel());
            return;
        }
        int idx = tipo.getSelectedIndex();
        ClsPanelGrafico g;
        List<Object[]> f = actual.filas();
        if (idx == 0) {
            g = new ClsPanelGrafico(TipoGrafico.LINEAS);
            Set<String> fechas = new TreeSet<>();
            Map<String, Map<String, Double>> obras = new HashMap<>();
            for (Object[] r : f) {
                String obra = String.valueOf(r[0]);
                String fec = String.valueOf(r[1]);
                double val = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
                fechas.add(fec);
                obras.computeIfAbsent(obra, k -> new HashMap<>()).put(fec, val);
            }
            String[] ets = fechas.toArray(new String[0]);
            g.setEtiquetas(ets);
            for (Map.Entry<String, Map<String, Double>> e : obras.entrySet()) {
                double[] vals = new double[ets.length];
                for (int i = 0; i < ets.length; i++) {
                    vals[i] = e.getValue().getOrDefault(ets[i], 0.0);
                }
                g.agregarSerie(e.getKey(), vals);
            }
        } else if (idx == 1) {
            g = new ClsPanelGrafico(TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] cons = new double[f.size()], stk = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[0]);
                cons[i] = f.get(i)[2] == null ? 0 : ((Number) f.get(i)[2]).doubleValue();
                stk[i] = f.get(i)[3] == null ? 0 : ((Number) f.get(i)[3]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Consumo acumulado", cons);
            g.agregarSerie("Stock actual", stk);
        } else if (idx == 2) {
            g = new ClsPanelGrafico(TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] prod = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[1]);
                prod[i] = f.get(i)[4] == null ? 0 : ((Number) f.get(i)[4]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Producción total", prod);
        } else {
            g = new ClsPanelGrafico(TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] ing = new double[f.size()], egr = new double[f.size()], sal = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[0]);
                ing[i] = f.get(i)[1] == null ? 0 : ((Number) f.get(i)[1]).doubleValue();
                egr[i] = f.get(i)[2] == null ? 0 : ((Number) f.get(i)[2]).doubleValue();
                sal[i] = f.get(i)[3] == null ? 0 : ((Number) f.get(i)[3]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Ingresos", ing);
            g.agregarSerie("Egresos", egr);
            g.agregarSerie("Saldo", sal);
        }
        int div = split.getDividerLocation();
        split.setBottomComponent(g);
        split.setDividerLocation(div);
    }

    private void exportar() {
        if (actual == null) {
            return;
        }
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new java.io.File("sigoc_reporte_" + (tipo.getSelectedIndex() + 1) + ".csv"));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            service.exportarCsv(actual, ch.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "Reporte exportado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
