package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
cierreplanilla
. */
public class 
ClsCierrePlanilla
 extends ClsEntidadBase {
    public 
ClsCierrePlanilla
() { }
    public 
ClsCierrePlanilla
(int id) { super(id); }
    private int idObra;
    public int getIdObra() { return idObra; }
    public void setIdObra(int idObra) { this.idObra=idObra; }
    private java.time.LocalDate fechaInicioPeriodo;
    public java.time.LocalDate getFechaInicioPeriodo() { return fechaInicioPeriodo; }
    public void setFechaInicioPeriodo(java.time.LocalDate fechaInicioPeriodo) { this.fechaInicioPeriodo=fechaInicioPeriodo; }
    private java.time.LocalDate fechaFinPeriodo;
    public java.time.LocalDate getFechaFinPeriodo() { return fechaFinPeriodo; }
    public void setFechaFinPeriodo(java.time.LocalDate fechaFinPeriodo) { this.fechaFinPeriodo=fechaFinPeriodo; }
    private java.time.LocalDateTime fechaCierre;
    public java.time.LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(java.time.LocalDateTime fechaCierre) { this.fechaCierre=fechaCierre; }
    private java.math.BigDecimal montoTotal;
    public java.math.BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(java.math.BigDecimal montoTotal) { this.montoTotal=montoTotal; }
    private int idUsuario;
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario=idUsuario; }
}
