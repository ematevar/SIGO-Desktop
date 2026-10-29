package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
ingreso
. */
public class 
ClsIngreso
 extends ClsEntidadBase {
    public 
ClsIngreso
() { }
    public 
ClsIngreso
(int id) { super(id); }
    private int idObra;
    public int getIdObra() { return idObra; }
    public void setIdObra(int idObra) { this.idObra=idObra; }
    private java.time.LocalDate fecha;
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha=fecha; }
    private String concepto;
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto=concepto; }
    private java.math.BigDecimal monto;
    public java.math.BigDecimal getMonto() { return monto; }
    public void setMonto(java.math.BigDecimal monto) { this.monto=monto; }
}
