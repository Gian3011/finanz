package com.mycompany.finanz.controlador;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.modelo.LimiteGasto;
import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.servicio.PresupuestoServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Presupuesto mensual, límites por categoría y alertas. */
@WebServlet(name = "PresupuestoServlet", urlPatterns = {"/presupuestos"})
public class PresupuestoServlet extends BaseServlet {

    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException, ServletException, IOException {
        YearMonth mes = Validar.mes(request.getParameter("periodo"));
        Presupuesto presupuesto = presupuestoServicio.buscar(idUsuario, mes);

        request.setAttribute("mes", mes);
        request.setAttribute("categoriasGasto", categoriaDAO.listarPorTipo(idUsuario, "GASTO"));

        if (presupuesto != null) {
            List<LimiteGasto> limites = presupuestoServicio.listarLimites(presupuesto);
            Map<Integer, BigDecimal> gastoPorCategoria = new HashMap<>();
            BigDecimal sumaLimites = BigDecimal.ZERO;
            for (LimiteGasto l : limites) {
                gastoPorCategoria.put(l.getIdCategoria(),
                        presupuestoServicio.calcularGastadoEnCategoria(presupuesto, l.getIdCategoria()));
                sumaLimites = sumaLimites.add(l.getMontoLimite());
            }

            request.setAttribute("presupuesto", presupuesto);
            request.setAttribute("gastado", presupuestoServicio.calcularGastado(presupuesto));
            request.setAttribute("disponible", presupuestoServicio.calcularDisponible(presupuesto));
            request.setAttribute("alerta", presupuestoServicio.evaluarAlerta(presupuesto));
            request.setAttribute("limites", limites);
            request.setAttribute("gastoPorCategoria", gastoPorCategoria);
            request.setAttribute("sumaLimites", sumaLimites);
        }
        vista(request, response, "presupuesto");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException {
        String accion = request.getParameter("accion");

        if ("guardarPresupuesto".equals(accion)) {
            YearMonth mes = Validar.mes(request.getParameter("periodo"));
            presupuestoServicio.guardarPresupuesto(idUsuario, mes,
                    Validar.monto(request.getParameter("montoPresupuesto"), "monto del presupuesto"));
            exito(request, "Presupuesto guardado.");
            avisos(request, presupuestoServicio.verificarAlertas(idUsuario, mes.atDay(1)));

        } else if ("guardarLimite".equals(accion)) {
            presupuestoServicio.guardarLimite(idUsuario,
                    Validar.entero(request.getParameter("idPresupuesto"), "presupuesto"),
                    Validar.entero(request.getParameter("idCategoria"), "categoría"),
                    Validar.monto(request.getParameter("montoLimite"), "límite"));
            exito(request, "Límite por categoría guardado.");
            avisos(request, presupuestoServicio.verificarAlertas(idUsuario,
                    Validar.mes(request.getParameter("periodo")).atDay(1)));

        } else if ("eliminarLimite".equals(accion)) {
            if (presupuestoServicio.eliminarLimite(idUsuario, Validar.entero(request.getParameter("idLimite"), "límite"))) {
                exito(request, "Límite eliminado.");
            }
        }
    }
}
