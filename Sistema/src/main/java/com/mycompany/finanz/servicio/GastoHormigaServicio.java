package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.Gasto;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Regla de negocio: determina si un gasto es "gasto hormiga".
 * Un gasto se considera hormiga si su monto es bajo Y se repite
 * con frecuencia en la misma categoría dentro de un período reciente.
 */
public class GastoHormigaServicio {

    private static final BigDecimal UMBRAL_MONTO = new BigDecimal("20.00");
    private static final int UMBRAL_FRECUENCIA = 5; // veces en el período
    private static final int DIAS_PERIODO = 30;

    private final MovimientoDAO movimientoDAO;

    public GastoHormigaServicio(MovimientoDAO movimientoDAO) {
        this.movimientoDAO = movimientoDAO;
    }

    /**
     * Evalúa un gasto recién ingresado. Se llama justo antes de guardarlo.
     *
     * Nota: en gastos nuevos de una categoría poco usada, la frecuencia
     * acumulada aún puede ser baja, así que este gasto podría no marcarse
     * como hormiga todavía aunque el patrón se confirme más adelante.
     * Para eso existe reevaluarGastosDelUsuario().
     */
    public boolean evaluarGastoHormiga(Gasto gasto) throws SQLException {
        // Criterio 1: monto bajo
        if (gasto.getMonto().compareTo(UMBRAL_MONTO) >= 0) {
            return false; // un gasto "grande" no es hormiga, sin importar la frecuencia
        }

        // Criterio 2: frecuencia en la misma categoría, últimos 30 días
        int frecuencia = movimientoDAO.contarGastosPorCategoriaEnPeriodo(
                gasto.getIdUsuario(),
                gasto.getIdCategoria(),
                DIAS_PERIODO
        );

        return frecuencia >= UMBRAL_FRECUENCIA;
    }
}
