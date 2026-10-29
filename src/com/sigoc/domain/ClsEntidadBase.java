package com.sigoc.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Datos de una entidad de la aplicación, con propiedades vinculadas a columnas SQL. */
public abstract class ClsEntidadBase {
    private int id;
    private final Map<String,Object> propiedades = new LinkedHashMap<>();
    protected ClsEntidadBase() { }
    protected ClsEntidadBase(int id) { this.id=id; }
    public int getId() { return id; }
    public void setId(int id) { this.id=id; }
    public Object get(String propiedad) { return propiedades.get(propiedad); }
    public void set(String propiedad,Object valor) { propiedades.put(propiedad,valor); }
    public Map<String,Object> getPropiedades() { return Collections.unmodifiableMap(propiedades); }
}
