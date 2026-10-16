package com.sigoc.negocio.dao;

import com.sigoc.config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class ClsMaterialDAO {
    public void registrarConsumo(int material,int partida,int usuario,LocalDate fecha,double cantidad) throws Exception {
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion()){
            c.setAutoCommit(false);
            try {
                double stock;
                try(PreparedStatement ps=c.prepareStatement("SELECT stock_actual FROM material WHERE id_material=? FOR UPDATE")){
                    ps.setInt(1,material);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("El material seleccionado no existe.");stock=rs.getDouble(1);}
                }
                if(stock<cantidad)throw new IllegalArgumentException("Stock insuficiente. Disponible: "+stock);
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO consumo_material(id_partida,id_material,fecha_consumo,cantidad,id_usuario) VALUES(?,?,?,?,?)")){
                    ps.setInt(1,partida);ps.setInt(2,material);ps.setDate(3,java.sql.Date.valueOf(fecha));ps.setDouble(4,cantidad);ps.setInt(5,usuario);ps.executeUpdate();
                }
                try(PreparedStatement ps=c.prepareStatement("UPDATE material SET stock_actual=stock_actual-? WHERE id_material=?")){
                    ps.setDouble(1,cantidad);ps.setInt(2,material);ps.executeUpdate();
                }
                c.commit();
            } catch(Exception ex){c.rollback();throw ex;} finally {c.setAutoCommit(true);}
        }
    }
    public double stockMinimo(int material) throws Exception {
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement ps=c.prepareStatement("SELECT stock_actual,stock_minimo FROM material WHERE id_material=?")){
            ps.setInt(1,material);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("El material seleccionado no existe.");return rs.getDouble(1)-rs.getDouble(2);}
        }
    }
}
