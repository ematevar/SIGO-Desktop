package com.sigoc.application.port.out;

import com.sigoc.application.service.ClsReporteService.Reporte;

public interface ReporteRepositoryPort {
    Reporte generar(int tipo) throws Exception;
}
