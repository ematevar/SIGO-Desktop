package com.sigoc.application.port.out;

import java.time.LocalDate;

public interface AvanceRepositoryPort {
    Double ultimoPorcentaje(int partida) throws Exception;
    void registrar(int partida, LocalDate fecha, double porcentaje, String observaciones, int usuario) throws Exception;
}
