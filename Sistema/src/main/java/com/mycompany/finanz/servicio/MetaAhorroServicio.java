package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.AporteAhorroDAO;
import com.mycompany.finanz.dao.MetaAhorroDAO;
import com.mycompany.finanz.modelo.AporteAhorro;
import com.mycompany.finanz.modelo.MetaAhorro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lógica de negocio de las metas de ahorro: creación, aportes, progreso y
 * actualización automática del estado a CUMPLIDA.
 */
public class MetaAhorroServicio {

    private final MetaAhorroDAO metaAhorroDAO;
    private final AporteAhorroDAO aporteAhorroDAO;

    public MetaAhorroServicio() {
        this(new MetaAhorroDAO(), new AporteAhorroDAO());
    }

    public MetaAhorroServicio(MetaAhorroDAO metaAhorroDAO, AporteAhorroDAO aporteAhorroDAO) {
        this.metaAhorroDAO = metaAhorroDAO;
        this.aporteAhorroDAO = aporteAhorroDAO;
    }

    public List<MetaAhorro> listar(int idUsuario) throws SQLException {
        return metaAhorroDAO.listarPorUsuario(idUsuario);
    }

    public MetaAhorro crear(int idUsuario, String nombre, BigDecimal montoObjetivo, LocalDate fechaObjetivo)
            throws ValidacionException, SQLException {
        String nombreLimpio = Validar.texto(nombre, "nombre de la meta", 100);
        if (!fechaObjetivo.isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha objetivo debe ser posterior a hoy.");
        }
        if (fechaObjetivo.isAfter(LocalDate.now().plusYears(30))) {
            throw new ValidacionException("La fecha objetivo no puede ser mayor a 30 años.");
        }
        MetaAhorro meta = new MetaAhorro(idUsuario, nombreLimpio, montoObjetivo, fechaObjetivo);
        metaAhorroDAO.insertar(meta);
        return meta;
    }

    /**
     * Registra un aporte a la meta del usuario. Devuelve true si con este
     * aporte la meta quedó cumplida.
     */
    public boolean registrarAporte(int idUsuario, int idMeta, BigDecimal monto, LocalDate fecha)
            throws ValidacionException, SQLException {
        MetaAhorro meta = metaAhorroDAO.buscarPorId(idMeta, idUsuario);
        if (meta == null) {
            throw new ValidacionException("No se encontró la meta indicada.");
        }
        if (meta.isCumplida()) {
            throw new ValidacionException("Esta meta ya está cumplida. ¡Crea una nueva!");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha del aporte no puede ser posterior a hoy.");
        }

        aporteAhorroDAO.insertar(new AporteAhorro(idMeta, monto, fecha));
        return actualizarEstadoSiCumplida(meta);
    }

    public boolean eliminar(int idUsuario, int idMeta) throws SQLException {
        return metaAhorroDAO.eliminar(idMeta, idUsuario);
    }

    public List<AporteAhorro> aportes(int idMeta) throws SQLException {
        return aporteAhorroDAO.listarPorMeta(idMeta);
    }

    /** Progreso de la meta como porcentaje (0-100) del monto objetivo alcanzado. */
    public static int calcularProgreso(MetaAhorro meta) {
        if (meta.getMontoObjetivo() == null || meta.getMontoObjetivo().signum() == 0) {
            return 0;
        }
        int porcentaje = meta.getMontoAhorrado().multiply(BigDecimal.valueOf(100))
                .divide(meta.getMontoObjetivo(), 0, RoundingMode.DOWN).intValue();
        return Math.min(100, Math.max(0, porcentaje));
    }

    /**
     * Cuánto debería aportar el usuario cada mes, desde hoy, para alcanzar la meta
     * en su fecha objetivo. Si la fecha ya pasó o es este mes, devuelve todo lo que falta.
     */
    public static BigDecimal aporteMensualNecesario(MetaAhorro meta, LocalDate hoy) {
        BigDecimal faltante = meta.getMontoFaltante();
        if (faltante.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        long meses = ChronoUnit.MONTHS.between(hoy, meta.getFechaObjetivo());
        if (meses <= 0) {
            return faltante;
        }
        return faltante.divide(BigDecimal.valueOf(meses), 2, RoundingMode.CEILING);
    }

    /** true si al ritmo necesario la meta está retrasada (menos ahorro del esperado a esta fecha). */
    public static boolean estaEnRiesgo(MetaAhorro meta, LocalDate hoy) {
        if (meta.isCumplida() || meta.getFechaCreacion() == null) {
            return false;
        }
        if (!hoy.isBefore(meta.getFechaObjetivo())) {
            return true;
        }
        long total = ChronoUnit.DAYS.between(meta.getFechaCreacion(), meta.getFechaObjetivo());
        long transcurrido = ChronoUnit.DAYS.between(meta.getFechaCreacion(), hoy);
        if (total <= 0) {
            return true;
        }
        BigDecimal esperado = meta.getMontoObjetivo().multiply(BigDecimal.valueOf(transcurrido))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        // margen del 25 % antes de considerarla en riesgo
        return meta.getMontoAhorrado().compareTo(esperado.multiply(new BigDecimal("0.75"))) < 0;
    }

    private boolean actualizarEstadoSiCumplida(MetaAhorro meta) throws SQLException {
        BigDecimal totalAportado = aporteAhorroDAO.sumarAportesPorMeta(meta.getIdMeta());
        if (totalAportado.compareTo(meta.getMontoObjetivo()) >= 0) {
            metaAhorroDAO.actualizarEstado(meta.getIdMeta(), MetaAhorro.CUMPLIDA);
            return true;
        }
        return false;
    }
}
