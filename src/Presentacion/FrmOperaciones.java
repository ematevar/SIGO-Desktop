package Presentacion;

import config.ClsConstantes;
import Negocio.ClsAvanceService;
import Negocio.ClsMaterialService;
import Negocio.ClsOperacionService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;

/**
 * Formulario de operaciones transaccionales: Avances (RF-05), Compras (RF-07),
 * Consumos (RF-08), Asistencia (RF-11), Producción (RF-12) y Finanzas (RF-13, RF-14, RF-15).
 * Cumple estrictamente RNF-05 (sin acceso directo a BD desde la capa de presentación) y
 * lee todos los controles Swing dentro del EDT antes de invocar al {@code SwingWorker}.
 */
public class FrmOperaciones extends JFrame {

    private record Opcion(int id, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }

    private static final Map<String, String> TABLAS = Map.of(
            "AVANCES", "avance_partida",
            "COMPRAS", "compra_material",
            "CONSUMOS", "consumo_material",
            "ASISTENCIA", "asistencia",
            "PRODUCCION", "produccion_empleado",
            "FINANZAS", "egreso"
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
    private final String rolSesion;
    private final ClsOperacionService operaciones = new ClsOperacionService();
    private final ClsAvanceService avances = new ClsAvanceService();
    private final ClsMaterialService materiales = new ClsMaterialService();

    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable grid = new JTable(model);
    private final JPanel panelFormulario = new JPanel(new BorderLayout());
    private final Map<String, JComponent> campos = new LinkedHashMap<>();

    public FrmOperaciones(String modulo, int idUsuario) {
        this(modulo, idUsuario, null);
    }

    public FrmOperaciones(String modulo, int idUsuario, String rolSesion) {
        super("Operaciones: " + modulo);
        this.modulo = modulo;
        this.idUsuario = idUsuario;
        this.rolSesion = rolSesion;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1060, 630);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Módulo actual: " + modulo));
        JButton btnActualizar = new JButton("Actualizar tabla");
        btnActualizar.addActionListener(e -> cargar());
        top.add(btnActualizar);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(570);

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

        cargar();
    }

    @SuppressWarnings("unchecked")
    private void cargar() {
        String t = TABLAS.get(modulo);
        if (t == null) {
            return;
        }

        ClsSwingUtil.ejecutarAsync(
                this,
                () -> {
                    Map<String, Object> hist = operaciones.listarHistorial(t);
                    var columns = (List<String>) hist.get("columnas");
                    var rows = (List<Object[]>) hist.get("filas");

                    Map<String, List<Map<String, Object>>> combos = new HashMap<>();
                    String[] tablasDependientes = switch (modulo) {
                        case "AVANCES" -> new String[]{"partida"};
                        case "COMPRAS" -> new String[]{"proveedor", "obra", "material"};
                        case "CONSUMOS" -> new String[]{"partida", "material"};
                        case "ASISTENCIA" -> new String[]{"empleado", "obra"};
                        case "PRODUCCION" -> new String[]{"empleado", "partida"};
                        case "FINANZAS" -> new String[]{"obra"};
                        default -> new String[0];
                    };
                    for (String td : tablasDependientes) {
                        combos.put(td, operaciones.opciones(td));
                    }

                    // Mapear IDs foráneos (nunca la primera columna PK de la propia tabla) a nombres descriptivos
                    for (int i = 1; i < columns.size(); i++) {
                        String col = columns.get(i).toLowerCase();
                        if (col.startsWith("id_")) {
                            String fk = col.substring(3);
                            if (combos.containsKey(fk)) {
                                var list = combos.get(fk);
                                var map = new HashMap<Integer, String>();
                                for (var mapItem : list) {
                                    map.put((Integer) mapItem.get("id"), (String) mapItem.get("nombre"));
                                }
                                for (Object[] r : rows) {
                                    if (r[i] instanceof Integer id && map.containsKey(id)) {
                                        r[i] = map.get(id);
                                    }
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
                    for (var r : rows) {
                        model.addRow(r);
                    }

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
                agregarCombo(p, "partida", "Partida (Obra Activa)", dataCombos.get("partida"));
                agregarFecha(p, "fecha", "Fecha de reporte");
                agregarTexto(p, "porcentaje", "Porcentaje acumulado (%)", "");
                agregarTexto(p, "observacion", "Observación", "");
            }
            case "COMPRAS" -> {
                agregarCombo(p, "proveedor", "Proveedor activo", dataCombos.get("proveedor"));
                agregarCombo(p, "obra", "Obra destino", dataCombos.get("obra"));
                agregarCombo(p, "material", "Material", dataCombos.get("material"));
                agregarFecha(p, "fecha", "Fecha de compra");
                agregarFijo(p, "tipoDoc", "Tipo comprobante", new String[]{"Factura", "Boleta"});
                agregarTexto(p, "comprobante", "N° comprobante", "");
                agregarTexto(p, "cantidad", "Cantidad comprada", "");
                agregarTexto(p, "precio", "Precio unitario (S/)", "");
            }
            case "CONSUMOS" -> {
                agregarCombo(p, "partida", "Partida (Obra Activa)", dataCombos.get("partida"));
                agregarCombo(p, "material", "Material", dataCombos.get("material"));
                agregarFecha(p, "fecha", "Fecha de consumo");
                agregarTexto(p, "cantidad", "Cantidad a consumir", "");
            }
            case "ASISTENCIA" -> {
                agregarCombo(p, "empleado", "Trabajador activo", dataCombos.get("empleado"));
                agregarCombo(p, "obra", "Obra activa", dataCombos.get("obra"));
                agregarFecha(p, "fecha", "Fecha");
                JComboBox<String> cboEstado = agregarFijo(p, "estado", "Estado", new String[]{"PRESENTE", "TARDANZA", "FALTA", "PERMISO"});
                agregarTexto(p, "entrada", "Hora Entrada (HH:MM:SS)", "08:00:00");
                agregarTexto(p, "salida", "Hora Salida (HH:MM:SS)", "17:00:00");

                cboEstado.addActionListener(e -> {
                    String est = (String) cboEstado.getSelectedItem();
                    boolean requiereHoras = "PRESENTE".equals(est) || "TARDANZA".equals(est);
                    JTextField tfEnt = (JTextField) campos.get("entrada");
                    JTextField tfSal = (JTextField) campos.get("salida");
                    tfEnt.setEnabled(requiereHoras);
                    tfSal.setEnabled(requiereHoras);
                    if (!requiereHoras) {
                        tfEnt.setText("");
                        tfSal.setText("");
                    } else if (tfEnt.getText().isBlank()) {
                        tfEnt.setText("08:00:00");
                        tfSal.setText("17:00:00");
                    }
                });
            }
            case "PRODUCCION" -> {
                agregarCombo(p, "empleado", "Trabajador (con asistencia en el día)", dataCombos.get("empleado"));
                agregarCombo(p, "partida", "Partida activa", dataCombos.get("partida"));
                agregarFecha(p, "fecha", "Fecha de producción");
                agregarTexto(p, "cantidad", "Cantidad producida", "");
            }
            case "FINANZAS" -> {
                JComboBox<String> cboTipo = agregarFijo(p, "tipoOp", "Tipo de Operación", new String[]{"Ingreso", "Egreso manual", "Cierre de planilla"});
                agregarCombo(p, "obra", "Obra activa", dataCombos.get("obra"));
                agregarFecha(p, "fecha", "Fecha de Op. / Desde (Cierre)");
                agregarFecha(p, "hasta", "Hasta (Solo Cierre)");
                agregarTexto(p, "concepto", "Concepto / Motivo (Ingreso/Egreso)", "");
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
            for (var r : rows) {
                cb.addItem(new Opcion((Integer) r.get("id"), r.get("nombre").toString()));
            }
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
        if (o == null) {
            throw new IllegalArgumentException("Seleccione una opción válida para '" + key + "'.");
        }
        return o.id();
    }

    private String valStr(String key) {
        JComponent comp = campos.get(key);
        String text = "";
        if (comp instanceof JTextField tf) {
            text = tf.getText().trim();
        } else if (comp instanceof JComboBox<?> cb && cb.getSelectedItem() != null) {
            text = cb.getSelectedItem().toString();
        }
        if (text.isEmpty() && !key.equals("observacion") && !key.equals("concepto") && !key.equals("entrada") && !key.equals("salida")) {
            throw new IllegalArgumentException("Debe completar el campo requerido '" + key + "'.");
        }
        return text;
    }

    private LocalDate valFecha(String key) {
        JComponent comp = campos.get(key);
        if (comp instanceof JDateChooser dc) {
            java.util.Date d = dc.getDate();
            if (d == null) {
                throw new IllegalArgumentException("Debe seleccionar una fecha válida.");
            }
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
        // Leer todos los campos del formulario en el EDT antes de lanzar el SwingWorker
        final Runnable[] postAccionEdt = new Runnable[1];
        final ClsSwingUtil.TareaVoid tareaBg;

        try {
            switch (modulo) {
                case "AVANCES" -> {
                    int idPartida = valId("partida");
                    LocalDate fecha = valFecha("fecha");
                    double pct = valDouble("porcentaje");
                    String obs = valStr("observacion");
                    tareaBg = () -> {
                        double avanceObra = avances.registrar(idPartida, fecha, pct, obs, idUsuario, rolSesion);
                        postAccionEdt[0] = () -> JOptionPane.showMessageDialog(
                                this,
                                String.format("Avance registrado correctamente.\nAvance físico acumulado ponderado de la obra: %.2f %% (RF-05)", avanceObra),
                                "Avance Acumulado de Obra",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    };
                }
                case "COMPRAS" -> {
                    int idProv = valId("proveedor");
                    int idObra = valId("obra");
                    int idMat = valId("material");
                    String tipoDoc = valStr("tipoDoc");
                    String comp = valStr("comprobante");
                    LocalDate fecha = valFecha("fecha");
                    double cant = valDouble("cantidad");
                    double precio = valDouble("precio");
                    tareaBg = () -> operaciones.compra(idProv, idObra, idMat, tipoDoc, comp, fecha, cant, precio, rolSesion);
                }
                case "CONSUMOS" -> {
                    int idMat = valId("material");
                    int idPartida = valId("partida");
                    LocalDate fecha = valFecha("fecha");
                    double cant = valDouble("cantidad");
                    tareaBg = () -> {
                        boolean alerta = materiales.registrarConsumo(idMat, idPartida, idUsuario, fecha, cant, rolSesion);
                        double acumPartida = materiales.consumoAcumuladoPartida(idPartida, idMat);
                        postAccionEdt[0] = () -> {
                            String msg = String.format("Consumo registrado.\nConsumo acumulado de este material en la partida: %.2f", acumPartida);
                            if (alerta) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        msg + "\n\n¡ALERTA DE ALMACÉN (RF-08): El stock actual quedó igual o por debajo del stock mínimo!",
                                        "Alerta de Stock Mínimo",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            } else {
                                JOptionPane.showMessageDialog(this, msg, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            }
                        };
                    };
                }
                case "ASISTENCIA" -> {
                    int idEmp = valId("empleado");
                    int idObra = valId("obra");
                    LocalDate fecha = valFecha("fecha");
                    String estado = valStr("estado");
                    String entrada = ("FALTA".equalsIgnoreCase(estado) || "PERMISO".equalsIgnoreCase(estado)) ? "" : valStr("entrada");
                    String salida = ("FALTA".equalsIgnoreCase(estado) || "PERMISO".equalsIgnoreCase(estado)) ? "" : valStr("salida");
                    tareaBg = () -> operaciones.asistencia(idEmp, idObra, fecha, entrada, salida, estado, rolSesion);
                }
                case "PRODUCCION" -> {
                    int idEmp = valId("empleado");
                    int idPartida = valId("partida");
                    LocalDate fecha = valFecha("fecha");
                    double cant = valDouble("cantidad");
                    tareaBg = () -> operaciones.produccion(idEmp, idPartida, fecha, cant, rolSesion);
                }
                case "FINANZAS" -> {
                    String tipo = valStr("tipoOp");
                    int idObra = valId("obra");
                    LocalDate fecha = valFecha("fecha");
                    if ("Cierre de planilla".equals(tipo)) {
                        LocalDate hasta = valFecha("hasta");
                        tareaBg = () -> {
                            double total = operaciones.cerrarPlanilla(idObra, fecha, hasta, idUsuario, rolSesion);
                            postAccionEdt[0] = () -> JOptionPane.showMessageDialog(
                                    this,
                                    String.format("Planilla cerrada e inmutable (RF-14).\nEgresos tipo Planilla generados por un total de: S/ %.2f", total),
                                    "Cierre de Planilla Exitoso",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        };
                    } else if ("Ingreso".equals(tipo)) {
                        String concepto = valStr("concepto");
                        double monto = valDouble("monto");
                        tareaBg = () -> operaciones.ingreso(idObra, fecha, concepto, monto, rolSesion);
                    } else {
                        String concepto = valStr("concepto");
                        double monto = valDouble("monto");
                        tareaBg = () -> operaciones.egreso(idObra, fecha, concepto, monto, rolSesion);
                    }
                }
                default -> {
                    return;
                }
            }
        } catch (Exception ex) {
            error(ex);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "¿Confirma registrar esta operación?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        ClsSwingUtil.ejecutarVoid(
                this,
                tareaBg,
                () -> {
                    if (postAccionEdt[0] != null) {
                        postAccionEdt[0].run();
                    } else {
                        JOptionPane.showMessageDialog(this, "Registro guardado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    }
                    cargar();
                },
                this::error
        );
    }

    private void limpiarFormulario() {
        for (JComponent c : campos.values()) {
            if (c instanceof JTextField tf) {
                if (tf.isEnabled()) {
                    tf.setText("");
                }
            } else if (c instanceof JDateChooser dc) {
                if (dc.isEnabled()) {
                    dc.setDate(new java.util.Date());
                }
            }
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(ex), "Validación", JOptionPane.ERROR_MESSAGE);
    }
}
