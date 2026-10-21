package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.LimiteGastoDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.dao.PresupuestoDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.servicio.PresupuestoServicio;

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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PresupuestoServlet", urlPatterns = {"/PresupuestoServlet"})
public class PresupuestoServlet extends HttpServlet {

    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
    private final LimiteGastoDAO limiteGastoDAO = new LimiteGastoDAO();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio(movimientoDAO, limiteGastoDAO);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer idUsuario = obtenerIdUsuarioDeSesion(request, response);
        if (idUsuario == null) {
            return;
        }

        try {
            // Si no se especifica período por parámetro, se usa el mes actual
            String periodoTexto = request.getParameter("periodo"); // formato "yyyy-MM"
            LocalDate periodo = (periodoTexto != null && !periodoTexto.isBlank())
                    ? YearMonth.parse(periodoTexto).atDay(1)
                    : LocalDate.now().withDayOfMonth(1);

            Presupuesto presupuesto = presupuestoDAO.buscarPorUsuarioYPeriodo(idUsuario, periodo);
            List<Categoria> categorias = categoriaDAO.listarTodas();
            List<Categoria> categoriasGasto = categoriaDAO.listarPorTipo("GASTO");

            request.setAttribute("periodo", periodo);
            request.setAttribute("categorias", categorias);
            request.setAttribute("categoriasGasto", categoriasGasto);

            if (presupuesto != null) {
                BigDecimal disponible = presupuestoServicio.calcularDisponible(presupuesto);
                PresupuestoServicio.NivelAlerta alerta = presupuestoServicio.evaluarAlerta(presupuesto);
                List<LimiteGasto> limites = limiteGastoDAO.listarPorPresupuesto(presupuesto.getIdPresupuesto());

                Map<Integer, BigDecimal> gastoPorCategoria = new HashMap<>();
                BigDecimal sumaLimites = BigDecimal.ZERO;
                for (LimiteGasto l : limites) {
                    gastoPorCategoria.put(l.getIdCategoria(),
                            presupuestoServicio.calcularGastadoEnCategoria(presupuesto, l.getIdCategoria()));
                    sumaLimites = sumaLimites.add(l.getMontoLimite());
                }

                request.setAttribute("presupuesto", presupuesto);
                request.setAttribute("disponible", disponible);
                request.setAttribute("alerta", alerta);
                request.setAttribute("limites", limites);
                request.setAttribute("gastoPorCategoria", gastoPorCategoria);
                request.setAttribute("sumaLimites", sumaLimites);
            }

            request.getRequestDispatcher("fragmentos/presupuesto.jsp").forward(request, response);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al cargar el presupuesto.");
            request.getRequestDispatcher("fragmentos/presupuesto.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer idUsuario = obtenerIdUsuarioDeSesion(request, response);
        if (idUsuario == null) {
            return;
        }

        String accion = request.getParameter("accion");

        try {
            if ("guardarPresupuesto".equals(accion)) {
                guardarPresupuesto(request, idUsuario);
            } else if ("guardarLimite".equals(accion)) {
                guardarLimite(request);
            }

            // Volvemos a mostrar el mismo período que se estaba editando
            doGet(request, response);

        } catch (NumberFormatException e) {
            request.setAttribute("error", "Alguno de los montos ingresados no es válido.");
            doGet(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Ocurrió un error al guardar.");
            doGet(request, response);
        }
    }

    private void guardarPresupuesto(HttpServletRequest request, int idUsuario) throws SQLException {
        String periodoTexto = request.getParameter("periodo"); // "yyyy-MM"
        String montoTexto = request.getParameter("montoPresupuesto");

        if (periodoTexto == null || periodoTexto.isBlank() || montoTexto == null || montoTexto.isBlank()) {
            request.setAttribute("error", "Indica el período y el monto del presupuesto.");
            return;
        }

        LocalDate periodo = YearMonth.parse(periodoTexto).atDay(1);
        LocalDate mesActual = LocalDate.now().withDayOfMonth(1);

        if (periodo.isBefore(mesActual)) {
            request.setAttribute("error", "Solo puedes definir el presupuesto del mes actual en adelante.");
            return;
        }

        BigDecimal monto = new BigDecimal(montoTexto);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("error", "El monto del presupuesto debe ser mayor a cero.");
            return;
        }

        // Si ya existe un presupuesto para ese mes, se actualiza con el último valor ingresado.
        Presupuesto existente = presupuestoDAO.buscarPorUsuarioYPeriodo(idUsuario, periodo);

        if (existente != null) {
            existente.setMontoPresupuesto(monto);
            presupuestoDAO.actualizar(existente);
        } else {
            Presupuesto nuevo = new Presupuesto(idUsuario, periodo, monto);
            presupuestoDAO.insertar(nuevo);
        }
    }

    private void guardarLimite(HttpServletRequest request) throws SQLException {
        int idPresupuesto = Integer.parseInt(request.getParameter("idPresupuesto"));
        int idCategoria = Integer.parseInt(request.getParameter("idCategoria"));
        BigDecimal montoLimite = new BigDecimal(request.getParameter("montoLimite"));

        if (montoLimite.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("error", "El límite debe ser mayor a cero.");
            return;
        }

        Presupuesto presupuesto = presupuestoDAO.buscarPorId(idPresupuesto);
        if (presupuesto == null) {
            request.setAttribute("error", "No se encontró el presupuesto indicado.");
            return;
        }

        // La suma de todos los límites por categoría no puede superar el presupuesto mensual.
        List<LimiteGasto> limites = limiteGastoDAO.listarPorPresupuesto(idPresupuesto);
        BigDecimal sumaOtrasCategorias = BigDecimal.ZERO;
        for (LimiteGasto l : limites) {
            if (l.getIdCategoria() != idCategoria) {
                sumaOtrasCategorias = sumaOtrasCategorias.add(l.getMontoLimite());
            }
        }

        BigDecimal totalConNuevoLimite = sumaOtrasCategorias.add(montoLimite);
        if (totalConNuevoLimite.compareTo(presupuesto.getMontoPresupuesto()) > 0) {
            request.setAttribute("error",
                    "La suma de los límites por categoría ya está superando el presupuesto mensual. "
                    + "Reduce este monto o ajusta los límites existentes.");
            return;
        }

        LimiteGasto existente = limiteGastoDAO.buscarPorPresupuestoYCategoria(idPresupuesto, idCategoria);

        if (existente != null) {
            existente.setMontoLimite(montoLimite);
            limiteGastoDAO.actualizar(existente);
        } else {
            LimiteGasto nuevo = new LimiteGasto(idPresupuesto, idCategoria, montoLimite);
            limiteGastoDAO.insertar(nuevo);
        }
    }

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
