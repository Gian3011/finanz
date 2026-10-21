package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Presupuesto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAO {

    public int insertar(Presupuesto presupuesto) throws SQLException {
        String sql = "INSERT INTO presupuesto (id_usuario, periodo, monto_presupuesto) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, presupuesto.getIdUsuario());
            ps.setObject(2, presupuesto.getPeriodo());
            ps.setBigDecimal(3, presupuesto.getMontoPresupuesto());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                presupuesto.setIdPresupuesto(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public Presupuesto buscarPorId(int idPresupuesto) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_presupuesto = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    /**
     * Busca el presupuesto de un usuario para un período específico
     * (ej. LocalDate.of(2026, 9, 1) para representar "septiembre 2026").
     * Devuelve null si el usuario aún no definió presupuesto para ese período.
     */
    public Presupuesto buscarPorUsuarioYPeriodo(int idUsuario, java.time.LocalDate periodo) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_usuario = ? AND periodo = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, periodo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<Presupuesto> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_usuario = ? ORDER BY periodo DESC";
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public void actualizar(Presupuesto presupuesto) throws SQLException {
        String sql = "UPDATE presupuesto SET periodo = ?, monto_presupuesto = ? WHERE id_presupuesto = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setObject(1, presupuesto.getPeriodo());
            ps.setBigDecimal(2, presupuesto.getMontoPresupuesto());
            ps.setInt(3, presupuesto.getIdPresupuesto());
            ps.executeUpdate();
        }
    }

    /**
     * Ojo: si el presupuesto tiene límites por categoría asociados (tabla limite_gasto),
     * MySQL rechazará el borrado por la Foreign Key. Elimina primero los límites con
     * LimiteGastoDAO.eliminarPorPresupuesto() y luego llama a este método.
     */
    public void eliminar(int idPresupuesto) throws SQLException {
        String sql = "DELETE FROM presupuesto WHERE id_presupuesto = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ps.executeUpdate();
        }
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setIdPresupuesto(rs.getInt("id_presupuesto"));
        p.setIdUsuario(rs.getInt("id_usuario"));

        java.sql.Date periodo = rs.getDate("periodo");
        if (periodo != null) {
            p.setPeriodo(periodo.toLocalDate());
        }

        p.setMontoPresupuesto(rs.getBigDecimal("monto_presupuesto"));
        return p;
    }
}
