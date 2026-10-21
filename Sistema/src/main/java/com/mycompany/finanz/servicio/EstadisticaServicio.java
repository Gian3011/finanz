package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.AporteAhorroDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.ResumenPeriodo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas de hábitos de gasto: resumen mensual, evolución de los últimos
 * meses y comparación entre dos periodos.
 */
public class EstadisticaServicio {

    private final MovimientoDAO movimientoDAO;
    private final AporteAhorroDAO aporteAhorroDAO;

    public EstadisticaServicio() {
        this(new MovimientoDAO(), new AporteAhorroDAO());
    }

    public EstadisticaServicio(MovimientoDAO movimientoDAO, AporteAhorroDAO aporteAhorroDAO) {
        this.movimientoDAO = movimientoDAO;
        this.aporteAhorroDAO = aporteAhorroDAO;
    }

    /** Totales completos de un mes (ingresos, gastos, ahorro en metas, hormiga y por categoría). */
    public ResumenPeriodo resumenMes(int idUsuario, YearMonth mes) throws SQLException {
        LocalDate desde = mes.atDay(1);
        LocalDate hasta = mes.atEndOfMonth();

        ResumenPeriodo r = new ResumenPeriodo(mes);
        r.setIngresos(movimientoDAO.sumarPorTipoYPeriodo(idUsuario, "INGRESO", desde, hasta));
        r.setGastos(movimientoDAO.sumarPorTipoYPeriodo(idUsuario, "GASTO", desde, hasta));
        r.setAhorroMetas(aporteAhorroDAO.sumarAportesPorUsuarioYPeriodo(idUsuario, desde, hasta));
        r.setGastosHormiga(movimientoDAO.sumarGastosHormiga(idUsuario, desde, hasta));
        r.setGastosPorCategoria(movimientoDAO.gastosPorCategoria(idUsuario, desde, hasta));
        return r;
    }

    /** Ingresos y gastos de los últimos "cantidad" meses (incluye el actual), del más antiguo al más reciente. */
    public List<ResumenPeriodo> ultimosMeses(int idUsuario, int cantidad) throws SQLException {
        YearMonth actual = YearMonth.now();
        YearMonth inicio = actual.minusMonths(cantidad - 1L);
        Map<YearMonth, BigDecimal[]> totales = movimientoDAO.totalesPorMes(
                idUsuario, inicio.atDay(1), actual.atEndOfMonth());

        List<ResumenPeriodo> meses = new ArrayList<>();
        for (YearMonth mes = inicio; !mes.isAfter(actual); mes = mes.plusMonths(1)) {
            ResumenPeriodo r = new ResumenPeriodo(mes);
            BigDecimal[] valores = totales.get(mes);
            if (valores != null) {
                r.setIngresos(valores[0]);
                r.setGastos(valores[1]);
            }
            meses.add(r);
        }
        return meses;
    }

    /** Variación porcentual de "anterior" a "actual" (null si no hay base para comparar). */
    public static Integer variacion(BigDecimal anterior, BigDecimal actual) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 0, java.math.RoundingMode.HALF_UP).intValue();
    }
}
