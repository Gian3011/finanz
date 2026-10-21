<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.servicio.PresupuestoServicio"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.time.YearMonth"%>
<%@page import="java.util.*"%>
<%
    YearMonth mes = (YearMonth) request.getAttribute("mes");
    Presupuesto presupuesto = (Presupuesto) request.getAttribute("presupuesto");
    List<Categoria> categoriasGasto = (List<Categoria>) request.getAttribute("categoriasGasto");
    boolean editable = !mes.isBefore(YearMonth.now());
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6 flex flex-wrap items-end justify-between gap-4">
    <div>
        <h2 class="brand-serif text-2xl font-bold">Presupuesto de <%= Formato.mes(mes) %></h2>
        <p class="mt-1 muted">Planifica cuánto gastar y recibe alertas al llegar al 80 % o al superarlo.</p>
    </div>
    <form action="presupuestos" method="get" class="flex items-end gap-2">
        <label class="text-xs font-semibold muted">Mes
            <input type="month" name="periodo" value="<%= mes %>" class="campo mt-1" data-enviar-al-cambiar>
        </label>
    </form>
</div>

<div class="mb-6 flex flex-wrap gap-3">
    <% if (editable) { %>
    <button type="button" class="btn btn-primario" data-abrir-modal="modal-presupuesto-mensual">
        <i data-lucide="wallet"></i><%= presupuesto == null ? "Definir presupuesto mensual" : "Editar presupuesto" %>
    </button>
    <% } %>
    <button type="button" class="btn btn-verde" data-abrir-modal="modal-limite-categoria" <%= presupuesto == null || !editable ? "disabled title=\"Primero define el presupuesto mensual\"" : "" %>>
        <i data-lucide="sliders-horizontal"></i>Límite por categoría
    </button>
</div>

<% if (presupuesto == null) { %>
    <article class="surface rounded-2xl p-10 text-center">
        <p class="text-4xl">🗓️</p>
        <p class="mt-3 font-semibold">Todavía no has definido un presupuesto para este mes.</p>
        <p class="mt-1 text-sm muted"><%= editable ? "Define cuánto quieres gastar como máximo y FinanZ te avisará cuando te acerques." : "Solo puedes crear presupuestos del mes actual en adelante." %></p>
    </article>
<% } else {
    BigDecimal gastado = (BigDecimal) request.getAttribute("gastado");
    BigDecimal disponible = (BigDecimal) request.getAttribute("disponible");
    BigDecimal sumaLimites = (BigDecimal) request.getAttribute("sumaLimites");
    PresupuestoServicio.NivelAlerta alerta = (PresupuestoServicio.NivelAlerta) request.getAttribute("alerta");
    List<LimiteGasto> limites = (List<LimiteGasto>) request.getAttribute("limites");
    Map<Integer, BigDecimal> gastoPorCategoria = (Map<Integer, BigDecimal>) request.getAttribute("gastoPorCategoria");

    int porcentaje = Formato.porcentaje(gastado, presupuesto.getMontoPresupuesto());
    String color = "var(--forest)", estado = "Vas bien", chip = "chip-ok";
    if (alerta == PresupuestoServicio.NivelAlerta.CERCA) {
        color = "var(--sun)"; estado = "Cerca del límite (80 % o más)"; chip = "chip-alerta";
    } else if (alerta == PresupuestoServicio.NivelAlerta.SUPERADO) {
        color = "var(--coral)"; estado = "Presupuesto superado"; chip = "chip-hormiga";
    }
%>
    <article class="surface mb-6 rounded-2xl p-6">
        <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
                <p class="text-sm font-medium muted">Presupuesto mensual</p>
                <p class="mt-1 text-3xl font-bold"><%= Formato.soles(presupuesto.getMontoPresupuesto()) %></p>
            </div>
            <span class="chip <%= chip %>" style="font-size:13px;padding:6px 12px"><%= estado %> · <%= porcentaje %> %</span>
        </div>

        <div class="mt-6">
            <div class="barra" style="height:16px"><span style="width:<%= Math.min(100, porcentaje) %>%;background:<%= color %>"></span></div>
            <div class="mt-3 grid gap-3 text-sm sm:grid-cols-3">
                <p>Gastado: <strong><%= Formato.soles(gastado) %></strong></p>
                <p>Disponible: <strong style="color:<%= color %>"><%= Formato.soles(disponible) %></strong></p>
                <p class="muted">Asignado a categorías: <%= Formato.soles(sumaLimites) %></p>
            </div>
        </div>
    </article>

    <article class="surface rounded-2xl p-6">
        <h3 class="text-lg font-bold">Límites por categoría</h3>
        <p class="mb-5 mt-1 text-sm muted">Define cuánto quieres gastar como máximo en cada categoría.</p>

        <% if (limites.isEmpty()) { %>
            <p class="py-6 text-center text-sm muted">Todavía no definiste límites por categoría.</p>
        <% } else { %>
            <div class="grid gap-4 md:grid-cols-2">
            <% for (LimiteGasto l : limites) {
                BigDecimal gastadoCat = gastoPorCategoria.getOrDefault(l.getIdCategoria(), BigDecimal.ZERO);
                int pctCat = Formato.porcentaje(gastadoCat, l.getMontoLimite());
                PresupuestoServicio.NivelAlerta nivelCat = PresupuestoServicio.nivel(gastadoCat, l.getMontoLimite());
                String colorCat = nivelCat == PresupuestoServicio.NivelAlerta.SUPERADO ? "var(--coral)"
                        : nivelCat == PresupuestoServicio.NivelAlerta.CERCA ? "var(--sun)" : "var(--forest)";
            %>
                <div class="rounded-2xl border p-4" style="border-color:var(--line)">
                    <div class="flex items-start justify-between gap-2">
                        <h4 class="font-bold"><%= Html.esc(l.getNombreCategoria()) %></h4>
                        <div class="flex items-center gap-1">
                            <span class="text-xs font-bold" style="color:<%= colorCat %>"><%= pctCat %> %</span>
                            <% if (editable) { %>
                            <form action="presupuestos" method="post" data-confirmar="¿Eliminar el límite de <%= Html.esc(l.getNombreCategoria()) %>?">
                                <input type="hidden" name="accion" value="eliminarLimite">
                                <input type="hidden" name="idLimite" value="<%= l.getIdLimite() %>">
                                <button type="submit" class="btn btn-icono" aria-label="Eliminar límite"><i data-lucide="x"></i></button>
                            </form>
                            <% } %>
                        </div>
                    </div>
                    <p class="mt-1 text-sm muted"><%= Formato.soles(gastadoCat) %> de <%= Formato.soles(l.getMontoLimite()) %></p>
                    <div class="barra mt-3"><span style="width:<%= Math.min(100, pctCat) %>%;background:<%= colorCat %>"></span></div>
                    <% if (nivelCat == PresupuestoServicio.NivelAlerta.SUPERADO) { %>
                        <p class="mt-2 text-xs font-semibold" style="color:var(--coral)">Superaste el límite de esta categoría.</p>
                    <% } else if (nivelCat == PresupuestoServicio.NivelAlerta.CERCA) { %>
                        <p class="mt-2 text-xs font-semibold" style="color:var(--sun)">Estás cerca del límite.</p>
                    <% } %>
                </div>
            <% } %>
            </div>
        <% } %>
    </article>
<% } %>

<!-- Modal: presupuesto mensual -->
<div id="modal-presupuesto-mensual" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-presupuesto">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-presupuesto" class="text-lg font-bold">Presupuesto mensual</h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="presupuestos" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="guardarPresupuesto">
            <div>
                <label class="etiqueta" for="periodo-presupuesto">Mes</label>
                <input type="month" id="periodo-presupuesto" name="periodo" required min="<%= YearMonth.now() %>"
                       value="<%= editable ? mes : YearMonth.now() %>" class="campo">
                <p class="mt-1 text-xs muted">Solo el mes actual en adelante. Si el mes ya tiene presupuesto, se actualiza.</p>
            </div>
            <div>
                <label class="etiqueta" for="montoPresupuesto">Monto (S/)</label>
                <input type="number" id="montoPresupuesto" name="montoPresupuesto" step="0.01" min="0.01" required class="campo"
                       value="<%= presupuesto != null ? presupuesto.getMontoPresupuesto().toPlainString() : "" %>">
            </div>
            <button type="submit" class="btn btn-primario w-full">Guardar presupuesto</button>
        </form>
    </div>
</div>

<!-- Modal: límite por categoría -->
<% if (presupuesto != null && editable) { %>
<div id="modal-limite-categoria" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-limite">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-limite" class="text-lg font-bold">Límite por categoría</h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="presupuestos" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="guardarLimite">
            <input type="hidden" name="periodo" value="<%= mes %>">
            <input type="hidden" name="idPresupuesto" value="<%= presupuesto.getIdPresupuesto() %>">
            <div>
                <label class="etiqueta" for="idCategoria-limite">Categoría de gasto</label>
                <select id="idCategoria-limite" name="idCategoria" class="campo">
                    <% for (Categoria c : categoriasGasto) { %>
                    <option value="<%= c.getIdCategoria() %>"><%= Html.esc(c.getNombreCategoria()) %></option>
                    <% } %>
                </select>
            </div>
            <div>
                <label class="etiqueta" for="montoLimite">Límite (S/)</label>
                <input type="number" id="montoLimite" name="montoLimite" step="0.01" min="0.01" required class="campo">
                <p class="mt-1 text-xs muted">La suma de los límites no puede superar el presupuesto mensual.</p>
            </div>
            <button type="submit" class="btn btn-verde w-full">Guardar límite</button>
        </form>
    </div>
</div>
<% } %>
