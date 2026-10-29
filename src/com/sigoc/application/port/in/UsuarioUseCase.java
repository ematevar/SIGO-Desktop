package com.sigoc.application.port.in;

import com.sigoc.domain.ClsUsuario;
import java.util.List;
import java.util.Map;

public interface UsuarioUseCase {
    String nuevoCaptcha();
    List<Map<String, Object>> listar() throws Exception;
    List<Map<String, Object>> roles() throws Exception;
    void crear(String nombre, String usuario, String clave, int rol) throws Exception;
    void cambiarEstado(int usuario, boolean activo) throws Exception;
    void actualizarDatos(int usuario, String nombre, int rol) throws Exception;
    void cambiarClave(int usuario, String actual, String nueva) throws Exception;
    ClsUsuario iniciarSesion(String usuario, String clave, String captchaEsperado, String captchaIngresado) throws Exception;
}
