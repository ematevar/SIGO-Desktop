package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
material
. */
public class 
ClsMaterial
 extends ClsEntidadBase {
    public 
ClsMaterial
() { }
    public 
ClsMaterial
(int id) { super(id); }
    private String nombre;
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    private String unidadMedida;
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida=unidadMedida; }
    private java.math.BigDecimal stockActual;
    public java.math.BigDecimal getStockActual() { return stockActual; }
    public void setStockActual(java.math.BigDecimal stockActual) { this.stockActual=stockActual; }
    private java.math.BigDecimal stockMinimo;
    public java.math.BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(java.math.BigDecimal stockMinimo) { this.stockMinimo=stockMinimo; }
    private java.math.BigDecimal costoUnitarioRef;
    public java.math.BigDecimal getCostoUnitarioRef() { return costoUnitarioRef; }
    public void setCostoUnitarioRef(java.math.BigDecimal costoUnitarioRef) { this.costoUnitarioRef=costoUnitarioRef; }
}
