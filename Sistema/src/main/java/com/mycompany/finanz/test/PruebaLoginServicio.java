package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.CredencialesInvalidasException;
import com.mycompany.finanz.servicio.CuentaBloqueadaException;
import com.mycompany.finanz.servicio.LoginServicio;

import java.time.LocalDate;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaLoginServicio {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        LoginServicio loginServicio = new LoginServicio(usuarioDAO);

        try {
            // 0. Crear usuario de prueba (contraseña en texto plano por ahora)
            Usuario usuario = new Usuario();
            usuario.setNombres("Tomas");
            usuario.setApellidos("Vidal");
            usuario.setEmail("tomas.vidal.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("clave123"); // texto plano por ahora, será BCrypt después
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);
            System.out.println("Usuario de prueba creado con id: " + idUsuario);

            // 1. Login con contraseña correcta
            Usuario logueado = loginServicio.login("tomas.vidal.prueba@correo.com", "clave123");
            System.out.println("Login correcto para: " + logueado.getNombres());

            // 2. Login con contraseña incorrecta, 3 veces seguidas -> debe bloquear
            for (int intento = 1; intento <= 3; intento++) {
                try {
                    loginServicio.login("tomas.vidal.prueba@correo.com", "clave_incorrecta");
                } catch (CredencialesInvalidasException e) {
                    System.out.println("Intento fallido " + intento + ": " + e.getMessage());
                } catch (CuentaBloqueadaException e) {
                    System.out.println("Bloqueada en el intento " + intento + ": " + e.getMessage());
                }
            }

            // 3. Intentar login de nuevo (incluso con la contraseña correcta) -> debe seguir bloqueada
            try {
                loginServicio.login("tomas.vidal.prueba@correo.com", "clave123");
                System.out.println("ERROR: no debería haber dejado entrar, la cuenta debía estar bloqueada");
            } catch (CuentaBloqueadaException e) {
                System.out.println("Correcto: la cuenta sigue bloqueada -> " + e.getMessage());
            }

            // 4. Confirmar el estado en la base de datos
            Usuario estadoFinal = usuarioDAO.buscarPorId(idUsuario);
            System.out.println("Intentos fallidos guardados: " + estadoFinal.getIntentosFallidos());
            System.out.println("Bloqueado: " + estadoFinal.isBloqueado() + " (esperado: true)");

            // 5. Limpiar
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
