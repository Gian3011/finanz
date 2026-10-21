package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.AporteAhorro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AporteAhorroDAO {

    public int insertar(AporteAhorro aporte) throws SQLException {
        String sql = "INSERT INTO aporte_ahorro (id_meta, monto_aporte, fecha) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, aporte.getIdMeta());
            ps.setBigDecimal(2, aporte.getMontoAporte());
            ps.setObject(3, aporte.getFecha());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idGenerado = generatedKeys.getInt(1);
                aporte.setIdAporte(idGenerado);
                return idGenerado;
            }
            return -1;
        }
    }

    public AporteAhorro buscarPorId(int idAporte) throws SQLException {
        String sql = "SELECT * FROM aporte_ahorro WHERE id_aporte = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAporte);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        }
    }

    public List<AporteAhorro> listarPorMeta(int idMeta) throws SQLException {
        String sql = "SELECT * FROM aporte_ahorro WHERE id_meta = ? ORDER BY fecha ASC";
        List<AporteAhorro> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Suma todos los aportes de una meta. Lo usa MetaAhorroServicio para calcular
     * el progreso (monto_actual acumulado / monto_objetivo).
     */
    public BigDecimal sumarAportesPorMeta(int idMeta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto_aporte), 0) FROM aporte_ahorro WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    public void eliminar(int idAporte) throws SQLException {
        String sql = "DELETE FROM aporte_ahorro WHERE id_aporte = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAporte);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina todos los aportes de una meta. Se usa antes de borrar la meta,
     * para no violar la Foreign Key.
     */
    public void eliminarPorMeta(int idMeta) throws SQLException {
        String sql = "DELETE FROM aporte_ahorro WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            ps.executeUpdate();
        }
    }

    private AporteAhorro mapear(ResultSet rs) throws SQLException {
        AporteAhorro a = new AporteAhorro();
        a.setIdAporte(rs.getInt("id_aporte"));
        a.setIdMeta(rs.getInt("id_meta"));
        a.setMontoAporte(rs.getBigDecimal("monto_aporte"));

        java.sql.Date fecha = rs.getDate("fecha");
        if (fecha != null) {
            a.setFecha(fecha.toLocalDate());
        }

        return a;
    }
}
