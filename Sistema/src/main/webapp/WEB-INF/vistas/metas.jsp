<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.servicio.MetaAhorroServicio"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.temporal.ChronoUnit"%>
<%@page import="java.util.*"%>
<%
    List<MetaAhorro> metas = (List<MetaAhorro>) request.getAttribute("metas");
    Map<Integer, List<AporteAhorro>> aportes = (Map<Integer, List<AporteAhorro>>) request.getAttribute("aportes");
    LocalDate hoy = LocalDate.now();
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6 flex flex-wrap items-end justify-between gap-4">
    <div>
        <h2 class="brand-serif text-2xl font-bold">Metas de ahorro</h2>
        <p class="mt-1 muted">Convierte tus planes en objetivos con monto y fecha, y registra cada aporte.</p>
    </div>
    <button type="button" class="btn btn-verde" data-abrir-modal="modal-nueva-meta"><i data-lucide="plus"></i>Nueva meta</button>
</div>

<% if (metas.isEmpty()) { %>
    <article class="surface rounded-2xl p-10 text-center">
        <p class="text-4xl">🎯</p>
        <p class="mt-3 font-semibold">Aún no tienes metas de ahorro.</p>
        <p class="mt-1 text-sm muted">Empieza con algo concreto: un fondo de emergencia, un viaje o una laptop.</p>
    </article>
<% } else { %>
<div class="grid gap-5 lg:grid-cols-2">
    <% for (MetaAhorro m : metas) {
        int progreso = MetaAhorroServicio.calcularProgreso(m);
        boolean riesgo = MetaAhorroServicio.estaEnRiesgo(m, hoy);
        long diasRestantes = ChronoUnit.DAYS.between(hoy, m.getFechaObjetivo());
        List<AporteAhorro> listaAportes = aportes.get(m.getIdMeta());
    %>
    <article class="surface rounded-2xl p-5">
        <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
                <h3 class="truncate text-lg font-bold"><%= Html.esc(m.getNombreMeta()) %></h3>
                <p class="text-xs muted">Objetivo: <%= Formato.fecha(m.getFechaObjetivo()) %>
                    <% if (!m.isCumplida()) { %> · <%= diasRestantes >= 0 ? diasRestantes + " días restantes" : "fecha vencida" %><% } %></p>
            </div>
            <% if (m.isCumplida()) { %>
                <span class="chip chip-ok">✓ Cumplida</span>
            <% } else if (riesgo) { %>
                <span class="chip chip-alerta">Retrasada</span>
            <% } else { %>
                <span class="chip chip-ok">En progreso</span>
            <% } %>
        </div>

        <div class="mt-4 flex items-end justify-between">
            <p class="text-2xl font-bold"><%= Formato.soles(m.getMontoAhorrado()) %></p>
            <p class="text-sm muted">de <%= Formato.soles(m.getMontoObjetivo()) %></p>
        </div>
        <div class="barra mt-2" style="height:14px"><span style="width:<%= progreso %>%;background:<%= m.isCumplida() ? "var(--forest)" : (riesgo ? "var(--sun)" : "var(--celeste)") %>"></span></div>
        <p class="mt-2 text-sm"><strong><%= progreso %> %</strong> completado
            <% if (!m.isCumplida()) { %> · faltan <%= Formato.soles(m.getMontoFaltante()) %><% } %></p>

        <% if (!m.isCumplida()) { %>
        <p class="mt-3 rounded-xl p-3 text-sm" style="background:var(--paper)">
            💡 Para llegar a tiempo, aporta unos <strong><%= Formato.soles(MetaAhorroServicio.aporteMensualNecesario(m, hoy)) %></strong> al mes.
        </p>
        <% } %>

        <% if (listaAportes != null && !listaAportes.isEmpty()) { %>
        <details class="mt-3">
            <summary class="cursor-pointer text-sm font-semibold muted">Aportes (<%= listaAportes.size() %>)</summary>
            <ul class="mt-2 space-y-1 text-sm">
                <% for (AporteAhorro a : listaAportes) { %>
                <li class="flex justify-between"><span class="muted"><%= Formato.fecha(a.getFecha()) %></span><span class="font-semibold texto-ingreso">+ <%= Formato.soles(a.getMontoAporte()) %></span></li>
                <% } %>
            </ul>
        </details>
        <% } %>

        <div class="mt-4 flex flex-wrap gap-2">
            <% if (!m.isCumplida()) { %>
            <button type="button" class="btn btn-primario" data-abrir-modal="modal-aporte"
                    data-campo-id-meta="<%= m.getIdMeta() %>" data-campo-nombre-meta="<%= Html.esc(m.getNombreMeta()) %>">
                <i data-lucide="piggy-bank"></i>Registrar aporte
            </button>
            <% } %>
            <form action="metas" method="post" data-confirmar="¿Eliminar la meta &quot;<%= Html.esc(m.getNombreMeta()) %>&quot; y todos sus aportes?">
                <input type="hidden" name="accion" value="eliminarMeta">
                <input type="hidden" name="idMeta" value="<%= m.getIdMeta() %>">
                <button type="submit" class="btn btn-peligro"><i data-lucide="trash-2"></i>Eliminar</button>
            </form>
        </div>
    </article>
    <% } %>
</div>
<% } %>

<!-- Modal: nueva meta -->
<div id="modal-nueva-meta" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-nueva-meta">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-nueva-meta" class="text-lg font-bold">Nueva meta de ahorro</h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="metas" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="crearMeta">
            <div>
                <label class="etiqueta" for="nombreMeta">¿Para qué ahorras?</label>
                <input type="text" id="nombreMeta" name="nombreMeta" required maxlength="100" class="campo" placeholder="Ej. Viaje a Cusco">
            </div>
            <div>
                <label class="etiqueta" for="montoObjetivo">Monto objetivo (S/)</label>
                <input type="number" id="montoObjetivo" name="montoObjetivo" step="0.01" min="0.01" required class="campo">
            </div>
            <div>
                <label class="etiqueta" for="fechaObjetivo">Fecha objetivo</label>
                <input type="date" id="fechaObjetivo" name="fechaObjetivo" required min="<%= hoy.plusDays(1) %>" class="campo">
            </div>
            <button type="submit" class="btn btn-verde w-full">Crear meta</button>
        </form>
    </div>
</div>

<!-- Modal: aporte -->
<div id="modal-aporte" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-aporte">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-aporte" class="text-lg font-bold">Aporte a <span data-rellenar="nombreMeta"></span></h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="metas" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="aportar">
            <input type="hidden" name="idMeta" data-rellenar="idMeta">
            <div>
                <label class="etiqueta" for="montoAporte">Monto del aporte (S/)</label>
                <input type="number" id="montoAporte" name="montoAporte" step="0.01" min="0.01" required class="campo">
            </div>
            <div>
                <label class="etiqueta" for="fechaAporte">Fecha</label>
                <input type="date" id="fechaAporte" name="fechaAporte" required max="<%= hoy %>" value="<%= hoy %>" class="campo">
            </div>
            <button type="submit" class="btn btn-primario w-full">Guardar aporte</button>
        </form>
    </div>
</div>
