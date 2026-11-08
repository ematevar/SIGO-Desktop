package Presentacion;

import config.ClsConstantes;
import Entidad.ClsUsuario;
import Negocio.ClsMaterialService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * Panel principal de control que restringe los módulos según los 3 perfiles del SRS
 * (Administrador, Residente de Obra, Almacenero), muestra alertas de stock mínimo al Almacenero
 * y cierra todas las ventanas abiertas al cerrar sesión (RF-01, RF-06, RF-08, RF-10).
 */
public class FrmPrincipal extends JFrame {

    private static final String[][] PERMISOS = {
        {"OBRAS", ClsConstantes.ROL_ADMIN},
        {"PARTIDAS", ClsConstantes.ROL_RESIDENTE, ClsConstantes.ROL_ADMIN},
        {"AVANCES", ClsConstantes.ROL_RESIDENTE},
        {"MATERIALES", ClsConstantes.ROL_ALMACENERO, ClsConstantes.ROL_ADMIN},
        {"PROVEEDORES", ClsConstantes.ROL_ADMIN},
        {"COMPRAS", ClsConstantes.ROL_ALMACENERO},
        {"CONSUMOS", ClsConstantes.ROL_RESIDENTE},
        {"PERSONAL", ClsConstantes.ROL_ADMIN, ClsConstantes.ROL_RESIDENTE},
        {"ASISTENCIA", ClsConstantes.ROL_RESIDENTE},
        {"PRODUCCION", ClsConstantes.ROL_RESIDENTE},
        {"FINANZAS", ClsConstantes.ROL_ADMIN},
        {"REPORTES", ClsConstantes.ROL_ADMIN},
        {"USUARIOS", ClsConstantes.ROL_ADMIN}
    };

    /** Ventanas abiertas: solo una por módulo. */
    private final Map<String, JFrame> ventanasAbiertas = new HashMap<>();
    private final ClsUsuario sesion;
    private final JLabel lblAlertasStock = new JLabel();

    public FrmPrincipal(ClsUsuario sesion) {
        super(ClsConstantes.NOMBRE_APP);
        this.sesion = sesion;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(new Color(0, 102, 204));
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel tituloPanel = new JLabel("Panel de Control — SIGOC");
        tituloPanel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        tituloPanel.setForeground(Color.WHITE);

        String etiquetaRol = ClsConstantes.etiquetaRol(sesion.getRol());
        JLabel infoSesion = new JLabel(sesion.getUsername() + " · " + etiquetaRol);
        infoSesion.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        infoSesion.setForeground(Color.WHITE);

        encabezado.add(tituloPanel, BorderLayout.WEST);
        encabezado.add(infoSesion, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        String rolNorm = ClsConstantes.normalizarRol(sesion.getRol());
        JPanel tarjetas = new JPanel(new GridLayout(0, 3, 12, 12));
        tarjetas.setBorder(BorderFactory.createEmptyBorder(12, 18, 18, 18));
        for (String[] permiso : PERMISOS) {
            boolean permitido = false;
            for (int i = 1; i < permiso.length; i++) {
                if (permiso[i].equalsIgnoreCase(rolNorm)) {
                    permitido = true;
                    break;
                }
            }
            if (!permitido) {
                continue;
            }
            String nombreModulo = permiso[0];
            JButton modulo = new JButton("<html><center><b>" + nombreModulo + "</b><br>Abrir módulo</center></html>");
            modulo.addActionListener(e -> abrir(nombreModulo));
            tarjetas.add(modulo);
        }
        add(new JScrollPane(tarjetas), BorderLayout.CENTER);

        if (ClsConstantes.ROL_ALMACENERO.equals(rolNorm) || ClsConstantes.ROL_ADMIN.equals(rolNorm)) {
            JPanel pnlSur = new JPanel(new BorderLayout(8, 4));
            pnlSur.setBorder(BorderFactory.createEmptyBorder(4, 18, 12, 18));
            lblAlertasStock.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
            lblAlertasStock.setForeground(new Color(180, 60, 0));
            JButton btnVerAlertas = new JButton("Actualizar alertas de stock mínimo");
            btnVerAlertas.addActionListener(e -> cargarAlertasStock(true));
            pnlSur.add(lblAlertasStock, BorderLayout.CENTER);
            pnlSur.add(btnVerAlertas, BorderLayout.EAST);
            add(pnlSur, BorderLayout.SOUTH);
            cargarAlertasStock(false);
        }

        JMenuBar barra = new JMenuBar();
        JMenu cuenta = new JMenu("Sesión");
        JMenuItem cambiar = new JMenuItem("Cambiar contraseña");
        cambiar.addActionListener(e -> cambiarClave());
        JMenuItem salir = new JMenuItem("Cerrar sesión");
        salir.addActionListener(e -> cerrarSesion());
        cuenta.add(cambiar);
        cuenta.add(salir);
        barra.add(cuenta);
        setJMenuBar(barra);
    }

    /**
     * Cierra todas las ventanas de módulos abiertas y retorna al formulario de inicio de sesión (RF-01).
     */
    public void cerrarSesion() {
        List<JFrame> abiertas = new ArrayList<>(ventanasAbiertas.values());
        ventanasAbiertas.clear();
        for (JFrame f : abiertas) {
            if (f != null) {
                f.dispose();
            }
        }
        dispose();
        new FrmLogin().setVisible(true);
    }

    private void cargarAlertasStock(boolean mostrarDialogo) {
        ClsSwingUtil.ejecutarAsync(
                this,
                () -> new ClsMaterialService().listarAlertasStockMinimo(),
                alertas -> {
                    if (alertas.isEmpty()) {
                        lblAlertasStock.setForeground(new Color(30, 130, 50));
                        lblAlertasStock.setText("Inventario: Todos los materiales activos superan el stock mínimo.");
                        if (mostrarDialogo) {
                            JOptionPane.showMessageDialog(this, "No hay materiales en nivel de stock mínimo.", "Alertas de stock", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < alertas.size(); i++) {
                            var m = alertas.get(i);
                            if (i > 0) {
                                sb.append(" | ");
                            }
                            sb.append(m.get("nombre")).append(" (Actual: ").append(m.get("stock_actual"))
                              .append(" ").append(m.get("unidad_medida")).append(" / Mín: ").append(m.get("stock_minimo")).append(")");
                        }
                        lblAlertasStock.setForeground(new Color(180, 40, 20));
                        lblAlertasStock.setText("ALERTA DE STOCK MÍNIMO (" + alertas.size() + "): " + sb);
                        if (mostrarDialogo) {
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Materiales con stock igual o inferior al mínimo:\n\n" + sb.toString().replace(" | ", "\n• "),
                                    "Alerta de Stock Mínimo (RF-08)",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                    }
                },
                ex -> lblAlertasStock.setText("No se pudieron consultar las alertas de stock.")
        );
    }

    private void abrir(String modulo) {
        JFrame existente = ventanasAbiertas.get(modulo);
        if (existente != null && existente.isDisplayable()) {
            if (!existente.isVisible()) {
                existente.setVisible(true);
            }
            existente.toFront();
            existente.requestFocus();
            if (existente.getState() == JFrame.ICONIFIED) {
                existente.setState(JFrame.NORMAL);
            }
            return;
        }

        JFrame ventana = switch (modulo) {
            case "OBRAS", "PARTIDAS", "OBRAS Y PARTIDAS", "MATERIALES", "PROVEEDORES", "PERSONAL" ->
                new FrmRegistros(modulo, sesion.getRol());
            case "REPORTES" ->
                new FrmReportes(sesion.getRol());
            case "USUARIOS" ->
                new FrmUsuarios(sesion.getIdUsuario(), sesion.getRol());
            default ->
                new FrmOperaciones(modulo, sesion.getIdUsuario(), sesion.getRol());
        };

        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ventanasAbiertas.remove(modulo);
            }
        });

        ventanasAbiertas.put(modulo, ventana);
        ventana.setVisible(true);
    }

    private void cambiarClave() {
        JPasswordField actual = new JPasswordField();
        JPasswordField nueva = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Contraseña actual"));
        p.add(actual);
        p.add(new JLabel("Nueva contraseña (8+ caracteres)"));
        p.add(nueva);
        if (JOptionPane.showConfirmDialog(this, p, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        String pwdAct = new String(actual.getPassword());
        String pwdNueva = new String(nueva.getPassword());
        try {
            new Negocio.ClsUsuarioService().cambiarClave(sesion.getIdUsuario(), pwdAct, pwdNueva);
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(ex), "No se pudo cambiar la contraseña", JOptionPane.ERROR_MESSAGE);
        }
    }
}
