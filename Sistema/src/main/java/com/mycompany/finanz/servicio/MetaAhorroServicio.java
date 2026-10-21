package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.AporteAhorroDAO;
import com.mycompany.finanz.dao.MetaAhorroDAO;
import com.mycompany.finanz.modelo.MetaAhorro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;

/**
 * Lógica de negocio de las metas de ahorro: progreso (req. 12),
 * actualización automática de estado, y simulador de escenarios (req. 20).
 */
public class MetaAhorroServicio {

    private final MetaAhorroDAO metaAhorroDAO;
    private final AporteAhorroDAO aporteAhorroDAO;

    public MetaAhorroServicio(MetaAhorroDAO metaAhorroDAO, AporteAhorroDAO aporteAhorroDAO) {
        this.metaAhorroDAO = metaAhorroDAO;
        this.aporteAhorroDAO = aporteAhorroDAO;
    }

    /**
     * Calcula el progreso de una meta como porcentaje (0-100+) del monto objetivo alcanzado.
     */
    public double calcularProgreso(int idMeta) throws SQLException {
        MetaAhorro meta = metaAhorroDAO.buscarPorId(idMeta);
        if (meta == null) {
            throw new IllegalArgumentException("Meta no encontrada: " + idMeta);
        }

        BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(idMeta);
        if (meta.getMontoObjetivo().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return totalAportado
                .divide(meta.getMontoObjetivo(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    /**
     * Revisa si la meta ya alcanzó su monto objetivo y, de ser así,
     * actualiza su estado a "CUMPLIDA". Se puede llamar después de cada aporte.
     */
    public boolean actualizarEstadoSiCumplida(int idMeta) throws SQLException {
        MetaAhorro meta = metaAhorroDAO.buscarPorId(idMeta);
        BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(idMeta);

        if (totalAportado.compareTo(meta.getMontoObjetivo()) >= 0
                && !"CUMPLIDA".equals(meta.getEstado())) {
            metaAhorroDAO.actualizarEstado(idMeta, "CUMPLIDA");
            return true;
        }
        return false;
    }

    /**
     * Simula cuánto debería aportar el usuario cada mes para alcanzar
     * la meta en la fecha objetivo (requerimiento 20).
     * Devuelve el aporte mensual necesario a partir de hoy.
     */
    public BigDecimal simularAporteMensualNecesario(int idMeta) throws SQLException {
        MetaAhorro meta = metaAhorroDAO.buscarPorId(idMeta);
        if (meta == null) {
            throw new IllegalArgumentException("Meta no encontrada: " + idMeta);
        }

        BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(idMeta);
        BigDecimal montoFaltante = meta.getMontoObjetivo().subtract(totalAportado);

        if (montoFaltante.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // la meta ya está cumplida
        }

        int mesesRestantes = Period.between(LocalDate.now(), meta.getFechaObjetivo()).getMonths()
                + Period.between(LocalDate.now(), meta.getFechaObjetivo()).getYears() * 12;

        if (mesesRestantes <= 0) {
            return montoFaltante; // la fecha objetivo ya pasó o es este mes: falta todo de una vez
        }

        return montoFaltante.divide(new BigDecimal(mesesRestantes), 2, RoundingMode.HALF_UP);
    }

    /**
     * Variante del simulador: dado un aporte mensual fijo que el usuario propone,
     * calcula en cuántos meses alcanzaría la meta.
     */
    public int simularMesesParaAlcanzarMeta(int idMeta, BigDecimal aporteMensual) throws SQLException {
        MetaAhorro meta = metaAhorroDAO.buscarPorId(idMeta);
        BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(idMeta);
        BigDecimal montoFaltante = meta.getMontoObjetivo().subtract(totalAportado);

        if (montoFaltante.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (aporteMensual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El aporte mensual debe ser mayor a cero");
        }

        BigDecimal meses = montoFaltante.divide(aporteMensual, 0, RoundingMode.UP);
        return meses.intValue();
    }
}
