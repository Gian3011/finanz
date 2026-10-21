package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.Gasto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Regla de negocio: determina si un gasto es "gasto hormiga".
 * Un gasto se considera hormiga si su monto es bajo (menos de S/ 20) Y en
 * los últimos 30 días hubo al menos 5 gastos pequeños en la misma categoría
 * (contando el nuevo). Cuando el patrón se confirma, también se marcan los
 * gastos pequeños anteriores de esa categoría dentro de la ventana.
 */
public class GastoHormigaServicio {

    public static final BigDecimal UMBRAL_MONTO = new BigDecimal("20.00");
    public static final int UMBRAL_FRECUENCIA = 5;
    public static final int DIAS_PERIODO = 30;

    private final MovimientoDAO movimientoDAO;

    public GastoHormigaServicio(MovimientoDAO movimientoDAO) {
        this.movimientoDAO = movimientoDAO;
    }

    /** Criterio de monto: solo los gastos pequeños pueden ser hormiga. */
    public static boolean esMontoPequeno(BigDecimal monto) {
        return monto != null && monto.compareTo(UMBRAL_MONTO) < 0;
    }

    /** Evalúa un gasto antes de guardarlo. */
    public boolean evaluarGastoHormiga(Gasto gasto) throws SQLException {
        if (!esMontoPequeno(gasto.getMonto())) {
            return false; // un gasto "grande" no es hormiga, sin importar la frecuencia
        }

        LocalDate hasta = gasto.getFecha();
        LocalDate desde = hasta.minusDays(DIAS_PERIODO - 1L);
        int anteriores = movimientoDAO.contarGastosPequenos(
                gasto.getIdUsuario(), gasto.getIdCategoria(), UMBRAL_MONTO, desde, hasta);

        return anteriores + 1 >= UMBRAL_FRECUENCIA;
    }

    /** Una vez guardado un gasto hormiga, marca también los gastos pequeños previos del patrón. */
    public void marcarPatron(Gasto gasto) throws SQLException {
        LocalDate hasta = gasto.getFecha();
        movimientoDAO.marcarGastosPequenosComoHormiga(gasto.getIdUsuario(), gasto.getIdCategoria(),
                UMBRAL_MONTO, hasta.minusDays(DIAS_PERIODO - 1L), hasta);
    }
}
