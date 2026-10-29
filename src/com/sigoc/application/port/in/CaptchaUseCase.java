package com.sigoc.application.port.in;

public interface CaptchaUseCase {
    String generar();
    boolean validar(String esperado, String ingresado);
}
