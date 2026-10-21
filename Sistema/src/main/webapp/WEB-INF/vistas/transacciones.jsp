<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycompany.finanz.modelo.*"%>
<%@page import="com.mycompany.finanz.util.*"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.util.List"%>
<%!
    /** Lista de categorías como JSON [{"id":1,"nombre":"..."}] para el combo dinámico. */
    private String categoriasJson(List<Categoria> categorias) {
        StringBuilder sb = new StringBuilder("[");
        for (Categoria c : categorias) {
            if (sb.length() > 1) sb.append(',');
            sb.append("{\"id\":").append(c.getIdCategoria()).append(",\"nombre\":").append(Json.texto(c.getNombreCategoria())).append('}');
        }
        return sb.append(']').toString();
    }
%>
<%
    List<Movimiento> movimientos = (List<Movimiento>) request.getAttribute("movimientos");
    List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
    List<Categoria> categoriasGasto = (List<Categoria>) request.getAttribute("categoriasGasto");
    List<Categoria> categoriasIngreso = (List<Categoria>) request.getAttribute("categoriasIngreso");
    String hoy = LocalDate.now().toString();
%>
<%@include file="_mensajes.jspf"%>

<div class="grid gap-6 xl:grid-cols-3">

    <!-- Formulario de registro -->
    <aside class="surface h-fit rounded-2xl p-5">
        <h2 class="text-lg font-bold">Añadir movimiento</h2>
        <p class="mt-1 text-sm muted">Registra tus ingresos y gastos para mantener el control.</p>

        <form action="transacciones" method="post" class="mt-5 space-y-4">
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="etiqueta" for="tipo-movimiento">Tipo</label>
                    <select name="tipo" id="tipo-movimiento" class="campo">
                        <option value="GASTO">Gasto</option>
                        <option value="INGRESO">Ingreso</option>
                    </select>
                </div>
                <div>
                    <label class="etiqueta" for="monto">Importe (S/)</label>
                    <input type="number" id="monto" name="monto" min="0.01" max="99999999.99" step="0.01" required class="campo" inputmode="decimal">
                </div>
            </div>

            <div>
                <label class="etiqueta" for="descripcion">Descripción</label>
                <input type="text" id="descripcion" name="descripcion" required maxlength="150" class="campo" placeholder="Ej. Menú del día">
            </div>

            <div>
                <div class="mb-1 flex items-center justify-between">
                    <label class="etiqueta mb-0" for="select-categoria">Categoría</label>
                    <button type="button" class="chip chip-ok" data-abrir-modal="modal-nueva-categoria">+ Nueva</button>
                </div>
                <select name="idCategoria" id="select-categoria" class="campo"
                        data-gasto="<%= Html.esc(categoriasJson(categoriasGasto)) %>"
                        data-ingreso="<%= Html.esc(categoriasJson(categoriasIngreso)) %>"></select>
            </div>

            <div id="campo-fuente" class="hidden">
                <label class="etiqueta" for="fuente">Fuente del ingreso <span class="muted">(opcional)</span></label>
                <input type="text" id="fuente" name="fuente" maxlength="100" class="campo" placeholder="Ej. Empleador, cliente...">
            </div>

            <div>
                <label class="etiqueta" for="fecha">Fecha</label>
                <input type="date" id="fecha" name="fecha" required max="<%= hoy %>" value="<%= hoy %>" class="campo">
            </div>

            <button type="submit" class="btn btn-primario w-full">Guardar movimiento</button>
        </form>
    </aside>

    <!-- Historial -->
    <article class="surface rounded-2xl p-5 xl:col-span-2">
        <h2 class="mb-4 text-lg font-bold">Historial</h2>

        <div class="mb-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <input type="search" id="filtro-descripcion" placeholder="Buscar por descripción..." class="filtro campo" aria-label="Buscar por descripción">
            <select id="filtro-tipo" class="filtro campo" aria-label="Filtrar por tipo">
                <option value="">Todos los tipos</option>
                <option value="GASTO">Gastos</option>
                <option value="INGRESO">Ingresos</option>
            </select>
            <select id="filtro-categoria" class="filtro campo" aria-label="Filtrar por categoría">
                <option value="">Todas las categorías</option>
                <% for (Categoria c : categorias) { %>
                <option value="<%= Html.esc(c.getNombreCategoria()) %>"><%= Html.esc(c.getNombreCategoria()) %></option>
                <% } %>
            </select>
            <label class="text-xs font-semibold muted">Desde
                <input type="date" id="filtro-desde" class="filtro campo mt-1">
            </label>
            <label class="text-xs font-semibold muted">Hasta
                <input type="date" id="filtro-hasta" class="filtro campo mt-1">
            </label>
            <div class="flex items-end gap-3">
                <label class="flex items-center gap-2 pb-2 text-sm font-semibold">
                    <input type="checkbox" id="filtro-hormiga" class="filtro"> Solo hormiga
                </label>
                <button type="button" id="btn-limpiar-filtros" class="btn btn-suave ml-auto">Limpiar</button>
            </div>
        </div>
        <p id="totales-filtro" class="mb-3 text-xs font-semibold muted"></p>

        <% if (movimientos.isEmpty()) { %>
        <p class="py-10 text-center text-sm muted">Todavía no registraste movimientos.</p>
        <% } else { %>
        <ul id="lista-movimientos" class="max-h-[560px] space-y-2 overflow-y-auto pr-1">
            <% for (Movimiento m : movimientos) {
                boolean esGasto = m instanceof Gasto;
                boolean esHormiga = esGasto && ((Gasto) m).isEsGastoHormiga();
                String descripcion = m.getDescripcion() == null ? "" : m.getDescripcion();
            %>
            <li class="fila-movimiento flex items-center gap-3 rounded-xl border px-4 py-3" style="border-color:var(--line)"
                data-tipo="<%= m.getTipoMovimiento() %>"
                data-categoria="<%= Html.esc(m.getNombreCategoria()) %>"
                data-descripcion="<%= Html.esc(descripcion.toLowerCase()) %>"
                data-fecha="<%= m.getFecha() %>"
                data-monto="<%= m.getMonto().toPlainString() %>"
                data-hormiga="<%= esHormiga %>">
                <div class="min-w-0 flex-1">
                    <p class="truncate font-semibold"><%= Html.esc(descripcion) %></p>
                    <p class="text-xs muted">
                        <%= Formato.fecha(m.getFecha()) %> · <%= Html.esc(m.getNombreCategoria()) %>
                        <% if (!esGasto && ((Ingreso) m).getFuente() != null) { %> · <%= Html.esc(((Ingreso) m).getFuente()) %><% } %>
                        <% if (esHormiga) { %> <span class="chip chip-hormiga">🐜 gasto hormiga</span><% } %>
                    </p>
                </div>
                <p class="whitespace-nowrap font-bold <%= esGasto ? "texto-gasto" : "texto-ingreso" %>">
                    <%= esGasto ? "−" : "+" %> <%= Formato.soles(m.getMonto()) %>
                </p>
                <div class="flex items-center">
                    <% if (esGasto) { %>
                    <form action="transacciones" method="post">
                        <input type="hidden" name="accion" value="hormiga">
                        <input type="hidden" name="idMovimiento" value="<%= m.getIdMovimiento() %>">
                        <input type="hidden" name="valor" value="<%= !esHormiga %>">
                        <button type="submit" class="btn btn-icono" title="<%= esHormiga ? "Quitar marca de gasto hormiga" : "Marcar como gasto hormiga" %>"
                                aria-label="<%= esHormiga ? "Quitar marca de gasto hormiga" : "Marcar como gasto hormiga" %>">🐜</button>
                    </form>
                    <% } %>
                    <form action="transacciones" method="post" data-confirmar="¿Eliminar este movimiento? Esta acción no se puede deshacer.">
                        <input type="hidden" name="accion" value="eliminar">
                        <input type="hidden" name="idMovimiento" value="<%= m.getIdMovimiento() %>">
                        <button type="submit" class="btn btn-icono" title="Eliminar" aria-label="Eliminar movimiento"><i data-lucide="trash-2"></i></button>
                    </form>
                </div>
            </li>
            <% } %>
        </ul>
        <p id="sin-resultados-filtro" class="hidden py-10 text-center text-sm muted">Ningún movimiento coincide con los filtros.</p>
        <% } %>
    </article>
</div>

<!-- Modal: nueva categoría -->
<div id="modal-nueva-categoria" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="titulo-nueva-categoria">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 id="titulo-nueva-categoria" class="text-lg font-bold">Nueva categoría</h3>
            <button type="button" class="btn btn-icono" data-cerrar-modal aria-label="Cerrar">✕</button>
        </div>
        <form action="transacciones" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="agregarCategoria">
            <div>
                <label class="etiqueta" for="nombreCategoria">Nombre</label>
                <input type="text" id="nombreCategoria" name="nombreCategoria" required maxlength="50" class="campo">
            </div>
            <div>
                <label class="etiqueta" for="tipoCategoria">Tipo</label>
                <select id="tipoCategoria" name="tipoCategoria" class="campo">
                    <option value="GASTO">Gasto</option>
                    <option value="INGRESO">Ingreso</option>
                </select>
            </div>
            <button type="submit" class="btn btn-primario w-full">Crear categoría</button>
        </form>
    </div>
</div>
