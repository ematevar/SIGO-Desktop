package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
compramaterial
. */
public class 
ClsCompraMaterial
 extends ClsEntidadBase {
    public 
ClsCompraMaterial
() { }
    public 
ClsCompraMaterial
(int id) { super(id); }
    private int idProveedor;
    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int idProveedor) { this.idProveedor=idProveedor; }
    private java.time.LocalDate fechaCompra;
    public java.time.LocalDate getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(java.time.LocalDate fechaCompra) { this.fechaCompra=fechaCompra; }
    private String numeroComprobante;
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante=numeroComprobante; }
    private java.math.BigDecimal total;
    public java.math.BigDecimal getTotal() { return total; }
    public void setTotal(java.math.BigDecimal total) { this.total=total; }
}
