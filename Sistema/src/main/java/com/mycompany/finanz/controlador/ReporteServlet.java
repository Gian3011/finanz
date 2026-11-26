package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.UsuarioDAO;
import com.mycompany.finanz.modelo.FormatoReporte;
import com.mycompany.finanz.modelo.Usuario;
import com.mycompany.finanz.servicio.CorreoServicio;
import com.mycompany.finanz.servicio.ReporteServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;
import com.mycompany.finanz.util.Html;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * /reportes            -> sección para elegir el mes y el formato, historial de reportes y de envíos
 * /reportes/descargar  -> descarga el resumen financiero del mes (?mes=AAAA-MM&formato=pdf|xlsx|docx)
 * /reportes/pdf        -> atajo anterior, equivale a formato=pdf
 * /reportes/enviar     -> POST: envía el resumen por correo y responde un fragmento HTML con el resultado
 * /reportes/verificar  -> POST: verifica por POP3 si un envío rebotó y vuelve a mostrar la sección
 */
@WebServlet(name = "ReporteServlet",
        urlPatterns = {"/reportes", "/reportes/descargar", "/reportes/pdf", "/reportes/enviar", "/reportes/verificar"})
public class ReporteServlet extends BaseServlet {

    private static final Logger LOG = Logger.getLogger(ReporteServlet.class.getName());

    private final ReporteServicio reporteServicio = new ReporteServicio();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {

        String ruta = request.getServletPath();
        if (ruta.equals("/reportes/enviar")) {
            throw new ValidacionException("Para enviar un reporte usa el botón \"Enviar por correo\".");
        }
        if (ruta.equals("/reportes/descargar") || ruta.equals("/reportes/pdf")) {
            YearMonth mes = mesValido(request.getParameter("mes"));
            FormatoReporte formato = ruta.endsWith("/pdf")
                    ? FormatoReporte.PDF
                    : formatoValido(request.getParameter("formato"));

            byte[] archivo = reporteServicio.generar(usuarioDAO.buscarPorId(idUsuario), mes, formato);
            response.setContentType(formato.getTipoContenido());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"FinanZ-resumen-" + mes + "." + formato.getExtension() + "\"");
            response.setContentLength(archivo.length);
            response.getOutputStream().write(archivo);
            return;
        }

        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        request.setAttribute("historial", reporteServicio.historial(idUsuario));
        request.setAttribute("envios", reporteServicio.historialEnvios(idUsuario));
        request.setAttribute("correoCuenta", usuario == null ? "" : usuario.getEmail());
        vista(request, response, "reportes");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {

        String ruta = request.getServletPath();
        if (ruta.equals("/reportes/enviar")) {
            enviarPorCorreo(request, response, idUsuario);
        } else if (ruta.equals("/reportes/verificar")) {
            verificarEntrega(request, idUsuario);
        }
    }

    /** Envía el reporte y responde solo el mensaje (el modal lo muestra sin recargar la sección). */
    private void enviarPorCorreo(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws IOException {
        try {
            YearMonth mes = mesValido(request.getParameter("mes"));
            FormatoReporte formato = formatoValido(request.getParameter("formato"));
            String destino = Validar.email(request.getParameter("correoDestino"));

            reporteServicio.enviarPorCorreo(usuarioDAO.buscarPorId(idUsuario), mes, formato, destino);
            fragmento(response, HttpServletResponse.SC_OK, "mensaje mensaje-exito", "Reporte enviado a " + destino);
        } catch (ValidacionException e) {
            fragmento(response, HttpServletResponse.SC_BAD_REQUEST, "mensaje-error", e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error de base de datos al enviar un reporte por correo", e);
            fragmento(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "mensaje-error",
                    "No se pudo registrar el envío. Intenta nuevamente en unos segundos.");
        }
    }

    private void verificarEntrega(HttpServletRequest request, int idUsuario) throws SQLException, ValidacionException {
        int idEnvio = Validar.entero(request.getParameter("idEnvio"), "envío");
        CorreoServicio.EstadoEntrega estado = reporteServicio.verificarEnvio(idUsuario, idEnvio);
        String destino = reporteServicio.buscarEnvio(idUsuario, idEnvio).getCorreoDestino();
        switch (estado) {
            case REBOTADO:
                avisos(request, List.of("El correo enviado a " + destino + " rebotó: el servidor del destinatario "
                        + "no lo aceptó. Revisa la dirección y vuelve a enviarlo."));
                break;
            case ENTREGADO_SIN_REBOTE:
                exito(request, "Sin rebotes: el correo enviado a " + destino + " fue aceptado por el servidor del destinatario.");
                break;
            default:
                avisos(request, List.of("No se pudo verificar la entrega en este momento. Intenta nuevamente más tarde."));
        }
    }

    private static YearMonth mesValido(String valor) throws ValidacionException {
        YearMonth mes = Validar.mes(valor);
        if (mes.isAfter(YearMonth.now())) {
            throw new ValidacionException("No se puede generar el reporte de un mes futuro.");
        }
        return mes;
    }

    private static FormatoReporte formatoValido(String valor) throws ValidacionException {
        FormatoReporte formato = FormatoReporte.desde(valor);
        if (formato == null) {
            throw new ValidacionException("Elige un formato válido: PDF, Excel o Word.");
        }
        return formato;
    }

    private static void fragmento(HttpServletResponse response, int estado, String clase, String mensaje)
            throws IOException {
        response.setStatus(estado);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<p class=\"" + clase + "\" role=\"" + (estado == 200 ? "status" : "alert") + "\">"
                + Html.esc(mensaje) + "</p>");
        response.flushBuffer(); // la respuesta queda enviada: BaseServlet no vuelve a mostrar la sección
    }
}
