package com.mycompany.finanz.controlador;

import com.mycompany.finanz.modelo.ResultadoImportacion;
import com.mycompany.finanz.servicio.ImportacionServicio;
import com.mycompany.finanz.servicio.MovimientoServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;
import com.mycompany.finanz.util.LectorPdf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registro, clasificación, consulta y filtrado de ingresos y gastos.
 *
 * /transacciones/importar (POST, multipart):
 *  - accion=analizar  -> lee el PDF subido y muestra la vista previa de los gastos encontrados
 *  - accion=confirmar -> registra las filas que el usuario dejó marcadas
 */
@WebServlet(name = "MovimientoServlet", urlPatterns = {"/transacciones", "/transacciones/importar"})
@MultipartConfig(maxFileSize = LectorPdf.MAX_BYTES, maxRequestSize = LectorPdf.MAX_BYTES + 1024 * 1024)
public class MovimientoServlet extends BaseServlet {

    private final MovimientoServicio movimientoServicio = new MovimientoServicio();
    private final ImportacionServicio importacionServicio = new ImportacionServicio(movimientoServicio);

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        request.setAttribute("movimientos", movimientoServicio.listar(idUsuario));
        request.setAttribute("categorias", movimientoServicio.categorias(idUsuario));
        request.setAttribute("categoriasGasto", movimientoServicio.categoriasPorTipo(idUsuario, "GASTO"));
        request.setAttribute("categoriasIngreso", movimientoServicio.categoriasPorTipo(idUsuario, "INGRESO"));
        vista(request, response, "transacciones");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
        if ("/transacciones/importar".equals(request.getServletPath())) {
            String accionImportar;
            try {
                accionImportar = request.getParameter("accion");
            } catch (IllegalStateException e) { // la petición supera maxRequestSize
                throw new ValidacionException("El PDF supera el tamaño máximo de 5 MB.");
            }
            if ("confirmar".equals(accionImportar)) {
                confirmarImportacion(request, idUsuario);
            } else {
                analizarPdf(request, response, idUsuario);
            }
            return;
        }

        String accion = request.getParameter("accion");

        if ("agregarCategoria".equals(accion)) {
            movimientoServicio.crearCategoria(idUsuario, request.getParameter("nombreCategoria"),
                    request.getParameter("tipoCategoria"));
            exito(request, "Categoría creada correctamente.");

        } else if ("eliminar".equals(accion)) {
            if (movimientoServicio.eliminar(idUsuario, Validar.entero(request.getParameter("idMovimiento"), "movimiento"))) {
                exito(request, "Movimiento eliminado.");
            }

        } else if ("hormiga".equals(accion)) {
            boolean marcar = "true".equals(request.getParameter("valor"));
            movimientoServicio.clasificarHormiga(idUsuario,
                    Validar.entero(request.getParameter("idMovimiento"), "movimiento"), marcar);
            exito(request, marcar ? "Gasto marcado como gasto hormiga." : "Se quitó la marca de gasto hormiga.");

        } else {
            String tipo = request.getParameter("tipo");
            List<String> avisos = movimientoServicio.registrar(idUsuario,
                    tipo,
                    Validar.entero(request.getParameter("idCategoria"), "categoría"),
                    Validar.monto(request.getParameter("monto"), "monto"),
                    Validar.fecha(request.getParameter("fecha"), "fecha"),
                    Validar.texto(request.getParameter("descripcion"), "descripción", 150),
                    Validar.textoOpcional(request.getParameter("fuente"), "fuente", 100));
            exito(request, "INGRESO".equals(tipo) ? "Ingreso registrado." : "Gasto registrado.");
            avisos(request, avisos);
        }
    }

    /** Lee el PDF en memoria (no se guarda) y muestra la vista previa para revisar. */
    private void analizarPdf(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
        Part archivo;
        try {
            archivo = request.getPart("archivo");
        } catch (IllegalStateException e) { // supera maxFileSize
            throw new ValidacionException("El PDF supera el tamaño máximo de 5 MB.");
        }
        if (archivo == null || archivo.getSize() == 0) {
            throw new ValidacionException("Selecciona un archivo PDF.");
        }
        String nombre = archivo.getSubmittedFileName() == null ? "documento.pdf"
                : Paths.get(archivo.getSubmittedFileName()).getFileName().toString();
        if (!nombre.toLowerCase().endsWith(".pdf")) {
            throw new ValidacionException("Solo se pueden importar archivos PDF.");
        }
        byte[] contenido;
        try (InputStream entrada = archivo.getInputStream()) {
            contenido = entrada.readAllBytes();
        }
        // La clave del PDF solo se usa para abrirlo; no se guarda ni se registra en los logs
        ResultadoImportacion resultado = importacionServicio.analizar(idUsuario, contenido,
                request.getParameter("clavePdf"), nombre);

        request.setAttribute("importacion", resultado);
        request.setAttribute("categoriasGasto", movimientoServicio.categoriasPorTipo(idUsuario, "GASTO"));
        vista(request, response, "importacion"); // la respuesta queda enviada
    }

    /** Registra las filas marcadas. Cada fila se valida por separado: una con error no detiene a las demás. */
    private void confirmarImportacion(HttpServletRequest request, int idUsuario) throws SQLException, ValidacionException {
        String[] filas = request.getParameterValues("fila");
        if (filas == null || filas.length == 0) {
            throw new ValidacionException("No marcaste ningún gasto para registrar.");
        }
        if (filas.length > ImportacionServicio.MAX_MOVIMIENTOS) {
            throw new ValidacionException("Se pueden registrar como máximo " + ImportacionServicio.MAX_MOVIMIENTOS + " gastos a la vez.");
        }
        int registrados = 0;
        List<String> errores = new ArrayList<>();
        Set<String> avisos = new LinkedHashSet<>();
        for (String fila : filas) {
            int i = Validar.entero(fila, "fila");
            try {
                avisos.addAll(movimientoServicio.registrar(idUsuario, "GASTO",
                        Validar.entero(request.getParameter("idCategoria_" + i), "categoría"),
                        Validar.monto(request.getParameter("monto_" + i), "monto"),
                        Validar.fecha(request.getParameter("fecha_" + i), "fecha"),
                        Validar.texto(request.getParameter("descripcion_" + i), "descripción", 150),
                        null));
                registrados++;
            } catch (ValidacionException e) {
                errores.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (registrados == 0) {
            throw new ValidacionException("No se registró ningún gasto. " + String.join(" ", errores));
        }
        exito(request, registrados == 1 ? "Se registró 1 gasto importado." : "Se registraron " + registrados + " gastos importados.");
        List<String> mensajes = new ArrayList<>();
        if (!errores.isEmpty()) {
            mensajes.add("No se registraron " + errores.size() + " fila(s). " + String.join(" ", errores));
        }
        mensajes.addAll(avisos);
        avisos(request, mensajes);
    }
}
