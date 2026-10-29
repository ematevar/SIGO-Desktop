package com.sigoc.application.port.out;

import com.sigoc.domain.ClsUsuario;
import java.util.List;
import java.util.Map;

public interface UsuarioRepositoryPort {
    List<Map<String, Object>> listar() throws Exception;
    List<Map<String, Object>> roles() throws Exception;
    void crear(String nombre, String usuario, String hashClave, int rol) throws Exception;
    void estado(int usuario, boolean activo) throws Exception;
    void actualizarDatos(int usuario, String nombre, int rol) throws Exception;
    boolean autenticarPorId(int usuario, String actual) throws Exception;
    void actualizarClave(int usuario, String hashNueva) throws Exception;
    ClsUsuario autenticar(String usuario, String clave) throws Exception;
}
