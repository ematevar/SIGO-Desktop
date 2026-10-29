package com.sigoc.application.port.in;

import java.time.LocalDate;

public interface MaterialUseCase {
    boolean registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) throws Exception;
}
