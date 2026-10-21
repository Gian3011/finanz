package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.GastoHormigaServicio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaGastoHormigaServicio {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        MovimientoDAO movimientoDAO = new MovimientoDAO();
        GastoHormigaServicio hormigaServicio = new GastoHormigaServicio(movimientoDAO);

        try {
            // 0. Usuario y categoría de prueba
            Usuario usuario = new Usuario();
            usuario.setNombres("Pedro");
            usuario.setApellidos("Diaz");
            usuario.setEmail("pedro.diaz.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);

            int idCategoria = categoriaDAO.insertar(new Categoria("Snacks", "GASTO"));
            System.out.println("Usuario id: " + idUsuario + " | Categoria id: " + idCategoria);

            // 1. Primer gasto pequeño: todavía no hay frecuencia acumulada -> no es hormiga aún
            Gasto gasto1 = new Gasto(idUsuario, idCategoria, new BigDecimal("8.00"), LocalDate.now(), "Galletas");
            boolean esHormiga1 = hormigaServicio.evaluarGastoHormiga(gasto1);
            gasto1.setEsGastoHormiga(esHormiga1);
            movimientoDAO.insertar(gasto1);
            System.out.println("Gasto 1 (S/8.00) -> esGastoHormiga: " + esHormiga1);

            // 2. Insertamos 4 gastos pequeños más en la misma categoría (total 5)
            for (int i = 2; i <= 7; i++) {
                Gasto g = new Gasto(idUsuario, idCategoria, new BigDecimal("7.50"), LocalDate.now(), "Snack " + i);
                boolean esHormiga = hormigaServicio.evaluarGastoHormiga(g);
                g.setEsGastoHormiga(esHormiga);
                movimientoDAO.insertar(g);
                System.out.println("Gasto " + i + " (S/7.50) -> esGastoHormiga: " + esHormiga);
            }

            // 3. Un gasto grande en la misma categoría -> nunca debe marcarse como hormiga
            Gasto gastoGrande = new Gasto(idUsuario, idCategoria, new BigDecimal("50.00"), LocalDate.now(), "Compra grande");
            boolean esHormigaGrande = hormigaServicio.evaluarGastoHormiga(gastoGrande);
            gastoGrande.setEsGastoHormiga(esHormigaGrande);
            movimientoDAO.insertar(gastoGrande);
            System.out.println("Gasto grande (S/50.00) -> esGastoHormiga: " + esHormigaGrande + " (esperado: false)");

            // 4. Limpiar datos de prueba (elimina todos los movimientos del usuario de prueba)
            movimientoDAO.listarPorUsuario(idUsuario).forEach(m -> {
                try {
                    movimientoDAO.eliminar(m.getIdMovimiento());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            categoriaDAO.eliminar(idCategoria);
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
