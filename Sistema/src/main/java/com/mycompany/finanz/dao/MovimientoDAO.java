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
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    public int insertar(Movimiento movimiento) throws SQLException {
        String sql = "INSERT INTO movimiento_financiero "
                + "(id_usuario, id_categoria, monto, fecha, descripcion, tipo_movimiento, es_gasto_hormiga) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movimiento.getIdUsuario());
            ps.setInt(2, movimiento.getIdCategoria());
            ps.setBigDecimal(3, movimiento.getMonto());
            ps.setObject(4, movimiento.getFecha());
            ps.setString(5, movimiento.getDescripcion());
            ps.setString(6, movimiento.getTipoMovimiento());

            if (movimiento instanceof Gasto) {
                Gasto gasto = (Gasto) movimiento;
                if (gasto.getEsGastoHormiga() != null) {
                    ps.setBoolean(7, gasto.getEsGastoHormiga());
                } else {
                    ps.setNull(7, java.sql.Types.BOOLEAN);
                }
            } else {
                ps.setNull(7, java.sql.Types.BOOLEAN);
            }

            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                movimiento.setIdMovimiento(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public Movimiento buscarPorId(int idMovimiento) throws SQLException {
        String sql = "SELECT * FROM movimiento_financiero WHERE id_movimiento = ?";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<Movimiento> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM movimiento_financiero WHERE id_usuario = ? ORDER BY fecha DESC";
        List<Movimiento> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Filtra el historial por rango de fechas (requerimiento 5: consultar y
     * filtrar historial).
     */
    public List<Movimiento> listarPorUsuarioYPeriodo(int idUsuario, java.time.LocalDate desde, java.time.LocalDate hasta) throws SQLException {
        String sql = "SELECT * FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND fecha BETWEEN ? AND ? ORDER BY fecha DESC";
        List<Movimiento> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Cuenta cuántos gastos tuvo el usuario en una categoría en los últimos
     * "dias". Lo usa GastoHormigaServicio para evaluar el criterio de
     * frecuencia.
     */
    public int contarGastosPorCategoriaEnPeriodo(int idUsuario, int idCategoria, int dias) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND id_categoria = ? "
                + "AND tipo_movimiento = 'GASTO' "
                + "AND fecha >= DATE_SUB(CURDATE(), INTERVAL ? DAY)";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setInt(3, dias);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void actualizarFlagHormiga(int idMovimiento, boolean esHormiga) throws SQLException {
        String sql = "UPDATE movimiento_financiero SET es_gasto_hormiga = ? WHERE id_movimiento = ?";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, esHormiga);
            ps.setInt(2, idMovimiento);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idMovimiento) throws SQLException {
        String sql = "DELETE FROM movimiento_financiero WHERE id_movimiento = ?";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);
            ps.executeUpdate();
        }
    }

    /**
     * Mapea la fila hacia Ingreso o Gasto según tipo_movimiento (polimorfismo
     * del modelo).
     */
    private Movimiento mapear(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo_movimiento");

        Movimiento movimiento;
        if ("INGRESO".equals(tipo)) {
            movimiento = new Ingreso();
        } else {
            Gasto gasto = new Gasto();
            boolean esHormiga = rs.getBoolean("es_gasto_hormiga");
            gasto.setEsGastoHormiga(rs.wasNull() ? null : esHormiga);
            movimiento = gasto;
        }

        movimiento.setIdMovimiento(rs.getInt("id_movimiento"));
        movimiento.setIdUsuario(rs.getInt("id_usuario"));
        movimiento.setIdCategoria(rs.getInt("id_categoria"));
        movimiento.setMonto(rs.getBigDecimal("monto"));

        java.sql.Date fecha = rs.getDate("fecha");
        if (fecha != null) {
            movimiento.setFecha(fecha.toLocalDate());
        }

        movimiento.setDescripcion(rs.getString("descripcion"));

        return movimiento;
    }
    // Agrega estos dos métodos dentro de la clase MovimientoDAO (no reemplaces el archivo completo).
// Necesitan este import adicional al inicio del archivo:
// import java.math.BigDecimal;

    /**
     * Suma el total de gastos de un usuario en un rango de fechas. La usa
     * PresupuestoServicio para calcular el monto disponible (requerimiento 8).
     */
    public BigDecimal sumarGastosPorUsuarioYPeriodo(int idUsuario, java.time.LocalDate desde, java.time.LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND tipo_movimiento = 'GASTO' AND fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    /**
     * Suma el total de gastos de un usuario en una categoría específica, en un
     * rango de fechas. La usa PresupuestoServicio para verificar si se supera
     * el límite de una categoría (requerimiento 7).
     */
    public BigDecimal sumarGastosPorCategoriaYPeriodo(int idUsuario, int idCategoria, 
            java.time.LocalDate desde, java.time.LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimiento_financiero "
                + "WHERE id_usuario = ? AND id_categoria = ? AND tipo_movimiento = 'GASTO' AND fecha BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idCategoria);
            ps.setObject(3, desde);
            ps.setObject(4, hasta);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }
}
