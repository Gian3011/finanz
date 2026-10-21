package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.AporteAhorro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AporteAhorroDAO {

    public int insertar(AporteAhorro aporte) throws SQLException {
        String sql = "INSERT INTO aporte_ahorro (id_meta, monto_aporte, fecha_aporte) VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, aporte.getIdMeta());
            ps.setBigDecimal(2, aporte.getMontoAporte());
            ps.setObject(3, aporte.getFecha());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int idGenerado = generatedKeys.getInt(1);
                    aporte.setIdAporte(idGenerado);
                    return idGenerado;
                }
            }
            return -1;
        }
    }

    public List<AporteAhorro> listarPorMeta(int idMeta) throws SQLException {
        String sql = "SELECT * FROM aporte_ahorro WHERE id_meta = ? ORDER BY fecha_aporte DESC, id_aporte DESC";
        List<AporteAhorro> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AporteAhorro a = new AporteAhorro();
                    a.setIdAporte(rs.getInt("id_aporte"));
                    a.setIdMeta(rs.getInt("id_meta"));
                    a.setMontoAporte(rs.getBigDecimal("monto_aporte"));
                    a.setFecha(rs.getDate("fecha_aporte").toLocalDate());
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    /** Suma todos los aportes de una meta (progreso = acumulado / monto objetivo). */
    public BigDecimal sumarAportesPorMeta(int idMeta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto_aporte), 0) FROM aporte_ahorro WHERE id_meta = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMeta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /** Total aportado a todas las metas del usuario en un rango (ahorro del periodo). */
    public BigDecimal sumarAportesPorUsuarioYPeriodo(int idUsuario, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(a.monto_aporte), 0) FROM aporte_ahorro a "
                + "JOIN meta_ahorro m ON m.id_meta = a.id_meta "
                + "WHERE m.id_usuario = ? AND a.fecha_aporte BETWEEN ? AND ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setObject(2, desde);
            ps.setObject(3, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
