package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.SimulacionAhorro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SimulacionAhorroDAO {

    public int insertar(SimulacionAhorro s) throws SQLException {
        String sql = "INSERT INTO simulacion_ahorro (id_usuario, monto_objetivo, monto_inicial, aporte_periodico, "
                + "frecuencia, numero_periodos, fecha_objetivo) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, s.getIdUsuario());
            ps.setBigDecimal(2, s.getMontoObjetivo());
            ps.setBigDecimal(3, s.getMontoInicial());
            ps.setBigDecimal(4, s.getAportePeriodico());
            ps.setString(5, s.getFrecuencia().name());
            ps.setInt(6, s.getNumeroPeriodos());
            ps.setObject(7, s.getFechaObjetivo());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdSimulacion(keys.getInt(1));
                    return s.getIdSimulacion();
                }
            }
            return -1;
        }
    }

    public List<SimulacionAhorro> listarRecientes(int idUsuario, int limite) throws SQLException {
        String sql = "SELECT * FROM simulacion_ahorro WHERE id_usuario = ? ORDER BY fecha_creacion DESC, id_simulacion DESC LIMIT ?";
        List<SimulacionAhorro> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SimulacionAhorro s = new SimulacionAhorro();
                    s.setIdSimulacion(rs.getInt("id_simulacion"));
                    s.setIdUsuario(rs.getInt("id_usuario"));
                    s.setMontoObjetivo(rs.getBigDecimal("monto_objetivo"));
                    s.setMontoInicial(rs.getBigDecimal("monto_inicial"));
                    s.setAportePeriodico(rs.getBigDecimal("aporte_periodico"));
                    s.setFrecuencia(SimulacionAhorro.Frecuencia.valueOf(rs.getString("frecuencia")));
                    s.setNumeroPeriodos(rs.getInt("numero_periodos"));
                    s.setFechaObjetivo(rs.getDate("fecha_objetivo").toLocalDate());
                    s.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                    lista.add(s);
                }
            }
        }
        return lista;
    }

    public boolean eliminar(int idSimulacion, int idUsuario) throws SQLException {
        String sql = "DELETE FROM simulacion_ahorro WHERE id_simulacion = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSimulacion);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }
}
