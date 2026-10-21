<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.servicio.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.util.*"%>
<%!
    /** Texto de variación respecto al mes anterior ("+12 % vs mes anterior"). */
    private String variacion(BigDecimal antes, BigDecimal ahora, boolean subirEsBueno) {
        Integer v = EstadisticaServicio.variacion(antes, ahora);
        if (v == null) {
            return "<span class=\"muted\">Sin datos del mes anterior</span>";
        }
        boolean bueno = (v >= 0) == subirEsBueno;
        String color = v == 0 ? "var(--muted)" : (bueno ? "var(--forest)" : "var(--coral)");
        return "<span style=\"color:" + color + "\">" + (v > 0 ? "▲ +" : (v < 0 ? "▼ " : "")) + v + " %</span>"
                + " <span class=\"muted\">vs mes anterior</span>";
    }
%>
<%
    ResumenPeriodo r = (ResumenPeriodo) request.getAttribute("resumen");
    ResumenPeriodo ant = (ResumenPeriodo) request.getAttribute("resumenAnterior");
    List<ResumenPeriodo> meses = (List<ResumenPeriodo>) request.getAttribute("ultimosMeses");
    Presupuesto presupuesto = (Presupuesto) request.getAttribute("presupuesto");
    List<MetaAhorro> metas = (List<MetaAhorro>) request.getAttribute("metas");
    List<Recomendacion> recomendaciones = (List<Recomendacion>) request.getAttribute("recomendaciones");

    List<String> etiquetasMeses = new ArrayList<>();
    List<BigDecimal> ingresosMeses = new ArrayList<>();
    List<BigDecimal> gastosMeses = new ArrayList<>();
    for (ResumenPeriodo m : meses) {
        etiquetasMeses.add(Formato.mesCorto(m.getMes()));
        ingresosMeses.add(m.getIngresos());
        gastosMeses.add(m.getGastos());
    }
    String seriesMeses = "[{\"nombre\":\"Ingresos\",\"valores\":" + Json.numeros(ingresosMeses)
            + "},{\"nombre\":\"Gastos\",\"valores\":" + Json.numeros(gastosMeses) + "}]";
    String seriesCategorias = "[{\"nombre\":\"Gastos\",\"valores\":" + Json.numeros(r.getGastosPorCategoria().values()) + "}]";
%>
<%@include file="_mensajes.jspf"%>

<div class="mb-6">
    <h2 class="brand-serif text-2xl font-bold">Hola, <%= Html.esc(session.getAttribute("nombreUsuario")) %> 👋</h2>
    <p class="mt-1 muted">Así van tus finanzas en <%= Formato.mes(r.getMes()) %>.</p>
</div>

<!-- Indicadores del mes -->
<section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Indicadores del mes">
    <article class="surface kpi">
        <p class="text-sm font-semibold muted">Ingresos</p>
        <p class="valor texto-ingreso"><%= Formato.soles(r.getIngresos()) %></p>
        <p class="mt-2 text-xs"><%= variacion(ant.getIngresos(), r.getIngresos(), true) %></p>
    </article>
    <article class="surface kpi">
        <p class="text-sm font-semibold muted">Gastos</p>
        <p class="valor texto-gasto"><%= Formato.soles(r.getGastos()) %></p>
        <p class="mt-2 text-xs"><%= variacion(ant.getGastos(), r.getGastos(), false) %></p>
    </article>
    <article class="surface kpi">
        <p class="text-sm font-semibold muted">Balance del mes</p>
        <p class="valor" style="color:<%= r.getBalance().signum() < 0 ? "var(--coral)" : "var(--azul)" %>"><%= Formato.soles(r.getBalance()) %></p>
        <p class="mt-2 text-xs muted">Conservas el <strong><%= r.getTasaAhorro() %> %</strong> de tus ingresos</p>
    </article>
    <article class="surface kpi">
        <p class="text-sm font-semibold muted">Ahorrado en metas</p>
        <p class="valor" style="color:var(--forest)"><%= Formato.soles(r.getAhorroMetas()) %></p>
        <p class="mt-2 text-xs muted">Gastos hormiga: <strong class="texto-gasto"><%= Formato.soles(r.getGastosHormiga()) %></strong></p>
    </article>
</section>

<!-- Gráficos -->
<section class="mt-6 grid gap-6 xl:grid-cols-5">
    <article class="surface rounded-2xl p-5 xl:col-span-3">
        <div class="mb-3 flex items-center justify-between">
            <h3 class="text-lg font-bold">Ingresos vs. gastos</h3>
            <span class="text-xs muted">Últimos 6 meses</span>
        </div>
        <div class="grafico">
            <canvas data-grafico="barras" role="img" aria-label="Gráfico de ingresos y gastos de los últimos 6 meses"
                    data-colores="#176b4a,#d4553c"
                    data-etiquetas="<%= Html.esc(Json.textos(etiquetasMeses)) %>"
                    data-series="<%= Html.esc(seriesMeses) %>"></canvas>
        </div>
    </article>

    <article class="surface rounded-2xl p-5 xl:col-span-2">
        <div class="mb-3 flex items-center justify-between">
            <h3 class="text-lg font-bold">¿En qué gastas?</h3>
            <span class="text-xs muted"><%= Formato.mes(r.getMes()) %></span>
        </div>
        <% if (r.getGastosPorCategoria().isEmpty()) { %>
            <p class="py-16 text-center text-sm muted">Aún no registraste gastos este mes.</p>
        <% } else { %>
        <div class="grafico">
            <canvas data-grafico="dona" role="img" aria-label="Distribución de gastos por categoría"
                    data-etiquetas="<%= Html.esc(Json.textos(r.getGastosPorCategoria().keySet())) %>"
                    data-series="<%= Html.esc(seriesCategorias) %>"></canvas>
        </div>
        <% } %>
    </article>
</section>

<section class="mt-6 grid gap-6 xl:grid-cols-3">
    <!-- Presupuesto -->
    <article class="surface rounded-2xl p-5">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Presupuesto</h3>
            <a href="#presupuestos" class="text-sm font-semibold" style="color:var(--celeste)">Ver detalle</a>
        </div>
        <% if (presupuesto == null) { %>
            <p class="text-sm muted">Aún no definiste tu presupuesto de este mes.</p>
            <a href="#presupuestos" class="btn btn-primario mt-4">Definir presupuesto</a>
        <% } else {
            BigDecimal gastado = (BigDecimal) request.getAttribute("gastadoPresupuesto");
            PresupuestoServicio.NivelAlerta nivel = (PresupuestoServicio.NivelAlerta) request.getAttribute("nivelPresupuesto");
            int pct = Formato.porcentaje(gastado, presupuesto.getMontoPresupuesto());
            String color = nivel == PresupuestoServicio.NivelAlerta.SUPERADO ? "var(--coral)"
                    : nivel == PresupuestoServicio.NivelAlerta.CERCA ? "var(--sun)" : "var(--forest)";
        %>
            <p class="text-3xl font-bold"><%= pct %> %</p>
            <p class="text-sm muted">usado de <%= Formato.soles(presupuesto.getMontoPresupuesto()) %></p>
            <div class="barra mt-4"><span style="width:<%= Math.min(100, pct) %>%;background:<%= color %>"></span></div>
            <p class="mt-3 text-sm">
                Disponible: <strong style="color:<%= color %>"><%= Formato.soles(presupuesto.getMontoPresupuesto().subtract(gastado)) %></strong>
            </p>
        <% } %>
    </article>

    <!-- Metas -->
    <article class="surface rounded-2xl p-5">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Metas de ahorro</h3>
            <a href="#metas" class="text-sm font-semibold" style="color:var(--celeste)">Ver todas</a>
        </div>
        <% if (metas.isEmpty()) { %>
            <p class="text-sm muted">Todavía no tienes metas. ¡Crea la primera!</p>
            <a href="#metas" class="btn btn-verde mt-4">Crear meta</a>
        <% } else {
            int mostradas = 0;
            for (MetaAhorro m : metas) {
                if (mostradas++ == 3) break;
                int progreso = MetaAhorroServicio.calcularProgreso(m);
        %>
            <div class="mb-4">
                <div class="flex justify-between text-sm">
                    <span class="font-semibold"><%= Html.esc(m.getNombreMeta()) %></span>
                    <span class="muted"><%= progreso %> %</span>
                </div>
                <div class="barra mt-2"><span style="width:<%= progreso %>%;background:var(--forest)"></span></div>
                <p class="mt-1 text-xs muted"><%= Formato.soles(m.getMontoAhorrado()) %> de <%= Formato.soles(m.getMontoObjetivo()) %></p>
            </div>
        <%  }
        } %>
    </article>

    <!-- Recomendaciones -->
    <article class="surface rounded-2xl p-5">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Recomendaciones</h3>
            <a href="#analisis" class="text-sm font-semibold" style="color:var(--celeste)">Ver análisis</a>
        </div>
        <ul class="space-y-3">
        <%  int contador = 0;
            for (Recomendacion rec : recomendaciones) {
                if (contador++ == 3) break; %>
            <li class="rounded-xl p-3" style="background:var(--paper)">
                <p class="text-sm font-bold"><%= Html.esc(rec.getTitulo()) %></p>
                <p class="mt-1 text-xs muted"><%= Html.esc(rec.getDescripcion()) %></p>
            </li>
        <% } %>
        </ul>
    </article>
</section>

<!-- Accesos rápidos -->
<section class="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-label="Accesos rápidos">
    <a href="#transacciones" class="surface flex items-center gap-3 rounded-2xl p-4 font-semibold"><i data-lucide="plus-circle"></i> Registrar movimiento</a>
    <a href="#simulador" class="surface flex items-center gap-3 rounded-2xl p-4 font-semibold"><i data-lucide="calculator"></i> Simular un ahorro</a>
    <a href="#reportes" class="surface flex items-center gap-3 rounded-2xl p-4 font-semibold"><i data-lucide="file-down"></i> Descargar reporte PDF</a>
    <a href="#educacion" class="surface flex items-center gap-3 rounded-2xl p-4 font-semibold"><i data-lucide="graduation-cap"></i> Aprender finanzas</a>
</section>
