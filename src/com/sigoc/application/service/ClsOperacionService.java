package com.sigoc.application.service;

import com.sigoc.application.port.in.OperacionUseCase;
import com.sigoc.application.port.out.OperacionRepositoryPort;
import java.time.LocalDate;

public class ClsOperacionService implements OperacionUseCase {
    private final OperacionRepositoryPort dao;

    public ClsOperacionService(OperacionRepositoryPort dao) {
        this.dao = dao;
    }
    public java.util.List<java.util.Map<String,Object>> opciones(String tabla)throws Exception{return dao.opciones(tabla);}
    public void compra(int proveedor,int obra,int material,String tipoComprobante,String comprobante,LocalDate fecha,double cantidad,double precio)throws Exception{
        if(proveedor<=0||obra<=0||material<=0)throw new IllegalArgumentException("Selecciona proveedor, obra y material.");
        if(tipoComprobante==null||(!tipoComprobante.equals("Boleta")&&!tipoComprobante.equals("Factura")))throw new IllegalArgumentException("El tipo de comprobante debe ser Boleta o Factura.");
        if(comprobante==null||comprobante.isBlank())throw new IllegalArgumentException("Ingresa el número de comprobante.");
        if(fecha==null||fecha.isAfter(LocalDate.now())||cantidad<=0||precio<=0||!Double.isFinite(cantidad*precio))throw new IllegalArgumentException("Fecha, cantidad o precio no válidos.");
        dao.registrarCompra(proveedor,obra,material,tipoComprobante,comprobante.trim(),fecha,cantidad,precio);
    }
    public void asistencia(int empleado,int obra,LocalDate fecha,String entrada,String salida,String estado)throws Exception{
        if(empleado<=0||obra<=0||fecha==null||fecha.isAfter(LocalDate.now()))throw new IllegalArgumentException("Empleado, obra o fecha inválidos.");
        if(estado==null||estado.isBlank())throw new IllegalArgumentException("Selecciona un estado de asistencia.");
        if((estado.equalsIgnoreCase("PRESENTE")||estado.equalsIgnoreCase("TARDANZA"))&&(entrada==null||entrada.isBlank()))throw new IllegalArgumentException("Registra la hora de entrada.");
        try{if(entrada!=null&&!entrada.isBlank())java.time.LocalTime.parse(entrada);if(salida!=null&&!salida.isBlank())java.time.LocalTime.parse(salida);if(entrada!=null&&!entrada.isBlank()&&salida!=null&&!salida.isBlank()&&!java.time.LocalTime.parse(salida).isAfter(java.time.LocalTime.parse(entrada)))throw new IllegalArgumentException("La salida debe ser posterior a la entrada.");}
        catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Usa el formato de hora HH:MM:SS.");}
        dao.registrarAsistencia(empleado,obra,fecha,entrada==null?"":entrada.trim(),salida==null?"":salida.trim(),estado);
    }
    public void produccion(int empleado,int partida,LocalDate fecha,double cantidad)throws Exception{
        if(empleado<=0||partida<=0||fecha==null||fecha.isAfter(LocalDate.now())||cantidad<=0||!Double.isFinite(cantidad))throw new IllegalArgumentException("Datos de producción inválidos.");
        dao.registrarProduccion(empleado,partida,fecha,cantidad);
    }
    public void ingreso(int obra,LocalDate fecha,String concepto,double monto)throws Exception{validarFinanzas(obra,fecha,concepto,monto);dao.registrarIngreso(obra,fecha,concepto.trim(),monto);}
    public void egreso(int obra,LocalDate fecha,String concepto,double monto)throws Exception{validarFinanzas(obra,fecha,concepto,monto);dao.registrarEgreso(obra,fecha,concepto.trim(),monto);}
    private void validarFinanzas(int obra,LocalDate fecha,String concepto,double monto){if(obra<=0||fecha==null||fecha.isAfter(LocalDate.now())||concepto==null||concepto.isBlank()||monto<=0||!Double.isFinite(monto))throw new IllegalArgumentException("Completa obra, fecha, concepto y un monto positivo.");}
    public double cerrarPlanilla(int obra,LocalDate desde,LocalDate hasta,int usuario)throws Exception{
        if(obra<=0||usuario<=0||desde==null||hasta==null||hasta.isBefore(desde)||hasta.isAfter(LocalDate.now()))throw new IllegalArgumentException("Obra o periodo inválido.");return dao.cerrarPlanilla(obra,desde,hasta,usuario);
    }
}
