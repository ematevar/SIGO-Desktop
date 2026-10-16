package com.sigoc.negocio.dao;

import com.sigoc.config.ClsConexionBD;
import com.sigoc.entidad.ClsUsuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClsUsuarioDAO {
    public boolean autenticarPorId(int usuario,String clave)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("SELECT contrasena_hash FROM usuario WHERE id_usuario=? AND estado=1")){
            p.setInt(1,usuario);try(ResultSet rs=p.executeQuery()){return rs.next()&&verificar(clave,rs.getString(1));}
        }
    }
    public java.util.List<java.util.Map<String,Object>> listar() throws Exception {
        var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();var s=c.createStatement();ResultSet rs=s.executeQuery("SELECT u.id_usuario,u.nombre_completo,u.usuario_login,r.nombre_rol,u.estado FROM usuario u JOIN rol r ON r.id_rol=u.id_rol ORDER BY u.id_usuario")){
            while(rs.next()){var row=new java.util.LinkedHashMap<String,Object>();row.put("id_usuario",rs.getInt(1));row.put("nombre_completo",rs.getString(2));row.put("usuario_login",rs.getString(3));row.put("nombre_rol",rs.getString(4));row.put("estado",rs.getInt(5));rows.add(row);}
        }return rows;
    }
    public java.util.List<java.util.Map<String,Object>> roles() throws Exception {
        var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();var s=c.createStatement();ResultSet rs=s.executeQuery("SELECT id_rol,nombre_rol FROM rol ORDER BY nombre_rol")){
            while(rs.next()){var row=new java.util.LinkedHashMap<String,Object>();row.put("id",rs.getInt(1));row.put("nombre",rs.getString(2));rows.add(row);}
        }return rows;
    }
    public void crear(String nombre,String login,String hash,int rol) throws Exception {
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado) VALUES(?,?,?,?,1)")){
            p.setString(1,nombre);p.setString(2,login);p.setString(3,hash);p.setInt(4,rol);p.executeUpdate();
        }
    }
    public void estado(int usuario,boolean activo)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("UPDATE usuario SET estado=? WHERE id_usuario=?")){p.setInt(1,activo?1:0);p.setInt(2,usuario);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Usuario no encontrado.");}
    }
    public void actualizarDatos(int usuario,String nombre,int rol)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("UPDATE usuario SET nombre_completo=?,id_rol=? WHERE id_usuario=?")){p.setString(1,nombre);p.setInt(2,rol);p.setInt(3,usuario);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Usuario no encontrado.");}
    }
    public void actualizarClave(int usuario,String hash)throws Exception{
        try(Connection c=ClsConexionBD.getInstancia().abrirConexion();PreparedStatement p=c.prepareStatement("UPDATE usuario SET contrasena_hash=? WHERE id_usuario=?")){p.setString(1,hash);p.setInt(2,usuario);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Usuario no encontrado.");}
    }
    public ClsUsuario autenticar(String usuario, String clave) throws Exception {
        String sql = "SELECT u.id_usuario,u.usuario_login,u.contrasena_hash,r.nombre_rol FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.usuario_login=? AND u.estado=1";
        try (Connection c=ClsConexionBD.getInstancia().abrirConexion(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next() || !verificar(clave, rs.getString("contrasena_hash"))) return null;
                return new ClsUsuario(rs.getInt("id_usuario"),rs.getString("usuario_login"),rs.getString("nombre_rol"));
            }
        }
    }
    private boolean verificar(String clave, String almacenada) throws Exception {
        if (almacenada == null || !almacenada.startsWith("PBKDF2$")) return false;
        String[] p=almacenada.split("\\$",-1); if (p.length!=4) return false;
        int rondas=Integer.parseInt(p[1]); if (rondas<10000 || rondas>1000000) return false;
        var spec=new javax.crypto.spec.PBEKeySpec(clave.toCharArray(),java.util.Base64.getDecoder().decode(p[2]),rondas,256);
        byte[] esperado=java.util.Base64.getDecoder().decode(p[3]);
        byte[] actual=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return java.security.MessageDigest.isEqual(esperado,actual);
    }
}
