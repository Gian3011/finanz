package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Presupuesto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    presupuesto.setIdPresupuesto(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    /** Busca un presupuesto solo si pertenece al usuario indicado. */
    public Presupuesto buscarPorId(int idPresupuesto, int idUsuario) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_presupuesto = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPresupuesto);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Busca el presupuesto de un usuario para un período (el mes se representa
     * con su primer día, ej. 2026-10-01). Devuelve null si aún no lo definió.
     */
    public Presupuesto buscarPorUsuarioYPeriodo(int idUsuario, LocalDate periodo) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_usuario = ? AND periodo = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Presupuesto> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM presupuesto WHERE id_usuario = ? ORDER BY periodo DESC";
        List<Presupuesto> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public void actualizarMonto(int idPresupuesto, int idUsuario, BigDecimal monto) throws SQLException {
        String sql = "UPDATE presupuesto SET monto_presupuesto = ? WHERE id_presupuesto = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, monto);
            ps.setInt(2, idPresupuesto);
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        }
    }

    private Presupuesto mapear(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setIdPresupuesto(rs.getInt("id_presupuesto"));
        p.setIdUsuario(rs.getInt("id_usuario"));
        p.setPeriodo(rs.getDate("periodo").toLocalDate());
        p.setMontoPresupuesto(rs.getBigDecimal("monto_presupuesto"));
        return p;
    }
}
