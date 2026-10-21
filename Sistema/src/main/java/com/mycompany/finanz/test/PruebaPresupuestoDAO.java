package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.LimiteGastoDAO;
import com.mycompany.finanz.dao.PresupuestoDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaPresupuestoDAO {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
        LimiteGastoDAO limiteGastoDAO = new LimiteGastoDAO();

        try {
            // 0. Datos previos: usuario y categoría de prueba
            Usuario usuario = new Usuario();
            usuario.setNombres("Luis");
            usuario.setApellidos("Rojas");
            usuario.setEmail("luis.rojas.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);

            int idCategoria = categoriaDAO.insertar(new Categoria("Transporte", "GASTO"));
            System.out.println("Usuario id: " + idUsuario + " | Categoria id: " + idCategoria);

            // 1. Insertar presupuesto (periodo = primer día del mes)
            LocalDate periodoSeptiembre = LocalDate.of(2026, 9, 1);
            Presupuesto presupuesto = new Presupuesto(idUsuario, periodoSeptiembre, new BigDecimal("2000.00"));
            int idPresupuesto = presupuestoDAO.insertar(presupuesto);
            System.out.println("Presupuesto insertado con id: " + idPresupuesto);

            // 2. Buscar por usuario y período
            Presupuesto encontrado = presupuestoDAO.buscarPorUsuarioYPeriodo(idUsuario, periodoSeptiembre);
            System.out.println("Presupuesto de 2026-09: S/ " + encontrado.getMontoPresupuesto());

            // 3. Actualizar monto
            encontrado.setMontoPresupuesto(new BigDecimal("2500.00"));
            presupuestoDAO.actualizar(encontrado);
            System.out.println("Monto actualizado: S/ " + presupuestoDAO.buscarPorId(idPresupuesto).getMontoPresupuesto());

            // 4. Insertar límite por categoría
            LimiteGasto limite = new LimiteGasto(idPresupuesto, idCategoria, new BigDecimal("300.00"));
            int idLimite = limiteGastoDAO.insertar(limite);
            System.out.println("Limite insertado con id: " + idLimite);

            // 5. Buscar límite por presupuesto y categoría
            LimiteGasto limiteEncontrado = limiteGastoDAO.buscarPorPresupuestoYCategoria(idPresupuesto, idCategoria);
            System.out.println("Limite de la categoría: S/ " + limiteEncontrado.getMontoLimite());

            // 6. Listar límites del presupuesto
            List<LimiteGasto> limites = limiteGastoDAO.listarPorPresupuesto(idPresupuesto);
            System.out.println("Total de límites en el presupuesto: " + limites.size());

            // 7. Limpiar (primero límites, luego presupuesto, categoría y usuario, por las Foreign Keys)
            limiteGastoDAO.eliminarPorPresupuesto(idPresupuesto);
            presupuestoDAO.eliminar(idPresupuesto);
            categoriaDAO.eliminar(idCategoria);
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
