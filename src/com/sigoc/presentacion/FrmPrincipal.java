package com.sigoc.presentacion;

import com.sigoc.config.ClsConstantes;
import com.sigoc.entidad.ClsUsuario;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class FrmPrincipal extends JFrame {

    private static final String[][] PERMISOS = {
        {"OBRAS Y PARTIDAS", "ADMIN", "INGENIERO"}, {"AVANCES", "ADMIN", "INGENIERO"},
        {"MATERIALES", "ADMIN", "ALMACENERO"}, {"PROVEEDORES", "ADMIN", "ALMACENERO"},
        {"COMPRAS", "ADMIN", "ALMACENERO"}, {"CONSUMOS", "ADMIN", "ALMACENERO"},
        {"PERSONAL", "ADMIN", "INGENIERO"}, {"ASISTENCIA", "ADMIN", "INGENIERO"},
        {"FINANZAS", "ADMIN", "CONTADOR"}, {"REPORTES", "ADMIN", "INGENIERO", "CONTADOR"}, {"USUARIOS", "ADMIN"}
    };

    /** Ventanas abiertas: solo una por módulo. */
    private final Map<String, JFrame> ventanasAbiertas = new HashMap<>();

    public FrmPrincipal(ClsUsuario sesion) {
        super(ClsConstantes.NOMBRE_APP);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 580);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(new java.awt.Color(0, 102, 204));
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        JLabel tituloPanel = new JLabel("Panel de Control");
        tituloPanel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        tituloPanel.setForeground(java.awt.Color.WHITE);
        
        JLabel infoSesion = new JLabel(sesion.getUsername() + " · " + sesion.getRol());
        infoSesion.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        infoSesion.setForeground(java.awt.Color.WHITE);
        
        encabezado.add(tituloPanel, BorderLayout.WEST);
        encabezado.add(infoSesion, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);
        JPanel tarjetas = new JPanel(new GridLayout(0, 3, 12, 12));
        tarjetas.setBorder(BorderFactory.createEmptyBorder(12, 18, 18, 18));
        for (String[] permiso : PERMISOS) {
            boolean permitido = false;
            for (int i = 1; i < permiso.length; i++) {
                if (permiso[i].equalsIgnoreCase(sesion.getRol())) permitido = true;
            }
            if (!permitido) continue;
            JButton modulo = new JButton("<html><center><b>" + permiso[0] + "</b><br>Abrir módulo</center></html>");
            modulo.addActionListener(e -> abrir(permiso[0], sesion));
            tarjetas.add(modulo);
        }
        add(new JScrollPane(tarjetas), BorderLayout.CENTER);
        JMenuBar barra = new JMenuBar();
        JMenu cuenta = new JMenu("Sesión");
        JMenuItem cambiar = new JMenuItem("Cambiar contraseña");
        cambiar.addActionListener(e -> cambiarClave(sesion));
        JMenuItem salir = new JMenuItem("Cerrar sesión");
        salir.addActionListener(e -> { dispose(); new FrmLogin().setVisible(true); });
        cuenta.add(cambiar); cuenta.add(salir); barra.add(cuenta); setJMenuBar(barra);
    }

    private void abrir(String modulo, ClsUsuario sesion) {
        // Si ya está abierta y visible, traer al frente
        JFrame existente = ventanasAbiertas.get(modulo);
        if (existente != null && existente.isDisplayable()) {
            if (!existente.isVisible()) existente.setVisible(true);
            existente.toFront();
            existente.requestFocus();
            if (existente.getState() == JFrame.ICONIFIED) existente.setState(JFrame.NORMAL);
            return;
        }

        // Crear nueva ventana según módulo
        JFrame ventana = switch (modulo) {
            case "OBRAS Y PARTIDAS", "MATERIALES", "PROVEEDORES" -> new FrmRegistros(modulo);
            case "REPORTES" -> new FrmReportes();
            case "USUARIOS" -> new FrmUsuarios(sesion.getIdUsuario());
            default -> new FrmOperaciones(modulo, sesion.getIdUsuario());
        };

        // Al cerrar la ventana, eliminar del mapa
        ventana.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { ventanasAbiertas.remove(modulo); }
        });

        ventanasAbiertas.put(modulo, ventana);
        ventana.setVisible(true);
    }

    private void cambiarClave(ClsUsuario sesion) {
        JPasswordField actual = new JPasswordField(), nueva = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Contraseña actual")); p.add(actual);
        p.add(new JLabel("Nueva contraseña (8+ caracteres)")); p.add(nueva);
        if (JOptionPane.showConfirmDialog(this, p, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            new com.sigoc.negocio.ClsUsuarioService().cambiarClave(sesion.getIdUsuario(), new String(actual.getPassword()), new String(nueva.getPassword()));
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cambiar la contraseña", JOptionPane.ERROR_MESSAGE);
        }
    }
}
