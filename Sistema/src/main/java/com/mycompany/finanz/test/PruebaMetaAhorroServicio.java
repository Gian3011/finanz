package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.AporteAhorroDAO;
import com.mycompany.finanz.dao.MetaAhorroDAO;
import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.AporteAhorro;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.MetaAhorroServicio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Clase de prueba temporal. NO es parte del sistema final.
 */
public class PruebaMetaAhorroServicio {

    public static void main(String[] args) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        MetaAhorroDAO metaAhorroDAO = new MetaAhorroDAO();
        AporteAhorroDAO aporteAhorroDAO = new AporteAhorroDAO();
        MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio(metaAhorroDAO, aporteAhorroDAO);

        try {
            // 0. Usuario de prueba
            Usuario usuario = new Usuario();
            usuario.setNombres("Diego");
            usuario.setApellidos("Salas");
            usuario.setEmail("diego.salas.prueba@correo.com");
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setPasswordHash("hash_prueba");
            usuario.setProveedor("local");
            int idUsuario = usuarioDAO.insertar(usuario);
            System.out.println("Usuario de prueba creado con id: " + idUsuario);

            // 1. Crear meta: S/ 1200 en 6 meses
            MetaAhorro meta = new MetaAhorro(idUsuario, "Viaje a Cusco", new BigDecimal("1200.00"),
                    LocalDate.now().plusMonths(6));
            int idMeta = metaAhorroDAO.insertar(meta);
            System.out.println("Meta insertada con id: " + idMeta);

            // 2. Simulador ANTES de aportar nada: ¿cuánto debería aportar cada mes?
            BigDecimal aporteSugerido = metaAhorroServicio.simularAporteMensualNecesario(idMeta);
            System.out.println("Aporte mensual sugerido (sin aportes previos): S/ " + aporteSugerido
                    + " (esperado ~200.00, ya que 1200/6 = 200)");

            // 3. Registrar dos aportes
            aporteAhorroDAO.insertar(new AporteAhorro(idMeta, new BigDecimal("300.00"), LocalDate.now()));
            aporteAhorroDAO.insertar(new AporteAhorro(idMeta, new BigDecimal("200.00"), LocalDate.now()));
            System.out.println("Aportes registrados: S/300 + S/200 = S/500");

            // 4. Calcular progreso
            double progreso = metaAhorroServicio.calcularProgreso(idMeta);
            System.out.println("Progreso: " + String.format("%.2f", progreso) + "% (esperado ~41.67%)");

            // 5. Simulador DESPUÉS de los aportes (debería bajar el monto mensual sugerido)
            BigDecimal aporteSugerido2 = metaAhorroServicio.simularAporteMensualNecesario(idMeta);
            System.out.println("Aporte mensual sugerido (con S/500 ya aportados): S/ " + aporteSugerido2);

            // 6. Simular con un aporte mensual propuesto por el usuario: ¿en cuántos meses llega?
            int meses = metaAhorroServicio.simularMesesParaAlcanzarMeta(idMeta, new BigDecimal("150.00"));
            System.out.println("Con aportes de S/150 mensuales, llegaría en " + meses + " meses");

            // 7. Completar la meta y verificar que se marque CUMPLIDA
            aporteAhorroDAO.insertar(new AporteAhorro(idMeta, new BigDecimal("700.00"), LocalDate.now()));
            boolean seCumplio = metaAhorroServicio.actualizarEstadoSiCumplida(idMeta);
            System.out.println("¿Se marcó como cumplida? " + seCumplio + " (esperado: true)");
            System.out.println("Estado final: " + metaAhorroDAO.buscarPorId(idMeta).getEstado());

            // 8. Limpiar
            aporteAhorroDAO.eliminarPorMeta(idMeta);
            metaAhorroDAO.eliminar(idMeta);
            usuarioDAO.eliminar(idUsuario);
            System.out.println("Datos de prueba eliminados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
