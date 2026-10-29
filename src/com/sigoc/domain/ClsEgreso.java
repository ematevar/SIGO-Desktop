package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
egreso
. */
public class 
ClsEgreso
 extends ClsEntidadBase {
    public 
ClsEgreso
() { }
    public 
ClsEgreso
(int id) { super(id); }
    private int idObra;
    public int getIdObra() { return idObra; }
    public void setIdObra(int idObra) { this.idObra=idObra; }
    private java.time.LocalDate fecha;
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha=fecha; }
    private String tipoEgreso;
    public String getTipoEgreso() { return tipoEgreso; }
    public void setTipoEgreso(String tipoEgreso) { this.tipoEgreso=tipoEgreso; }
    private String concepto;
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto=concepto; }
    private java.math.BigDecimal monto;
    public java.math.BigDecimal getMonto() { return monto; }
    public void setMonto(java.math.BigDecimal monto) { this.monto=monto; }
    private Integer idCompra;
    public Integer getIdCompra() { return idCompra; }
    public void setIdCompra(Integer idCompra) { this.idCompra=idCompra; }
    private Integer idCierre;
    public Integer getIdCierre() { return idCierre; }
    public void setIdCierre(Integer idCierre) { this.idCierre=idCierre; }
}
