package com.mycompany.finanz.controlador;

import com.mycompany.finanz.servicio.MovimientoServicio;
import com.mycompany.finanz.servicio.ValidacionException;
import com.mycompany.finanz.servicio.Validar;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Registro, clasificación, consulta y filtrado de ingresos y gastos. */
@WebServlet(name = "MovimientoServlet", urlPatterns = {"/transacciones"})
public class MovimientoServlet extends BaseServlet {

    private final MovimientoServicio movimientoServicio = new MovimientoServicio();

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
            throws SQLException, ValidacionException {
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
}
