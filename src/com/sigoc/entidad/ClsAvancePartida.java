package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
avancepartida
. */
public class 
ClsAvancePartida
 extends ClsEntidadBase {
    public 
ClsAvancePartida
() { }
    public 
ClsAvancePartida
(int id) { super(id); }
    private int idPartida;
    public int getIdPartida() { return idPartida; }
    public void setIdPartida(int idPartida) { this.idPartida=idPartida; }
    private java.time.LocalDate fechaReporte;
    public java.time.LocalDate getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(java.time.LocalDate fechaReporte) { this.fechaReporte=fechaReporte; }
    private java.math.BigDecimal porcentajeAvance;
    public java.math.BigDecimal getPorcentajeAvance() { return porcentajeAvance; }
    public void setPorcentajeAvance(java.math.BigDecimal porcentajeAvance) { this.porcentajeAvance=porcentajeAvance; }
    private String observaciones;
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones=observaciones; }
    private int idUsuario;
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario=idUsuario; }
}
