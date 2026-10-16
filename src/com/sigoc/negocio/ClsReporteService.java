package com.sigoc.negocio;

import com.sigoc.config.ClsConexionBD;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Consultas gerenciales y exportación CSV compatible con Excel. */
public class ClsReporteService {
    public record Reporte(String titulo,List<String> columnas,List<Object[]> filas){}
    public Reporte generar(int tipo)throws Exception{
        String[] sql={
            "SELECT o.nombre AS obra,fechas.fecha_reporte,ROUND(SUM(p.peso_presupuestal*COALESCE((SELECT a.porcentaje_avance FROM avance_partida a WHERE a.id_partida=p.id_partida AND a.fecha_reporte<=fechas.fecha_reporte ORDER BY a.fecha_reporte DESC,a.id_avance DESC LIMIT 1),0))/NULLIF(SUM(p.peso_presupuestal),0),2) AS avance_fisico_ponderado_pct FROM obra o JOIN partida p ON p.id_obra=o.id_obra JOIN (SELECT DISTINCT p2.id_obra,a.fecha_reporte FROM avance_partida a JOIN partida p2 ON p2.id_partida=a.id_partida) fechas ON fechas.id_obra=o.id_obra GROUP BY o.id_obra,o.nombre,fechas.fecha_reporte ORDER BY o.nombre,fechas.fecha_reporte",
            "SELECT m.nombre AS material,m.unidad_medida,COALESCE((SELECT SUM(c.cantidad) FROM consumo_material c WHERE c.id_material=m.id_material),0) AS consumo_acumulado,m.stock_actual,m.stock_minimo FROM material m ORDER BY m.nombre",
            "SELECT e.dni,e.nombres_apellidos,e.cargo,(SELECT COUNT(*) FROM asistencia a WHERE a.id_empleado=e.id_empleado) AS registros_asistencia,(SELECT COALESCE(SUM(p.cantidad_producida),0) FROM produccion_empleado p WHERE p.id_empleado=e.id_empleado) AS produccion_total FROM empleado e ORDER BY e.nombres_apellidos",
            "SELECT o.nombre AS obra,COALESCE((SELECT SUM(i.monto) FROM ingreso i WHERE i.id_obra=o.id_obra),0) AS ingresos,COALESCE((SELECT SUM(e.monto) FROM egreso e WHERE e.id_obra=o.id_obra),0) AS egresos,COALESCE((SELECT SUM(i.monto) FROM ingreso i WHERE i.id_obra=o.id_obra),0)-COALESCE((SELECT SUM(e.monto) FROM egreso e WHERE e.id_obra=o.id_obra),0) AS saldo FROM obra o ORDER BY o.nombre"
        };
        String[] titulos={"Avance físico ejecutado ponderado","Consumo acumulado y stock","Desempeño del personal","Estado financiero por obra"};
        if(tipo<0||tipo>=sql.length)throw new IllegalArgumentException("Reporte desconocido.");
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();var st=c.createStatement();ResultSet rs=st.executeQuery(sql[tipo])){
            var md=rs.getMetaData();List<String> cols=new ArrayList<>();for(int i=1;i<=md.getColumnCount();i++)cols.add(md.getColumnLabel(i));List<Object[]> rows=new ArrayList<>();
            while(rs.next()){Object[] row=new Object[cols.size()];for(int i=0;i<row.length;i++)row[i]=rs.getObject(i+1);rows.add(row);}return new Reporte(titulos[tipo],cols,rows);
        }
    }
    public void exportarCsv(Reporte reporte,java.nio.file.Path path)throws Exception{
        try(var w=java.nio.file.Files.newBufferedWriter(path,java.nio.charset.StandardCharsets.UTF_8)){
            w.write("\uFEFF");w.write(reporte.columnas().stream().map(this::csv).collect(java.util.stream.Collectors.joining(",")));w.newLine();
            for(Object[] row:reporte.filas()){for(int i=0;i<row.length;i++){if(i>0)w.write(',');w.write(csv(row[i]==null?"":row[i].toString()));}w.newLine();}
        }
    }
    private String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
