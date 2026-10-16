package com.sigoc.presentacion;

import com.sigoc.config.ClsConexionBD;
import com.sigoc.negocio.ClsAvanceService;
import com.sigoc.negocio.ClsMaterialService;
import com.sigoc.negocio.ClsOperacionService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;

public class FrmOperaciones extends JFrame {

    private record Opcion(int id, String texto) {
        @Override public String toString() { return texto; }
    }

    private static final Map<String, String> TABLAS = Map.of(
        "AVANCES", "avance_partida", "COMPRAS", "compra_material", 
        "CONSUMOS", "consumo_material", "ASISTENCIA", "asistencia", 
        "PERSONAL", "empleado", "FINANZAS", "egreso"
    );

    private static final Map<String, String> TITULOS_COLUMNAS = Map.ofEntries(
        Map.entry("id_avance", "ID"), Map.entry("id_partida", "Partida"),
        Map.entry("id_obra", "Obra"), Map.entry("id_proveedor", "Proveedor"),
        Map.entry("id_material", "Material"), Map.entry("id_empleado", "Empleado"),
        Map.entry("id_usuario", "Usuario"), Map.entry("id_compra", "ID Compra"),
        Map.entry("id_consumo", "ID Consumo"), Map.entry("id_asistencia", "ID Asistencia"),
        Map.entry("id_produccion", "ID Producción"), Map.entry("id_ingreso", "ID Ingreso"),
        Map.entry("id_egreso", "ID Egreso"), Map.entry("id_cierre", "ID Cierre"),
        Map.entry("fecha_reporte", "Fecha Reporte"), Map.entry("fecha_compra", "Fecha Compra"),
        Map.entry("fecha_consumo", "Fecha Consumo"), Map.entry("fecha", "Fecha"),
        Map.entry("porcentaje_avance", "Avance (%)"), Map.entry("tipo_comprobante", "Tipo Comprobante"),
        Map.entry("numero_comprobante", "N° Comprobante"), Map.entry("total", "Total (S/)"),
        Map.entry("cantidad", "Cantidad"), Map.entry("precio_unitario", "P. Unitario (S/)"),
        Map.entry("subtotal", "Subtotal (S/)"), Map.entry("hora_entrada", "Entrada"),
        Map.entry("hora_salida", "Salida"), Map.entry("estado", "Estado"),
        Map.entry("cantidad_producida", "Cant. Producida"), Map.entry("tipo_egreso", "Tipo Egreso"),
        Map.entry("concepto", "Concepto"), Map.entry("monto", "Monto (S/)"),
        Map.entry("observaciones", "Observaciones"), Map.entry("nombres_apellidos", "Empleado")
    );

    private final String modulo;
    private final int idUsuario;
    private final ClsOperacionService operaciones = new ClsOperacionService();
    private final ClsAvanceService avances = new ClsAvanceService();
    private final ClsMaterialService materiales = new ClsMaterialService();
    
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable grid = new JTable(model);
    private final JPanel panelFormulario = new JPanel(new BorderLayout());
    private final Map<String, JComponent> campos = new LinkedHashMap<>();

    public FrmOperaciones(String modulo, int idUsuario) {
        super("Operaciones: " + modulo);
        this.modulo = modulo;
        this.idUsuario = idUsuario;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1040, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Módulo actual: " + modulo));
        JButton btnActualizar = new JButton("Actualizar tabla");
        btnActualizar.addActionListener(e -> cargar());
        top.add(btnActualizar);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(560);
        
        grid.setShowGrid(true);
        grid.setRowHeight(24);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollGrid = new JScrollPane(grid);
        scrollGrid.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        split.setLeftComponent(scrollGrid);
        
        panelFormulario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            BorderFactory.createTitledBorder("Registrar " + modulo)
        ));
        split.setRightComponent(new JScrollPane(panelFormulario));
        add(split, BorderLayout.CENTER);

        cargar(); // Carga tabla y construye el formulario
    }

    private void cargar() {
        String t = TABLAS.get(modulo);
        if (t == null) return;
        
        ClsSwingUtil.ejecutarAsync(this,
            () -> {
                // 1. Cargar historial omitiendo columnas técnicas de auditoría
                var columns = new java.util.ArrayList<String>();
                var rows = new java.util.ArrayList<Object[]>();
                try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
                     var st = c.createStatement();
                     var rs = st.executeQuery("SELECT * FROM " + t + " WHERE activo = 1 ORDER BY 1 DESC LIMIT 200")) {
                    int n = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= n; i++) {
                        String colLabel = rs.getMetaData().getColumnLabel(i);
                        if (!colLabel.equalsIgnoreCase("activo") && !colLabel.equalsIgnoreCase("fecha_borrado")) {
                            columns.add(colLabel);
                        }
                    }
                    while (rs.next()) {
                        Object[] row = new Object[columns.size()];
                        for (int i = 0; i < columns.size(); i++) {
                            row[i] = rs.getObject(columns.get(i));
                        }
                        rows.add(row);
                    }
                }
                
                // 2. Cargar dependencias de FK para el formulario
                Map<String, List<Map<String, Object>>> combos = new java.util.HashMap<>();
                String[] tablasDependientes = switch (modulo) {
                    case "AVANCES" -> new String[]{"partida"};
                    case "COMPRAS" -> new String[]{"proveedor", "obra", "material"};
                    case "CONSUMOS" -> new String[]{"partida", "material"};
                    case "ASISTENCIA" -> new String[]{"empleado", "obra"};
                    case "PERSONAL" -> new String[]{"empleado", "partida"};
                    case "FINANZAS" -> new String[]{"obra"};
                    default -> new String[0];
                };
                for (String td : tablasDependientes) combos.put(td, operaciones.opciones(td));
                
                // 3. Mapear IDs foráneos a Nombres descriptivos en la tabla
                for (int i = 0; i < columns.size(); i++) {
                    String col = columns.get(i).toLowerCase();
                    if (col.startsWith("id_")) {
                        String fk = col.substring(3);
                        if (combos.containsKey(fk)) {
                            var list = combos.get(fk);
                            var map = new java.util.HashMap<Integer, String>();
                            for (var mapItem : list) map.put((Integer) mapItem.get("id"), (String) mapItem.get("nombre"));
                            for (Object[] r : rows) {
                                if (r[i] instanceof Integer id && map.containsKey(id)) r[i] = map.get(id);
                            }
                        }
                    }
                }
                
                return new Object[]{columns, rows, combos};
            },
            result -> {
                var cols = (List<String>) result[0];
                var rows = (List<Object[]>) result[1];
                var combos = (Map<String, List<Map<String, Object>>>) result[2];
                
                model.setRowCount(0);
                model.setColumnCount(0);
                for (String c : cols) {
                    model.addColumn(TITULOS_COLUMNAS.getOrDefault(c.toLowerCase(), c));
                }
                for (var r : rows) model.addRow(r);
                
                // Auto-dimensionar columnas para que nunca se solapen
                ClsSwingUtil.autoAjustarColumnas(grid);
                
                construirFormulario(combos);
            },
            this::error
        );
    }

    private void construirFormulario(Map<String, List<Map<String, Object>>> dataCombos) {
        panelFormulario.removeAll();
        campos.clear();
        
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 8));
        
        switch (modulo) {
            case "AVANCES" -> {
                agregarCombo(p, "partida", "Partida", dataCombos.get("partida"));
                agregarFecha(p, "fecha", "Fecha");
                agregarTexto(p, "porcentaje", "Porcentaje acumulado (%)", "");
                agregarTexto(p, "observacion", "Observación", "");
            }
            case "COMPRAS" -> {
                agregarCombo(p, "proveedor", "Proveedor", dataCombos.get("proveedor"));
                agregarCombo(p, "obra", "Obra", dataCombos.get("obra"));
                agregarCombo(p, "material", "Material", dataCombos.get("material"));
                agregarFecha(p, "fecha", "Fecha");
                agregarFijo(p, "tipoDoc", "Tipo comprobante", new String[]{"Factura", "Boleta"});
                agregarTexto(p, "comprobante", "N° comprobante", "");
                agregarTexto(p, "cantidad", "Cantidad", "");
                agregarTexto(p, "precio", "Precio unitario (S/)", "");
            }
            case "CONSUMOS" -> {
                agregarCombo(p, "partida", "Partida", dataCombos.get("partida"));
                agregarCombo(p, "material", "Material", dataCombos.get("material"));
                agregarFecha(p, "fecha", "Fecha");
                agregarTexto(p, "cantidad", "Cantidad a consumir", "");
            }
            case "ASISTENCIA" -> {
                agregarCombo(p, "empleado", "Empleado", dataCombos.get("empleado"));
                agregarCombo(p, "obra", "Obra", dataCombos.get("obra"));
                agregarFecha(p, "fecha", "Fecha");
                agregarTexto(p, "entrada", "Hora Entrada (HH:MM:SS)", "08:00:00");
                agregarTexto(p, "salida", "Hora Salida (HH:MM:SS)", "17:00:00");
                agregarFijo(p, "estado", "Estado", new String[]{"PRESENTE", "FALTA", "TARDANZA", "PERMISO"});
            }
            case "PERSONAL" -> { // Producción a destajo
                agregarCombo(p, "empleado", "Empleado", dataCombos.get("empleado"));
                agregarCombo(p, "partida", "Partida", dataCombos.get("partida"));
                agregarFecha(p, "fecha", "Fecha");
                agregarTexto(p, "cantidad", "Cantidad producida", "");
            }
            case "FINANZAS" -> {
                JComboBox<String> cboTipo = agregarFijo(p, "tipoOp", "Tipo de Operación", new String[]{"Ingreso", "Egreso manual", "Cierre de planilla"});
                agregarCombo(p, "obra", "Obra", dataCombos.get("obra"));
                agregarFecha(p, "fecha", "Fecha de Op. / Desde (Cierre)");
                agregarFecha(p, "hasta", "Hasta (Solo Cierre)");
                agregarTexto(p, "concepto", "Concepto (Ingreso/Egreso)", "");
                agregarTexto(p, "monto", "Monto S/ (Ingreso/Egreso)", "");
                
                cboTipo.addActionListener(e -> {
                    boolean esCierre = "Cierre de planilla".equals(cboTipo.getSelectedItem());
                    campos.get("hasta").setEnabled(esCierre);
                    campos.get("concepto").setEnabled(!esCierre);
                    campos.get("monto").setEnabled(!esCierre);
                });
                cboTipo.setSelectedIndex(0);
            }
        }

        panelFormulario.add(p, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnRegistrar = new JButton("Registrar / Guardar");
        
        btnRegistrar.addActionListener(e -> registrar());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        
        btnPanel.add(btnLimpiar);
        btnPanel.add(btnRegistrar);
        panelFormulario.add(btnPanel, BorderLayout.SOUTH);
        
        panelFormulario.revalidate();
        panelFormulario.repaint();
    }

    private void agregarFecha(JPanel p, String key, String label) {
        p.add(new JLabel(label));
        JDateChooser dc = new JDateChooser(new java.util.Date());
        dc.setDateFormatString("yyyy-MM-dd");
        if (dc.getDateEditor().getUiComponent() instanceof JTextField tf) {
            tf.setEditable(false);
        }
        campos.put(key, dc);
        p.add(dc);
    }

    private void agregarTexto(JPanel p, String key, String label, String init) {
        p.add(new JLabel(label));
        JTextField tf = new JTextField(init);
        campos.put(key, tf);
        p.add(tf);
    }

    private void agregarCombo(JPanel p, String key, String label, List<Map<String, Object>> rows) {
        p.add(new JLabel(label));
        JComboBox<Opcion> cb = new JComboBox<>();
        if (rows != null) {
            for (var r : rows) cb.addItem(new Opcion((Integer) r.get("id"), r.get("nombre").toString()));
        }
        campos.put(key, cb);
        p.add(cb);
    }
    
    private JComboBox<String> agregarFijo(JPanel p, String key, String label, String[] opciones) {
        p.add(new JLabel(label));
        JComboBox<String> cb = new JComboBox<>(opciones);
        campos.put(key, cb);
        p.add(cb);
        return cb;
    }

    private int valId(String key) {
        Opcion o = (Opcion) ((JComboBox<?>) campos.get(key)).getSelectedItem();
        if (o == null) throw new IllegalArgumentException("Seleccione una opción válida para el campo.");
        return o.id();
    }

    private String valStr(String key) {
        JComponent comp = campos.get(key);
        String text = "";
        if (comp instanceof JTextField tf) text = tf.getText().trim();
        else if (comp instanceof JComboBox<?> cb && cb.getSelectedItem() != null) text = cb.getSelectedItem().toString();
        
        if (text.isEmpty() && !key.equals("observacion") && !key.equals("concepto")) {
            throw new IllegalArgumentException("Debe completar el campo requerido.");
        }
        return text;
    }
    
    private LocalDate valFecha(String key) {
        JComponent comp = campos.get(key);
        if (comp instanceof com.toedter.calendar.JDateChooser dc) {
            java.util.Date d = dc.getDate();
            if (d == null) throw new IllegalArgumentException("Debe seleccionar una fecha válida.");
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(valStr(key));
    }
    
    private double valDouble(String key) {
        String s = valStr(key).replace("S/", "").replace("$", "").replace(" ", "").replace(",", "");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor '" + s + "' debe ser numérico.");
        }
    }

    private void registrar() {
        if (JOptionPane.showConfirmDialog(this, "¿Confirma registrar esta operación?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        ClsSwingUtil.ejecutarVoid(this,
            () -> {
                switch (modulo) {
                    case "AVANCES" -> avances.registrar(valId("partida"), valFecha("fecha"), valDouble("porcentaje"), valStr("observacion"), idUsuario);
                    case "COMPRAS" -> operaciones.compra(valId("proveedor"), valId("obra"), valId("material"), valStr("tipoDoc"), valStr("comprobante"), valFecha("fecha"), valDouble("cantidad"), valDouble("precio"));
                    case "CONSUMOS" -> {
                        boolean alerta = materiales.registrarConsumo(valId("material"), valId("partida"), idUsuario, valFecha("fecha"), valDouble("cantidad"));
                        if (alerta) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Consumo registrado. El stock quedó por debajo del mínimo.", "Alerta de almacén", JOptionPane.WARNING_MESSAGE));
                    }
                    case "ASISTENCIA" -> operaciones.asistencia(valId("empleado"), valId("obra"), valFecha("fecha"), valStr("entrada"), valStr("salida"), valStr("estado"));
                    case "PERSONAL" -> operaciones.produccion(valId("empleado"), valId("partida"), valFecha("fecha"), valDouble("cantidad"));
                    case "FINANZAS" -> {
                        String tipo = valStr("tipoOp");
                        if ("Cierre de planilla".equals(tipo)) {
                            double total = operaciones.cerrarPlanilla(valId("obra"), valFecha("fecha"), valFecha("hasta"), idUsuario);
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Planilla cerrada. Total: S/ " + total));
                        } else if ("Ingreso".equals(tipo)) {
                            operaciones.ingreso(valId("obra"), valFecha("fecha"), valStr("concepto"), valDouble("monto"));
                        } else {
                            operaciones.egreso(valId("obra"), valFecha("fecha"), valStr("concepto"), valDouble("monto"));
                        }
                    }
                }
            },
            () -> {
                JOptionPane.showMessageDialog(this, "Registro guardado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargar(); // Recarga historial y combos
            },
            this::error
        );
    }

    private void limpiarFormulario() {
        for (JComponent c : campos.values()) {
            if (c instanceof JTextField tf) {
                if (tf.isEnabled()) tf.setText("");
            } else if (c instanceof com.toedter.calendar.JDateChooser dc) {
                if (dc.isEnabled()) dc.setDate(new java.util.Date());
            }
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Ocurrió un error inesperado." : ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
