package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
detallecompra
. */
public class 
ClsDetalleCompra
 extends ClsEntidadBase {
    public 
ClsDetalleCompra
() { }
    public 
ClsDetalleCompra
(int id) { super(id); }
    private int idCompra;
    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra=idCompra; }
    private int idMaterial;
    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial=idMaterial; }
    private java.math.BigDecimal cantidad;
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(java.math.BigDecimal cantidad) { this.cantidad=cantidad; }
    private java.math.BigDecimal precioUnitario;
    public java.math.BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(java.math.BigDecimal precioUnitario) { this.precioUnitario=precioUnitario; }
    private java.math.BigDecimal subtotal;
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(java.math.BigDecimal subtotal) { this.subtotal=subtotal; }
}
