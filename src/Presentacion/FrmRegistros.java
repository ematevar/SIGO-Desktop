package Presentacion;

import config.ClsConstantes;
import Negocio.ClsCatalogoService;
import Negocio.ClsOperacionService;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Formulario maestro de mantenimiento para Obras (RF-03), Partidas (RF-04),
 * Materiales (RF-06), Proveedores (RF-09) y Personal/Trabajadores (RF-10).
 */
public class FrmRegistros extends JFrame {

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("id_obra", "ID"), Map.entry("id_partida", "ID"), Map.entry("id_material", "ID"),
            Map.entry("id_proveedor", "ID"), Map.entry("id_empleado", "ID"),
            Map.entry("nombre", "Nombre"), Map.entry("estado", "Estado"),
            Map.entry("cliente", "Cliente"), Map.entry("ubicacion", "Ubicación"),
            Map.entry("presupuesto_total", "Presupuesto Total (S/)"),
            Map.entry("fecha_inicio_plan", "Fecha de Inicio"),
            Map.entry("fecha_fin_plan", "Fecha de Fin"),
            Map.entry("unidad_medida", "Unidad de Medida"),
            Map.entry("metrado_planificado", "Metrado Planificado"),
            Map.entry("peso_presupuestal", "Peso Presupuestal (%)"),
            Map.entry("stock_actual", "Stock Actual"),
            Map.entry("stock_minimo", "Stock Mínimo"),
            Map.entry("costo_unitario_ref", "Costo Unitario Ref. (S/)"),
            Map.entry("razon_social", "Razón Social"), Map.entry("ruc", "RUC"),
            Map.entry("telefono", "Teléfono"), Map.entry("direccion", "Dirección"),
            Map.entry("nombres_apellidos", "Nombres y Apellidos"), Map.entry("dni", "DNI"),
            Map.entry("cargo", "Cargo"), Map.entry("jornal_diario", "Jornal Diario (S/)"),
            Map.entry("fecha_ingreso", "Fecha de Ingreso")
    );

    private static final Map<String, String> FK_TABLAS = Map.of("id_obra", "obra");
    private static final Map<String, String> FK_ETIQUETAS = Map.of("id_obra", "Obra");

    private static final Set<String> COLUMNAS_NUMERICAS = Set.of(
            "presupuesto_total", "metrado_planificado", "peso_presupuestal",
            "stock_minimo", "stock_actual", "costo_unitario_ref", "jornal_diario"
    );

    private static final Set<String> COLUMNAS_OPCIONALES = Set.of(
            "ubicacion", "direccion", "telefono", "observaciones"
    );

    private static final Map<String, String[]> OPCIONES = Map.of(
            "OBRAS", new String[]{"obra"},
            "PARTIDAS", new String[]{"partida"},
            "OBRAS Y PARTIDAS", new String[]{"obra", "partida"},
            "MATERIALES", new String[]{"material"},
            "PROVEEDORES", new String[]{"proveedor"},
            "PERSONAL", new String[]{"empleado"}
    );

    private final ClsCatalogoService service = new ClsCatalogoService();
    private final ClsOperacionService opService = new ClsOperacionService();
    private final String rolSesion;

    private JComboBox<String> cboTabla;
    private JTable tablaDatos;
    private DefaultTableModel modelo;
    private JPanel pnlFormularioContenido;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnEliminar;
    private JButton btnCerrarObra;
    private JButton btnLimpiar;

    private final LinkedHashMap<String, JComponent> campos = new LinkedHashMap<>();
    private final List<Map<String, Object>> listaDatosActual = new ArrayList<>();
    private String pkActual;
    private Object idSeleccionado;

    public FrmRegistros(String modulo) {
        this(modulo, null);
    }

    public FrmRegistros(String modulo, String rolSesion) {
        this.rolSesion = rolSesion;
        setTitle("Gestión — " + modulo);
        setSize(980, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents(modulo);
        cargar();
    }

    private void initComponents(String modulo) {
        setLayout(new BorderLayout());

        JPanel pnlNorte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlNorte.add(new JLabel("Catálogo:"));

        String[] tablasDisponibles = OPCIONES.getOrDefault(modulo, new String[0]);
        if ("OBRAS Y PARTIDAS".equals(modulo) && ClsConstantes.ROL_RESIDENTE.equals(ClsConstantes.normalizarRol(rolSesion))) {
            tablasDisponibles = new String[]{"partida"};
        }
        cboTabla = new JComboBox<>(tablasDisponibles);
        cboTabla.addActionListener(e -> cargar());
        pnlNorte.add(cboTabla);

        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargar());
        pnlNorte.add(btnActualizar);

        add(pnlNorte, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(530);
        add(splitPane, BorderLayout.CENTER);

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaDatos = new JTable(modelo);
        tablaDatos.setShowGrid(true);
        tablaDatos.setRowHeight(24);
        tablaDatos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarRegistro();
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        scrollTabla.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        splitPane.setLeftComponent(scrollTabla);

        JPanel pnlDerecha = new JPanel(new BorderLayout());
        pnlDerecha.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new TitledBorder("Detalle del registro")
        ));

        pnlFormularioContenido = new JPanel(new GridBagLayout());
        JScrollPane scrollForm = new JScrollPane(pnlFormularioContenido);
        scrollForm.setBorder(null);
        pnlDerecha.add(scrollForm, BorderLayout.CENTER);

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnEliminar = new JButton("Desactivar / Baja");
        btnCerrarObra = new JButton("Cerrar Obra");
        btnLimpiar = new JButton("Limpiar");

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnEliminar.addActionListener(e -> eliminar());
        btnCerrarObra.addActionListener(e -> cerrarObra());
        btnLimpiar.addActionListener(e -> limpiar());

        pnlBotones.add(btnNuevo);
        pnlBotones.add(btnGuardar);
        pnlBotones.add(btnEliminar);
        pnlBotones.add(btnCerrarObra);
        pnlBotones.add(btnLimpiar);

        pnlDerecha.add(pnlBotones, BorderLayout.SOUTH);
        splitPane.setRightComponent(pnlDerecha);

        actualizarEstadoBotones();
    }

    private String seleccion() {
        return (String) cboTabla.getSelectedItem();
    }

    private String getEtiqueta(String columna) {
        if (pkActual != null && columna.equalsIgnoreCase(pkActual)) {
            return "ID";
        }
        if (FK_ETIQUETAS.containsKey(columna)) {
            return FK_ETIQUETAS.get(columna);
        }
        if (ETIQUETAS.containsKey(columna)) {
            return ETIQUETAS.get(columna);
        }
        String[] partes = columna.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void actualizarEstadoBotones() {
        boolean haySeleccion = (idSeleccionado != null);
        btnEliminar.setEnabled(haySeleccion);
        boolean esObra = "obra".equalsIgnoreCase(seleccion());
        btnCerrarObra.setVisible(esObra);
        btnCerrarObra.setEnabled(haySeleccion && esObra);
    }

    private record CargaData(List<String> columnas, String pk, List<Map<String, Object>> data, Map<String, List<Map<String, Object>>> fkOptions) {}

    private void cargar() {
        String tabla = seleccion();
        if (tabla == null) {
            return;
        }

        ClsSwingUtil.ejecutarAsync(this, () -> {
            List<String> columnas = service.columnas(tabla);
            String pk = service.clavePrimaria(tabla);
            List<Map<String, Object>> data = service.listar(tabla);

            Map<String, List<Map<String, Object>>> fkOptions = new HashMap<>();
            for (String col : columnas) {
                if (!col.equalsIgnoreCase(pk) && FK_TABLAS.containsKey(col)) {
                    fkOptions.put(col, opService.opciones(FK_TABLAS.get(col)));
                }
            }
            return new CargaData(columnas, pk, data, fkOptions);

        }, res -> {
            pkActual = res.pk();
            idSeleccionado = null;

            listaDatosActual.clear();
            listaDatosActual.addAll(res.data());

            modelo.setRowCount(0);
            modelo.setColumnCount(0);

            for (String col : res.columnas()) {
                modelo.addColumn(getEtiqueta(col));
            }

            for (Map<String, Object> fila : res.data()) {
                Object[] rowData = new Object[res.columnas().size()];
                for (int i = 0; i < res.columnas().size(); i++) {
                    String colName = res.columnas().get(i);
                    Object val = fila.get(colName);

                    if (!colName.equalsIgnoreCase(pkActual) && FK_TABLAS.containsKey(colName) && val != null && res.fkOptions().containsKey(colName)) {
                        String strVal = val.toString();
                        for (Map<String, Object> opMap : res.fkOptions().get(colName)) {
                            Object idVal = opMap.get("id");
                            if (idVal != null && idVal.toString().equals(strVal)) {
                                val = opMap.get("nombre");
                                break;
                            }
                        }
                    }
                    rowData[i] = val;
                }
                modelo.addRow(rowData);
            }

            ClsSwingUtil.autoAjustarColumnas(tablaDatos);

            pnlFormularioContenido.removeAll();
            campos.clear();

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;

            for (String col : res.columnas()) {
                gbc.gridx = 0;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                pnlFormularioContenido.add(new JLabel(getEtiqueta(col) + ":"), gbc);

                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;

                JComponent comp;

                if (col.equalsIgnoreCase(pkActual) || "stock_actual".equalsIgnoreCase(col)) {
                    JTextField txt = new JTextField(20);
                    txt.setEditable(false);
                    txt.setBackground(new Color(235, 235, 235));
                    comp = txt;
                } else if (!col.equalsIgnoreCase(pkActual) && FK_TABLAS.containsKey(col)) {
                    JComboBox<Opcion> cb = new JComboBox<>();
                    List<Map<String, Object>> ops = res.fkOptions().get(col);
                    if (ops != null) {
                        for (Map<String, Object> opMap : ops) {
                            Object idVal = opMap.get("id");
                            Object nomVal = opMap.get("nombre");
                            if (idVal != null && nomVal != null) {
                                cb.addItem(new Opcion(Integer.parseInt(idVal.toString()), nomVal.toString()));
                            }
                        }
                    }
                    comp = cb;
                } else if (col.toLowerCase().contains("fecha")) {
                    JDateChooser dc = new JDateChooser();
                    dc.setDateFormatString("yyyy-MM-dd");
                    if (dc.getDateEditor().getUiComponent() instanceof JTextField tf) {
                        tf.setEditable(false);
                    }
                    comp = dc;
                } else if ("estado".equalsIgnoreCase(col)) {
                    JComboBox<String> cbEstado = new JComboBox<>();
                    if ("obra".equalsIgnoreCase(tabla)) {
                        cbEstado.addItem("Planificada");
                        cbEstado.addItem("En Ejecución");
                        cbEstado.addItem("Pausada");
                        cbEstado.addItem("Finalizada");
                    } else {
                        cbEstado.addItem("ACTIVO");
                        cbEstado.addItem("INACTIVO");
                    }
                    comp = cbEstado;
                } else {
                    comp = new JTextField(20);
                }

                campos.put(col, comp);
                pnlFormularioContenido.add(comp, gbc);

                gbc.gridy++;
            }

            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            pnlFormularioContenido.add(Box.createVerticalGlue(), gbc);

            pnlFormularioContenido.revalidate();
            pnlFormularioContenido.repaint();

            limpiar();

        }, err -> {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(err), "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void seleccionarRegistro() {
        int row = tablaDatos.getSelectedRow();
        if (row < 0 || pkActual == null || row >= listaDatosActual.size()) {
            return;
        }

        int modelRow = tablaDatos.convertRowIndexToModel(row);
        Map<String, Object> fila = listaDatosActual.get(modelRow);

        idSeleccionado = fila.get(pkActual);

        for (String col : campos.keySet()) {
            Object valor = fila.get(col);
            JComponent comp = campos.get(col);

            if (comp instanceof JTextField txt) {
                txt.setText(valor == null ? "" : valor.toString());
            } else if (comp instanceof JDateChooser dc) {
                if (valor instanceof Date d) {
                    dc.setDate(d);
                } else if (valor instanceof java.time.LocalDate ld) {
                    dc.setDate(java.sql.Date.valueOf(ld));
                } else if (valor != null && !valor.toString().trim().isEmpty()) {
                    try {
                        dc.setDate(new SimpleDateFormat("yyyy-MM-dd").parse(valor.toString().trim()));
                    } catch (Exception ex) {
                        dc.setDate(null);
                    }
                } else {
                    dc.setDate(null);
                }
            } else if (comp instanceof JComboBox<?> cb) {
                if (valor != null) {
                    String strVal = valor.toString();
                    for (int j = 0; j < cb.getItemCount(); j++) {
                        Object item = cb.getItemAt(j);
                        if (item instanceof Opcion op) {
                            if (String.valueOf(op.id()).equals(strVal) || op.texto().equalsIgnoreCase(strVal)) {
                                cb.setSelectedIndex(j);
                                break;
                            }
                        } else if (item != null && item.toString().equalsIgnoreCase(strVal)) {
                            cb.setSelectedIndex(j);
                            break;
                        }
                    }
                } else {
                    cb.setSelectedIndex(-1);
                }
            }
        }

        btnGuardar.setEnabled(true);
        actualizarEstadoBotones();
    }

    private void nuevo() {
        tablaDatos.clearSelection();
        idSeleccionado = null;
        limpiarCampos();
        btnGuardar.setEnabled(true);
        actualizarEstadoBotones();

        for (var entry : campos.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(pkActual) && !"stock_actual".equalsIgnoreCase(entry.getKey())) {
                entry.getValue().requestFocus();
                break;
            }
        }
    }

    private void limpiar() {
        tablaDatos.clearSelection();
        idSeleccionado = null;
        limpiarCampos();
        btnGuardar.setEnabled(false);
        actualizarEstadoBotones();
    }

    private void limpiarCampos() {
        for (Map.Entry<String, JComponent> entry : campos.entrySet()) {
            JComponent comp = entry.getValue();
            if (comp instanceof JTextField txt) {
                txt.setText("");
            } else if (comp instanceof JDateChooser dc) {
                dc.setDate(null);
            } else if (comp instanceof JComboBox<?> cb) {
                if (cb.getItemCount() > 0) {
                    cb.setSelectedIndex(0);
                }
            }
        }
    }

    private void guardar() {
        String tabla = seleccion();
        if (tabla == null || pkActual == null) {
            return;
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        Date fechaInicio = null;
        Date fechaFin = null;

        for (var entry : campos.entrySet()) {
            String col = entry.getKey();
            if (col.equalsIgnoreCase(pkActual) || "stock_actual".equalsIgnoreCase(col)) {
                continue;
            }

            JComponent comp = entry.getValue();

            if (comp instanceof JTextField tf) {
                String val = tf.getText().trim();
                boolean esOpcional = COLUMNAS_OPCIONALES.contains(col.toLowerCase());

                if (val.isEmpty()) {
                    if (!esOpcional) {
                        JOptionPane.showMessageDialog(this, "El campo '" + getEtiqueta(col) + "' es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
                        tf.requestFocus();
                        return;
                    }
                    datos.put(col, null);
                } else if (COLUMNAS_NUMERICAS.contains(col.toLowerCase())) {
                    String rawNum = val.replace("S/", "").replace("$", "").replace(" ", "").replace(",", "");
                    try {
                        double numVal = Double.parseDouble(rawNum);
                        datos.put(col, numVal);
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "El campo '" + getEtiqueta(col) + "' debe ser un número válido.", "Validación", JOptionPane.WARNING_MESSAGE);
                        tf.requestFocus();
                        return;
                    }
                } else {
                    datos.put(col, val);
                }
            } else if (comp instanceof JDateChooser dc) {
                Date d = dc.getDate();
                if (d == null) {
                    JOptionPane.showMessageDialog(this, "Debe seleccionar una fecha para '" + getEtiqueta(col) + "'.", "Validación", JOptionPane.WARNING_MESSAGE);
                    dc.requestFocus();
                    return;
                }
                if (col.equalsIgnoreCase("fecha_inicio_plan")) {
                    fechaInicio = d;
                }
                if (col.equalsIgnoreCase("fecha_fin_plan")) {
                    fechaFin = d;
                }
                datos.put(col, new SimpleDateFormat("yyyy-MM-dd").format(d));
            } else if (comp instanceof JComboBox<?> cb) {
                Object sel = cb.getSelectedItem();
                if (sel instanceof Opcion op) {
                    datos.put(col, op.id());
                } else if (sel != null) {
                    datos.put(col, sel.toString());
                } else {
                    datos.put(col, null);
                }
            }
        }

        if (fechaInicio != null && fechaFin != null && fechaFin.before(fechaInicio)) {
            JOptionPane.showMessageDialog(this, "La Fecha de Fin no puede ser anterior a la Fecha de Inicio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object id = idSeleccionado;
        String pk = pkActual;

        ClsSwingUtil.ejecutarVoid(this, () -> {
            service.guardar(tabla, pk, id, datos, rolSesion);
        }, () -> {
            JOptionPane.showMessageDialog(this, id == null ? "Registro creado correctamente." : "Registro actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargar();
        }, err -> {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(err), "Validación", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void eliminar() {
        String tabla = seleccion();
        if (tabla == null || pkActual == null || idSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para dar de baja o desactivar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this, "¿Está seguro de desactivar / dar de baja este registro?", "Confirmar acción", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }

        Object id = idSeleccionado;
        String pk = pkActual;

        ClsSwingUtil.ejecutarVoid(this, () -> {
            service.eliminar(tabla, pk, id, rolSesion);
        }, () -> {
            JOptionPane.showMessageDialog(this, "Registro dado de baja correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargar();
        }, err -> {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(err), "No permitido", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void cerrarObra() {
        if (!"obra".equalsIgnoreCase(seleccion()) || idSeleccionado == null) {
            return;
        }
        int idObra = Integer.parseInt(idSeleccionado.toString());
        int resp = JOptionPane.showConfirmDialog(this, "¿Desea cerrar/finalizar esta obra? Una obra finalizada conserva su historial pero ya no admite nuevos movimientos.", "Cerrar Obra (RF-03)", JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }
        ClsSwingUtil.ejecutarVoid(this, () -> {
            service.cerrarObra(idObra, rolSesion);
        }, () -> {
            JOptionPane.showMessageDialog(this, "La obra fue marcada como Finalizada.", "Obra cerrada", JOptionPane.INFORMATION_MESSAGE);
            cargar();
        }, err -> {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(err), "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private record Opcion(int id, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }
}
