package com.sigoc.infrastructure.out.persistence;

import com.sigoc.infrastructure.config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.sigoc.application.port.out.OperacionRepositoryPort;

/** Operaciones transaccionales de inventario, asistencia y finanzas. */
public class ClsOperacionDAO implements OperacionRepositoryPort {
    public void registrarCompra(int proveedor,int obra,int material,String tipoComprobante,String comprobante,LocalDate fecha,double cantidad,double precio)throws Exception{
        transaccion(c->{
            int compra;
            try(PreparedStatement p=c.prepareStatement("INSERT INTO compra_material(id_proveedor,tipo_comprobante,fecha_compra,numero_comprobante,total) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                p.setInt(1,proveedor);p.setString(2,tipoComprobante);p.setDate(3,java.sql.Date.valueOf(fecha));p.setString(4,comprobante);p.setDouble(5,cantidad*precio);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();compra=k.getInt(1);}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO detalle_compra(id_compra,id_material,cantidad,precio_unitario,subtotal) VALUES(?,?,?,?,?)")){p.setInt(1,compra);p.setInt(2,material);p.setDouble(3,cantidad);p.setDouble(4,precio);p.setDouble(5,cantidad*precio);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE material SET stock_actual=stock_actual+?,costo_unitario_ref=? WHERE id_material=?")){p.setDouble(1,cantidad);p.setDouble(2,precio);p.setInt(3,material);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Material inexistente.");}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO egreso(id_obra,fecha,tipo_egreso,concepto,monto,id_compra) VALUES(?,?,'Material',?,?,?)")){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(fecha));p.setString(3,"Compra "+comprobante);p.setDouble(4,cantidad*precio);p.setInt(5,compra);p.executeUpdate();}
            return null;
        });
    }
    public void registrarAsistencia(int empleado,int obra,LocalDate fecha,String entrada,String salida,String estado)throws Exception{
        String sql="INSERT INTO asistencia(id_empleado,id_obra,fecha,hora_entrada,hora_salida,estado) VALUES(?,?,?,?,?,?)";
        transaccion(c->{
            try(PreparedStatement lock=c.prepareStatement("SELECT id_empleado FROM empleado WHERE id_empleado=? FOR UPDATE")){lock.setInt(1,empleado);try(ResultSet rs=lock.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Empleado inexistente.");}}
            try(PreparedStatement exists=c.prepareStatement("SELECT 1 FROM asistencia WHERE id_empleado=? AND id_obra=? AND fecha=?")){exists.setInt(1,empleado);exists.setInt(2,obra);exists.setDate(3,java.sql.Date.valueOf(fecha));try(ResultSet rs=exists.executeQuery()){if(rs.next())throw new IllegalArgumentException("Ya existe asistencia para este empleado, obra y fecha.");}}
            try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,empleado);p.setInt(2,obra);p.setDate(3,java.sql.Date.valueOf(fecha));p.setTime(4,entrada.isBlank()?null:java.sql.Time.valueOf(entrada));p.setTime(5,salida.isBlank()?null:java.sql.Time.valueOf(salida));p.setString(6,estado);p.executeUpdate();}return null;
        });
    }
    public void registrarProduccion(int empleado,int partida,LocalDate fecha,double cantidad)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("INSERT INTO produccion_empleado(id_empleado,id_partida,fecha,cantidad_producida) VALUES(?,?,?,?)")){p.setInt(1,empleado);p.setInt(2,partida);p.setDate(3,java.sql.Date.valueOf(fecha));p.setDouble(4,cantidad);p.executeUpdate();}
    }
    public void registrarIngreso(int obra,LocalDate fecha,String concepto,double monto)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("INSERT INTO ingreso(id_obra,fecha,concepto,monto) VALUES(?,?,?,?)")){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(fecha));p.setString(3,concepto);p.setDouble(4,monto);p.executeUpdate();}
    }
    public void registrarEgreso(int obra,LocalDate fecha,String concepto,double monto)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("INSERT INTO egreso(id_obra,fecha,tipo_egreso,concepto,monto) VALUES(?,?,'Otro',?,?)")){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(fecha));p.setString(3,concepto);p.setDouble(4,monto);p.executeUpdate();}
    }
    public double cerrarPlanilla(int obra,LocalDate desde,LocalDate hasta,int usuario)throws Exception{
        return transaccion(c->{
            double total=0;
            String q="SELECT COALESCE(SUM(e.jornal_diario),0) FROM asistencia a JOIN empleado e ON e.id_empleado=a.id_empleado WHERE a.id_obra=? AND a.fecha BETWEEN ? AND ? AND UPPER(a.estado) IN ('PRESENTE','ASISTIO','ASISTIÓ')";
            try(PreparedStatement p=c.prepareStatement(q)){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(desde));p.setDate(3,java.sql.Date.valueOf(hasta));try(ResultSet r=p.executeQuery()){r.next();total=r.getDouble(1);}}
            if(total<=0)throw new IllegalArgumentException("No hay asistencias presentes para cerrar en ese periodo.");
            int cierre;
            try(PreparedStatement p=c.prepareStatement("INSERT INTO cierre_planilla(id_obra,fecha_inicio_periodo,fecha_fin_periodo,monto_total,id_usuario) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(desde));p.setDate(3,java.sql.Date.valueOf(hasta));p.setDouble(4,total);p.setInt(5,usuario);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();cierre=r.getInt(1);}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO egreso(id_obra,fecha,tipo_egreso,concepto,monto,id_cierre) VALUES(?,?,'Planilla',?,?,?)")){p.setInt(1,obra);p.setDate(2,java.sql.Date.valueOf(hasta));p.setString(3,"Planilla del "+desde+" al "+hasta);p.setDouble(4,total);p.setInt(5,cierre);p.executeUpdate();}
            return total;
        });
    }
    public List<Map<String,Object>> opciones(String tabla)throws Exception{
        Map<String,String> campos=Map.of("obra","id_obra,nombre","material","id_material,nombre","proveedor","id_proveedor,razon_social","empleado","id_empleado,nombres_apellidos","partida","id_partida,nombre");
        String c=campos.get(tabla);if(c==null)throw new IllegalArgumentException("Opción no permitida");String[] col=c.split(",");var out=new java.util.ArrayList<Map<String,Object>>();
        try(Connection x=ClsConexionBD.getInstancia().abrirConexion();var p=x.createStatement();ResultSet r=p.executeQuery("SELECT "+c+" FROM "+tabla+" WHERE activo=1 ORDER BY 2")){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getInt(1));m.put("nombre",r.getString(2));out.add(m);}}return out;
    }
    @FunctionalInterface private interface Trabajo<T>{T ejecutar(Connection c)throws Exception;}
    private <T>T transaccion(Trabajo<T> trabajo)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion()){c.setAutoCommit(false);try{T result=trabajo.ejecutar(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}}
    }
}
