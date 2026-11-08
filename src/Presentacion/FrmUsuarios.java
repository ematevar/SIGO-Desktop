package Presentacion;

import config.ClsConstantes;
import Negocio.ClsUsuarioService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Gestión de usuarios del sistema exclusiva para el perfil Administrador (RF-01, RF-02).
 * Permite crear, editar, activar/desactivar lógicamente conservando el historial
 * e impide desactivar al único Administrador activo.
 */
public class FrmUsuarios extends JFrame {

    private record Rol(int id, String nombre) {
        @Override
        public String toString() {
            return ClsConstantes.etiquetaRol(nombre) + " (" + nombre + ")";
        }
    }

    private final int sesion;
    private final String rolSesion;
    private final ClsUsuarioService service = new ClsUsuarioService();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nombre", "Usuario", "Rol", "Activo"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public FrmUsuarios(int sesion) {
        this(sesion, ClsConstantes.ROL_ADMIN);
    }

    public FrmUsuarios(int sesion, String rolSesion) {
        super("Usuarios y seguridad — SIGOC");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.sesion = sesion;
        this.rolSesion = rolSesion;
        setSize(860, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel p = new JPanel(new FlowLayout());
        JButton add = new JButton("Crear usuario");
        JButton edit = new JButton("Editar nombre / rol");
        JButton state = new JButton("Activar / desactivar");
        JButton password = new JButton("Cambiar mi contraseña");
        JButton reload = new JButton("Actualizar");
        add.addActionListener(e -> crear());
        edit.addActionListener(e -> editar());
        state.addActionListener(e -> estado());
        password.addActionListener(e -> clave());
        reload.addActionListener(e -> cargar());
        p.add(add);
        p.add(edit);
        p.add(state);
        p.add(password);
        p.add(reload);
        add(p, BorderLayout.SOUTH);
        cargar();
    }

    private void cargar() {
        ClsSwingUtil.ejecutarAsync(
                this,
                () -> service.listar(rolSesion),
                lista -> {
                    model.setRowCount(0);
                    for (var r : lista) {
                        int act = ((Number) r.get("estado")).intValue();
                        model.addRow(new Object[]{
                            r.get("id_usuario"),
                            r.get("nombre_completo"),
                            r.get("usuario_login"),
                            r.get("nombre_rol"),
                            act == 1 ? "Sí" : "No"
                        });
                    }
                    ClsSwingUtil.autoAjustarColumnas(table);
                },
                this::error
        );
    }

    private void crear() {
        ClsSwingUtil.ejecutarAsync(
                this,
                () -> service.roles(),
                roles -> {
                    JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
                    JTextField nombre = new JTextField();
                    JTextField login = new JTextField();
                    JPasswordField pass = new JPasswordField();
                    JComboBox<Rol> role = new JComboBox<>();
                    for (var r : roles) {
                        role.addItem(new Rol(((Number) r.get("id")).intValue(), r.get("nombre").toString()));
                    }
                    p.add(new JLabel("Nombre completo"));
                    p.add(nombre);
                    p.add(new JLabel("Usuario"));
                    p.add(login);
                    p.add(new JLabel("Contraseña inicial (8+ caracteres)"));
                    p.add(pass);
                    p.add(new JLabel("Rol"));
                    p.add(role);
                    if (JOptionPane.showConfirmDialog(this, p, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    String nomVal = nombre.getText();
                    String logVal = login.getText();
                    String pwdVal = new String(pass.getPassword());
                    Rol selected = (Rol) role.getSelectedItem();
                    int idRol = selected == null ? 0 : selected.id();
                    ClsSwingUtil.ejecutarVoid(
                            this,
                            () -> service.crear(nomVal, logVal, pwdVal, idRol, rolSesion),
                            this::cargar,
                            this::error
                    );
                },
                this::error
        );
    }

    private void estado() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        int id = ((Number) model.getValueAt(r, 0)).intValue();
        if (id == sesion) {
            JOptionPane.showMessageDialog(this, "No puedes desactivar tu propia sesión activa.");
            return;
        }
        boolean active = "Sí".equals(model.getValueAt(r, 4));
        ClsSwingUtil.ejecutarVoid(
                this,
                () -> service.cambiarEstado(id, !active, rolSesion),
                this::cargar,
                this::error
        );
    }

    private void editar() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        int id = ((Number) model.getValueAt(r, 0)).intValue();
        String currentName = model.getValueAt(r, 1).toString();
        String currentRole = model.getValueAt(r, 3) != null ? model.getValueAt(r, 3).toString() : "";

        ClsSwingUtil.ejecutarAsync(
                this,
                () -> service.roles(),
                roles -> {
                    JTextField nombre = new JTextField(currentName);
                    JComboBox<Rol> role = new JComboBox<>();
                    for (var x : roles) {
                        Rol rol = new Rol(((Number) x.get("id")).intValue(), x.get("nombre").toString());
                        role.addItem(rol);
                        if (rol.nombre().equalsIgnoreCase(currentRole)) {
                            role.setSelectedItem(rol);
                        }
                    }
                    JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
                    p.add(new JLabel("Nombre completo"));
                    p.add(nombre);
                    p.add(new JLabel("Rol"));
                    p.add(role);
                    if (JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    String nomVal = nombre.getText();
                    Rol selected = (Rol) role.getSelectedItem();
                    int idRol = selected == null ? 0 : selected.id();
                    ClsSwingUtil.ejecutarVoid(
                            this,
                            () -> service.actualizarDatos(id, nomVal, idRol, rolSesion),
                            this::cargar,
                            this::error
                    );
                },
                this::error
        );
    }

    private void clave() {
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
            service.cambiarClave(sesion, pwdAct, pwdNueva);
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(e), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
