package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.MetaAhorro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetaAhorroDAO {

    /** Incluye el total aportado a cada meta para mostrar su progreso. */
    private static final String SELECT_BASE = "SELECT m.*, "
            + "(SELECT COALESCE(SUM(a.monto_aporte), 0) FROM aporte_ahorro a WHERE a.id_meta = m.id_meta) monto_ahorrado "
            + "FROM meta_ahorro m ";

    public int insertar(MetaAhorro meta) throws SQLException {
        String sql = "INSERT INTO meta_ahorro (id_usuario, nombre_meta, monto_objetivo, fecha_objetivo, estado) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, meta.getIdUsuario());
            ps.setString(2, meta.getNombreMeta());
            ps.setBigDecimal(3, meta.getMontoObjetivo());
            ps.setObject(4, meta.getFechaObjetivo());
            ps.setString(5, meta.getEstado());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    meta.setIdMeta(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    /** Busca la meta solo si pertenece al usuario. */
    public MetaAhorro buscarPorId(int idMeta, int idUsuario) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_meta = ? AND m.id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<MetaAhorro> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_usuario = ? ORDER BY m.estado DESC, m.fecha_objetivo ASC";
        List<MetaAhorro> lista = new ArrayList<>();

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

    public void actualizarEstado(int idMeta, String estado) throws SQLException {
        String sql = "UPDATE meta_ahorro SET estado = ? WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idMeta);
            ps.executeUpdate();
        }
    }

    /** Elimina la meta del usuario; sus aportes se borran en cascada (ON DELETE CASCADE). */
    public boolean eliminar(int idMeta, int idUsuario) throws SQLException {
        String sql = "DELETE FROM meta_ahorro WHERE id_meta = ? AND id_usuario = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private MetaAhorro mapear(ResultSet rs) throws SQLException {
        MetaAhorro m = new MetaAhorro();
        m.setIdMeta(rs.getInt("id_meta"));
        m.setIdUsuario(rs.getInt("id_usuario"));
        m.setNombreMeta(rs.getString("nombre_meta"));
        m.setMontoObjetivo(rs.getBigDecimal("monto_objetivo"));
        m.setFechaObjetivo(rs.getDate("fecha_objetivo").toLocalDate());
        m.setEstado(rs.getString("estado"));
        java.sql.Date creacion = rs.getDate("fecha_creacion");
        m.setFechaCreacion(creacion == null ? null : creacion.toLocalDate());
        m.setMontoAhorrado(rs.getBigDecimal("monto_ahorrado"));
        return m;
    }
}
