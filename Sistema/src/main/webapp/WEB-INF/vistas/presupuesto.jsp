<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="com.mycompany.finanz.modelo.*" %>
<%@ page import="com.mycompany.finanz.servicio.PresupuestoServicio" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.math.RoundingMode" %>

<div class="mb-6">
    
    <p class="mt-1" style="color:var(--muted)">Controla cuánto llevas gastado cada mes.</p>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p class="mensaje-error mb-4"><%= request.getAttribute("error") %></p>
<% } %>

<%
    LocalDate periodo = (LocalDate) request.getAttribute("periodo");
    Presupuesto presupuesto = (Presupuesto) request.getAttribute("presupuesto");
    String mesActualTexto = LocalDate.now().withDayOfMonth(1).toString().substring(0, 7);
%>

<div class="mb-6 flex flex-wrap gap-3">
    <button type="button" id="btn-presupuesto-mensual"
            class="rounded-xl px-4 py-3 text-sm font-bold" style="background:#38b6ff;color:white">
        + Presupuesto mensual
    </button>

    <button type="button" id="btn-limite-categoria" <% if (presupuesto == null) { %>disabled<% } %>
            class="rounded-xl px-4 py-3 text-sm font-bold"
            style="<% if (presupuesto == null) { %>background:var(--line);color:var(--muted);cursor:not-allowed<% } else { %>background:var(--forest);color:white<% } %>"
            title="<% if (presupuesto == null) { %>Primero define el presupuesto mensual<% } %>">
        + Categoría
    </button>
</div>

<%
    if (presupuesto != null) {
        PresupuestoServicio.NivelAlerta alerta = (PresupuestoServicio.NivelAlerta) request.getAttribute("alerta");
        BigDecimal disponible = (BigDecimal) request.getAttribute("disponible");
        BigDecimal total = presupuesto.getMontoPresupuesto();
        BigDecimal gastado = total.subtract(disponible);

        int porcentaje = 0;
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            porcentaje = gastado.multiply(new BigDecimal(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
        }
        if (porcentaje > 100) porcentaje = 100;
        if (porcentaje < 0) porcentaje = 0;

        String colorBarra = "var(--forest)";
        String bgBadge = "var(--mint)";
        String colorBadge = "var(--forest)";
        if (alerta == PresupuestoServicio.NivelAlerta.CERCA) {
            colorBarra = "var(--sun)"; bgBadge = "#fff2d2"; colorBadge = "#a36a00";
        } else if (alerta == PresupuestoServicio.NivelAlerta.SUPERADO) {
            colorBarra = "var(--coral)"; bgBadge = "#ffe2dc"; colorBadge = "#b7432d";
        }
%>

    <article class="surface mb-6 rounded-2xl p-6">
        <div class="flex items-start justify-between">
            <div>
                <p class="text-sm font-medium" style="color:var(--muted)">Presupuesto de <%= periodo %></p>
                <h3 class="mt-1 text-2xl font-bold">S/ <%= total %></h3>
            </div>
            <span class="rounded-lg px-3 py-2 text-xs font-bold" style="background:<%= bgBadge %>;color:<%= colorBadge %>">
                <%= porcentaje %>%
            </span>
        </div>

        <div class="mt-6">
            <div class="mb-2 flex justify-between text-sm">
                <span>Gastado: <strong>S/ <%= gastado %></strong></span>
                <span style="color:var(--muted)">Disponible: S/ <%= disponible %></span>
            </div>
            <div class="h-4 overflow-hidden rounded-full" style="background:var(--line)">
                <div class="h-full rounded-full" style="width:<%= porcentaje %>%;background:<%= colorBarra %>"></div>
            </div>
        </div>

        <p class="mt-3 text-xs" style="color:var(--muted)">
            Estado: <strong style="color:<%= colorBarra %>"><%= alerta %></strong>
        </p>
    </article>

    <article class="surface rounded-2xl p-6">
        <h3 class="mb-1 text-lg font-bold">Límites por categoría</h3>
        <p class="mb-5 text-sm" style="color:var(--muted)">Define cuánto quieres gastar como máximo en cada categoría.</p>

        <%
            List<LimiteGasto> limites = (List<LimiteGasto>) request.getAttribute("limites");
            List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
            Map<Integer, BigDecimal> gastoPorCategoria = (Map<Integer, BigDecimal>) request.getAttribute("gastoPorCategoria");
        %>

        <% if (limites == null || limites.isEmpty()) { %>
            <p class="py-6 text-center text-sm" style="color:var(--muted)">Todavía no definiste límites por categoría.</p>
        <% } else { %>
            <div class="grid gap-5 md:grid-cols-2">
                <%
                    for (LimiteGasto l : limites) {
                        String nombreCategoria = "Categoría " + l.getIdCategoria();
                        if (categorias != null) {
                            for (Categoria c : categorias) {
                                if (c.getIdCategoria() == l.getIdCategoria()) {
                                    nombreCategoria = c.getNombreCategoria();
                                }
                            }
                        }

                        BigDecimal gastadoCategoria = (gastoPorCategoria != null && gastoPorCategoria.get(l.getIdCategoria()) != null)
                                ? gastoPorCategoria.get(l.getIdCategoria()) : BigDecimal.ZERO;
                        BigDecimal limiteCategoria = l.getMontoLimite();

                        int porcentajeCategoria = 0;
                        if (limiteCategoria.compareTo(BigDecimal.ZERO) > 0) {
                            porcentajeCategoria = gastadoCategoria.multiply(new BigDecimal(100))
                                    .divide(limiteCategoria, 0, RoundingMode.HALF_UP).intValue();
                        }
                        boolean superadoCategoria = porcentajeCategoria > 100;
                        if (porcentajeCategoria > 100) porcentajeCategoria = 100;
                        if (porcentajeCategoria < 0) porcentajeCategoria = 0;

                        String colorCategoria = "var(--forest)";
                        if (superadoCategoria) {
                            colorCategoria = "var(--coral)";
                        } else if (porcentajeCategoria >= 80) {
                            colorCategoria = "var(--sun)";
                        }
                %>
                        <article class="rounded-2xl border p-5" style="border-color:var(--line)">
                            <div class="flex items-start justify-between">
                                <h4 class="font-bold"><%= nombreCategoria %></h4>
                                <span class="text-xs font-bold" style="color:<%= colorCategoria %>"><%= porcentajeCategoria %>%</span>
                            </div>
                            <p class="mt-1 text-sm" style="color:var(--muted)">
                                S/ <%= gastadoCategoria %> de S/ <%= limiteCategoria %>
                            </p>
                            <div class="mt-3 h-3 overflow-hidden rounded-full" style="background:var(--line)">
                                <div class="h-full rounded-full" style="width:<%= porcentajeCategoria %>%;background:<%= colorCategoria %>"></div>
                            </div>
                            <% if (superadoCategoria) { %>
                                <p class="mt-2 text-xs font-semibold" style="color:var(--coral)">Superaste el límite de esta categoría.</p>
                            <% } %>
                        </article>
                <%
                    }
                %>
            </div>
        <% } %>
    </article>

<% } else { %>
    <p class="py-10 text-center text-sm" style="color:var(--muted)">Todavía no has definido un presupuesto para este período.</p>
<% } %>

<!-- Modal: presupuesto mensual -->
<div id="modal-presupuesto-mensual" class="hidden fixed inset-0 z-50 flex items-center justify-center p-4" style="background:rgba(0,0,0,.45)">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Presupuesto mensual</h3>
            <button type="button" class="btn-cerrar-modal text-xl leading-none" style="color:var(--muted)">&times;</button>
        </div>

        <form action="PresupuestoServlet" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="guardarPresupuesto">

            <div>
                <label class="mb-1 block text-sm font-semibold">Período (mes)</label>
                <input type="month" name="periodo" required min="<%= mesActualTexto %>"
                       value="<%= periodo != null && !periodo.isBefore(LocalDate.now().withDayOfMonth(1)) ? periodo.toString().substring(0, 7) : mesActualTexto %>"
                       class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
                <p class="mt-1 text-xs" style="color:var(--muted)">Solo puedes elegir el mes actual en adelante. Si repites un mes, se actualiza con el último monto.</p>
            </div>

            <div>
                <label class="mb-1 block text-sm font-semibold">Monto del presupuesto (S/)</label>
                <input type="number" step="0.01" min="0.01" name="montoPresupuesto" required
                       class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
            </div>

            <button type="submit" class="w-full rounded-xl px-4 py-3 font-bold" style="background:#38b6ff;color:white">
                Guardar presupuesto
            </button>
        </form>
    </div>
</div>

<!-- Modal: límite por categoría -->
<div id="modal-limite-categoria" class="hidden fixed inset-0 z-50 flex items-center justify-center p-4" style="background:rgba(0,0,0,.45)">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Límite por categoría</h3>
            <button type="button" class="btn-cerrar-modal text-xl leading-none" style="color:var(--muted)">&times;</button>
        </div>

        <% if (presupuesto != null) { %>
            <form action="PresupuestoServlet" method="post" class="space-y-4">
                <input type="hidden" name="accion" value="guardarLimite">
                <input type="hidden" name="periodo" value="<%= periodo.toString().substring(0, 7) %>">
                <input type="hidden" name="idPresupuesto" value="<%= presupuesto.getIdPresupuesto() %>">

                <div>
                    <label class="mb-1 block text-sm font-semibold">Categoría (gasto)</label>
                    <select name="idCategoria" class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
                        <%
                            List<Categoria> categoriasGasto = (List<Categoria>) request.getAttribute("categoriasGasto");
                            if (categoriasGasto != null) {
                                for (Categoria c : categoriasGasto) {
                        %>
                                    <option value="<%= c.getIdCategoria() %>"><%= c.getNombreCategoria() %></option>
                        <%
                                }
                            }
                        %>
                    </select>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-semibold">Límite (S/)</label>
                    <input type="number" step="0.01" min="0.01" name="montoLimite" required
                           class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
                </div>

                <button type="submit" class="w-full rounded-xl px-4 py-3 font-bold" style="background:#38b6ff;color:white">
                    Guardar límite
                </button>
            </form>
        <% } %>
    </div>
</div>
