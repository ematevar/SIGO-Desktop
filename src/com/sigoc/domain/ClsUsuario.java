package com.sigoc.domain;

public class ClsUsuario {
    private int idUsuario;
    private String nombreCompleto;
    private String username;
    private String rol;
    private String passwordHash;
    private int idRol;
    private boolean activo;
    public ClsUsuario() { }
    public ClsUsuario(int idUsuario, String username, String rol) { this.idUsuario=idUsuario; this.username=username; this.rol=rol; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario=idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto=nombreCompleto; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username=username; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol=rol; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash=passwordHash; }
    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol=idRol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo=activo; }
}
