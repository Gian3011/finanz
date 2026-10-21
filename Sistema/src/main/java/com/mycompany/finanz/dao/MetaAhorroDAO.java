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

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                meta.setIdMeta(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public MetaAhorro buscarPorId(int idMeta) throws SQLException {
        String sql = "SELECT * FROM meta_ahorro WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<MetaAhorro> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM meta_ahorro WHERE id_usuario = ? ORDER BY fecha_objetivo ASC";
        List<MetaAhorro> lista = new ArrayList<>();

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

    public void actualizar(MetaAhorro meta) throws SQLException {
        String sql = "UPDATE meta_ahorro SET nombre_meta = ?, monto_objetivo = ?, fecha_objetivo = ?, estado = ? "
                + "WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, meta.getNombreMeta());
            ps.setBigDecimal(2, meta.getMontoObjetivo());
            ps.setObject(3, meta.getFechaObjetivo());
            ps.setString(4, meta.getEstado());
            ps.setInt(5, meta.getIdMeta());
            ps.executeUpdate();
        }
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

    /**
     * Ojo: si la meta tiene aportes asociados (tabla aporte_ahorro), MySQL rechazará
     * el borrado por la Foreign Key. Elimina primero los aportes con
     * AporteAhorroDAO.eliminarPorMeta() y luego llama a este método.
     */
    public void eliminar(int idMeta) throws SQLException {
        String sql = "DELETE FROM meta_ahorro WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ps.executeUpdate();
        }
    }

    private MetaAhorro mapear(ResultSet rs) throws SQLException {
        MetaAhorro m = new MetaAhorro();
        m.setIdMeta(rs.getInt("id_meta"));
        m.setIdUsuario(rs.getInt("id_usuario"));
        m.setNombreMeta(rs.getString("nombre_meta"));
        m.setMontoObjetivo(rs.getBigDecimal("monto_objetivo"));

        java.sql.Date fecha = rs.getDate("fecha_objetivo");
        if (fecha != null) {
            m.setFechaObjetivo(fecha.toLocalDate());
        }

        m.setEstado(rs.getString("estado"));
        return m;
    }
}
