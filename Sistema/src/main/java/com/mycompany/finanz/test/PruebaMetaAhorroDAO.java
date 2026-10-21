package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.AporteAhorroDAO;
import com.mycompany.finanz.dao.MetaAhorroDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.AporteAhorro;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaMetaAhorroDAO {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        MetaAhorroDAO metaAhorroDAO = new MetaAhorroDAO();
        AporteAhorroDAO aporteAhorroDAO = new AporteAhorroDAO();

        try {
            // 0. Usuario de prueba
            Usuario usuario = new Usuario();
            usuario.setNombres("Carla");
            usuario.setApellidos("Vega");
            usuario.setEmail("carla.vega.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);
            System.out.println("Usuario de prueba creado con id: " + idUsuario);

            // 1. Crear meta de ahorro (requerimiento 10)
            MetaAhorro meta = new MetaAhorro(idUsuario, "Viaje a Cusco", new BigDecimal("1200.00"),
                    LocalDate.now().plusMonths(6));
            int idMeta = metaAhorroDAO.insertar(meta);
            System.out.println("Meta insertada con id: " + idMeta + " (estado inicial: " + meta.getEstado() + ")");

            // 2. Registrar aportes (requerimiento 11)
            aporteAhorroDAO.insertar(new AporteAhorro(idMeta, new BigDecimal("200.00"), LocalDate.now()));
            aporteAhorroDAO.insertar(new AporteAhorro(idMeta, new BigDecimal("150.00"), LocalDate.now()));
            System.out.println("Dos aportes registrados.");

            // 3. Listar aportes de la meta
            List<AporteAhorro> aportes = aporteAhorroDAO.listarPorMeta(idMeta);
            System.out.println("Total de aportes: " + aportes.size());

            // 4. Calcular progreso (requerimiento 12)
            BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(idMeta);
            MetaAhorro metaActual = metaAhorroDAO.buscarPorId(idMeta);
            double progreso = totalAportado.doubleValue() / metaActual.getMontoObjetivo().doubleValue() * 100;
            System.out.println("Aportado: S/ " + totalAportado + " de S/ " + metaActual.getMontoObjetivo()
                    + " -> Progreso: " + String.format("%.1f", progreso) + "%");

            // 5. Listar metas del usuario
            List<MetaAhorro> metas = metaAhorroDAO.listarPorUsuario(idUsuario);
            System.out.println("Total de metas del usuario: " + metas.size());

            // 6. Actualizar estado de la meta
            metaAhorroDAO.actualizarEstado(idMeta, "EN_PROGRESO");
            System.out.println("Estado actualizado a: " + metaAhorroDAO.buscarPorId(idMeta).getEstado());

            // 7. Limpiar (primero aportes, luego meta, luego usuario, por las Foreign Keys)
            aporteAhorroDAO.eliminarPorMeta(idMeta);
            metaAhorroDAO.eliminar(idMeta);
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
