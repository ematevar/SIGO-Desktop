package com.sigoc.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OperacionUseCase {
    List<Map<String, Object>> opciones(String tabla) throws Exception;
    void compra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio) throws Exception;
    void asistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) throws Exception;
    void produccion(int empleado, int partida, LocalDate fecha, double cantidad) throws Exception;
    void ingreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception;
    void egreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception;
    double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario) throws Exception;
}
