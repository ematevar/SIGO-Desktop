package com.sigoc.negocio;

import com.sigoc.negocio.dao.ClsAvanceDAO;
import java.time.LocalDate;

public class ClsAvanceService {
    private final ClsAvanceDAO dao=new ClsAvanceDAO();
    public void registrar(int partida, LocalDate fecha, double porcentaje, String observaciones, int usuario) throws Exception {
        if(partida<=0||usuario<=0)throw new IllegalArgumentException("Selecciona una partida y un usuario válidos.");
        if(fecha==null||fecha.isAfter(LocalDate.now()))throw new IllegalArgumentException("La fecha del avance no puede estar vacía ni ser futura.");
        if(!Double.isFinite(porcentaje)||porcentaje<0||porcentaje>100)throw new IllegalArgumentException("El avance debe estar entre 0 y 100 %.");
        Double ultimo=dao.ultimoPorcentaje(partida);
        if(ultimo!=null&&porcentaje<ultimo)throw new IllegalArgumentException("El avance acumulado no puede ser menor al último registrado ("+ultimo+" %).");
        dao.registrar(partida,fecha,porcentaje,observaciones,usuario);
    }
}
