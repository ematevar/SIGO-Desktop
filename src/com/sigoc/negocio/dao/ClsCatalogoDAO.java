package com.sigoc.negocio.dao;

import com.sigoc.config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CRUD parametrizado para tablas maestras permitidas. */
public class ClsCatalogoDAO {
    private static final Set<String> TABLAS=Set.of("obra","partida","material","proveedor","empleado","rol");
    private String tabla(String nombre){if(!TABLAS.contains(nombre))throw new IllegalArgumentException("Tabla de catálogo no permitida.");return nombre;}
    
    public List<String> columnas(String nombre)throws Exception{
        String t=tabla(nombre);List<String> out=new ArrayList<>();
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();var ps=c.prepareStatement("SELECT * FROM "+t+" LIMIT 0");ResultSet rs=ps.executeQuery()){
            for(int i=1;i<=rs.getMetaData().getColumnCount();i++){
                String col = rs.getMetaData().getColumnLabel(i);
                if (!col.equalsIgnoreCase("activo") && !col.equalsIgnoreCase("fecha_borrado")) {
                    out.add(col);
                }
            }
        }return out;
    }
    
    public String clavePrimaria(String nombre)throws Exception{
        String t=tabla(nombre);try(Connection c=ClsConexionBD.getInstancia().abrirConexion()){
            try(ResultSet rs=c.getMetaData().getPrimaryKeys(c.getCatalog(),null,t)){if(rs.next())return rs.getString("COLUMN_NAME");}
        }throw new IllegalStateException("La tabla no tiene clave primaria: "+t);
    }
    
    public List<Map<String,Object>> listar(String nombre)throws Exception{
        String t=tabla(nombre);List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();var st=c.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM "+t+" WHERE activo=1 ORDER BY 1 DESC LIMIT 500")){
            int n=rs.getMetaData().getColumnCount();while(rs.next()){
                Map<String,Object> row=new LinkedHashMap<>();
                for(int i=1;i<=n;i++){
                    String col = rs.getMetaData().getColumnLabel(i);
                    if (!col.equalsIgnoreCase("activo") && !col.equalsIgnoreCase("fecha_borrado")) {
                        row.put(col,rs.getObject(i));
                    }
                }
                out.add(row);
            }
        }return out;
    }
    
    public void guardar(String nombre,String pk,Object id,Map<String,Object> values)throws Exception{
        String t=tabla(nombre);List<String> cols=columnas(t);List<String> editables=cols.stream().filter(c->!c.equalsIgnoreCase(pk)&&!(t.equals("material")&&c.equals("stock_actual"))).toList();
        if(editables.isEmpty())throw new IllegalArgumentException("No hay campos para guardar.");
        String sql;
        if(id==null){String fields=String.join(",",editables);String marks=String.join(",",java.util.Collections.nCopies(editables.size(),"?"));sql="INSERT INTO "+t+" ("+fields+") VALUES ("+marks+")";}
        else{String assigns=String.join(",",editables.stream().map(c->c+"=?").toList());sql="UPDATE "+t+" SET "+assigns+" WHERE "+pk+"=?";}
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement ps=c.prepareStatement(sql)){
            int i=1;for(String col:editables)ps.setObject(i++,values.get(col));if(id!=null)ps.setObject(i,id);
            if(ps.executeUpdate()!=1)throw new IllegalStateException("No se guardó ningún registro.");
        }
    }
    
    public void eliminar(String nombre,String pk,Object id)throws Exception{
        String t=tabla(nombre);try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement ps=c.prepareStatement("UPDATE "+t+" SET activo=0, fecha_borrado=CURRENT_TIMESTAMP WHERE "+pk+"=?")){
            ps.setObject(1,id);if(ps.executeUpdate()!=1)throw new IllegalStateException("No se encontró el registro.");
        }
    }
}
