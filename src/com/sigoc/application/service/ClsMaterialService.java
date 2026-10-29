package com.sigoc.application.service;

import com.sigoc.application.port.in.MaterialUseCase;
import com.sigoc.application.port.out.MaterialRepositoryPort;
import java.time.LocalDate;

public class ClsMaterialService implements MaterialUseCase {
    private final MaterialRepositoryPort dao;

    public ClsMaterialService(MaterialRepositoryPort dao) {
        this.dao = dao;
    }
    public boolean registrarConsumo(int material,int partida,int usuario,LocalDate fecha,double cantidad) throws Exception {
        if(material<=0||partida<=0||usuario<=0)throw new IllegalArgumentException("Selecciona material, partida y usuario.");
        if(fecha==null||fecha.isAfter(LocalDate.now()))throw new IllegalArgumentException("La fecha de consumo no es válida.");
        if(!Double.isFinite(cantidad)||cantidad<=0)throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        dao.registrarConsumo(material,partida,usuario,fecha,cantidad);
        return dao.stockMinimo(material)<0;
    }
}
