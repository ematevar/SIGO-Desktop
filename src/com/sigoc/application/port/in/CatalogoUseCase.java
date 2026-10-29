package com.sigoc.application.port.in;

import java.util.List;
import java.util.Map;

public interface CatalogoUseCase {
    List<String> columnas(String tabla) throws Exception;
    List<Map<String, Object>> listar(String tabla) throws Exception;
    String clavePrimaria(String tabla) throws Exception;
    void guardar(String tabla, String pk, Object id, Map<String, Object> datos) throws Exception;
    void eliminar(String tabla, String pk, Object id) throws Exception;
}
