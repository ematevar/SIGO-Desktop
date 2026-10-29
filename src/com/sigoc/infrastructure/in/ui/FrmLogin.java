package com.sigoc.infrastructure.in.ui;

import com.sigoc.infrastructure.config.ClsConexionBD;
import com.sigoc.infrastructure.config.ClsConstantes;
import com.sigoc.domain.ClsUsuario;
import com.sigoc.application.service.ClsUsuarioService;
import com.sigoc.application.port.in.UsuarioUseCase;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.*;

public class FrmLogin extends JFrame {

    private final UsuarioUseCase service = new ClsUsuarioService(new com.sigoc.infrastructure.out.persistence.ClsUsuarioDAO(), new com.sigoc.application.service.ClsCaptchaService());
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
        formulario.add(new JLabel(""));
        formulario.add(entrar);
        
        JLabel lblTitulo = new JLabel("SIGOC Desktop", SwingConstants.CENTER);
        lblTitulo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
        lblTitulo.setForeground(new java.awt.Color(0, 120, 215)); // Azul moderno
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
        try {
            ClsUsuario sesion = service.iniciarSesion(usuario.getText(), new String(clave.getPassword()), codigoCaptcha, captchaEntrada.getText());
            dispose();
            new FrmPrincipal(sesion).setVisible(true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Verifica tus datos", JOptionPane.WARNING_MESSAGE);
            renovarCaptcha();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos. Revisa config.properties y la conexión del proveedor.\n" + ex.getMessage(), "Error de conexión", JOptionPane.ERROR_MESSAGE);
            renovarCaptcha();
        }
    }
}
