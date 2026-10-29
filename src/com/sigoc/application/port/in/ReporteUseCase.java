package com.sigoc.application.port.in;

import com.sigoc.application.service.ClsReporteService.Reporte;

public interface ReporteUseCase {
    Reporte generar(int tipo) throws Exception;
    void exportarCsv(Reporte reporte, java.nio.file.Path path) throws Exception;
}
