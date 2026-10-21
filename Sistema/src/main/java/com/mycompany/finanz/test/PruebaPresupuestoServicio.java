package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.LimiteGastoDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.PresupuestoDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.PresupuestoServicio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaPresupuestoServicio {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        MovimientoDAO movimientoDAO = new MovimientoDAO();
        PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
        LimiteGastoDAO limiteGastoDAO = new LimiteGastoDAO();
        PresupuestoServicio presupuestoServicio = new PresupuestoServicio(movimientoDAO, limiteGastoDAO);

        try {
            // 0. Usuario y categoría de prueba
            Usuario usuario = new Usuario();
            usuario.setNombres("Rosa");
            usuario.setApellidos("Fernandez");
            usuario.setEmail("rosa.fernandez.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);

            int idCategoria = categoriaDAO.insertar(new Categoria("Entretenimiento", "GASTO"));
            System.out.println("Usuario id: " + idUsuario + " | Categoria id: " + idCategoria);

            // 1. Presupuesto del mes actual
            LocalDate periodo = LocalDate.now().withDayOfMonth(1);
            Presupuesto presupuesto = new Presupuesto(idUsuario, periodo, new BigDecimal("1000.00"));
            int idPresupuesto = presupuestoDAO.insertar(presupuesto);
            presupuesto.setIdPresupuesto(idPresupuesto);
            System.out.println("Presupuesto insertado: S/ " + presupuesto.getMontoPresupuesto());

            // 2. Límite para la categoría "Entretenimiento"
            LimiteGasto limite = new LimiteGasto(idPresupuesto, idCategoria, new BigDecimal("100.00"));
            limiteGastoDAO.insertar(limite);
            System.out.println("Limite de categoria: S/ " + limite.getMontoLimite());

            // 3. Sin gastos aún: disponible = monto total, alerta = NINGUNA
            System.out.println("Disponible sin gastos: S/ " + presupuestoServicio.calcularDisponible(presupuesto));
            System.out.println("Alerta sin gastos: " + presupuestoServicio.evaluarAlerta(presupuesto));

            // 4. Registrar un gasto de 850 (85% del presupuesto de 1000) -> debería marcar CERCA
            Gasto gasto1 = new Gasto(idUsuario, idCategoria, new BigDecimal("850.00"), LocalDate.now(), "Salidas");
            movimientoDAO.insertar(gasto1);
            System.out.println("Disponible tras gasto de 850: S/ " + presupuestoServicio.calcularDisponible(presupuesto));
            System.out.println("Alerta tras gasto de 850: " + presupuestoServicio.evaluarAlerta(presupuesto) + " (esperado: CERCA)");

            // 5. Verificar límite de categoría: un gasto adicional de 50 en "Entretenimiento" supera el límite de 100
            boolean supera = presupuestoServicio.superaLimiteCategoria(presupuesto, idCategoria, new BigDecimal("50.00"));
            System.out.println("¿Supera limite de categoria con +50? " + supera + " (esperado: true, porque 850+50 > 100... "
                    + "nota: el gasto anterior ya estaba en esa categoria)");

            // 6. Registrar otro gasto para superar el presupuesto total
            Gasto gasto2 = new Gasto(idUsuario, idCategoria, new BigDecimal("300.00"), LocalDate.now(), "Extra");
            movimientoDAO.insertar(gasto2);
            System.out.println("Disponible tras gasto adicional de 300: S/ " + presupuestoServicio.calcularDisponible(presupuesto));
            System.out.println("Alerta final: " + presupuestoServicio.evaluarAlerta(presupuesto) + " (esperado: SUPERADO)");

            // 7. Limpiar
            movimientoDAO.listarPorUsuario(idUsuario).forEach(m -> {
                try {
                    movimientoDAO.eliminar(m.getIdMovimiento());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
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
