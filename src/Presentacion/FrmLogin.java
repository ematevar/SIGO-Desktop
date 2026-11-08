package Presentacion;

import config.ClsConstantes;
import Entidad.ClsUsuario;
import Negocio.ClsUsuarioService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.*;

/**
 * Ventana de autenticación con validación de credenciales PBKDF2, captcha regenerado
 * en cada intento fallido y manejo seguro de mensajes de error (RF-01, RNF-08).
 */
public class FrmLogin extends JFrame {

    private final ClsUsuarioService service = new ClsUsuarioService();
    private final JTextField usuario = new JTextField(18);
    private final JPasswordField clave = new JPasswordField(18);
    private final JTextField captchaEntrada = new JTextField(8);
    private final JLabel captcha = new JLabel(" ");
    private String codigoCaptcha = service.nuevoCaptcha();

    public FrmLogin() {
        super("Acceso — " + ClsConstantes.NOMBRE_APP);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 330);
        setLocationRelativeTo(null);
        JPanel formulario = new JPanel(new GridLayout(0, 2, 10, 12));
        formulario.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        formulario.add(new JLabel("Usuario"));
        formulario.add(usuario);
        formulario.add(new JLabel("Contraseña"));
        formulario.add(clave);

        JPanel pnlCaptchaTxt = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlCaptchaTxt.add(new JLabel("Código:  "));
        pnlCaptchaTxt.add(captcha);
        formulario.add(pnlCaptchaTxt);

        JPanel cp = new JPanel(new BorderLayout(8, 0));
        cp.add(captchaEntrada, BorderLayout.CENTER);
        JButton renovar = new JButton("Nuevo");
        renovar.addActionListener(e -> renovarCaptcha());
        cp.add(renovar, BorderLayout.EAST);
        formulario.add(cp);
        JButton entrar = new JButton("Iniciar sesión");
        entrar.addActionListener(e -> iniciar());
        getRootPane().setDefaultButton(entrar);
        formulario.add(new JLabel(""));
        formulario.add(entrar);

        JLabel lblTitulo = new JLabel("SIGOC Desktop", SwingConstants.CENTER);
        lblTitulo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
        lblTitulo.setForeground(new java.awt.Color(0, 120, 215));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        add(lblTitulo, BorderLayout.NORTH);
        add(formulario, BorderLayout.CENTER);
        renovarCaptcha();
    }

    private void renovarCaptcha() {
        codigoCaptcha = service.nuevoCaptcha();
        captcha.setText(codigoCaptcha);
        captcha.setFont(captcha.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        captchaEntrada.setText("");
    }

    private void iniciar() {
        String usr = usuario.getText();
        String pwd = new String(clave.getPassword());
        String cap = captchaEntrada.getText();
        try {
            ClsUsuario sesion = service.iniciarSesion(usr, pwd, codigoCaptcha, cap);
            if (sesion.isRequiereCambioClave()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Por seguridad, debes cambiar la contraseña inicial de la cuenta antes de continuar.",
                        "Cambio de contraseña requerido",
                        JOptionPane.INFORMATION_MESSAGE
                );
                if (!solicitarCambioInicial(sesion.getIdUsuario(), pwd)) {
                    renovarCaptcha();
                    return;
                }
            }
            dispose();
            new FrmPrincipal(sesion).setVisible(true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Verifica tus datos", JOptionPane.WARNING_MESSAGE);
            renovarCaptcha();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ClsConstantes.mensajeUsuario(ex),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
            renovarCaptcha();
        }
    }

    private boolean solicitarCambioInicial(int idUsuario, String claveActual) {
        while (true) {
            JPasswordField nueva = new JPasswordField(18);
            JPasswordField confirmar = new JPasswordField(18);
            JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
            p.add(new JLabel("Nueva contraseña (8+ caracteres):"));
            p.add(nueva);
            p.add(new JLabel("Confirmar nueva contraseña:"));
            p.add(confirmar);
            int op = JOptionPane.showConfirmDialog(this, p, "Actualizar contraseña inicial", JOptionPane.OK_CANCEL_OPTION);
            if (op != JOptionPane.OK_OPTION) {
                return false;
            }
            String n1 = new String(nueva.getPassword());
            String n2 = new String(confirmar.getPassword());
            if (!n1.equals(n2)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Validación", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            try {
                service.cambiarClave(idUsuario, claveActual, n1);
                JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ClsConstantes.mensajeUsuario(ex), "Validación", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
