package com.sigoc.application.port.in;

import java.time.LocalDate;

public interface AvanceUseCase {
    void registrar(int partida, LocalDate fecha, double porcentaje, String observaciones, int usuario) throws Exception;
}
