package Negocio;

import config.ClsConstantes;
import NegocioDAO.ClsOperacionDAO;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para operaciones de almacén (compras), personal (asistencia y producción)
 * y finanzas (ingresos, egresos y cierre de planilla) (RF-07, RF-11, RF-12, RF-13, RF-14, RF-15).
 */
public class ClsOperacionService {

    private final ClsOperacionDAO dao;

    public ClsOperacionService() {
        this(new ClsOperacionDAO());
    }

    public ClsOperacionService(ClsOperacionDAO dao) {
        this.dao = dao;
    }

    /**
     * Obtiene las opciones activas ({@code id}, {@code nombre}) de una tabla maestra para combos.
     *
     * @param tabla Nombre de la tabla.
     * @return Lista de opciones.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> opciones(String tabla) throws Exception {
        return dao.opciones(tabla);
    }

    /**
     * Lista el historial de registros activos de una tabla de operaciones a través de la capa DAO (RNF-05).
     *
     * @param tabla Nombre de la tabla de operación.
     * @return Mapa con {@code columnas} y {@code filas}.
     * @throws Exception Si ocurre un error SQL.
     */
    public Map<String, Object> listarHistorial(String tabla) throws Exception {
        return dao.listarHistorial(tabla);
    }

    /**
     * Registra una compra de material actualizando stock y creando el egreso tipo {@code Material} (RF-07).
     *
     * @param proveedor ID del proveedor.
     * @param obra ID de la obra.
     * @param material ID del material.
     * @param tipoComprobante Factura o Boleta.
     * @param comprobante Número de comprobante.
     * @param fecha Fecha de la compra.
     * @param cantidad Cantidad comprada (> 0).
     * @param precio Precio unitario (> 0).
     * @throws Exception Si los datos son inválidos.
     */
    public void compra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio) throws Exception {
        compra(proveedor, obra, material, tipoComprobante, comprobante, fecha, cantidad, precio, null);
    }

    /**
     * Registra una compra validando el rol de sesión (Almacenero o Administrador, RF-07).
     *
     * @param proveedor ID del proveedor.
     * @param obra ID de la obra.
     * @param material ID del material.
     * @param tipoComprobante Factura o Boleta.
     * @param comprobante Número de comprobante.
     * @param fecha Fecha de la compra.
     * @param cantidad Cantidad comprada (> 0).
     * @param precio Precio unitario (> 0).
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o los datos son inválidos.
     */
    public void compra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ALMACENERO, ClsConstantes.ROL_ADMIN);
        if (proveedor <= 0 || obra <= 0 || material <= 0) {
            throw new IllegalArgumentException("Selecciona proveedor, obra y material.");
        }
        if (tipoComprobante == null || (!tipoComprobante.equals("Boleta") && !tipoComprobante.equals("Factura"))) {
            throw new IllegalArgumentException("El tipo de comprobante debe ser Boleta o Factura.");
        }
        if (comprobante == null || comprobante.isBlank()) {
            throw new IllegalArgumentException("Ingresa el número de comprobante.");
        }
        if (fecha == null || fecha.isAfter(LocalDate.now()) || cantidad <= 0 || precio <= 0 || !Double.isFinite(cantidad * precio)) {
            throw new IllegalArgumentException("Fecha, cantidad o precio no válidos.");
        }
        dao.registrarCompra(proveedor, obra, material, tipoComprobante, comprobante.trim(), fecha, cantidad, precio);
    }

    /**
     * Registra la asistencia diaria de un trabajador verificando que en estado FALTA o PERMISO
     * no se guarden horas de entrada ni salida, y que en PRESENTE/TARDANZA la salida sea posterior
     * a la entrada (RF-11).
     *
     * @param empleado ID del trabajador.
     * @param obra ID de la obra.
     * @param fecha Fecha de asistencia.
     * @param entrada Hora de entrada.
     * @param salida Hora de salida.
     * @param estado Estado de asistencia.
     * @throws Exception Si los datos son inválidos o ya existe asistencia ese día.
     */
    public void asistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) throws Exception {
        asistencia(empleado, obra, fecha, entrada, salida, estado, null);
    }

    /**
     * Registra la asistencia diaria validando el rol de sesión (Residente de Obra, RF-11).
     *
     * @param empleado ID del trabajador.
     * @param obra ID de la obra.
     * @param fecha Fecha de asistencia.
     * @param entrada Hora de entrada.
     * @param salida Hora de salida.
     * @param estado Estado de asistencia.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o los datos son inválidos.
     */
    public void asistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_RESIDENTE);
        if (empleado <= 0 || obra <= 0 || fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Empleado, obra o fecha inválidos.");
        }
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("Selecciona un estado de asistencia.");
        }
        String estNorm = estado.trim().toUpperCase();
        String entLimpia = entrada == null ? "" : entrada.trim();
        String salLimpia = salida == null ? "" : salida.trim();

        if (estNorm.equals("FALTA") || estNorm.equals("PERMISO")) {
            entLimpia = "";
            salLimpia = "";
        } else if (estNorm.equals("PRESENTE") || estNorm.equals("TARDANZA")) {
            if (entLimpia.isBlank()) {
                throw new IllegalArgumentException("Registra la hora de entrada para el estado " + estNorm + ".");
            }
            try {
                LocalTime tEnt = LocalTime.parse(entLimpia);
                if (!salLimpia.isBlank()) {
                    LocalTime tSal = LocalTime.parse(salLimpia);
                    if (!tSal.isAfter(tEnt)) {
                        throw new IllegalArgumentException("La hora de salida debe ser posterior a la hora de entrada (RF-11).");
                    }
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Usa el formato de hora HH:MM:SS.");
            }
        } else {
            throw new IllegalArgumentException("Estado de asistencia no reconocido: " + estado);
        }
        dao.registrarAsistencia(empleado, obra, fecha, entLimpia, salLimpia, estNorm);
    }

    /**
     * Registra la producción de un trabajador en una partida activa verificando asistencia ese mismo día (RF-12).
     *
     * @param empleado ID del trabajador.
     * @param partida ID de la partida.
     * @param fecha Fecha de producción.
     * @param cantidad Cantidad producida (> 0).
     * @throws Exception Si el trabajador no asistió ese día a la obra o los datos son inválidos.
     */
    public void produccion(int empleado, int partida, LocalDate fecha, double cantidad) throws Exception {
        produccion(empleado, partida, fecha, cantidad, null);
    }

    /**
     * Registra la producción de un trabajador validando el rol de sesión (Residente de Obra, RF-12).
     *
     * @param empleado ID del trabajador.
     * @param partida ID de la partida.
     * @param fecha Fecha de producción.
     * @param cantidad Cantidad producida (> 0).
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o los datos son inválidos.
     */
    public void produccion(int empleado, int partida, LocalDate fecha, double cantidad, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_RESIDENTE);
        if (empleado <= 0 || partida <= 0 || fecha == null || fecha.isAfter(LocalDate.now()) || cantidad <= 0 || !Double.isFinite(cantidad)) {
            throw new IllegalArgumentException("Datos de producción inválidos.");
        }
        dao.registrarProduccion(empleado, partida, fecha, cantidad);
    }

    /**
     * Registra un ingreso financiero dentro del periodo de ejecución de una obra activa (RF-13).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del ingreso.
     * @param concepto Concepto o descripción.
     * @param monto Monto positivo (> 0).
     * @throws Exception Si los datos son inválidos o fuera del periodo de la obra.
     */
    public void ingreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception {
        ingreso(obra, fecha, concepto, monto, null);
    }

    /**
     * Registra un ingreso financiero validando el rol de sesión (Administrador, RF-13).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del ingreso.
     * @param concepto Concepto o descripción.
     * @param monto Monto positivo (> 0).
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o los datos son inválidos.
     */
    public void ingreso(int obra, LocalDate fecha, String concepto, double monto, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        validarFinanzas(obra, fecha, concepto, monto);
        dao.registrarIngreso(obra, fecha, concepto.trim(), monto);
    }

    /**
     * Registra un egreso manual tipo {@code Otro} con concepto obligatorio (RF-15).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del egreso.
     * @param concepto Motivo/concepto obligatorio.
     * @param monto Monto positivo (> 0).
     * @throws Exception Si los datos son inválidos.
     */
    public void egreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception {
        egreso(obra, fecha, concepto, monto, null);
    }

    /**
     * Registra un egreso manual tipo {@code Otro} validando que sea ejecutado exclusivamente por el Administrador (RF-15).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del egreso.
     * @param concepto Motivo/concepto obligatorio.
     * @param monto Monto positivo (> 0).
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no es Administrador o falta el motivo.
     */
    public void egreso(int obra, LocalDate fecha, String concepto, double monto, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        validarFinanzas(obra, fecha, concepto, monto);
        dao.registrarEgreso(obra, fecha, concepto.trim(), monto);
    }

    private void validarFinanzas(int obra, LocalDate fecha, String concepto, double monto) {
        if (obra <= 0 || fecha == null || fecha.isAfter(LocalDate.now()) || concepto == null || concepto.isBlank() || monto <= 0 || !Double.isFinite(monto)) {
            throw new IllegalArgumentException("Completa obra, fecha válida, concepto obligatorio y un monto mayor que cero.");
        }
    }

    /**
     * Genera y cierra la planilla de un periodo para una obra (RF-14).
     *
     * @param obra ID de la obra.
     * @param desde Fecha de inicio del periodo.
     * @param hasta Fecha de fin del periodo.
     * @param usuario ID del usuario Administrador.
     * @return Monto total cerrado.
     * @throws Exception Si el periodo es inválido o se superpone con un cierre previo.
     */
    public double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario) throws Exception {
        return cerrarPlanilla(obra, desde, hasta, usuario, null);
    }

    /**
     * Genera y cierra la planilla validando que sea ejecutada exclusivamente por el Administrador (RF-14).
     *
     * @param obra ID de la obra.
     * @param desde Fecha de inicio del periodo.
     * @param hasta Fecha de fin del periodo.
     * @param usuario ID del usuario Administrador.
     * @param rolSesion Rol del usuario en sesión.
     * @return Monto total cerrado.
     * @throws Exception Si no es Administrador o el periodo es inválido.
     */
    public double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (obra <= 0 || usuario <= 0 || desde == null || hasta == null || hasta.isBefore(desde) || hasta.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Selecciona una obra y un rango de fechas válido (inicio <= fin y no futuro).");
        }
        return dao.cerrarPlanilla(obra, desde, hasta, usuario);
    }
}
