package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.LimiteGasto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LimiteGastoDAO {

    private static final String SELECT_BASE = "SELECT l.*, c.nombre_categoria FROM limite_gasto l "
            + "JOIN categoria c ON c.id_categoria = l.id_categoria ";

    public int insertar(LimiteGasto limite) throws SQLException {
        String sql = "INSERT INTO limite_gasto (id_presupuesto, id_categoria, monto_limite) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, limite.getIdPresupuesto());
            ps.setInt(2, limite.getIdCategoria());
            ps.setBigDecimal(3, limite.getMontoLimite());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    limite.setIdLimite(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    public List<LimiteGasto> listarPorPresupuesto(int idPresupuesto) throws SQLException {
        String sql = SELECT_BASE + "WHERE l.id_presupuesto = ? ORDER BY c.nombre_categoria";
        List<LimiteGasto> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Busca el límite de una categoría dentro de un presupuesto.
     * Devuelve null si esa categoría no tiene límite definido.
     */
    public LimiteGasto buscarPorPresupuestoYCategoria(int idPresupuesto, int idCategoria) throws SQLException {
        String sql = SELECT_BASE + "WHERE l.id_presupuesto = ? AND l.id_categoria = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ps.setInt(2, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void actualizarMonto(LimiteGasto limite) throws SQLException {
        String sql = "UPDATE limite_gasto SET monto_limite = ? WHERE id_limite = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, limite.getMontoLimite());
            ps.setInt(2, limite.getIdLimite());
            ps.executeUpdate();
        }
    }

    /** Elimina un límite solo si su presupuesto pertenece al usuario. */
    public boolean eliminar(int idLimite, int idUsuario) throws SQLException {
        String sql = "DELETE l FROM limite_gasto l JOIN presupuesto p ON p.id_presupuesto = l.id_presupuesto "
                + "WHERE l.id_limite = ? AND p.id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idLimite);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private LimiteGasto mapear(ResultSet rs) throws SQLException {
        LimiteGasto l = new LimiteGasto();
        l.setIdLimite(rs.getInt("id_limite"));
        l.setIdPresupuesto(rs.getInt("id_presupuesto"));
        l.setIdCategoria(rs.getInt("id_categoria"));
        l.setNombreCategoria(rs.getString("nombre_categoria"));
        l.setMontoLimite(rs.getBigDecimal("monto_limite"));
        return l;
    }
}
