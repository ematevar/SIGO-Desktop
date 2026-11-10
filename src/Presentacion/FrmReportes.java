package Presentacion;

import config.ClsConstantes;
import Negocio.ClsOperacionService;
import Negocio.ClsReporteService;
import Presentacion.ClsPanelGrafico.TipoGrafico;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo de reportes gerenciales con filtros por obra y rango de fechas, tablas de solo lectura,
 * gráficos comparativos con título y sin caídas a cero, y exportación a PDF, Excel (.xlsx) y CSV (RF-16).
 */
public class FrmReportes extends JFrame {

    private record OpcionObra(int id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private final ClsReporteService service = new ClsReporteService();
    private final ClsOperacionService opService = new ClsOperacionService();
    private final String rolSesion;

    private final JComboBox<String> tipo = new JComboBox<>(new String[]{
        "1. Avance físico planificado vs. ejecutado (Curva S)",
        "2. Consumo de materiales real vs. presupuestado",
        "3. Asistencia y ranking de productividad del personal",
        "4. Estado financiero, utilidad y rentabilidad por obra"
    });
    private final JComboBox<OpcionObra> cboObra = new JComboBox<>();
    private final JDateChooser dcDesde = new JDateChooser();
    private final JDateChooser dcHasta = new JDateChooser();

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // RF-16: Los reportes son de solo lectura y sin cifras manuales
        }
    };
    private final JTable tbl = new JTable(modelo);
    private ClsReporteService.Reporte actual;
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    public FrmReportes() {
        this(null);
    }

    public FrmReportes(String rolSesion) {
        super("Reportes gerenciales — SIGOC");
        this.rolSesion = rolSesion;
        setSize(1180, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        dcDesde.setDateFormatString("yyyy-MM-dd");
        dcHasta.setDateFormatString("yyyy-MM-dd");
        dcDesde.setPreferredSize(new java.awt.Dimension(125, 26));
        dcHasta.setPreferredSize(new java.awt.Dimension(125, 26));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Reporte:"));
        top.add(tipo);
        top.add(new JLabel("Obra:"));
        cboObra.addItem(new OpcionObra(0, "Todas las obras"));
        top.add(cboObra);
        top.add(new JLabel("Desde:"));
        top.add(dcDesde);
        top.add(new JLabel("Hasta:"));
        top.add(dcHasta);

        JButton btnLimpiarFiltros = new JButton("Limpiar filtros");
        btnLimpiarFiltros.addActionListener(e -> {
            cboObra.setSelectedIndex(0);
            dcDesde.setDate(null);
            dcHasta.setDate(null);
            cargar();
        });

        JButton ver = new JButton("Generar");
        ver.addActionListener(e -> cargar());

        JButton exportarPdf = new JButton("Exportar PDF");
        exportarPdf.addActionListener(e -> exportar("pdf"));

        JButton exportarXlsx = new JButton("Exportar Excel (.xlsx)");
        exportarXlsx.addActionListener(e -> exportar("xlsx"));

        JButton exportarCsv = new JButton("Exportar CSV");
        exportarCsv.addActionListener(e -> exportar("csv"));

        top.add(ver);
        top.add(btnLimpiarFiltros);
        top.add(exportarPdf);
        top.add(exportarXlsx);
        top.add(exportarCsv);
        add(top, BorderLayout.NORTH);

        split.setTopComponent(new JScrollPane(tbl));
        split.setBottomComponent(new JPanel());
        split.setDividerLocation(240);
        add(split, BorderLayout.CENTER);

        cargarObrasYReporteInicial();
    }

    private void cargarObrasYReporteInicial() {
        ClsSwingUtil.ejecutarAsync(
                this,
                () -> opService.opciones("obra_todas"),
                obras -> {
                    cboObra.removeAllItems();
                    cboObra.addItem(new OpcionObra(0, "Todas las obras"));
                    for (var o : obras) {
                        cboObra.addItem(new OpcionObra((Integer) o.get("id"), o.get("nombre").toString()));
                    }
                    cargar();
                },
                ex -> cargar()
        );
    }

    private void cargar() {
        int selectedIndex = tipo.getSelectedIndex();
        OpcionObra opObra = (OpcionObra) cboObra.getSelectedItem();
        Integer idObra = (opObra == null || opObra.id() <= 0) ? null : opObra.id();
        LocalDate desde = dcDesde.getDate() == null ? null : new java.sql.Date(dcDesde.getDate().getTime()).toLocalDate();
        LocalDate hasta = dcHasta.getDate() == null ? null : new java.sql.Date(dcHasta.getDate().getTime()).toLocalDate();

        ClsSwingUtil.ejecutarAsync(
                this,
                () -> service.generar(selectedIndex, idObra, desde, hasta, rolSesion),
                reporte -> {
                    actual = reporte;
                    modelo.setRowCount(0);
                    modelo.setColumnCount(0);
                    actual.columnas().forEach(modelo::addColumn);
                    for (Object[] r : actual.filas()) {
                        modelo.addRow(r);
                    }
                    ClsSwingUtil.autoAjustarColumnas(tbl);
                    actualizarGrafico();
                },
                ex -> JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(ex), "Error", JOptionPane.ERROR_MESSAGE)
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
            g = new ClsPanelGrafico(actual.titulo(), TipoGrafico.LINEAS);
            Set<String> fechas = new TreeSet<>();
            Map<String, Map<String, Double>> planPorObra = new LinkedHashMap<>();
            Map<String, Map<String, Double>> ejecPorObra = new LinkedHashMap<>();
            for (Object[] r : f) {
                String obra = String.valueOf(r[0]);
                String fec = String.valueOf(r[1]);
                double plan = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
                double ejec = r[3] == null ? 0 : ((Number) r[3]).doubleValue();
                fechas.add(fec);
                planPorObra.computeIfAbsent(obra, k -> new HashMap<>()).put(fec, plan);
                ejecPorObra.computeIfAbsent(obra, k -> new HashMap<>()).put(fec, ejec);
            }
            String[] ets = fechas.toArray(new String[0]);
            g.setEtiquetas(ets);
            for (String obra : ejecPorObra.keySet()) {
                double[] valsPlan = new double[ets.length];
                double[] valsEjec = new double[ets.length];
                double ultimoPlan = 0.0;
                double ultimoEjec = 0.0;
                Map<String, Double> mapaPlan = planPorObra.get(obra);
                Map<String, Double> mapaEjec = ejecPorObra.get(obra);
                for (int i = 0; i < ets.length; i++) {
                    if (mapaPlan.containsKey(ets[i])) {
                        ultimoPlan = mapaPlan.get(ets[i]);
                    }
                    if (mapaEjec.containsKey(ets[i])) {
                        ultimoEjec = mapaEjec.get(ets[i]);
                    }
                    valsPlan[i] = ultimoPlan;
                    valsEjec[i] = ultimoEjec;
                }
                g.agregarSerie(obra + " (Planificado)", valsPlan);
                g.agregarSerie(obra + " (Ejecutado)", valsEjec);
            }
        } else if (idx == 1) {
            g = new ClsPanelGrafico(actual.titulo(), TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] pres = new double[f.size()];
            double[] real = new double[f.size()];
            double[] stk = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[0]);
                pres[i] = f.get(i)[2] == null ? 0 : ((Number) f.get(i)[2]).doubleValue();
                real[i] = f.get(i)[3] == null ? 0 : ((Number) f.get(i)[3]).doubleValue();
                stk[i] = f.get(i)[6] == null ? 0 : ((Number) f.get(i)[6]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Presupuestado", pres);
            g.agregarSerie("Consumo Real", real);
            g.agregarSerie("Stock Actual", stk);
        } else if (idx == 2) {
            g = new ClsPanelGrafico(actual.titulo(), TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] asist = new double[f.size()];
            double[] prod = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[1]);
                asist[i] = f.get(i)[4] == null ? 0 : ((Number) f.get(i)[4]).doubleValue();
                prod[i] = f.get(i)[5] == null ? 0 : ((Number) f.get(i)[5]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Asistencia (%)", asist);
            g.agregarSerie("Producción Total", prod);
        } else {
            g = new ClsPanelGrafico(actual.titulo(), TipoGrafico.BARRAS);
            String[] ets = new String[f.size()];
            double[] ing = new double[f.size()];
            double[] egr = new double[f.size()];
            double[] util = new double[f.size()];
            for (int i = 0; i < f.size(); i++) {
                ets[i] = String.valueOf(f.get(i)[0]);
                ing[i] = f.get(i)[1] == null ? 0 : ((Number) f.get(i)[1]).doubleValue();
                egr[i] = f.get(i)[5] == null ? 0 : ((Number) f.get(i)[5]).doubleValue();
                util[i] = f.get(i)[6] == null ? 0 : ((Number) f.get(i)[6]).doubleValue();
            }
            g.setEtiquetas(ets);
            g.agregarSerie("Ingresos (S/)", ing);
            g.agregarSerie("Total Egresos (S/)", egr);
            g.agregarSerie("Utilidad / Saldo (S/)", util);
        }
        int div = split.getDividerLocation();
        split.setBottomComponent(g);
        split.setDividerLocation(div);
    }

    private void exportar(String formato) {
        if (actual == null) {
            return;
        }
        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new java.io.File("sigoc_reporte_" + (tipo.getSelectedIndex() + 1) + "." + formato));
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = ch.getSelectedFile().toPath();
        try {
            switch (formato) {
                case "pdf" -> service.exportarPdf(actual, destino);
                case "xlsx" -> service.exportarExcelXlsx(actual, destino);
                default -> service.exportarCsv(actual, destino);
            }
            JOptionPane.showMessageDialog(this, "Reporte exportado exitosamente a:\n" + destino.toAbsolutePath(), "Exportación exitosa (RF-16)", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(ex), "Error al exportar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
