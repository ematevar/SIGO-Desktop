package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
partida
. */
public class 
ClsPartida
 extends ClsEntidadBase {
    public 
ClsPartida
() { }
    public 
ClsPartida
(int id) { super(id); }
    private int idObra;
    public int getIdObra() { return idObra; }
    public void setIdObra(int idObra) { this.idObra=idObra; }
    private String nombre;
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    private String unidadMedida;
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida=unidadMedida; }
    private java.math.BigDecimal metradoPlanificado;
    public java.math.BigDecimal getMetradoPlanificado() { return metradoPlanificado; }
    public void setMetradoPlanificado(java.math.BigDecimal metradoPlanificado) { this.metradoPlanificado=metradoPlanificado; }
    private java.math.BigDecimal pesoPresupuestal;
    public java.math.BigDecimal getPesoPresupuestal() { return pesoPresupuestal; }
    public void setPesoPresupuestal(java.math.BigDecimal pesoPresupuestal) { this.pesoPresupuestal=pesoPresupuestal; }
}
