package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
rol
. */
public class 
ClsRol
 extends ClsEntidadBase {
    public 
ClsRol
() { }
    public 
ClsRol
(int id) { super(id); }
    private String nombreRol;
    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol=nombreRol; }
}
