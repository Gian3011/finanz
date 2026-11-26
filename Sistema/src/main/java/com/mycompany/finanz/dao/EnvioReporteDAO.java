package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.EnvioReporte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Historial de reportes enviados por correo. Todas las consultas filtran por id_usuario. */
public class EnvioReporteDAO {

    public int insertar(EnvioReporte envio) throws SQLException {
        String sql = "INSERT INTO envio_reporte (id_usuario, periodo, formato, correo_destino, estado) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, envio.getIdUsuario());
            ps.setObject(2, envio.getPeriodo());
            ps.setString(3, envio.getFormato());
            ps.setString(4, envio.getCorreoDestino());
            ps.setString(5, envio.getEstado());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    envio.setIdEnvio(keys.getInt(1));
                }
            }
            return envio.getIdEnvio();
        }
    }

    public EnvioReporte buscarPorId(int idEnvio, int idUsuario) throws SQLException {
        String sql = "SELECT * FROM envio_reporte WHERE id_envio = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEnvio);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<EnvioReporte> listarRecientes(int idUsuario, int limite) throws SQLException {
        String sql = "SELECT * FROM envio_reporte WHERE id_usuario = ? ORDER BY fecha_envio DESC, id_envio DESC LIMIT ?";
        List<EnvioReporte> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Cantidad de envíos (exitosos o fallidos) del usuario desde la fecha indicada; sirve para el límite por hora. */
    public int contarDesde(int idUsuario, LocalDateTime desde) throws SQLException {
        String sql = "SELECT COUNT(*) FROM envio_reporte WHERE id_usuario = ? AND fecha_envio >= ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setTimestamp(2, Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean actualizarEstado(int idEnvio, int idUsuario, String estado) throws SQLException {
        String sql = "UPDATE envio_reporte SET estado = ? WHERE id_envio = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idEnvio);
            ps.setInt(3, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private EnvioReporte mapear(ResultSet rs) throws SQLException {
        EnvioReporte e = new EnvioReporte();
        e.setIdEnvio(rs.getInt("id_envio"));
        e.setIdUsuario(rs.getInt("id_usuario"));
        e.setPeriodo(rs.getDate("periodo").toLocalDate());
        e.setFormato(rs.getString("formato"));
        e.setCorreoDestino(rs.getString("correo_destino"));
        e.setFechaEnvio(rs.getTimestamp("fecha_envio").toLocalDateTime());
        e.setEstado(rs.getString("estado"));
        return e;
    }
}
