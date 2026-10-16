package com.sigoc.presentacion;

import com.sigoc.negocio.ClsUsuarioService;
import com.sigoc.presentacion.ClsSwingUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FrmUsuarios extends JFrame {

    private record Rol(int id, String nombre) {

        @Override
        public String toString() {
            return nombre;
        }
    }
    private final int sesion;
    private final ClsUsuarioService service = new ClsUsuarioService();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nombre", "Usuario", "Rol", "Activo"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public FrmUsuarios(int sesion) {
        super("Usuarios y seguridad");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.sesion = sesion;
        setSize(850, 440);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel p = new JPanel(new FlowLayout());
        JButton add = new JButton("Crear usuario"), edit = new JButton("Editar nombre / rol"), state = new JButton("Activar / desactivar"), password = new JButton("Cambiar mi contraseña"), reload = new JButton("Actualizar");
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
        ClsSwingUtil.ejecutarAsync(this,
            () -> service.listar(),
            lista -> {
                model.setRowCount(0);
                for (var r : lista) {
                    model.addRow(new Object[]{r.get("id_usuario"), r.get("nombre_completo"), r.get("usuario_login"), r.get("nombre_rol"), ((Number) r.get("estado")).intValue() == 1 ? "Sí" : "No"});
                }
            },
            this::error
        );
    }

    private void crear() {
        ClsSwingUtil.ejecutarAsync(this,
            () -> service.roles(),
            roles -> {
                JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
                JTextField nombre = new JTextField(), login = new JTextField();
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
                Rol selected = (Rol) role.getSelectedItem();
                ClsSwingUtil.ejecutarVoid(this,
                    () -> service.crear(nombre.getText(), login.getText(), new String(pass.getPassword()), selected == null ? 0 : selected.id()),
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
            JOptionPane.showMessageDialog(this, "No puedes desactivar tu propia sesión.");
            return;
        }
        boolean active = "Sí".equals(model.getValueAt(r, 4));
        ClsSwingUtil.ejecutarVoid(this,
            () -> service.cambiarEstado(id, !active),
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
        
        ClsSwingUtil.ejecutarAsync(this,
            () -> service.roles(),
            roles -> {
                JTextField nombre = new JTextField(currentName);
                JComboBox<Rol> role = new JComboBox<>();
                for (var x : roles) {
                    Rol rol = new Rol(((Number) x.get("id")).intValue(), x.get("nombre").toString());
                    role.addItem(rol);
                    if (rol.nombre().equals(currentRole)) {
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
                Rol selected = (Rol) role.getSelectedItem();
                ClsSwingUtil.ejecutarVoid(this,
                    () -> service.actualizarDatos(id, nombre.getText(), selected == null ? 0 : selected.id()),
                    this::cargar,
                    this::error
                );
            },
            this::error
        );
    }

    private void clave() {
        JPasswordField actual = new JPasswordField(), nueva = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Contraseña actual"));
        p.add(actual);
        p.add(new JLabel("Nueva contraseña (8+ caracteres)"));
        p.add(nueva);
        if (JOptionPane.showConfirmDialog(this, p, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            service.cambiarClave(sesion, new String(actual.getPassword()), new String(nueva.getPassword()));
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage() == null ? "No se pudo completar la operación." : e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
