package Negocio;

import config.ClsConstantes;
import NegocioDAO.ClsCatalogoDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para la gestión de catálogos maestros: Obras (RF-03), Partidas (RF-04),
 * Materiales (RF-06), Proveedores (RF-09) y Trabajadores/Empleados (RF-10).
 */
public class ClsCatalogoService {

    private final ClsCatalogoDAO dao;

    public ClsCatalogoService() {
        this(new ClsCatalogoDAO());
    }

    public ClsCatalogoService(ClsCatalogoDAO dao) {
        this.dao = dao;
    }

    /**
     * Obtiene las columnas visibles de una tabla de catálogo.
     *
     * @param tabla Nombre de la tabla.
     * @return Lista de nombres de columna.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<String> columnas(String tabla) throws Exception {
        return dao.columnas(tabla);
    }

    /**
     * Lista los registros activos de la tabla indicada.
     *
     * @param tabla Nombre de la tabla.
     * @return Lista de registros activos.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listar(String tabla) throws Exception {
        return dao.listar(tabla);
    }

    /**
     * Obtiene el nombre de la clave primaria de la tabla.
     *
     * @param tabla Nombre de la tabla.
     * @return Nombre de la columna PK.
     * @throws Exception Si ocurre un error SQL.
     */
    public String clavePrimaria(String tabla) throws Exception {
        return dao.clavePrimaria(tabla);
    }

    /**
     * Guarda o actualiza un registro de catálogo verificando todas las reglas de negocio del SRS.
     *
     * @param tabla Nombre de la tabla.
     * @param pk Nombre de la clave primaria.
     * @param id Valor del ID ({@code null} para crear).
     * @param datos Mapa de valores a guardar.
     * @throws Exception Si alguna validación de negocio falla.
     */
    public void guardar(String tabla, String pk, Object id, Map<String, Object> datos) throws Exception {
        guardar(tabla, pk, id, datos, null);
    }

    /**
     * Guarda o actualiza un registro verificando el rol en sesión y las reglas de negocio (RF-01, RF-03, RF-04, RF-06, RF-09, RF-10).
     *
     * @param tabla Nombre de la tabla.
     * @param pk Nombre de la clave primaria.
     * @param id Valor del ID ({@code null} para crear).
     * @param datos Mapa de valores a guardar.
     * @param rolSesion Rol del usuario autenticado.
     * @throws Exception Si no tiene permisos o falla una regla de negocio.
     */
    public void guardar(String tabla, String pk, Object id, Map<String, Object> datos, String rolSesion) throws Exception {
        verificarPermisoCatalogo(tabla, rolSesion);
        if (datos == null || datos.isEmpty()) {
            throw new IllegalArgumentException("Completa los datos del registro.");
        }
        validarCatalogo(tabla, id, datos);
        dao.guardar(tabla, pk, id, datos);
    }

    /**
     * Elimina o desactiva un registro verificando las restricciones de integridad del SRS:
     * <ul>
     *   <li>RF-03: No eliminar una obra si tiene partidas o movimientos registrados.</li>
     *   <li>RF-06: No dar de baja un material con {@code stock_actual > 0}.</li>
     *   <li>RF-09: No eliminar un proveedor con compras (solo desactivar).</li>
     * </ul>
     *
     * @param tabla Nombre de la tabla.
     * @param pk Nombre de la clave primaria.
     * @param id ID del registro.
     * @throws Exception Si la baja no está permitida por las reglas de negocio.
     */
    public void eliminar(String tabla, String pk, Object id) throws Exception {
        eliminar(tabla, pk, id, null);
    }

    /**
     * Elimina o desactiva un registro verificando permisos de rol y reglas del SRS.
     *
     * @param tabla Nombre de la tabla.
     * @param pk Nombre de la clave primaria.
     * @param id ID del registro.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o la operación está restringida.
     */
    public void eliminar(String tabla, String pk, Object id, String rolSesion) throws Exception {
        verificarPermisoCatalogo(tabla, rolSesion);
        if (id == null) {
            throw new IllegalArgumentException("Selecciona un registro.");
        }
        int idNum = Integer.parseInt(id.toString());
        switch (tabla) {
            case "obra" -> {
                if (dao.obraTieneMovimientos(idNum)) {
                    throw new IllegalArgumentException("No se puede eliminar una obra que tiene partidas o movimientos registrados (RF-03). Utilice la opción de cerrar/finalizar obra.");
                }
            }
            case "material" -> {
                double stock = dao.stockActualMaterial(idNum);
                if (stock > 0) {
                    throw new IllegalArgumentException("No se puede dar de baja un material con stock mayor a cero (Stock actual: " + stock + ").");
                }
            }
            case "partida" -> {
                if (dao.partidaTieneMovimientos(idNum)) {
                    throw new IllegalArgumentException("No se puede eliminar una partida que ya registra avances, consumos o producción.");
                }
            }
            case "proveedor" -> {
                // RF-09: Si tiene compras o no, en el modelo el borrado es lógico (desactivar).
                // Se desactiva conservando su historial de compras.
            }
        }
        dao.eliminar(tabla, pk, id);
    }

    /**
     * Cierra (marca como {@code Finalizada}) una obra que tiene partidas o movimientos (RF-03).
     *
     * @param idObra ID de la obra.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o falla la actualización.
     */
    public void cerrarObra(int idObra, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (idObra <= 0) {
            throw new IllegalArgumentException("Selecciona una obra válida.");
        }
        dao.cerrarObra(idObra);
    }

    private void verificarPermisoCatalogo(String tabla, String rolSesion) {
        if (rolSesion == null) {
            return;
        }
        switch (tabla) {
            case "obra", "proveedor", "rol" ->
                ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
            case "partida" ->
                ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_RESIDENTE, ClsConstantes.ROL_ADMIN);
            case "material" ->
                ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ALMACENERO, ClsConstantes.ROL_ADMIN);
            case "empleado" ->
                ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN, ClsConstantes.ROL_RESIDENTE);
            default ->
                throw new IllegalArgumentException("Catálogo no permitido.");
        }
    }

    private void validarCatalogo(String tabla, Object id, Map<String, Object> d) throws Exception {
        switch (tabla) {
            case "obra" -> {
                requerir(d, "nombre");
                requerir(d, "cliente");
                positivo(d, "presupuesto_total");
                redondearMoneda(d, "presupuesto_total");
                fechasValidas(d, "fecha_inicio_plan", "fecha_fin_plan");
            }
            case "partida" -> {
                positivo(d, "id_obra");
                int idObra = (int) parse(d, "id_obra");
                if (!dao.esObraActiva(idObra)) {
                    throw new IllegalArgumentException("Solo se pueden registrar o editar partidas en una obra activa (RF-04).");
                }
                requerir(d, "nombre");
                requerir(d, "unidad_medida");
                positivo(d, "metrado_planificado");
                positivo(d, "peso_presupuestal");
                numeroEnRango(d, "peso_presupuestal", 0.01, 100.0);
                double pesoNuevo = parse(d, "peso_presupuestal");
                double sumaExistente = dao.sumaPesosPartidas(idObra, id);
                if (sumaExistente + pesoNuevo > 100.0001) {
                    double disponible = Math.max(0.0, 100.0 - sumaExistente);
                    throw new IllegalArgumentException(String.format(
                            "La suma de pesos presupuestales de la obra no puede superar el 100 %% (RF-04). Acumulado actual: %.2f %%, disponible: %.2f %%.",
                            sumaExistente, disponible));
                }
            }
            case "material" -> {
                if (d.containsKey("stock_actual")) {
                    d.remove("stock_actual");
                }
                requerir(d, "nombre");
                requerir(d, "unidad_medida");
                numeroNoNegativo(d, "stock_minimo");
                numeroNoNegativo(d, "costo_unitario_ref");
                redondearMoneda(d, "costo_unitario_ref");
            }
            case "proveedor" -> {
                requerir(d, "razon_social");
                String ruc = texto(d, "ruc");
                if (ruc.length() != 11 || !ruc.chars().allMatch(Character::isDigit)) {
                    throw new IllegalArgumentException("El RUC debe tener exactamente 11 dígitos numéricos (RF-09).");
                }
                if (dao.existeRuc(ruc, id)) {
                    throw new IllegalArgumentException("Ya existe un proveedor registrado con el RUC " + ruc + " (RF-09).");
                }
            }
            case "empleado" -> {
                requerir(d, "nombres_apellidos");
                String dni = texto(d, "dni");
                if (dni.length() != 8 || !dni.chars().allMatch(Character::isDigit)) {
                    throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos numéricos (RF-10).");
                }
                if (dao.existeDni(dni, id)) {
                    throw new IllegalArgumentException("Ya existe un trabajador registrado con el DNI " + dni + " (RF-10).");
                }
                requerir(d, "cargo");
                positivo(d, "jornal_diario");
                redondearMoneda(d, "jornal_diario");
                requerir(d, "fecha_ingreso");
            }
            case "rol" ->
                requerir(d, "nombre_rol");
            default ->
                throw new IllegalArgumentException("Catálogo no permitido.");
        }
    }

    private void requerir(Map<String, Object> d, String k) {
        if (texto(d, k).isBlank()) {
            throw new IllegalArgumentException("Completa el campo obligatorio: " + k + ".");
        }
    }

    private String texto(Map<String, Object> d, String k) {
        Object v = d.get(k);
        return v == null ? "" : v.toString().trim();
    }

    private void positivo(Map<String, Object> d, String k) {
        double x = parse(d, k);
        if (x <= 0) {
            throw new IllegalArgumentException("El campo " + k + " debe ser mayor que cero.");
        }
    }

    private void numeroNoNegativo(Map<String, Object> d, String k) {
        if (d.containsKey(k) && !texto(d, k).isBlank() && parse(d, k) < 0) {
            throw new IllegalArgumentException("El campo " + k + " no puede ser negativo.");
        }
    }

    private void numeroEnRango(Map<String, Object> d, String k, double min, double max) {
        double x = parse(d, k);
        if (x < min || x > max) {
            throw new IllegalArgumentException("El campo " + k + " debe estar entre " + min + " y " + max + ".");
        }
    }

    private void redondearMoneda(Map<String, Object> d, String k) {
        if (d.containsKey(k) && !texto(d, k).isBlank()) {
            BigDecimal bd = BigDecimal.valueOf(parse(d, k)).setScale(2, RoundingMode.HALF_UP);
            d.put(k, bd);
        }
    }

    private double parse(Map<String, Object> d, String k) {
        try {
            double x = Double.parseDouble(texto(d, k));
            if (!Double.isFinite(x)) {
                throw new NumberFormatException();
            }
            return x;
        } catch (Exception e) {
            throw new IllegalArgumentException("El campo " + k + " debe ser un número finito válido.");
        }
    }

    private void fechasValidas(Map<String, Object> d, String desde, String hasta) {
        try {
            var a = LocalDate.parse(texto(d, desde));
            var b = LocalDate.parse(texto(d, hasta));
            if (b.isBefore(a)) {
                throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Usa fechas con formato AAAA-MM-DD.");
        }
    }
}
