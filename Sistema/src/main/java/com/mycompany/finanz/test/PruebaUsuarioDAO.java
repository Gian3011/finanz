package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Usuario;

import java.time.LocalDate;

/**
 * Clase de prueba temporal. NO es parte del sistema final,
 * solo sirve para verificar que el DAO funciona antes de
 * conectarlo a un Servlet. Se puede borrar más adelante.
 */
public class PruebaUsuarioDAO {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        try {
            // 1. Insertar un usuario de prueba
            Usuario nuevo = new Usuario();
            nuevo.setNombres("Juan");
            nuevo.setApellidos("Perez");
            nuevo.setEmail("juan.perez.prueba@correo.com");
            nuevo.setFechaRegistro(LocalDate.now());
            nuevo.setPasswordHash("hash_de_prueba_123"); // luego será BCrypt real
            nuevo.setProveedor("local");
            nuevo.setIntentosFallidos(0);
            nuevo.setBloqueado(false);

            int idGenerado = usuarioDAO.insertar(nuevo);
            System.out.println("Usuario insertado con id: " + idGenerado);

            // 2. Buscarlo por id
            Usuario encontradoPorId = usuarioDAO.buscarPorId(idGenerado);
            System.out.println("Encontrado por id: " + encontradoPorId.getNombres() + " " + encontradoPorId.getApellidos());

            // 3. Buscarlo por email
            Usuario encontradoPorEmail = usuarioDAO.buscarPorEmail("juan.perez.prueba@correo.com");
            System.out.println("Encontrado por email: " + encontradoPorEmail.getEmail());

            // 4. Actualizar intentos fallidos
            usuarioDAO.actualizarIntentosFallidos(idGenerado, 2);
            Usuario actualizado = usuarioDAO.buscarPorId(idGenerado);
            System.out.println("Intentos fallidos actualizados: " + actualizado.getIntentosFallidos());

            // 5. Eliminar el usuario de prueba (para no dejar basura en la BD)
            usuarioDAO.eliminar(idGenerado);
            System.out.println("Usuario de prueba eliminado.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
