package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UsuarioDAO {

    public int insertar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (nombres, apellidos, email, password_hash) VALUES (?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombres());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    usuario.setIdUsuario(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Guarda el contador de intentos fallidos y, si corresponde, hasta cuándo queda bloqueada la cuenta. */
    public void actualizarIntentos(int idUsuario, int intentos, LocalDateTime bloqueadoHasta) throws SQLException {
        String sql = "UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, intentos);
            ps.setTimestamp(2, bloqueadoHasta == null ? null : Timestamp.valueOf(bloqueadoHasta));
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarPerfil(int idUsuario, String nombres, String apellidos) throws SQLException {
        String sql = "UPDATE usuario SET nombres = ?, apellidos = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarPassword(int idUsuario, String passwordHash) throws SQLException {
        String sql = "UPDATE usuario SET password_hash = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));

        Timestamp registro = rs.getTimestamp("fecha_registro");
        u.setFechaRegistro(registro == null ? null : registro.toLocalDateTime());
        Timestamp bloqueo = rs.getTimestamp("bloqueado_hasta");
        u.setBloqueadoHasta(bloqueo == null ? null : bloqueo.toLocalDateTime());
        return u;
    }
}
