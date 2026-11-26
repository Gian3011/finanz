<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.util.List"%>
<%
    ResultadoImportacion importacion = (ResultadoImportacion) request.getAttribute("importacion");
    List<Categoria> categoriasGasto = (List<Categoria>) request.getAttribute("categoriasGasto");
    List<MovimientoDetectado> filas = importacion.getMovimientos();
    LocalDate hoy = LocalDate.now();
    LocalDate fechaMinima = com.mycompany.finanz.servicio.MovimientoServicio.fechaMinimaPermitida(hoy);
    int marcadas = 0;
    for (MovimientoDetectado f : filas) { if (f.isSeleccionado()) marcadas++; }
%>
<%@include file="_mensajes.jspf"%>

<section class="surface rounded-2xl p-5">
    <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
            <h2 class="text-lg font-bold">Revisa los gastos encontrados</h2>
            <p class="mt-1 text-sm muted">
                <span class="font-semibold"><%= Html.esc(importacion.getNombreArchivo()) %></span> ·
                <%= importacion.getTipo().getNombre() %> ·
                <%= filas.size() + (filas.size() == 1 ? " gasto encontrado" : " gastos encontrados") %>
                <% if (importacion.getOmitidos() > 0) { %>
                · <%= importacion.getOmitidos() + (importacion.getOmitidos() == 1 ? " pago o abono omitido" : " pagos o abonos omitidos") %>
                <% } %>
            </p>
            <p class="mt-1 text-xs muted">Corrige lo que necesites y desmarca lo que no quieras registrar. Nada se guarda hasta que confirmes.</p>
        </div>
        <button type="button" class="btn btn-suave" data-navegar="transacciones">Cancelar</button>
    </div>

    <form action="transacciones/importar" method="post" class="mt-5" id="form-importacion">
        <input type="hidden" name="accion" value="confirmar">

        <div class="mb-2 flex items-center gap-2 px-3 text-xs font-semibold muted sm:grid sm:grid-cols-12">
            <label class="flex items-center gap-2 sm:col-span-1">
                <input type="checkbox" data-seleccionar-todos <%= marcadas == filas.size() ? "checked" : "" %> aria-label="Seleccionar todos">
                <span class="sm:hidden">Seleccionar todos</span>
            </label>
            <span class="hidden sm:col-span-2 sm:block">Fecha</span>
            <span class="hidden sm:col-span-4 sm:block">Descripción</span>
            <span class="hidden sm:col-span-3 sm:block">Categoría</span>
            <span class="hidden text-right sm:col-span-2 sm:block">Importe (S/)</span>
        </div>

        <ul class="space-y-2">
            <% for (int i = 0; i < filas.size(); i++) {
                MovimientoDetectado f = filas.get(i);
            %>
            <li class="fila-importacion rounded-xl border p-3" style="border-color:var(--line)">
                <div class="grid gap-2 sm:grid-cols-12 sm:items-center">
                    <label class="flex items-center gap-2 sm:col-span-1">
                        <input type="checkbox" name="fila" value="<%= i %>" <%= f.isSeleccionado() ? "checked" : "" %> aria-label="Registrar fila <%= i + 1 %>">
                        <span class="text-sm font-semibold sm:hidden">Registrar</span>
                    </label>
                    <input type="date" name="fecha_<%= i %>" value="<%= f.getFecha() == null ? "" : f.getFecha() %>"
                           min="<%= fechaMinima %>" max="<%= hoy %>" class="campo sm:col-span-2" aria-label="Fecha">
                    <input type="text" name="descripcion_<%= i %>" value="<%= Html.esc(f.getDescripcion()) %>" maxlength="150"
                           class="campo sm:col-span-4" aria-label="Descripción">
                    <select name="idCategoria_<%= i %>" class="campo sm:col-span-3" aria-label="Categoría">
                        <% for (Categoria c : categoriasGasto) { %>
                        <option value="<%= c.getIdCategoria() %>" <%= c.getIdCategoria() == f.getIdCategoria() ? "selected" : "" %>><%= Html.esc(c.getNombreCategoria()) %></option>
                        <% } %>
                    </select>
                    <input type="number" name="monto_<%= i %>" value="<%= f.getMonto().setScale(2).toPlainString() %>"
                           min="0.01" max="99999999.99" step="0.01" inputmode="decimal" class="campo text-right sm:col-span-2"
                           aria-label="Importe en soles" data-monto-importado>
                </div>
                <% if (f.getObservacion() != null) { %>
                <p class="mt-2 text-xs"><span class="chip chip-alerta">Revisar</span> <%= Html.esc(f.getObservacion()) %></p>
                <% } %>
            </li>
            <% } %>
        </ul>

        <div class="mt-5 flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm font-semibold" id="resumen-importacion" aria-live="polite"></p>
            <button type="submit" class="btn btn-primario"><i data-lucide="check"></i>Registrar seleccionados</button>
        </div>
    </form>
</section>
