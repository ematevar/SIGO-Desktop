package com.sigoc.infrastructure.out.persistence;

import com.sigoc.infrastructure.config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import com.sigoc.application.port.out.AvanceRepositoryPort;

public class ClsAvanceDAO implements AvanceRepositoryPort {
    public Double ultimoPorcentaje(int idPartida) throws Exception {
        String sql="SELECT porcentaje_avance FROM avance_partida WHERE id_partida=? ORDER BY fecha_reporte DESC,id_avance DESC LIMIT 1";
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,idPartida);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getDouble(1):null;}
        }
    }
    public void registrar(int idPartida, LocalDate fecha, double porcentaje, String observaciones, int idUsuario) throws Exception {
        String sql="INSERT INTO avance_partida(id_partida,fecha_reporte,porcentaje_avance,observaciones,id_usuario) VALUES(?,?,?,?,?)";
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement lock=c.prepareStatement("SELECT id_partida FROM partida WHERE id_partida=? FOR UPDATE")){lock.setInt(1,idPartida);try(ResultSet rs=lock.executeQuery()){if(!rs.next())throw new IllegalArgumentException("La partida no existe.");}}
                try(PreparedStatement latest=c.prepareStatement("SELECT fecha_reporte,porcentaje_avance FROM avance_partida WHERE id_partida=? ORDER BY fecha_reporte DESC,id_avance DESC LIMIT 1")){latest.setInt(1,idPartida);try(ResultSet rs=latest.executeQuery()){if(rs.next()){if(fecha.isBefore(rs.getDate(1).toLocalDate()))throw new IllegalArgumentException("La fecha no puede ser anterior al último avance registrado.");if(porcentaje<rs.getDouble(2))throw new IllegalArgumentException("El avance acumulado no puede disminuir.");}}}
                try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,idPartida);ps.setDate(2,java.sql.Date.valueOf(fecha));ps.setDouble(3,porcentaje);ps.setString(4,observaciones);ps.setInt(5,idUsuario);ps.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
        }
    }
}
