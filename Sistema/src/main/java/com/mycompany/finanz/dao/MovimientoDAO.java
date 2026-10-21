package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Ingreso;
import com.mycompany.finanz.modelo.Movimiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Acceso a movimiento_financiero y a sus especializaciones ingreso / gasto.
 * Todas las consultas filtran por id_usuario para que nadie pueda ver ni
 * modificar movimientos de otra persona.
 */
public class MovimientoDAO {

    private static final String SELECT_BASE =
            "SELECT m.*, c.nombre_categoria, g.es_gasto_hormiga, i.fuente "
            + "FROM movimiento_financiero m "
            + "JOIN categoria c ON c.id_categoria = m.id_categoria "
            + "LEFT JOIN gasto g ON g.id_movimiento = m.id_movimiento "
            + "LEFT JOIN ingreso i ON i.id_movimiento = m.id_movimiento ";

    /**
     * Inserta el movimiento y su fila en ingreso o gasto dentro de UNA transacción:
     * si falla cualquiera de los dos INSERT se hace rollback y no queda nada a medias.
     */
    public int insertar(Movimiento movimiento) throws SQLException {
        String sqlMovimiento = "INSERT INTO movimiento_financiero "
                + "(id_usuario, id_categoria, monto, fecha, descripcion, tipo_movimiento) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);
            try {
                int idGenerado;
                try (PreparedStatement ps = con.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, movimiento.getIdUsuario());
                    ps.setInt(2, movimiento.getIdCategoria());
                    ps.setBigDecimal(3, movimiento.getMonto());
                    ps.setObject(4, movimiento.getFecha());
                    ps.setString(5, movimiento.getDescripcion());
                    ps.setString(6, movimiento.getTipoMovimiento());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se obtuvo el id del movimiento");
                        }
                        idGenerado = keys.getInt(1);
                    }
                }

                if (movimiento instanceof Gasto) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO gasto (id_movimiento, es_gasto_hormiga) VALUES (?, ?)")) {
                        ps.setInt(1, idGenerado);
                        ps.setBoolean(2, ((Gasto) movimiento).isEsGastoHormiga());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO ingreso (id_movimiento, fuente) VALUES (?, ?)")) {
                        ps.setInt(1, idGenerado);
                        ps.setString(2, ((Ingreso) movimiento).getFuente());
                        ps.executeUpdate();
                    }
                }

                con.commit();
                movimiento.setIdMovimiento(idGenerado);
                return idGenerado;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Movimiento> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_usuario = ? ORDER BY m.fecha DESC, m.id_movimiento DESC";
        return listar(sql, idUsuario);
    }

    /** Filtra el historial por rango de fechas (requerimiento: consultar y filtrar historial). */
    public List<Movimiento> listarPorUsuarioYPeriodo(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_usuario = ? AND m.fecha BETWEEN ? AND ? "
                + "ORDER BY m.fecha DESC, m.id_movimiento DESC";
        return listar(sql, idUsuario, desde, hasta);
    }

    public List<Movimiento> listarGastosHormiga(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_usuario = ? AND g.es_gasto_hormiga = 1 AND m.fecha BETWEEN ? AND ? "
                + "ORDER BY m.fecha DESC";
        return listar(sql, idUsuario, desde, hasta);
    }

    /**
     * Cuenta los gastos "pequeños" (monto menor al umbral) del usuario en una categoría
     * dentro de un rango de fechas. Lo usa GastoHormigaServicio para el criterio de frecuencia.
     */
    public int contarGastosPequenos(int idUsuario, int idCategoria, BigDecimal umbral,
                                    LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND id_categoria = ? AND tipo_movimiento = 'GASTO' "
                + "AND monto < ? AND fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setBigDecimal(3, umbral);
            ps.setObject(4, desde);
            ps.setObject(5, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Marca como hormiga todos los gastos pequeños de la categoría en el rango (cuando se confirma el patrón). */
    public void marcarGastosPequenosComoHormiga(int idUsuario, int idCategoria, BigDecimal umbral,
                                                LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "UPDATE gasto g JOIN movimiento_financiero m ON m.id_movimiento = g.id_movimiento "
                + "SET g.es_gasto_hormiga = 1 "
                + "WHERE m.id_usuario = ? AND m.id_categoria = ? AND m.monto < ? AND m.fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setBigDecimal(3, umbral);
            ps.setObject(4, desde);
            ps.setObject(5, hasta);
            ps.executeUpdate();
        }
    }

    /** Permite al usuario clasificar manualmente un gasto como hormiga (o quitarle la marca). */
    public boolean actualizarFlagHormiga(int idMovimiento, int idUsuario, boolean esHormiga) throws SQLException {
        String sql = "UPDATE gasto g JOIN movimiento_financiero m ON m.id_movimiento = g.id_movimiento "
                + "SET g.es_gasto_hormiga = ? WHERE g.id_movimiento = ? AND m.id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, esHormiga);
            ps.setInt(2, idMovimiento);
            ps.setInt(3, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    /** Elimina el movimiento (ingreso/gasto se borran en cascada). Solo si pertenece al usuario. */
    public boolean eliminar(int idMovimiento, int idUsuario) throws SQLException {
        String sql = "DELETE FROM movimiento_financiero WHERE id_movimiento = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    /** Suma los movimientos de un tipo (INGRESO o GASTO) en un rango de fechas. */
    public BigDecimal sumarPorTipoYPeriodo(int idUsuario, String tipo, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND tipo_movimiento = ? AND fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setString(2, tipo);
            ps.setObject(3, desde);
            ps.setObject(4, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public BigDecimal sumarGastosPorUsuarioYPeriodo(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        return sumarPorTipoYPeriodo(idUsuario, "GASTO", desde, hasta);
    }

    /** Total gastado en una categoría dentro de un rango (para límites por categoría). */
    public BigDecimal sumarGastosPorCategoriaYPeriodo(int idUsuario, int idCategoria,
                                                      LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND id_categoria = ? AND tipo_movimiento = 'GASTO' AND fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setObject(3, desde);
            ps.setObject(4, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public BigDecimal sumarGastosHormiga(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(m.monto), 0) FROM movimiento_financiero m "
                + "JOIN gasto g ON g.id_movimiento = m.id_movimiento "
                + "WHERE m.id_usuario = ? AND g.es_gasto_hormiga = 1 AND m.fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /** Gasto total por categoría en un rango, de mayor a menor (para gráficos y reportes). */
    public Map<String, BigDecimal> gastosPorCategoria(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT c.nombre_categoria, SUM(m.monto) total FROM movimiento_financiero m "
                + "JOIN categoria c ON c.id_categoria = m.id_categoria "
                + "WHERE m.id_usuario = ? AND m.tipo_movimiento = 'GASTO' AND m.fecha BETWEEN ? AND ? "
                + "GROUP BY c.nombre_categoria ORDER BY total DESC";
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        }
        return resultado;
    }

    /**
     * Ingresos y gastos agrupados por mes: cada valor es {ingresos, gastos}.
     * Los meses sin movimientos no aparecen (el servicio los rellena con cero).
     */
    public Map<YearMonth, BigDecimal[]> totalesPorMes(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT YEAR(fecha) anio, MONTH(fecha) mes, "
                + "SUM(CASE WHEN tipo_movimiento = 'INGRESO' THEN monto ELSE 0 END) ingresos, "
                + "SUM(CASE WHEN tipo_movimiento = 'GASTO' THEN monto ELSE 0 END) gastos "
                + "FROM movimiento_financiero WHERE id_usuario = ? AND fecha BETWEEN ? AND ? "
                + "GROUP BY YEAR(fecha), MONTH(fecha)";
        Map<YearMonth, BigDecimal[]> resultado = new TreeMap<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
                            new BigDecimal[]{rs.getBigDecimal("ingresos"), rs.getBigDecimal("gastos")});
                }
            }
        }
        return resultado;
    }

    private List<Movimiento> listar(String sql, int idUsuario, Object... parametros) throws SQLException {
        List<Movimiento> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 2, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Mapea la fila hacia Ingreso o Gasto según tipo_movimiento (polimorfismo del modelo).
     */
    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento movimiento;
        if ("INGRESO".equals(rs.getString("tipo_movimiento"))) {
            Ingreso ingreso = new Ingreso();
            ingreso.setFuente(rs.getString("fuente"));
            movimiento = ingreso;
        } else {
            Gasto gasto = new Gasto();
            gasto.setEsGastoHormiga(rs.getBoolean("es_gasto_hormiga"));
            movimiento = gasto;
        }

        movimiento.setIdMovimiento(rs.getInt("id_movimiento"));
        movimiento.setIdUsuario(rs.getInt("id_usuario"));
        movimiento.setIdCategoria(rs.getInt("id_categoria"));
        movimiento.setNombreCategoria(rs.getString("nombre_categoria"));
        movimiento.setMonto(rs.getBigDecimal("monto"));
        java.sql.Date fecha = rs.getDate("fecha");
        movimiento.setFecha(fecha == null ? null : fecha.toLocalDate());
        movimiento.setDescripcion(rs.getString("descripcion"));
        return movimiento;
    }
}
