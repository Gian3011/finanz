package com.mycompany.finanz.dao;

import com.mycompany.finanz.modelo.ContenidoEducativo;
import com.mycompany.finanz.modelo.Cuestionario;
import com.mycompany.finanz.modelo.Opcion;
import com.mycompany.finanz.modelo.Pregunta;
import com.mycompany.finanz.modelo.ResultadoCuestionario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenido educativo, cuestionarios (con sus preguntas y opciones) y resultados.
 */
public class EducacionDAO {

    private static final String SELECT_CONTENIDO = "SELECT c.*, "
            + "(SELECT q.id_cuestionario FROM cuestionario q WHERE q.id_contenido = c.id_contenido LIMIT 1) id_cuestionario "
            + "FROM contenido_educativo c ";

    public List<ContenidoEducativo> listarContenidos() throws SQLException {
        List<ContenidoEducativo> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(SELECT_CONTENIDO + "ORDER BY c.id_contenido");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearContenido(rs));
            }
        }
        return lista;
    }

    public ContenidoEducativo buscarContenido(int idContenido) throws SQLException {
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(SELECT_CONTENIDO + "WHERE c.id_contenido = ?")) {

            ps.setInt(1, idContenido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearContenido(rs) : null;
            }
        }
    }

    /** Carga el cuestionario con todas sus preguntas y opciones (en una sola consulta). */
    public Cuestionario buscarCuestionario(int idCuestionario) throws SQLException {
        String sql = "SELECT q.id_cuestionario, q.id_contenido, q.titulo, q.descripcion, "
                + "p.id_pregunta, p.enunciado, o.id_opcion, o.texto, o.es_correcta "
                + "FROM cuestionario q "
                + "JOIN pregunta p ON p.id_cuestionario = q.id_cuestionario "
                + "JOIN opcion o ON o.id_pregunta = p.id_pregunta "
                + "WHERE q.id_cuestionario = ? ORDER BY p.id_pregunta, o.id_opcion";

        Cuestionario cuestionario = null;
        Map<Integer, Pregunta> preguntas = new LinkedHashMap<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCuestionario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (cuestionario == null) {
                        cuestionario = new Cuestionario();
                        cuestionario.setIdCuestionario(rs.getInt("id_cuestionario"));
                        cuestionario.setIdContenido(rs.getInt("id_contenido"));
                        cuestionario.setTitulo(rs.getString("titulo"));
                        cuestionario.setDescripcion(rs.getString("descripcion"));
                    }
                    int idPregunta = rs.getInt("id_pregunta");
                    Pregunta pregunta = preguntas.get(idPregunta);
                    if (pregunta == null) {
                        pregunta = new Pregunta();
                        pregunta.setIdPregunta(idPregunta);
                        pregunta.setIdCuestionario(idCuestionario);
                        pregunta.setEnunciado(rs.getString("enunciado"));
                        preguntas.put(idPregunta, pregunta);
                    }
                    Opcion opcion = new Opcion();
                    opcion.setIdOpcion(rs.getInt("id_opcion"));
                    opcion.setIdPregunta(idPregunta);
                    opcion.setTexto(rs.getString("texto"));
                    opcion.setEsCorrecta(rs.getBoolean("es_correcta"));
                    pregunta.getOpciones().add(opcion);
                }
            }
        }
        if (cuestionario != null) {
            cuestionario.setPreguntas(new ArrayList<>(preguntas.values()));
        }
        return cuestionario;
    }

    public void insertarResultado(ResultadoCuestionario r) throws SQLException {
        String sql = "INSERT INTO resultado_cuestionario (id_usuario, id_cuestionario, puntaje, total_preguntas) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, r.getIdUsuario());
            ps.setInt(2, r.getIdCuestionario());
            ps.setInt(3, r.getPuntaje());
            ps.setInt(4, r.getTotalPreguntas());
            ps.executeUpdate();
        }
    }

    public List<ResultadoCuestionario> listarResultados(int idUsuario) throws SQLException {
        String sql = "SELECT r.*, q.titulo FROM resultado_cuestionario r "
                + "JOIN cuestionario q ON q.id_cuestionario = r.id_cuestionario "
                + "WHERE r.id_usuario = ? ORDER BY r.fecha DESC, r.id_resultado DESC";
        List<ResultadoCuestionario> lista = new ArrayList<>();

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResultadoCuestionario r = new ResultadoCuestionario();
                    r.setIdResultado(rs.getInt("id_resultado"));
                    r.setIdUsuario(rs.getInt("id_usuario"));
                    r.setIdCuestionario(rs.getInt("id_cuestionario"));
                    r.setTituloCuestionario(rs.getString("titulo"));
                    r.setPuntaje(rs.getInt("puntaje"));
                    r.setTotalPreguntas(rs.getInt("total_preguntas"));
                    r.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    private ContenidoEducativo mapearContenido(ResultSet rs) throws SQLException {
        ContenidoEducativo c = new ContenidoEducativo();
        c.setIdContenido(rs.getInt("id_contenido"));
        c.setTitulo(rs.getString("titulo"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setContenido(rs.getString("contenido"));
        c.setTema(rs.getString("tema"));
        int idCuestionario = rs.getInt("id_cuestionario");
        c.setIdCuestionario(rs.wasNull() ? null : idCuestionario);
        return c;
    }
}
