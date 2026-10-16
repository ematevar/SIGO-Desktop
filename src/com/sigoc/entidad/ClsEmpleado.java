package com.sigoc.entidad;

/** POJO de dominio; sus propiedades corresponden a la tabla 
empleado
. */
public class 
ClsEmpleado
 extends ClsEntidadBase {
    public 
ClsEmpleado
() { }
    public 
ClsEmpleado
(int id) { super(id); }
    private String nombresApellidos;
    public String getNombresApellidos() { return nombresApellidos; }
    public void setNombresApellidos(String nombresApellidos) { this.nombresApellidos=nombresApellidos; }
    private String dni;
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni=dni; }
    private String cargo;
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo=cargo; }
    private java.math.BigDecimal jornalDiario;
    public java.math.BigDecimal getJornalDiario() { return jornalDiario; }
    public void setJornalDiario(java.math.BigDecimal jornalDiario) { this.jornalDiario=jornalDiario; }
    private java.time.LocalDate fechaIngreso;
    public java.time.LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(java.time.LocalDate fechaIngreso) { this.fechaIngreso=fechaIngreso; }
}
