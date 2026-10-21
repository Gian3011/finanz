package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Ingreso;
import com.mycompany.finanz.modelo.Movimiento;
import com.mycompany.finanz.servicio.GastoHormigaServicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "MovimientoServlet", urlPatterns = {"/MovimientoServlet"})
public class MovimientoServlet extends HttpServlet {

    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final GastoHormigaServicio gastoHormigaServicio = new GastoHormigaServicio(movimientoDAO);

    /**
     * Lista los movimientos del usuario logueado y los categorías (para el combo del formulario).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer idUsuario = obtenerIdUsuarioDeSesion(request, response);
        if (idUsuario == null) {
            return; // obtenerIdUsuarioDeSesion ya redirigió al login
        }

        try {
            List<Movimiento> movimientos = movimientoDAO.listarPorUsuario(idUsuario);
            List<Categoria> categorias = categoriaDAO.listarTodas();
            List<Categoria> categoriasGasto = categoriaDAO.listarPorTipo("GASTO");
            List<Categoria> categoriasIngreso = categoriaDAO.listarPorTipo("INGRESO");

            request.setAttribute("movimientos", movimientos);
            request.setAttribute("categorias", categorias);
            request.setAttribute("categoriasGasto", categoriasGasto);
            request.setAttribute("categoriasIngreso", categoriasIngreso);
            request.getRequestDispatcher("fragmentos/transacciones.jsp").forward(request, response);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al cargar los movimientos.");
            request.getRequestDispatcher("fragmentos/transacciones.jsp").forward(request, response);
        }
    }

    /**
     * Registra un nuevo Ingreso o Gasto, evaluando gasto hormiga cuando corresponde.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer idUsuario = obtenerIdUsuarioDeSesion(request, response);
        if (idUsuario == null) {
            return;
        }

        String accion = request.getParameter("accion");
        if ("agregarCategoria".equals(accion)) {
            agregarCategoria(request, response);
            return;
        }

        try {
            String descripcion = request.getParameter("descripcion");
            String montoTexto = request.getParameter("monto");
            String tipo = request.getParameter("tipo");             // "INGRESO" o "GASTO"
            String idCategoriaTexto = request.getParameter("idCategoria");
            String fechaTexto = request.getParameter("fecha");      // formato yyyy-MM-dd (input type="date")

            // Validaciones básicas antes de tocar la base de datos
            if (descripcion == null || descripcion.isBlank()
                    || montoTexto == null || montoTexto.isBlank()
                    || tipo == null
                    || idCategoriaTexto == null || idCategoriaTexto.isBlank()
                    || fechaTexto == null || fechaTexto.isBlank()) {
                request.setAttribute("error", "Todos los campos son obligatorios.");
                doGet(request, response);
                return;
            }

            BigDecimal monto = new BigDecimal(montoTexto);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                request.setAttribute("error", "El monto debe ser mayor a cero.");
                doGet(request, response);
                return;
            }

            int idCategoria = Integer.parseInt(idCategoriaTexto);
            LocalDate fecha = LocalDate.parse(fechaTexto);

            Movimiento movimiento;
            if ("INGRESO".equalsIgnoreCase(tipo)) {
                movimiento = new Ingreso(idUsuario, idCategoria, monto, fecha, descripcion);
            } else {
                Gasto gasto = new Gasto(idUsuario, idCategoria, monto, fecha, descripcion);
                boolean esHormiga = gastoHormigaServicio.evaluarGastoHormiga(gasto);
                gasto.setEsGastoHormiga(esHormiga);
                movimiento = gasto;
            }

            movimientoDAO.insertar(movimiento);

            doGet(request, response);

        } catch (NumberFormatException e) {
            request.setAttribute("error", "El monto ingresado no es válido.");
            doGet(request, response);
        } catch (java.time.format.DateTimeParseException e) {
            request.setAttribute("error", "La fecha ingresada no es válida.");
            doGet(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al guardar el movimiento.");
            doGet(request, response);
        }
    }

    /**
     * Crea una nueva categoría (de tipo GASTO o INGRESO) y vuelve a mostrar
     * la pantalla de transacciones con el combo ya actualizado.
     */
    private void agregarCategoria(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String nombreCategoria = request.getParameter("nombreCategoria");
            String tipoCategoria = request.getParameter("tipoCategoria");

            if (nombreCategoria == null || nombreCategoria.isBlank()
                    || tipoCategoria == null
                    || !("GASTO".equalsIgnoreCase(tipoCategoria) || "INGRESO".equalsIgnoreCase(tipoCategoria))) {
                request.setAttribute("error", "Indica un nombre y un tipo válido (Gasto o Ingreso) para la categoría.");
                doGet(request, response);
                return;
            }

            categoriaDAO.insertar(new Categoria(nombreCategoria.trim(), tipoCategoria.toUpperCase()));

            doGet(request, response);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al crear la categoría.");
            doGet(request, response);
        }
    }

    /**
     * Devuelve el id del usuario logueado desde la sesión, o redirige al login
     * y devuelve null si no hay sesión activa.
     */
    private Integer obtenerIdUsuarioDeSesion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect("Login.jsp");
            return null;
        }
        return (Integer) session.getAttribute("idUsuario");
    }
}