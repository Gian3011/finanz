package com.mycompany.finanz.controlador;

import com.mycompany.finanz.modelo.Presupuesto;
import com.mycompany.finanz.servicio.EstadisticaServicio;
import com.mycompany.finanz.servicio.MetaAhorroServicio;
import com.mycompany.finanz.servicio.PresupuestoServicio;
import com.mycompany.finanz.servicio.RecomendacionServicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Panel principal: indicadores del mes, gráficos, presupuesto, metas y recomendaciones. */
@WebServlet(name = "ResumenServlet", urlPatterns = {"/resumen"})
public class ResumenServlet extends BaseServlet {

    private final EstadisticaServicio estadisticaServicio = new EstadisticaServicio();
    private final PresupuestoServicio presupuestoServicio = new PresupuestoServicio();
    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();
    private final RecomendacionServicio recomendacionServicio = new RecomendacionServicio();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        YearMonth mes = YearMonth.now();

        // Genera las alertas pendientes del mes (por ejemplo, tras importar datos)
        presupuestoServicio.verificarAlertas(idUsuario, LocalDate.now());

        request.setAttribute("resumen", estadisticaServicio.resumenMes(idUsuario, mes));
        request.setAttribute("resumenAnterior", estadisticaServicio.resumenMes(idUsuario, mes.minusMonths(1)));
        request.setAttribute("ultimosMeses", estadisticaServicio.ultimosMeses(idUsuario, 6));

        Presupuesto presupuesto = presupuestoServicio.buscar(idUsuario, mes);
        if (presupuesto != null) {
            request.setAttribute("presupuesto", presupuesto);
            request.setAttribute("gastadoPresupuesto", presupuestoServicio.calcularGastado(presupuesto));
            request.setAttribute("nivelPresupuesto", presupuestoServicio.evaluarAlerta(presupuesto));
        }
        request.setAttribute("metas", metaAhorroServicio.listar(idUsuario));
        request.setAttribute("recomendaciones", recomendacionServicio.generar(idUsuario));
        vista(request, response, "resumen");
    }
}
