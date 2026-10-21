package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.LimiteGastoDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Lógica de negocio del presupuesto mensual: monto disponible,
 * alertas de acercamiento/superación (req. 8 y 9), y límites por categoría (req. 7).
 */
public class PresupuestoServicio {

    private static final double UMBRAL_ALERTA_ACERCAMIENTO = 0.80; // 80% del presupuesto usado

    public enum NivelAlerta {
        NINGUNA, // aún hay margen razonable
        CERCA,   // ya se usó el 80% o más
        SUPERADO // se gastó más del presupuesto
    }

    private final MovimientoDAO movimientoDAO;
    private final LimiteGastoDAO limiteGastoDAO;

    public PresupuestoServicio(MovimientoDAO movimientoDAO, LimiteGastoDAO limiteGastoDAO) {
        this.movimientoDAO = movimientoDAO;
        this.limiteGastoDAO = limiteGastoDAO;
    }

    /**
     * Calcula el monto disponible del presupuesto mensual (requerimiento 8).
     */
    public BigDecimal calcularDisponible(Presupuesto presupuesto) throws SQLException {
        LocalDate[] rango = obtenerRangoDelPeriodo(presupuesto.getPeriodo());
        BigDecimal totalGastado = movimientoDAO.sumarGastosPorUsuarioYPeriodo(
                presupuesto.getIdUsuario(), rango[0], rango[1]);

        return presupuesto.getMontoPresupuesto().subtract(totalGastado);
    }

    /**
     * Evalúa si corresponde generar una alerta de presupuesto (requerimiento 9).
     */
    public NivelAlerta evaluarAlerta(Presupuesto presupuesto) throws SQLException {
        LocalDate[] rango = obtenerRangoDelPeriodo(presupuesto.getPeriodo());
        BigDecimal totalGastado = movimientoDAO.sumarGastosPorUsuarioYPeriodo(
                presupuesto.getIdUsuario(), rango[0], rango[1]);

        BigDecimal montoPresupuesto = presupuesto.getMontoPresupuesto();

        if (totalGastado.compareTo(montoPresupuesto) > 0) {
            return NivelAlerta.SUPERADO;
        }

        double porcentajeUsado = totalGastado.doubleValue() / montoPresupuesto.doubleValue();
        if (porcentajeUsado >= UMBRAL_ALERTA_ACERCAMIENTO) {
            return NivelAlerta.CERCA;
        }

        return NivelAlerta.NINGUNA;
    }

    /**
     * Verifica si un gasto nuevo haría superar el límite definido para su categoría
     * dentro del presupuesto (requerimiento 7). Devuelve true si se supera.
     * Si la categoría no tiene límite definido, siempre devuelve false (no aplica).
     */
    public boolean superaLimiteCategoria(Presupuesto presupuesto, int idCategoria, BigDecimal montoNuevoGasto) throws SQLException {
        LimiteGasto limite = limiteGastoDAO.buscarPorPresupuestoYCategoria(presupuesto.getIdPresupuesto(), idCategoria);
        if (limite == null) {
            return false; // esa categoría no tiene límite configurado
        }

        LocalDate[] rango = obtenerRangoDelPeriodo(presupuesto.getPeriodo());
        BigDecimal gastadoEnCategoria = movimientoDAO.sumarGastosPorCategoriaYPeriodo(
                presupuesto.getIdUsuario(), idCategoria, rango[0], rango[1]);

        BigDecimal totalConNuevoGasto = gastadoEnCategoria.add(montoNuevoGasto);
        return totalConNuevoGasto.compareTo(limite.getMontoLimite()) > 0;
    }

    /**
     * Calcula cuánto se lleva gastado en una categoría específica dentro del
     * período del presupuesto (para mostrar la barra de progreso por categoría).
     */
    public BigDecimal calcularGastadoEnCategoria(Presupuesto presupuesto, int idCategoria) throws SQLException {
        LocalDate[] rango = obtenerRangoDelPeriodo(presupuesto.getPeriodo());
        return movimientoDAO.sumarGastosPorCategoriaYPeriodo(
                presupuesto.getIdUsuario(), idCategoria, rango[0], rango[1]);
    }

    /**
     * Convierte el período (un LocalDate que representa el mes, ej. 2026-09-01)
     * en su primer y último día del mes.
     */
    private LocalDate[] obtenerRangoDelPeriodo(LocalDate periodo) {
        YearMonth yearMonth = YearMonth.from(periodo);
        LocalDate desde = yearMonth.atDay(1);
        LocalDate hasta = yearMonth.atEndOfMonth();
        return new LocalDate[]{desde, hasta};
    }
}
