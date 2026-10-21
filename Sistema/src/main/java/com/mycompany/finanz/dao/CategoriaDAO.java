package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public int insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categoria (nombre_categoria, tipo) VALUES (?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, categoria.getNombreCategoria());
            ps.setString(2, categoria.getTipo());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                categoria.setIdCategoria(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public Categoria buscarPorId(int idCategoria) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE id_categoria = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCategoria);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<Categoria> listarTodas() throws SQLException {
        String sql = "SELECT * FROM categoria ORDER BY nombre_categoria";
        List<Categoria> categorias = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categorias.add(mapear(rs));
            }
        }
        return categorias;
    }

    public List<Categoria> listarPorTipo(String tipo) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE tipo = ? ORDER BY nombre_categoria";
        List<Categoria> categorias = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tipo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categorias.add(mapear(rs));
            }
        }
        return categorias;
    }

    public void actualizar(Categoria categoria) throws SQLException {
        String sql = "UPDATE categoria SET nombre_categoria = ?, tipo = ? WHERE id_categoria = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, categoria.getNombreCategoria());
            ps.setString(2, categoria.getTipo());
            ps.setInt(3, categoria.getIdCategoria());
            ps.executeUpdate();
        }
    }

    public void eliminar(int idCategoria) throws SQLException {
        String sql = "DELETE FROM categoria WHERE id_categoria = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCategoria);
            ps.executeUpdate();
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setIdCategoria(rs.getInt("id_categoria"));
        c.setNombreCategoria(rs.getString("nombre_categoria"));
        c.setTipo(rs.getString("tipo"));
        return c;
    }
}
