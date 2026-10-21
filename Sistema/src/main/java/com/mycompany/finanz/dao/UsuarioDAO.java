package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class UsuarioDAO {

    public int insertar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (nombres, apellidos, email, fecha_registro, password_hash, "
                + "google_id, proveedor, foto_perfil) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombres());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, usuario.getEmail());
            ps.setObject(4, usuario.getFechaRegistro() != null ? usuario.getFechaRegistro() : LocalDate.now());
            ps.setString(5, usuario.getPasswordHash());
            ps.setString(6, usuario.getGoogleId());
            ps.setString(7, usuario.getProveedor());
            ps.setString(8, usuario.getFotoPerfil());

            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                usuario.setIdUsuario(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public Usuario buscarPorGoogleId(String googleId) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE google_id = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, googleId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public void actualizarIntentosFallidos(int idUsuario, int intentos) throws SQLException {
        String sql = "UPDATE usuario SET intentos_fallidos = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, intentos);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarBloqueado(int idUsuario, boolean bloqueado) throws SQLException {
        String sql = "UPDATE usuario SET bloqueado = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, bloqueado);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuario SET nombres = ?, apellidos = ?, email = ?, password_hash = ?, "
                + "foto_perfil = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario.getNombres());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getPasswordHash());
            ps.setString(5, usuario.getFotoPerfil());
            ps.setInt(6, usuario.getIdUsuario());

            ps.executeUpdate();
        }
    }

    public void eliminar(int idUsuario) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setEmail(rs.getString("email"));

        java.sql.Date fecha = rs.getDate("fecha_registro");
        if (fecha != null) {
            u.setFechaRegistro(fecha.toLocalDate());
        }

        u.setPasswordHash(rs.getString("password_hash"));
        u.setGoogleId(rs.getString("google_id"));
        u.setProveedor(rs.getString("proveedor"));
        u.setFotoPerfil(rs.getString("foto_perfil"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        u.setBloqueado(rs.getBoolean("bloqueado"));

        return u;
    }
}
