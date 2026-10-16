package com.sigoc.negocio;

import com.sigoc.entidad.ClsUsuario;
import com.sigoc.negocio.dao.ClsUsuarioDAO;

public class ClsUsuarioService {
    private final ClsUsuarioDAO dao = new ClsUsuarioDAO();
    private final ClsCaptchaService captcha = new ClsCaptchaService();
    public String nuevoCaptcha() { return captcha.generar(); }
    public java.util.List<java.util.Map<String,Object>> listar() throws Exception { return dao.listar(); }
    public java.util.List<java.util.Map<String,Object>> roles() throws Exception { return dao.roles(); }
    public void crear(String nombre,String usuario,String clave,int rol)throws Exception {
        if(nombre==null||nombre.isBlank()||usuario==null||!usuario.matches("[A-Za-z0-9._-]{3,40}"))throw new IllegalArgumentException("Completa el nombre y un usuario válido (3 a 40 caracteres).");
        if(clave==null||clave.length()<8)throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if(rol<=0)throw new IllegalArgumentException("Selecciona un rol.");dao.crear(nombre.trim(),usuario.trim(),hash(clave),rol);
    }
    public void cambiarEstado(int usuario,boolean activo)throws Exception{if(usuario<=0)throw new IllegalArgumentException("Usuario inválido.");dao.estado(usuario,activo);}
    public void actualizarDatos(int usuario,String nombre,int rol)throws Exception{if(usuario<=0||nombre==null||nombre.isBlank()||rol<=0)throw new IllegalArgumentException("Nombre o rol inválido.");dao.actualizarDatos(usuario,nombre.trim(),rol);}
    public void cambiarClave(int usuario,String actual,String nueva)throws Exception{
        if(nueva==null||nueva.length()<8)throw new IllegalArgumentException("La contraseña nueva debe tener al menos 8 caracteres.");
        if(!dao.autenticarPorId(usuario,actual))throw new IllegalArgumentException("La contraseña actual no es correcta.");dao.actualizarClave(usuario,hash(nueva));
    }
    private String hash(String clave)throws Exception{
        byte[] salt=new byte[16];new java.security.SecureRandom().nextBytes(salt);
        var spec=new javax.crypto.spec.PBEKeySpec(clave.toCharArray(),salt,120000,256);
        byte[] result=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return "PBKDF2$120000$"+java.util.Base64.getEncoder().encodeToString(salt)+"$"+java.util.Base64.getEncoder().encodeToString(result);
    }
    public ClsUsuario iniciarSesion(String usuario, String clave, String captchaEsperado, String captchaIngresado) throws Exception {
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) throw new IllegalArgumentException("Ingresa usuario y contraseña.");
        if (!captcha.validar(captchaEsperado,captchaIngresado)) throw new IllegalArgumentException("Captcha inválido.");
        ClsUsuario resultado=dao.autenticar(usuario.trim(),clave);
        if (resultado==null) throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        return resultado;
    }
}
