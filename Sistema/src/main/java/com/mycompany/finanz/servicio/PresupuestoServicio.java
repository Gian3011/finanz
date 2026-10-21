package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.AlertaDAO;
import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.LimiteGastoDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.PresupuestoDAO;
import com.mycompany.finanz.modelo.Alerta;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.util.Formato;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio del presupuesto mensual: monto disponible, límites por
 * categoría y alertas al acercarse (80 %) o superar lo planificado.
 */
public class PresupuestoServicio {

    static final BigDecimal UMBRAL_ALERTA_ACERCAMIENTO = new BigDecimal("0.80"); // 80 % usado

    public enum NivelAlerta {
        NINGUNA,  // aún hay margen razonable
        CERCA,    // ya se usó el 80 % o más
        SUPERADO  // se gastó más de lo planificado
    }

    private final PresupuestoDAO presupuestoDAO;
    private final LimiteGastoDAO limiteGastoDAO;
    private final MovimientoDAO movimientoDAO;
    private final CategoriaDAO categoriaDAO;
    private final AlertaDAO alertaDAO;

    public PresupuestoServicio() {
        this(new PresupuestoDAO(), new LimiteGastoDAO(), new MovimientoDAO(), new CategoriaDAO(), new AlertaDAO());
    }

    public PresupuestoServicio(PresupuestoDAO presupuestoDAO, LimiteGastoDAO limiteGastoDAO,
                               MovimientoDAO movimientoDAO, CategoriaDAO categoriaDAO, AlertaDAO alertaDAO) {
        this.presupuestoDAO = presupuestoDAO;
        this.limiteGastoDAO = limiteGastoDAO;
        this.movimientoDAO = movimientoDAO;
        this.categoriaDAO = categoriaDAO;
        this.alertaDAO = alertaDAO;
    }

    public Presupuesto buscar(int idUsuario, YearMonth mes) throws SQLException {
        return presupuestoDAO.buscarPorUsuarioYPeriodo(idUsuario, mes.atDay(1));
    }

    /**
     * Crea o actualiza el presupuesto de un mes. Solo se permite el mes actual o
     * meses futuros; si el mes ya tiene presupuesto se actualiza con el nuevo monto.
     */
    public Presupuesto guardarPresupuesto(int idUsuario, YearMonth mes, BigDecimal monto)
            throws ValidacionException, SQLException {
        if (mes.isBefore(YearMonth.now())) {
            throw new ValidacionException("Solo puedes definir el presupuesto del mes actual en adelante.");
        }

        Presupuesto existente = buscar(idUsuario, mes);
        if (existente != null) {
            BigDecimal sumaLimites = sumarLimites(existente.getIdPresupuesto(), null);
            if (monto.compareTo(sumaLimites) < 0) {
                throw new ValidacionException("El presupuesto no puede ser menor que la suma de tus límites por categoría ("
                        + Formato.soles(sumaLimites) + ").");
            }
            presupuestoDAO.actualizarMonto(existente.getIdPresupuesto(), idUsuario, monto);
            existente.setMontoPresupuesto(monto);
            return existente;
        }
        Presupuesto nuevo = new Presupuesto(idUsuario, mes.atDay(1), monto);
        presupuestoDAO.insertar(nuevo);
        return nuevo;
    }

    /**
     * Crea o actualiza el límite de una categoría de gasto. La suma de todos los
     * límites no puede superar el presupuesto mensual.
     */
    public void guardarLimite(int idUsuario, int idPresupuesto, int idCategoria, BigDecimal montoLimite)
            throws ValidacionException, SQLException {
        Presupuesto presupuesto = presupuestoDAO.buscarPorId(idPresupuesto, idUsuario);
        if (presupuesto == null) {
            throw new ValidacionException("No se encontró el presupuesto indicado.");
        }
        Categoria categoria = categoriaDAO.buscarDisponible(idCategoria, idUsuario);
        if (categoria == null || !"GASTO".equals(categoria.getTipo())) {
            throw new ValidacionException("Selecciona una categoría de gasto válida.");
        }

        BigDecimal totalConNuevoLimite = sumarLimites(idPresupuesto, idCategoria).add(montoLimite);
        if (totalConNuevoLimite.compareTo(presupuesto.getMontoPresupuesto()) > 0) {
            throw new ValidacionException("La suma de los límites por categoría ("
                    + Formato.soles(totalConNuevoLimite) + ") superaría el presupuesto mensual ("
                    + Formato.soles(presupuesto.getMontoPresupuesto()) + "). Reduce este monto o ajusta los límites existentes.");
        }

        LimiteGasto existente = limiteGastoDAO.buscarPorPresupuestoYCategoria(idPresupuesto, idCategoria);
        if (existente != null) {
            existente.setMontoLimite(montoLimite);
            limiteGastoDAO.actualizarMonto(existente);
        } else {
            limiteGastoDAO.insertar(new LimiteGasto(idPresupuesto, idCategoria, montoLimite));
        }
    }

    public boolean eliminarLimite(int idUsuario, int idLimite) throws SQLException {
        return limiteGastoDAO.eliminar(idLimite, idUsuario);
    }

    public List<LimiteGasto> listarLimites(Presupuesto presupuesto) throws SQLException {
        return limiteGastoDAO.listarPorPresupuesto(presupuesto.getIdPresupuesto());
    }

    public BigDecimal calcularGastado(Presupuesto presupuesto) throws SQLException {
        YearMonth mes = presupuesto.getMes();
        return movimientoDAO.sumarGastosPorUsuarioYPeriodo(presupuesto.getIdUsuario(), mes.atDay(1), mes.atEndOfMonth());
    }

    /** Monto disponible del presupuesto mensual (puede ser negativo si se excedió). */
    public BigDecimal calcularDisponible(Presupuesto presupuesto) throws SQLException {
        return presupuesto.getMontoPresupuesto().subtract(calcularGastado(presupuesto));
    }

    /** Cuánto se lleva gastado en una categoría dentro del mes del presupuesto. */
    public BigDecimal calcularGastadoEnCategoria(Presupuesto presupuesto, int idCategoria) throws SQLException {
        YearMonth mes = presupuesto.getMes();
        return movimientoDAO.sumarGastosPorCategoriaYPeriodo(
                presupuesto.getIdUsuario(), idCategoria, mes.atDay(1), mes.atEndOfMonth());
    }

    public NivelAlerta evaluarAlerta(Presupuesto presupuesto) throws SQLException {
        return nivel(calcularGastado(presupuesto), presupuesto.getMontoPresupuesto());
    }

    /** Regla pura (sin BD) para decidir el nivel de alerta de un gasto acumulado frente a un tope. */
    public static NivelAlerta nivel(BigDecimal gastado, BigDecimal tope) {
        if (tope == null || tope.signum() <= 0) {
            return NivelAlerta.NINGUNA;
        }
        if (gastado.compareTo(tope) > 0) {
            return NivelAlerta.SUPERADO;
        }
        // Se compara gastado >= 80 % del tope sin dividir, para evitar errores de redondeo
        return gastado.compareTo(tope.multiply(UMBRAL_ALERTA_ACERCAMIENTO)) >= 0 ? NivelAlerta.CERCA : NivelAlerta.NINGUNA;
    }

    /**
     * Revisa el presupuesto del mes de "fecha" y sus límites por categoría; registra
     * en la tabla alerta los avisos nuevos (una sola vez por nivel) y los devuelve
     * para mostrarlos al usuario.
     */
    public List<String> verificarAlertas(int idUsuario, LocalDate fecha) throws SQLException {
        List<String> nuevas = new ArrayList<>();
        Presupuesto presupuesto = buscar(idUsuario, YearMonth.from(fecha));
        if (presupuesto == null) {
            return nuevas;
        }

        String mesTexto = Formato.mes(presupuesto.getMes());
        BigDecimal gastado = calcularGastado(presupuesto);
        NivelAlerta nivelGeneral = nivel(gastado, presupuesto.getMontoPresupuesto());
        if (nivelGeneral != NivelAlerta.NINGUNA) {
            String mensaje = nivelGeneral == NivelAlerta.SUPERADO
                    ? "Superaste tu presupuesto de " + mesTexto + ": llevas " + Formato.soles(gastado)
                      + " de " + Formato.soles(presupuesto.getMontoPresupuesto()) + "."
                    : "Ya usaste el " + Formato.porcentaje(gastado, presupuesto.getMontoPresupuesto())
                      + " % de tu presupuesto de " + mesTexto + ".";
            registrarSiEsNueva(idUsuario, presupuesto.getIdPresupuesto(), null, nivelGeneral, mensaje, nuevas);
        }

        for (LimiteGasto limite : listarLimites(presupuesto)) {
            BigDecimal gastadoCategoria = calcularGastadoEnCategoria(presupuesto, limite.getIdCategoria());
            NivelAlerta nivelCategoria = nivel(gastadoCategoria, limite.getMontoLimite());
            if (nivelCategoria == NivelAlerta.NINGUNA) {
                continue;
            }
            String mensaje = nivelCategoria == NivelAlerta.SUPERADO
                    ? "Superaste el límite de " + limite.getNombreCategoria() + ": " + Formato.soles(gastadoCategoria)
                      + " de " + Formato.soles(limite.getMontoLimite()) + "."
                    : "Estás cerca del límite de " + limite.getNombreCategoria() + " ("
                      + Formato.porcentaje(gastadoCategoria, limite.getMontoLimite()) + " % usado).";
            registrarSiEsNueva(idUsuario, presupuesto.getIdPresupuesto(), limite.getIdLimite(), nivelCategoria, mensaje, nuevas);
        }
        return nuevas;
    }

    private void registrarSiEsNueva(int idUsuario, Integer idPresupuesto, Integer idLimite, NivelAlerta nivel,
                                    String mensaje, List<String> nuevas) throws SQLException {
        if (!alertaDAO.existe(idPresupuesto, idLimite, nivel.name())) {
            alertaDAO.insertar(new Alerta(idUsuario, idPresupuesto, idLimite, nivel.name(), mensaje));
            nuevas.add(mensaje);
        }
    }

    private BigDecimal sumarLimites(int idPresupuesto, Integer exceptoCategoria) throws SQLException {
        BigDecimal suma = BigDecimal.ZERO;
        for (LimiteGasto l : limiteGastoDAO.listarPorPresupuesto(idPresupuesto)) {
            if (exceptoCategoria == null || l.getIdCategoria() != exceptoCategoria) {
                suma = suma.add(l.getMontoLimite());
            }
        }
        return suma;
    }
}
