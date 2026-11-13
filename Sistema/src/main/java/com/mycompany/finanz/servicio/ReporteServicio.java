package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.ReporteRecomendacionDAO;
import com.mycompany.finanz.modelo.FormatoReporte;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.modelo.Recomendacion;
import com.mycompany.finanz.modelo.ReporteFinanciero;
import com.mycompany.finanz.modelo.ResumenPeriodo;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.util.Formato;
import com.mycompany.finanz.util.ReporteJasper;

import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el resumen financiero mensual del usuario con JasperReports
 * (plantilla reportes/resumen_financiero.jrxml) en PDF, Excel o Word
 * y registra la generación en la tabla reporte_financiero.
 */
public class ReporteServicio {

    private final EstadisticaServicio estadisticaServicio = new EstadisticaServicio();
    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio();
    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();
    private final RecomendacionServicio recomendacionServicio = new RecomendacionServicio();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final ReporteRecomendacionDAO reporteDAO = new ReporteRecomendacionDAO();

    public byte[] generar(Usuario usuario, YearMonth mes, FormatoReporte formato) throws SQLException {
        int idUsuario = usuario.getIdUsuario();
        ResumenPeriodo resumen = estadisticaServicio.resumenMes(idUsuario, mes);
        ResumenPeriodo anterior = estadisticaServicio.resumenMes(idUsuario, mes.minusMonths(1));
        List<Movimiento> movimientos = movimientoDAO.listarPorUsuarioYPeriodo(idUsuario, mes.atDay(1), mes.atEndOfMonth());
        Presupuesto presupuesto = presupuestoServicio.buscar(idUsuario, mes);
        BigDecimal gastado = presupuesto == null ? null : presupuestoServicio.calcularGastado(presupuesto);
        List<MetaAhorro> metas = metaAhorroServicio.listar(idUsuario);
        List<Recomendacion> recomendaciones = recomendacionServicio.generar(idUsuario);

        Map<String, Object> parametros = parametros(usuario, mes, resumen, anterior, presupuesto, gastado,
                metas, recomendaciones, movimientos, LocalDateTime.now());
        byte[] archivo = ReporteJasper.generar(parametros, formato);

        reporteDAO.insertarReporte(new ReporteFinanciero(idUsuario, mes.toString(), formato.getCodigo()));
        return archivo;
    }

    public List<ReporteFinanciero> historial(int idUsuario) throws SQLException {
        return reporteDAO.listarReportes(idUsuario, 10);
    }

    /** Arma los parámetros y las tablas de la plantilla a partir de los datos ya consultados. */
    static Map<String, Object> parametros(Usuario usuario, YearMonth mes, ResumenPeriodo resumen, ResumenPeriodo anterior,
                                          Presupuesto presupuesto, BigDecimal gastado, List<MetaAhorro> metas,
                                          List<Recomendacion> recomendaciones, List<Movimiento> movimientos,
                                          LocalDateTime generado) {
        Map<String, Object> p = new HashMap<>();
        p.put("MES", Formato.mes(mes));
        p.put("MES_ANTERIOR", Formato.mes(mes.minusMonths(1)));
        p.put("USUARIO", usuario.getNombreCompleto() + "  ·  " + usuario.getEmail());
        p.put("GENERADO", Formato.fechaHora(generado));
        p.put("INGRESOS", Formato.soles(resumen.getIngresos()));
        p.put("GASTOS", Formato.soles(resumen.getGastos()));
        p.put("BALANCE", Formato.soles(resumen.getBalance()));
        p.put("BALANCE_NEGATIVO", resumen.getBalance().signum() < 0);
        p.put("AHORRO_METAS", Formato.soles(resumen.getAhorroMetas()));
        p.put("GASTOS_HORMIGA", Formato.soles(resumen.getGastosHormiga()));
        p.put("TOTAL_MOVIMIENTOS", movimientos.size());

        if (presupuesto == null) {
            p.put("PRESUPUESTO", "No se definió presupuesto para este mes.");
        } else {
            BigDecimal monto = presupuesto.getMontoPresupuesto();
            p.put("PRESUPUESTO", "Presupuesto: " + Formato.soles(monto)
                    + "   ·   Gastado: " + Formato.soles(gastado) + " (" + Formato.porcentaje(gastado, monto) + " %)"
                    + "   ·   Disponible: " + Formato.soles(monto.subtract(gastado)));
        }

        List<Map<String, ?>> categorias = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : resumen.getGastosPorCategoria().entrySet()) {
            categorias.add(fila("categoria", e.getKey(), "monto", Formato.soles(e.getValue()),
                    "porcentaje", Formato.porcentaje(e.getValue(), resumen.getGastos()) + " %"));
        }
        if (categorias.isEmpty()) {
            categorias.add(fila("categoria", "No hay gastos registrados en este mes."));
        }
        p.put("DS_CATEGORIAS", new JRMapCollectionDataSource(categorias));

        List<Map<String, ?>> comparacion = new ArrayList<>();
        comparacion.add(filaComparacion("Ingresos", anterior.getIngresos(), resumen.getIngresos()));
        comparacion.add(filaComparacion("Gastos", anterior.getGastos(), resumen.getGastos()));
        comparacion.add(filaComparacion("Balance", anterior.getBalance(), resumen.getBalance()));
        comparacion.add(filaComparacion("Ahorro en metas", anterior.getAhorroMetas(), resumen.getAhorroMetas()));
        p.put("DS_COMPARACION", new JRMapCollectionDataSource(comparacion));

        List<Map<String, ?>> filasMetas = new ArrayList<>();
        for (MetaAhorro m : metas) {
            filasMetas.add(fila("meta", m.getNombreMeta(), "ahorrado", Formato.soles(m.getMontoAhorrado()),
                    "objetivo", Formato.soles(m.getMontoObjetivo()),
                    "progreso", MetaAhorroServicio.calcularProgreso(m) + " %",
                    "fecha", Formato.fecha(m.getFechaObjetivo())));
        }
        if (filasMetas.isEmpty()) {
            filasMetas.add(fila("meta", "Aún no hay metas de ahorro registradas."));
        }
        p.put("DS_METAS", new JRMapCollectionDataSource(filasMetas));

        List<Map<String, ?>> filasRecomendaciones = new ArrayList<>();
        for (Recomendacion r : recomendaciones) {
            filasRecomendaciones.add(fila("titulo", r.getTitulo(), "descripcion", r.getDescripcion()));
        }
        if (filasRecomendaciones.isEmpty()) {
            filasRecomendaciones.add(fila("titulo", "Sin recomendaciones", "descripcion",
                    "Registra tus movimientos para recibir recomendaciones personalizadas."));
        }
        p.put("DS_RECOMENDACIONES", new JRMapCollectionDataSource(filasRecomendaciones));

        List<Map<String, ?>> filasMovimientos = new ArrayList<>();
        for (Movimiento mov : movimientos) {
            boolean hormiga = mov instanceof Gasto && ((Gasto) mov).isEsGastoHormiga();
            String descripcion = (mov.getDescripcion() == null ? "" : mov.getDescripcion()) + (hormiga ? " (hormiga)" : "");
            filasMovimientos.add(fila("fecha", Formato.fecha(mov.getFecha()), "descripcion", descripcion,
                    "categoria", mov.getNombreCategoria(), "monto", Formato.soles(mov.aplicarABalance())));
        }
        if (filasMovimientos.isEmpty()) {
            filasMovimientos.add(fila("descripcion", "No hay movimientos registrados en este mes."));
        }
        p.put("DS_MOVIMIENTOS", new JRMapCollectionDataSource(filasMovimientos));
        return p;
    }

    private static Map<String, ?> filaComparacion(String concepto, BigDecimal antes, BigDecimal ahora) {
        Integer variacion = EstadisticaServicio.variacion(antes, ahora);
        return fila("concepto", concepto, "anterior", Formato.soles(antes), "actual", Formato.soles(ahora),
                "variacion", variacion == null ? "—" : (variacion > 0 ? "+" : "") + variacion + " %");
    }

    /** Crea una fila de tabla a partir de pares nombre-valor. */
    private static Map<String, ?> fila(String... pares) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            fila.put(pares[i], pares[i + 1]);
        }
        return fila;
    }
}
