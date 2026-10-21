package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Ingreso;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Clase de prueba temporal. NO es parte del sistema final,
 * solo sirve para verificar que el DAO funciona antes de
 * conectarlo a un Servlet. Se puede borrar más adelante.
 */
public class PruebaMovimientoDAO {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        MovimientoDAO movimientoDAO = new MovimientoDAO();

        try {
            // 0. Crear un usuario y una categoría de prueba (movimiento depende de ambos)
            Usuario usuario = new Usuario();
            usuario.setNombres("Ana");
            usuario.setApellidos("Torres");
            usuario.setEmail("ana.torres.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);
            System.out.println("Usuario de prueba creado con id: " + idUsuario);

            Categoria categoria = new Categoria("Snacks", "GASTO");
            int idCategoria = categoriaDAO.insertar(categoria);
            System.out.println("Categoria de prueba creada con id: " + idCategoria);

            // 1. Insertar un Ingreso
            Ingreso ingreso = new Ingreso(idUsuario, idCategoria, new BigDecimal("1500.00"), LocalDate.now(), "Sueldo del mes");
            int idIngreso = movimientoDAO.insertar(ingreso);
            System.out.println("Ingreso insertado con id: " + idIngreso);

            // 2. Insertar un Gasto
            Gasto gasto = new Gasto(idUsuario, idCategoria, new BigDecimal("8.50"), LocalDate.now(), "Galletas");
            gasto.setEsGastoHormiga(true);
            int idGasto = movimientoDAO.insertar(gasto);
            System.out.println("Gasto insertado con id: " + idGasto);

            // 3. Buscar por id y confirmar el tipo real del objeto (polimorfismo)
            Movimiento encontrado1 = movimientoDAO.buscarPorId(idIngreso);
            System.out.println("Movimiento " + idIngreso + " es instancia de: " + encontrado1.getClass().getSimpleName()
                    + " | aplicarABalance() = " + encontrado1.aplicarABalance());

            Movimiento encontrado2 = movimientoDAO.buscarPorId(idGasto);
            System.out.println("Movimiento " + idGasto + " es instancia de: " + encontrado2.getClass().getSimpleName()
                    + " | aplicarABalance() = " + encontrado2.aplicarABalance());

            // 4. Listar todos los movimientos del usuario
            List<Movimiento> movimientos = movimientoDAO.listarPorUsuario(idUsuario);
            System.out.println("Total de movimientos del usuario: " + movimientos.size());

            // 5. Probar el conteo para gasto hormiga
            int frecuencia = movimientoDAO.contarGastosPorCategoriaEnPeriodo(idUsuario, idCategoria, 30);
            System.out.println("Gastos en categoria 'Snacks' en los últimos 30 días: " + frecuencia);

            // 6. Actualizar el flag de gasto hormiga
            movimientoDAO.actualizarFlagHormiga(idGasto, false);
            Movimiento actualizado = movimientoDAO.buscarPorId(idGasto);
            System.out.println("Flag hormiga actualizado a: " + ((Gasto) actualizado).getEsGastoHormiga());

            // 7. Limpiar todo lo de prueba
            movimientoDAO.eliminar(idIngreso);
            movimientoDAO.eliminar(idGasto);
            categoriaDAO.eliminar(idCategoria);
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
