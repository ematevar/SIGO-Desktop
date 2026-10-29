package com.sigoc.application.service;

import com.sigoc.application.port.in.CatalogoUseCase;
import com.sigoc.application.port.out.CatalogoRepositoryPort;
import java.util.List;
import java.util.Map;

public class ClsCatalogoService implements CatalogoUseCase {
    private final CatalogoRepositoryPort dao;

    public ClsCatalogoService(CatalogoRepositoryPort dao) {
        this.dao = dao;
    }
    public List<String> columnas(String tabla)throws Exception{return dao.columnas(tabla);}
    public List<Map<String,Object>> listar(String tabla)throws Exception{return dao.listar(tabla);}
    public String clavePrimaria(String tabla)throws Exception{return dao.clavePrimaria(tabla);}
    public void guardar(String tabla,String pk,Object id,Map<String,Object> datos)throws Exception{
        if(datos==null||datos.isEmpty())throw new IllegalArgumentException("Completa los datos del registro.");
        validarCatalogo(tabla,datos);dao.guardar(tabla,pk,id,datos);
    }
    public void eliminar(String tabla,String pk,Object id)throws Exception{if(id==null)throw new IllegalArgumentException("Selecciona un registro.");dao.eliminar(tabla,pk,id);}
    private void validarCatalogo(String tabla,Map<String,Object> d){
        switch(tabla){
            case "obra"->{requerir(d,"nombre");requerir(d,"cliente");numeroNoNegativo(d,"presupuesto_total");fechasValidas(d,"fecha_inicio_plan","fecha_fin_plan");}
            case "partida"->{positivo(d,"id_obra");requerir(d,"nombre");requerir(d,"unidad_medida");numeroNoNegativo(d,"metrado_planificado");numeroEnRango(d,"peso_presupuestal",0,100);}
            case "material"->{requerir(d,"nombre");requerir(d,"unidad_medida");numeroNoNegativo(d,"stock_actual");numeroNoNegativo(d,"stock_minimo");numeroNoNegativo(d,"costo_unitario_ref");}
            case "proveedor"->{requerir(d,"razon_social");String ruc=texto(d,"ruc");if(ruc.length()!=11||!ruc.chars().allMatch(Character::isDigit))throw new IllegalArgumentException("El RUC debe tener 11 dígitos.");}
            case "empleado"->{requerir(d,"nombres_apellidos");String dni=texto(d,"dni");if(dni.length()!=8||!dni.chars().allMatch(Character::isDigit))throw new IllegalArgumentException("El DNI debe tener 8 dígitos.");requerir(d,"cargo");positivo(d,"jornal_diario");requerir(d,"fecha_ingreso");}
            case "rol"->requerir(d,"nombre_rol");
            default->throw new IllegalArgumentException("Catálogo no permitido.");
        }
    }
    private void requerir(Map<String,Object>d,String k){if(texto(d,k).isBlank())throw new IllegalArgumentException("Completa el campo "+k+".");}
    private String texto(Map<String,Object>d,String k){Object v=d.get(k);return v==null?"":v.toString().trim();}
    private void positivo(Map<String,Object>d,String k){double x=parse(d,k);if(x<=0)throw new IllegalArgumentException(k+" debe ser mayor que cero.");}
    private void numeroNoNegativo(Map<String,Object>d,String k){if(d.containsKey(k)&&!texto(d,k).isBlank()&&parse(d,k)<0)throw new IllegalArgumentException(k+" no puede ser negativo.");}
    private void numeroEnRango(Map<String,Object>d,String k,double min,double max){double x=parse(d,k);if(x<min||x>max)throw new IllegalArgumentException(k+" debe estar entre "+min+" y "+max+".");}
    private double parse(Map<String,Object>d,String k){try{double x=Double.parseDouble(texto(d,k));if(!Double.isFinite(x))throw new NumberFormatException();return x;}catch(Exception e){throw new IllegalArgumentException(k+" debe ser un número finito.");}}
    private void fechasValidas(Map<String,Object>d,String desde,String hasta){try{var a=java.time.LocalDate.parse(texto(d,desde));var b=java.time.LocalDate.parse(texto(d,hasta));if(b.isBefore(a))throw new IllegalArgumentException("La fecha fin debe ser posterior al inicio.");}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Usa fechas con formato AAAA-MM-DD.");}}
}
