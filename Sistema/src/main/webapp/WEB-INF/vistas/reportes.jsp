<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.ReporteFinanciero"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.time.YearMonth"%>
<%@page import="java.util.List"%>
<%
    List<ReporteFinanciero> historial = (List<ReporteFinanciero>) request.getAttribute("historial");
    YearMonth actual = YearMonth.now();
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Reportes</h2>
    <p class="mt-1 muted">Exporta el resumen financiero de un mes en formato PDF.</p>
</div>

<div class="grid gap-6 lg:grid-cols-2">
    <article class="surface rounded-2xl p-6">
        <h3 class="text-lg font-bold">Resumen financiero en PDF</h3>
        <p class="mt-1 text-sm muted">Incluye ingresos, gastos, balance, presupuesto, gastos por categoría, comparación con el mes
            anterior, metas, recomendaciones y el detalle de tus movimientos.</p>

        <!-- Descarga directa (no AJAX): el navegador guarda el archivo -->
        <form action="reportes/pdf" method="get" class="mt-5 flex flex-wrap items-end gap-3" data-nativo>
            <label class="text-xs font-semibold muted">Mes
                <input type="month" name="mes" value="<%= actual %>" max="<%= actual %>" required class="campo mt-1">
            </label>
            <button type="submit" class="btn btn-primario"><i data-lucide="file-down"></i>Descargar PDF</button>
        </form>
    </article>

    <article class="surface rounded-2xl p-6">
        <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold">Reportes generados</h3>
            <a href="#reportes" data-navegar="reportes" class="text-sm font-semibold" style="color:var(--celeste)">Actualizar</a>
        </div>
        <% if (historial.isEmpty()) { %>
            <p class="mt-4 text-sm muted">Aún no generaste reportes.</p>
        <% } else { %>
        <ul class="mt-4 space-y-2">
            <% for (ReporteFinanciero r : historial) {
                YearMonth mes = YearMonth.parse(r.getPeriodo()); %>
            <li class="flex items-center justify-between rounded-xl px-3 py-2" style="background:var(--paper)">
                <span class="text-sm"><strong><%= Formato.mes(mes) %></strong> <span class="muted">· <%= Html.esc(r.getFormato()) %> · <%= Formato.fechaHora(r.getFechaGeneracion()) %></span></span>
                <a class="btn btn-icono" href="reportes/pdf?mes=<%= mes %>" aria-label="Descargar de nuevo"><i data-lucide="download"></i></a>
            </li>
            <% } %>
        </ul>
        <% } %>
    </article>
</div>
