package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.util.PasswordUtil;

import java.sql.SQLException;

/**
 * Registro de cuentas nuevas y gestión del perfil (nombre y contraseña).
 */
public class UsuarioServicio {

    private final UsuarioDAO usuarioDAO;

    public UsuarioServicio(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public Usuario registrar(String nombres, String apellidos, String email,
                             String contrasena, String confirmacion) throws ValidacionException, SQLException {
        Usuario usuario = new Usuario(
                Validar.texto(nombres, "nombres", 100),
                Validar.texto(apellidos, "apellidos", 100),
                Validar.email(email));

        Validar.contrasenaSegura(contrasena);
        if (!contrasena.equals(confirmacion)) {
            throw new ValidacionException("Las contraseñas no coinciden.");
        }
        if (usuarioDAO.buscarPorEmail(usuario.getEmail()) != null) {
            throw new ValidacionException("Ya existe una cuenta registrada con ese correo.");
        }

        usuario.setPasswordHash(PasswordUtil.hashear(contrasena));
        usuarioDAO.insertar(usuario);
        return usuario;
    }

    public void actualizarPerfil(int idUsuario, String nombres, String apellidos) throws ValidacionException, SQLException {
        usuarioDAO.actualizarPerfil(idUsuario,
                Validar.texto(nombres, "nombres", 100),
                Validar.texto(apellidos, "apellidos", 100));
    }

    public void cambiarContrasena(int idUsuario, String actual, String nueva, String confirmacion)
            throws ValidacionException, SQLException {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null || !PasswordUtil.verificar(actual, usuario.getPasswordHash())) {
            throw new ValidacionException("La contraseña actual no es correcta.");
        }
        Validar.contrasenaSegura(nueva);
        if (!nueva.equals(confirmacion)) {
            throw new ValidacionException("Las contraseñas nuevas no coinciden.");
        }
        usuarioDAO.actualizarPassword(idUsuario, PasswordUtil.hashear(nueva));
    }

    public Usuario buscar(int idUsuario) throws SQLException {
        return usuarioDAO.buscarPorId(idUsuario);
    }
}
