package com.sigoc.application.service;

import java.security.SecureRandom;
import com.sigoc.application.port.in.CaptchaUseCase;

public final class ClsCaptchaService implements CaptchaUseCase {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    public String generar() {
        StringBuilder valor = new StringBuilder(6);
        for (int i = 0; i < 6; i++) valor.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        return valor.toString();
    }
    public boolean validar(String esperado, String ingresado) {
        return esperado != null && ingresado != null && esperado.equalsIgnoreCase(ingresado.trim());
    }
}
