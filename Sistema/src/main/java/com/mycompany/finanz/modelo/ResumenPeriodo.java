package com.mycompany.finanz.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Totales de un mes: ingresos, gastos, ahorro en metas y gasto por categoría.
 * Lo usan el resumen, la comparación de periodos y el reporte PDF.
 */
public class ResumenPeriodo {

    private final YearMonth mes;
    private BigDecimal ingresos = BigDecimal.ZERO;
    private BigDecimal gastos = BigDecimal.ZERO;
    private BigDecimal ahorroMetas = BigDecimal.ZERO;
    private BigDecimal gastosHormiga = BigDecimal.ZERO;
    private Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();

    public ResumenPeriodo(YearMonth mes) {
        this.mes = mes;
    }

    /** Ingresos menos gastos del mes. */
    public BigDecimal getBalance() {
        return ingresos.subtract(gastos);
    }

    /** Porcentaje del ingreso que no se gastó (0 si no hubo ingresos). */
    public int getTasaAhorro() {
        if (ingresos.signum() == 0) {
            return 0;
        }
        return getBalance().multiply(BigDecimal.valueOf(100))
                .divide(ingresos, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public YearMonth getMes() {
        return mes;
    }

    public BigDecimal getIngresos() {
        return ingresos;
    }

    public void setIngresos(BigDecimal ingresos) {
        this.ingresos = ingresos;
    }

    public BigDecimal getGastos() {
        return gastos;
    }

    public void setGastos(BigDecimal gastos) {
        this.gastos = gastos;
    }

    public BigDecimal getAhorroMetas() {
        return ahorroMetas;
    }

    public void setAhorroMetas(BigDecimal ahorroMetas) {
        this.ahorroMetas = ahorroMetas;
    }

    public BigDecimal getGastosHormiga() {
        return gastosHormiga;
    }

    public void setGastosHormiga(BigDecimal gastosHormiga) {
        this.gastosHormiga = gastosHormiga;
    }

    public Map<String, BigDecimal> getGastosPorCategoria() {
        return gastosPorCategoria;
    }

    public void setGastosPorCategoria(Map<String, BigDecimal> gastosPorCategoria) {
        this.gastosPorCategoria = gastosPorCategoria;
    }
}
