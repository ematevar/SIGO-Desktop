package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
asistencia
. */
public class 
ClsAsistencia
 extends ClsEntidadBase {
    public 
ClsAsistencia
() { }
    public 
ClsAsistencia
(int id) { super(id); }
    private int idEmpleado;
    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado=idEmpleado; }
    private int idObra;
    public int getIdObra() { return idObra; }
    public void setIdObra(int idObra) { this.idObra=idObra; }
    private java.time.LocalDate fecha;
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha=fecha; }
    private java.time.LocalTime horaEntrada;
    public java.time.LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(java.time.LocalTime horaEntrada) { this.horaEntrada=horaEntrada; }
    private java.time.LocalTime horaSalida;
    public java.time.LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(java.time.LocalTime horaSalida) { this.horaSalida=horaSalida; }
    private String estado;
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado=estado; }
}
