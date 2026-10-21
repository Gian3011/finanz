package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;

import java.sql.SQLException;

/**
 * Lógica de negocio del login: valida credenciales y aplica el
 * bloqueo de cuenta tras 3 intentos fallidos.
 *
 * IMPORTANTE - VERSIÓN TEMPORAL:
 * Por ahora la contraseña se compara en texto plano (equals). Esto es
 * solo mientras se prueba el flujo de Servlets/JSP. Antes de pasar a
 * producción, hay que reemplazar el método coincideContrasena() para
 * usar BCrypt.checkpw(contrasenaIngresada, usuario.getPasswordHash()),
 * y en el registro de usuarios guardar BCrypt.hashpw(...) en vez del
 * texto plano. Ese es el ÚNICO método que hay que tocar para migrar.
 */
public class LoginServicio {

    private static final int MAX_INTENTOS_FALLIDOS = 3;

    private final UsuarioDAO usuarioDAO;

    public LoginServicio(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Intenta autenticar al usuario. Si tiene éxito, resetea sus intentos
     * fallidos y devuelve el Usuario. Si falla, incrementa sus intentos
     * fallidos y, al llegar a 3, bloquea la cuenta.
     */
    public Usuario login(String email, String contrasenaIngresada)
            throws SQLException, CredencialesInvalidasException, CuentaBloqueadaException {

        Usuario usuario = usuarioDAO.buscarPorEmail(email);

        if (usuario == null) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        if (usuario.isBloqueado()) {
            throw new CuentaBloqueadaException(
                    "Tu cuenta está bloqueada por múltiples intentos fallidos. Contacta a soporte.");
        }

        if (!coincideContrasena(contrasenaIngresada, usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario);
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        // Login correcto: resetear el contador de intentos fallidos
        if (usuario.getIntentosFallidos() > 0) {
            usuarioDAO.actualizarIntentosFallidos(usuario.getIdUsuario(), 0);
        }

        return usuario;
    }

    /**
     * TODO: reemplazar por BCrypt.checkpw(contrasenaIngresada, hashGuardado)
     * cuando se implemente la encriptación de contraseñas.
     */
    private boolean coincideContrasena(String contrasenaIngresada, String hashGuardado) {
        return contrasenaIngresada != null && contrasenaIngresada.equals(hashGuardado);
    }

    private void registrarIntentoFallido(Usuario usuario) throws SQLException {
        int nuevosIntentos = usuario.getIntentosFallidos() + 1;
        usuarioDAO.actualizarIntentosFallidos(usuario.getIdUsuario(), nuevosIntentos);

        if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
            usuarioDAO.actualizarBloqueado(usuario.getIdUsuario(), true);
        }
    }
}
