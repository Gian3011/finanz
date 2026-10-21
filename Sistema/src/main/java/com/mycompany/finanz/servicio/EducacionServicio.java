package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.EducacionDAO;
import com.mycompany.finanz.modelo.ContenidoEducativo;
import com.mycompany.finanz.modelo.Cuestionario;
import com.mycompany.finanz.modelo.Opcion;
import com.mycompany.finanz.modelo.Pregunta;
import com.mycompany.finanz.modelo.ResultadoCuestionario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Contenido educativo y calificación de cuestionarios.
 */
public class EducacionServicio {

    private final EducacionDAO educacionDAO;

    public EducacionServicio() {
        this(new EducacionDAO());
    }

    public EducacionServicio(EducacionDAO educacionDAO) {
        this.educacionDAO = educacionDAO;
    }

    public List<ContenidoEducativo> contenidos() throws SQLException {
        return educacionDAO.listarContenidos();
    }

    public ContenidoEducativo contenido(int idContenido) throws SQLException {
        return educacionDAO.buscarContenido(idContenido);
    }

    public Cuestionario cuestionario(int idCuestionario) throws SQLException {
        return educacionDAO.buscarCuestionario(idCuestionario);
    }

    public List<ResultadoCuestionario> resultados(int idUsuario) throws SQLException {
        return educacionDAO.listarResultados(idUsuario);
    }

    /**
     * Califica las respuestas (idPregunta -> idOpcion elegida) y guarda el resultado.
     * Exige que se respondan todas las preguntas.
     */
    public ResultadoCuestionario calificar(int idUsuario, Cuestionario cuestionario, Map<Integer, Integer> respuestas)
            throws ValidacionException, SQLException {
        int correctas = contarCorrectas(cuestionario, respuestas);
        ResultadoCuestionario resultado = new ResultadoCuestionario(
                idUsuario, cuestionario.getIdCuestionario(), correctas, cuestionario.getPreguntas().size());
        educacionDAO.insertarResultado(resultado);
        return resultado;
    }

    /** Regla pura: cuenta respuestas correctas (lanza error si falta responder alguna). */
    public static int contarCorrectas(Cuestionario cuestionario, Map<Integer, Integer> respuestas) throws ValidacionException {
        int correctas = 0;
        for (Pregunta p : cuestionario.getPreguntas()) {
            Integer elegida = respuestas.get(p.getIdPregunta());
            if (elegida == null) {
                throw new ValidacionException("Responde todas las preguntas antes de enviar.");
            }
            Opcion correcta = p.getOpcionCorrecta();
            if (correcta != null && correcta.getIdOpcion() == elegida) {
                correctas++;
            }
        }
        return correctas;
    }
}
