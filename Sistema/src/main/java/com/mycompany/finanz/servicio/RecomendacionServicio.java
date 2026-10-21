package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.EducacionDAO;
import com.mycompany.finanz.dao.ReporteRecomendacionDAO;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Recomendacion;
import com.mycompany.finanz.modelo.ResumenPeriodo;
import com.mycompany.finanz.util.Formato;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Genera recomendaciones personalizadas a partir de reglas sobre los datos
 * del usuario (gastos vs ingresos, presupuesto, gastos hormiga, metas,
 * variación por categoría y uso de la sección educativa).
 */
public class RecomendacionServicio {

    private static final int TASA_AHORRO_IDEAL = 20; // regla 50/30/20

    private final EstadisticaServicio estadisticaServicio = new EstadisticaServicio();
    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio();
    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();
    private final EducacionDAO educacionDAO = new EducacionDAO();
    private final ReporteRecomendacionDAO recomendacionDAO = new ReporteRecomendacionDAO();

    /** Calcula las recomendaciones del momento y las guarda en el historial del día. */
    public List<Recomendacion> generar(int idUsuario) throws SQLException {
        List<Recomendacion> lista = new ArrayList<>();
        YearMonth mes = YearMonth.now();
        LocalDate hoy = LocalDate.now();
        ResumenPeriodo actual = estadisticaServicio.resumenMes(idUsuario, mes);
        ResumenPeriodo anterior = estadisticaServicio.resumenMes(idUsuario, mes.minusMonths(1));

        // 1. Gasta más de lo que gana
        if (actual.getIngresos().signum() > 0 && actual.getGastos().compareTo(actual.getIngresos()) > 0) {
            lista.add(new Recomendacion(idUsuario, "Estás gastando más de lo que ganas",
                    "Este mes tus gastos (" + Formato.soles(actual.getGastos()) + ") superan tus ingresos ("
                    + Formato.soles(actual.getIngresos()) + "). Revisa tus gastos variables y pospone las compras que no sean urgentes.",
                    Recomendacion.ALERTA));
        } else if (actual.getIngresos().signum() > 0 && actual.getTasaAhorro() < TASA_AHORRO_IDEAL) {
            // 2. Ahorra poco
            BigDecimal ideal = actual.getIngresos().multiply(BigDecimal.valueOf(TASA_AHORRO_IDEAL)).divide(BigDecimal.valueOf(100));
            lista.add(new Recomendacion(idUsuario, "Intenta ahorrar al menos el 20 %",
                    "Este mes te queda libre el " + actual.getTasaAhorro() + " % de tus ingresos. Según la regla 50/30/20, "
                    + "lo ideal sería reservar unos " + Formato.soles(ideal) + " para ahorro o deudas.",
                    Recomendacion.AHORRO));
        } else if (actual.getIngresos().signum() > 0) {
            lista.add(new Recomendacion(idUsuario, "¡Buen ritmo de ahorro!",
                    "Este mes conservas el " + actual.getTasaAhorro() + " % de tus ingresos. Destina ese excedente a tus metas "
                    + "para que no se diluya en gastos no planificados.", Recomendacion.LOGRO));
        }

        // 3. Gastos hormiga
        if (actual.getGastosHormiga().signum() > 0) {
            BigDecimal anual = actual.getGastosHormiga().multiply(BigDecimal.valueOf(12));
            lista.add(new Recomendacion(idUsuario, "Cuidado con los gastos hormiga",
                    "Llevas " + Formato.soles(actual.getGastosHormiga()) + " en gastos hormiga este mes. Si mantienes ese ritmo "
                    + "serían cerca de " + Formato.soles(anual) + " al año. Ponles un tope semanal.", Recomendacion.HABITO));
        }

        // 4. Presupuesto y límites
        Presupuesto presupuesto = presupuestoServicio.buscar(idUsuario, mes);
        if (presupuesto == null) {
            lista.add(new Recomendacion(idUsuario, "Define tu presupuesto del mes",
                    "Aún no tienes un presupuesto para " + Formato.mes(mes) + ". Planificar cuánto gastar te ayuda a "
                    + "recibir alertas antes de excederte.", Recomendacion.HABITO));
        } else {
            PresupuestoServicio.NivelAlerta nivel = presupuestoServicio.evaluarAlerta(presupuesto);
            if (nivel == PresupuestoServicio.NivelAlerta.SUPERADO) {
                lista.add(new Recomendacion(idUsuario, "Presupuesto superado",
                        "Ya gastaste más de los " + Formato.soles(presupuesto.getMontoPresupuesto()) + " planificados. "
                        + "Evita nuevos gastos no esenciales hasta fin de mes.", Recomendacion.ALERTA));
            } else if (nivel == PresupuestoServicio.NivelAlerta.CERCA) {
                lista.add(new Recomendacion(idUsuario, "Estás cerca de tu presupuesto",
                        "Te quedan " + Formato.soles(presupuestoServicio.calcularDisponible(presupuesto))
                        + " para el resto del mes. Prioriza lo indispensable.", Recomendacion.ALERTA));
            }
            for (LimiteGasto limite : presupuestoServicio.listarLimites(presupuesto)) {
                BigDecimal gastado = presupuestoServicio.calcularGastadoEnCategoria(presupuesto, limite.getIdCategoria());
                if (gastado.compareTo(limite.getMontoLimite()) > 0) {
                    lista.add(new Recomendacion(idUsuario, "Límite superado en " + limite.getNombreCategoria(),
                            "Gastaste " + Formato.soles(gastado) + " en " + limite.getNombreCategoria() + " y tu límite era "
                            + Formato.soles(limite.getMontoLimite()) + ". Compensa reduciendo esta categoría el resto del mes.",
                            Recomendacion.ALERTA));
                }
            }
        }

        // 5. Categoría que más creció respecto al mes anterior
        String categoriaCrecio = null;
        int mayorAumento = 0;
        for (Map.Entry<String, BigDecimal> e : actual.getGastosPorCategoria().entrySet()) {
            BigDecimal antes = anterior.getGastosPorCategoria().get(e.getKey());
            Integer variacion = EstadisticaServicio.variacion(antes, e.getValue());
            if (variacion != null && variacion > mayorAumento && e.getValue().compareTo(new BigDecimal("50")) > 0) {
                mayorAumento = variacion;
                categoriaCrecio = e.getKey();
            }
        }
        if (categoriaCrecio != null && mayorAumento >= 20) {
            lista.add(new Recomendacion(idUsuario, "Subió tu gasto en " + categoriaCrecio,
                    "Gastaste " + mayorAumento + " % más en " + categoriaCrecio + " que el mes pasado. "
                    + "Revisa si fue algo puntual o un hábito nuevo.", Recomendacion.HABITO));
        }

        // 6. Metas de ahorro
        List<MetaAhorro> metas = metaAhorroServicio.listar(idUsuario);
        boolean tieneFondoEmergencia = false;
        for (MetaAhorro meta : metas) {
            if (meta.getNombreMeta().toLowerCase().contains("emergencia")) {
                tieneFondoEmergencia = true;
            }
            if (MetaAhorroServicio.estaEnRiesgo(meta, hoy)) {
                lista.add(new Recomendacion(idUsuario, "Tu meta \"" + meta.getNombreMeta() + "\" va retrasada",
                        "Para llegar a tiempo necesitas aportar unos " + Formato.soles(MetaAhorroServicio.aporteMensualNecesario(meta, hoy))
                        + " al mes. Usa el simulador para probar otros escenarios.", Recomendacion.AHORRO));
            }
        }
        if (metas.isEmpty()) {
            lista.add(new Recomendacion(idUsuario, "Crea tu primera meta de ahorro",
                    "Ahorrar con un objetivo concreto (monto y fecha) es mucho más fácil. Empieza por algo alcanzable.",
                    Recomendacion.AHORRO));
        } else if (!tieneFondoEmergencia) {
            lista.add(new Recomendacion(idUsuario, "Considera un fondo de emergencia",
                    "Lo recomendable es tener ahorrados entre 3 y 6 meses de tus gastos básicos para imprevistos.",
                    Recomendacion.AHORRO));
        }

        // 7. Educación financiera
        if (educacionDAO.listarResultados(idUsuario).isEmpty()) {
            lista.add(new Recomendacion(idUsuario, "Aprende algo nuevo hoy",
                    "Visita la sección Educación: lee una lección corta y pon a prueba lo aprendido con un cuestionario.",
                    Recomendacion.EDUCACION));
        }

        recomendacionDAO.reemplazarRecomendacionesDeHoy(idUsuario, lista);
        return lista;
    }
}
