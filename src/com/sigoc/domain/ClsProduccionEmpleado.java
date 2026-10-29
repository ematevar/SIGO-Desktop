package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
produccionempleado
. */
public class 
ClsProduccionEmpleado
 extends ClsEntidadBase {
    public 
ClsProduccionEmpleado
() { }
    public 
ClsProduccionEmpleado
(int id) { super(id); }
    private int idEmpleado;
    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado=idEmpleado; }
    private int idPartida;
    public int getIdPartida() { return idPartida; }
    public void setIdPartida(int idPartida) { this.idPartida=idPartida; }
    private java.time.LocalDate fecha;
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha=fecha; }
    private java.math.BigDecimal cantidadProducida;
    public java.math.BigDecimal getCantidadProducida() { return cantidadProducida; }
    public void setCantidadProducida(java.math.BigDecimal cantidadProducida) { this.cantidadProducida=cantidadProducida; }
}
