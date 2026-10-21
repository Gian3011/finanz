package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.util.PasswordUtil;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Lógica de negocio del login: valida credenciales contra el hash guardado
 * y bloquea la cuenta 15 minutos tras 3 intentos fallidos seguidos.
 */
public class LoginServicio {

    static final int MAX_INTENTOS_FALLIDOS = 3;
    static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioDAO usuarioDAO;

    public LoginServicio(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Intenta autenticar al usuario. Si tiene éxito, resetea sus intentos
     * fallidos y devuelve el Usuario. Si falla, incrementa sus intentos
     * fallidos y, al llegar a 3, bloquea la cuenta temporalmente.
     */
    public Usuario login(String email, String contrasenaIngresada)
            throws SQLException, CredencialesInvalidasException, CuentaBloqueadaException {

        if (email == null || email.isBlank() || contrasenaIngresada == null || contrasenaIngresada.isEmpty()) {
            throw new CredencialesInvalidasException("Ingresa tu correo y contraseña.");
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim().toLowerCase());
        if (usuario == null) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }

        if (usuario.estaBloqueado()) {
            long minutos = Math.max(1, Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).toMinutes() + 1);
            throw new CuentaBloqueadaException("Tu cuenta está bloqueada por varios intentos fallidos. "
                    + "Inténtalo nuevamente en " + minutos + " minuto(s).");
        }

        if (!PasswordUtil.verificar(contrasenaIngresada, usuario.getPasswordHash())) {
            int restantes = registrarIntentoFallido(usuario);
            if (restantes == 0) {
                throw new CuentaBloqueadaException("Superaste los " + MAX_INTENTOS_FALLIDOS
                        + " intentos permitidos. Tu cuenta quedó bloqueada por " + MINUTOS_BLOQUEO + " minutos.");
            }
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos. Te quedan "
                    + restantes + " intento(s).");
        }

        // Login correcto: resetear el contador de intentos fallidos
        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuarioDAO.actualizarIntentos(usuario.getIdUsuario(), 0, null);
        }
        return usuario;
    }

    /** Devuelve cuántos intentos le quedan (0 = la cuenta acaba de bloquearse). */
    private int registrarIntentoFallido(Usuario usuario) throws SQLException {
        int nuevosIntentos = usuario.getIntentosFallidos() + 1;

        if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
            usuarioDAO.actualizarIntentos(usuario.getIdUsuario(), 0,
                    LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            return 0;
        }
        usuarioDAO.actualizarIntentos(usuario.getIdUsuario(), nuevosIntentos, null);
        return MAX_INTENTOS_FALLIDOS - nuevosIntentos;
    }
}
