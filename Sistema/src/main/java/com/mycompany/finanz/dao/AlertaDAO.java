package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.Alerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AlertaDAO {

    public void insertar(Alerta alerta) throws SQLException {
        String sql = "INSERT INTO alerta (id_usuario, id_presupuesto, id_limite, nivel, mensaje) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, alerta.getIdUsuario());
            setEnteroONulo(ps, 2, alerta.getIdPresupuesto());
            setEnteroONulo(ps, 3, alerta.getIdLimite());
            ps.setString(4, alerta.getNivel());
            ps.setString(5, alerta.getMensaje());
            ps.executeUpdate();
        }
    }

    /** true si ya se avisó ese nivel para el mismo presupuesto (o límite); evita alertas repetidas. */
    public boolean existe(Integer idPresupuesto, Integer idLimite, String nivel) throws SQLException {
        String sql = idLimite != null
                ? "SELECT 1 FROM alerta WHERE id_limite = ? AND nivel = ?"
                : "SELECT 1 FROM alerta WHERE id_presupuesto = ? AND id_limite IS NULL AND nivel = ?";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idLimite != null ? idLimite : idPresupuesto);
            ps.setString(2, nivel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Alerta> listarRecientes(int idUsuario, int limite) throws SQLException {
        String sql = "SELECT * FROM alerta WHERE id_usuario = ? ORDER BY fecha DESC, id_alerta DESC LIMIT ?";
        List<Alerta> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alerta a = new Alerta();
                    a.setIdAlerta(rs.getInt("id_alerta"));
                    a.setIdUsuario(rs.getInt("id_usuario"));
                    a.setNivel(rs.getString("nivel"));
                    a.setMensaje(rs.getString("mensaje"));
                    a.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    a.setEstado(rs.getString("estado"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    public int contarPendientes(int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM alerta WHERE id_usuario = ? AND estado = 'PENDIENTE'";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void marcarTodasLeidas(int idUsuario) throws SQLException {
        String sql = "UPDATE alerta SET estado = 'LEIDO' WHERE id_usuario = ? AND estado = 'PENDIENTE'";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private void setEnteroONulo(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }
}
