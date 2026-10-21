package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Recomendacion;
import com.mycompany.finanz.modelo.ReporteFinanciero;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de reportes PDF generados y recomendaciones emitidas al usuario.
 */
public class ReporteRecomendacionDAO {

    public void insertarReporte(ReporteFinanciero reporte) throws SQLException {
        String sql = "INSERT INTO reporte_financiero (id_usuario, periodo, formato) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reporte.getIdUsuario());
            ps.setString(2, reporte.getPeriodo());
            ps.setString(3, reporte.getFormato());
            ps.executeUpdate();
        }
    }

    public List<ReporteFinanciero> listarReportes(int idUsuario, int limite) throws SQLException {
        String sql = "SELECT * FROM reporte_financiero WHERE id_usuario = ? ORDER BY fecha_generacion DESC LIMIT ?";
        List<ReporteFinanciero> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReporteFinanciero r = new ReporteFinanciero();
                    r.setIdReporte(rs.getInt("id_reporte"));
                    r.setIdUsuario(idUsuario);
                    r.setFechaGeneracion(rs.getTimestamp("fecha_generacion").toLocalDateTime());
                    r.setPeriodo(rs.getString("periodo"));
                    r.setFormato(rs.getString("formato"));
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    /**
     * Reemplaza las recomendaciones de hoy por las recién calculadas, en una transacción
     * (así el historial guarda una "foto" por día sin duplicados).
     */
    public void reemplazarRecomendacionesDeHoy(int idUsuario, List<Recomendacion> recomendaciones) throws SQLException {
        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM recomendacion WHERE id_usuario = ? AND fecha = CURRENT_DATE")) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO recomendacion (id_usuario, titulo, descripcion, tipo) VALUES (?, ?, ?, ?)")) {
                    for (Recomendacion r : recomendaciones) {
                        ps.setInt(1, idUsuario);
                        ps.setString(2, r.getTitulo());
                        ps.setString(3, r.getDescripcion());
                        ps.setString(4, r.getTipo());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
