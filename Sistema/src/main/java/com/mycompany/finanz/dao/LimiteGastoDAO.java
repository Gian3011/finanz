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

    public int insertar(LimiteGasto limite) throws SQLException {
        String sql = "INSERT INTO limite_gasto (id_presupuesto, id_categoria, monto_limite) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, limite.getIdPresupuesto());
            ps.setInt(2, limite.getIdCategoria());
            ps.setBigDecimal(3, limite.getMontoLimite());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                limite.setIdLimite(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public LimiteGasto buscarPorId(int idLimite) throws SQLException {
        String sql = "SELECT * FROM limite_gasto WHERE id_limite = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idLimite);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<LimiteGasto> listarPorPresupuesto(int idPresupuesto) throws SQLException {
        String sql = "SELECT * FROM limite_gasto WHERE id_presupuesto = ?";
        List<LimiteGasto> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Busca el límite de una categoría específica dentro de un presupuesto.
     * Lo usará el Servicio para verificar si un gasto supera el límite de su categoría.
     * Devuelve null si esa categoría no tiene límite definido.
     */
    public LimiteGasto buscarPorPresupuestoYCategoria(int idPresupuesto, int idCategoria) throws SQLException {
        String sql = "SELECT * FROM limite_gasto WHERE id_presupuesto = ? AND id_categoria = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ps.setInt(2, idCategoria);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public void actualizar(LimiteGasto limite) throws SQLException {
        String sql = "UPDATE limite_gasto SET id_categoria = ?, monto_limite = ? WHERE id_limite = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limite.getIdCategoria());
            ps.setBigDecimal(2, limite.getMontoLimite());
            ps.setInt(3, limite.getIdLimite());
            ps.executeUpdate();
        }
    }

    public void eliminar(int idLimite) throws SQLException {
        String sql = "DELETE FROM limite_gasto WHERE id_limite = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idLimite);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina todos los límites de un presupuesto. Se usa antes de borrar el presupuesto
     * para no violar la Foreign Key.
     */
    public void eliminarPorPresupuesto(int idPresupuesto) throws SQLException {
        String sql = "DELETE FROM limite_gasto WHERE id_presupuesto = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ps.executeUpdate();
        }
    }

    private LimiteGasto mapear(ResultSet rs) throws SQLException {
        LimiteGasto l = new LimiteGasto();
        l.setIdLimite(rs.getInt("id_limite"));
        l.setIdPresupuesto(rs.getInt("id_presupuesto"));
        l.setIdCategoria(rs.getInt("id_categoria"));
        l.setMontoLimite(rs.getBigDecimal("monto_limite"));
        return l;
    }
}
