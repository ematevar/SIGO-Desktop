package Negocio;

import config.ClsConstantes;
import NegocioDAO.ClsMaterialDAO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para consumos de materiales y alertas de stock mínimo (RF-08).
 */
public class ClsMaterialService {

    private final ClsMaterialDAO dao;

    public ClsMaterialService() {
        this(new ClsMaterialDAO());
    }

    public ClsMaterialService(ClsMaterialDAO dao) {
        this.dao = dao;
    }

    /**
     * Registra un consumo de material y devuelve {@code true} si el stock resultante
     * queda igual o por debajo del stock mínimo ({@code <= stock_minimo}, RF-08).
     *
     * @param material ID del material.
     * @param partida ID de la partida.
     * @param usuario ID del usuario responsable.
     * @param fecha Fecha del consumo.
     * @param cantidad Cantidad a consumir (> 0).
     * @return {@code true} si se debe disparar la alerta de stock mínimo.
     * @throws Exception Si los datos son inválidos o el stock es insuficiente.
     */
    public boolean registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) throws Exception {
        return registrarConsumo(material, partida, usuario, fecha, cantidad, null);
    }

    /**
     * Registra un consumo de material verificando el rol de sesión (Residente de Obra, RF-08).
     *
     * @param material ID del material.
     * @param partida ID de la partida.
     * @param usuario ID del usuario responsable.
     * @param fecha Fecha del consumo.
     * @param cantidad Cantidad a consumir (> 0).
     * @param rolSesion Rol del usuario en sesión.
     * @return {@code true} si el stock resultante queda igual o por debajo del mínimo.
     * @throws Exception Si no tiene permisos o los datos son inválidos.
     */
    public boolean registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_RESIDENTE);
        if (material <= 0 || partida <= 0 || usuario <= 0) {
            throw new IllegalArgumentException("Selecciona material, partida y usuario.");
        }
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de consumo no es válida ni puede ser futura.");
        }
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        dao.registrarConsumo(material, partida, usuario, fecha, cantidad);
        return dao.stockMinimo(material) <= 0;
    }

    /**
     * Obtiene el consumo acumulado de un material en una partida específica (RF-08).
     *
     * @param partida ID de la partida.
     * @param material ID del material.
     * @return Cantidad total acumulada consumida.
     * @throws Exception Si ocurre un error SQL.
     */
    public double consumoAcumuladoPartida(int partida, int material) throws Exception {
        return dao.consumoAcumuladoPartida(partida, material);
    }

    /**
     * Lista todos los materiales activos que se encuentran en alerta ({@code stock_actual <= stock_minimo}).
     *
     * @return Lista de materiales con stock crítico.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listarAlertasStockMinimo() throws Exception {
        return dao.listarAlertasStockMinimo();
    }
}
