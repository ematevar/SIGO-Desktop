package Negocio;

import config.ClsConstantes;
import NegocioDAO.ClsAvanceDAO;
import java.time.LocalDate;

/**
 * Servicio de negocio para el registro de avances físicos de partidas y cálculo del avance acumulado de obra (RF-05).
 */
public class ClsAvanceService {

    private final ClsAvanceDAO dao;

    public ClsAvanceService() {
        this(new ClsAvanceDAO());
    }

    public ClsAvanceService(ClsAvanceDAO dao) {
        this.dao = dao;
    }

    /**
     * Registra un avance físico y devuelve el avance acumulado ponderado de la obra.
     *
     * @param partida ID de la partida.
     * @param fecha Fecha del reporte.
     * @param porcentaje Porcentaje de avance acumulado (0 a 100).
     * @param observaciones Observaciones del reporte.
     * @param usuario ID del usuario responsable.
     * @return Avance físico acumulado ponderado de la obra (%).
     * @throws Exception Si los datos son inválidos o el avance disminuye.
     */
    public double registrar(int partida, LocalDate fecha, double porcentaje, String observaciones, int usuario) throws Exception {
        return registrar(partida, fecha, porcentaje, observaciones, usuario, null);
    }

    /**
     * Registra un avance físico validando que el usuario tenga el rol de Residente de Obra (RF-05).
     *
     * @param partida ID de la partida.
     * @param fecha Fecha del reporte.
     * @param porcentaje Porcentaje de avance acumulado (0 a 100).
     * @param observaciones Observaciones del reporte.
     * @param usuario ID del usuario responsable.
     * @param rolSesion Rol del usuario en sesión.
     * @return Avance físico acumulado ponderado de la obra (%).
     * @throws Exception Si no es Residente de Obra o los datos son inválidos.
     */
    public double registrar(int partida, LocalDate fecha, double porcentaje, String observaciones, int usuario, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_RESIDENTE);
        if (partida <= 0 || usuario <= 0) {
            throw new IllegalArgumentException("Selecciona una partida y un usuario válidos.");
        }
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del avance no puede estar vacía ni ser futura.");
        }
        if (!Double.isFinite(porcentaje) || porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El avance debe estar entre 0 y 100 %.");
        }
        Double ultimo = dao.ultimoPorcentaje(partida);
        if (ultimo != null && porcentaje < ultimo) {
            throw new IllegalArgumentException("El avance acumulado no puede ser menor al último registrado (" + ultimo + " %).");
        }
        return dao.registrar(partida, fecha, porcentaje, observaciones, usuario);
    }
}
