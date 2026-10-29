package com.sigoc.domain;

/** POJO de dominio; sus propiedades corresponden a la tabla 
proveedor
. */
public class 
ClsProveedor
 extends ClsEntidadBase {
    public 
ClsProveedor
() { }
    public 
ClsProveedor
(int id) { super(id); }
    private String razonSocial;
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial=razonSocial; }
    private String ruc;
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc=ruc; }
    private String telefono;
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono=telefono; }
    private String direccion;
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion=direccion; }
}
