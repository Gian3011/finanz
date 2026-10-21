package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Las categorías con id_usuario NULL son las predeterminadas (las ven todos);
 * las demás son personales y solo las ve el usuario que las creó.
 */
public class CategoriaDAO {

    private static final String DISPONIBLES = "(id_usuario IS NULL OR id_usuario = ?)";

    public int insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categoria (id_usuario, nombre_categoria, tipo) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (categoria.getIdUsuario() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, categoria.getIdUsuario());
            }
            ps.setString(2, categoria.getNombreCategoria());
            ps.setString(3, categoria.getTipo());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    categoria.setIdCategoria(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    /** Devuelve la categoría solo si el usuario puede usarla (predeterminada o propia). */
    public Categoria buscarDisponible(int idCategoria, int idUsuario) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE id_categoria = ? AND " + DISPONIBLES;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCategoria);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Categoria> listarDisponibles(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE " + DISPONIBLES + " ORDER BY tipo, nombre_categoria";
        return listar(sql, idUsuario, null);
    }

    public List<Categoria> listarPorTipo(int idUsuario, String tipo) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE " + DISPONIBLES + " AND tipo = ? ORDER BY nombre_categoria";
        return listar(sql, idUsuario, tipo);
    }

    public boolean existeNombre(int idUsuario, String nombre, String tipo) throws SQLException {
        String sql = "SELECT 1 FROM categoria WHERE " + DISPONIBLES + " AND tipo = ? AND LOWER(nombre_categoria) = LOWER(?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setString(2, tipo);
            ps.setString(3, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Categoria> listar(String sql, int idUsuario, String tipo) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            if (tipo != null) {
                ps.setString(2, tipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categorias.add(mapear(rs));
                }
            }
        }
        return categorias;
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setIdCategoria(rs.getInt("id_categoria"));
        int idUsuario = rs.getInt("id_usuario");
        c.setIdUsuario(rs.wasNull() ? null : idUsuario);
        c.setNombreCategoria(rs.getString("nombre_categoria"));
        c.setTipo(rs.getString("tipo"));
        return c;
    }
}
