package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
consumomaterial
. */
public class 
ClsConsumoMaterial
 extends ClsEntidadBase {
    public 
ClsConsumoMaterial
() { }
    public 
ClsConsumoMaterial
(int id) { super(id); }
    private int idPartida;
    public int getIdPartida() { return idPartida; }
    public void setIdPartida(int idPartida) { this.idPartida=idPartida; }
    private int idMaterial;
    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial=idMaterial; }
    private java.time.LocalDate fechaConsumo;
    public java.time.LocalDate getFechaConsumo() { return fechaConsumo; }
    public void setFechaConsumo(java.time.LocalDate fechaConsumo) { this.fechaConsumo=fechaConsumo; }
    private java.math.BigDecimal cantidad;
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(java.math.BigDecimal cantidad) { this.cantidad=cantidad; }
    private int idUsuario;
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario=idUsuario; }
}
