package com.mycompany.finanz.controlador;

import com.mycompany.finanz.modelo.ContenidoEducativo;
import com.mycompany.finanz.modelo.Cuestionario;
import com.mycompany.finanz.modelo.Pregunta;
import com.mycompany.finanz.servicio.EducacionServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Educación financiera: lista de lecciones, lectura de una lección y su
 * cuestionario (con calificación y corrección).
 */
@WebServlet(name = "EducacionServlet", urlPatterns = {"/educacion"})
public class EducacionServlet extends BaseServlet {

    private final EducacionServicio educacionServicio = new EducacionServicio();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
        String leccion = request.getParameter("leccion");

        if (leccion != null && !leccion.isBlank()) {
            ContenidoEducativo contenido = educacionServicio.contenido(Validar.entero(leccion, "lección"));
            if (contenido == null) {
                throw new ValidacionException("La lección solicitada no existe.");
            }
            request.setAttribute("contenido", contenido);
            if (contenido.getIdCuestionario() != null) {
                request.setAttribute("cuestionario", educacionServicio.cuestionario(contenido.getIdCuestionario()));
            }
            vista(request, response, "leccion");
            return;
        }

        request.setAttribute("contenidos", educacionServicio.contenidos());
        request.setAttribute("resultados", educacionServicio.resultados(idUsuario));
        vista(request, response, "educacion");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException {
        Cuestionario cuestionario = educacionServicio.cuestionario(
                Validar.entero(request.getParameter("idCuestionario"), "cuestionario"));
        if (cuestionario == null) {
            throw new ValidacionException("El cuestionario no existe.");
        }

        Map<Integer, Integer> respuestas = new HashMap<>();
        for (Pregunta p : cuestionario.getPreguntas()) {
            String valor = request.getParameter("p" + p.getIdPregunta());
            if (valor != null && !valor.isBlank()) {
                respuestas.put(p.getIdPregunta(), Validar.entero(valor, "respuesta"));
            }
        }
        request.setAttribute("respuestas", respuestas);
        request.setAttribute("resultado", educacionServicio.calificar(idUsuario, cuestionario, respuestas));
    }
}
