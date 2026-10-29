package com.sigoc.application.port.out;

import java.time.LocalDate;

public interface MaterialRepositoryPort {
    void registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) throws Exception;
    double stockMinimo(int material) throws Exception;
}
