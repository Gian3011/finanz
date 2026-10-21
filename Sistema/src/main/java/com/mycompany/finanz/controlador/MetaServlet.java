package com.mycompany.finanz.controlador;

import com.mycompany.finanz.modelo.AporteAhorro;
import com.mycompany.finanz.modelo.MetaAhorro;
import com.mycompany.finanz.servicio.MetaAhorroServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Creación y seguimiento de metas de ahorro y registro de aportes. */
@WebServlet(name = "MetaServlet", urlPatterns = {"/metas"})
public class MetaServlet extends BaseServlet {

    private final MetaAhorroServicio metaAhorroServicio = new MetaAhorroServicio();

    @Override
    protected void mostrar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ServletException, IOException {
        List<MetaAhorro> metas = metaAhorroServicio.listar(idUsuario);
        Map<Integer, List<AporteAhorro>> aportes = new HashMap<>();
        for (MetaAhorro m : metas) {
            aportes.put(m.getIdMeta(), metaAhorroServicio.aportes(m.getIdMeta()));
        }
        request.setAttribute("metas", metas);
        request.setAttribute("aportes", aportes);
        vista(request, response, "metas");
    }

    @Override
    protected void procesar(HttpServletRequest request, HttpServletResponse response, int idUsuario)
            throws SQLException, ValidacionException {
        String accion = request.getParameter("accion");

        if ("crearMeta".equals(accion)) {
            metaAhorroServicio.crear(idUsuario,
                    request.getParameter("nombreMeta"),
                    Validar.monto(request.getParameter("montoObjetivo"), "monto objetivo"),
                    Validar.fecha(request.getParameter("fechaObjetivo"), "fecha objetivo"));
            exito(request, "Meta creada. ¡A ahorrar!");

        } else if ("aportar".equals(accion)) {
            boolean cumplida = metaAhorroServicio.registrarAporte(idUsuario,
                    Validar.entero(request.getParameter("idMeta"), "meta"),
                    Validar.monto(request.getParameter("montoAporte"), "monto del aporte"),
                    Validar.fecha(request.getParameter("fechaAporte"), "fecha del aporte"));
            exito(request, cumplida ? "¡Felicitaciones! Alcanzaste tu meta de ahorro." : "Aporte registrado.");

        } else if ("eliminarMeta".equals(accion)) {
            if (metaAhorroServicio.eliminar(idUsuario, Validar.entero(request.getParameter("idMeta"), "meta"))) {
                exito(request, "Meta eliminada.");
            }
        }
    }
}
