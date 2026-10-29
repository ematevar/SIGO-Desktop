package com.sigoc.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OperacionRepositoryPort {
    List<Map<String, Object>> opciones(String tabla) throws Exception;
    void registrarCompra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio) throws Exception;
    void registrarAsistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) throws Exception;
    void registrarProduccion(int empleado, int partida, LocalDate fecha, double cantidad) throws Exception;
    void registrarIngreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception;
    void registrarEgreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception;
    double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario) throws Exception;
}
