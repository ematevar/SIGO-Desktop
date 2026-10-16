package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
obra
. */
public class 
ClsObra
 extends ClsEntidadBase {
    public 
ClsObra
() { }
    public 
ClsObra
(int id) { super(id); }
    private String nombre;
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    private String cliente;
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente=cliente; }
    private String ubicacion;
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion=ubicacion; }
    private java.math.BigDecimal presupuestoTotal;
    public java.math.BigDecimal getPresupuestoTotal() { return presupuestoTotal; }
    public void setPresupuestoTotal(java.math.BigDecimal presupuestoTotal) { this.presupuestoTotal=presupuestoTotal; }
    private java.time.LocalDate fechaInicioPlan;
    public java.time.LocalDate getFechaInicioPlan() { return fechaInicioPlan; }
    public void setFechaInicioPlan(java.time.LocalDate fechaInicioPlan) { this.fechaInicioPlan=fechaInicioPlan; }
    private java.time.LocalDate fechaFinPlan;
    public java.time.LocalDate getFechaFinPlan() { return fechaFinPlan; }
    public void setFechaFinPlan(java.time.LocalDate fechaFinPlan) { this.fechaFinPlan=fechaFinPlan; }
    private String estado;
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado=estado; }
}
