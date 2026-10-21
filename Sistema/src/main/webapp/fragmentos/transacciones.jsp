<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@ page import="com.mycompany.finanz.modelo.*" %>
<%@ page import="java.util.List" %>

<div class="mb-6">
    
    <p class="mt-1" style="color:var(--muted)">Registra y consulta cada movimiento.</p>
</div>

<% if (request.getAttribute("error") != null) { %>
<p class="mensaje-error mb-4"><%= request.getAttribute("error") %></p>
<% } %>

<div class="grid gap-6 xl:grid-cols-3">

    <aside class="surface h-fit rounded-2xl p-5">
        <h3 class="text-lg font-bold">Añadir transacción</h3>

        <form action="MovimientoServlet" method="post" class="mt-5 space-y-4">

            <div>
                <label class="mb-1 block text-sm font-semibold">Descripción</label>
                <input type="text" name="descripcion" required maxlength="60"
                       class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
            </div>

            <div>
                <label class="mb-1 block text-sm font-semibold">Importe (S/)</label>
                <input type="number" name="monto" min="0.01" step="0.01" required
                       class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
            </div>

            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="mb-1 block text-sm font-semibold">Tipo</label>
                    <select name="tipo" id="tipo-movimiento" class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
                        <option value="GASTO">Gasto</option>
                        <option value="INGRESO">Ingreso</option>
                    </select>
                </div>

                <div>
                    <div class="mb-1 flex items-center justify-between">
                        <label class="block text-sm font-semibold">Categoría</label>
                        <button type="button" id="btn-nueva-categoria"
                                class="flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold"
                                style="background:var(--mint);color:var(--forest)" title="Añadir categoría">+</button>
                    </div>
                    <select name="idCategoria" id="select-categoria"
                            class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm"
                            data-gasto='<%
                                StringBuilder sbGasto = new StringBuilder("[");
                                List<Categoria> categoriasGasto = (List<Categoria>) request.getAttribute("categoriasGasto");
                                if (categoriasGasto != null) {
                                    boolean primero = true;
                                    for (Categoria c : categoriasGasto) {
                                        if (!primero) sbGasto.append(",");
                                        sbGasto.append("{\"id\":").append(c.getIdCategoria())
                                               .append(",\"nombre\":\"").append(c.getNombreCategoria().replace("\"", "\\\"")).append("\"}");
                                        primero = false;
                                    }
                                }
                                sbGasto.append("]");
                            %><%= sbGasto.toString() %>'
                            data-ingreso='<%
                                StringBuilder sbIngreso = new StringBuilder("[");
                                List<Categoria> categoriasIngreso = (List<Categoria>) request.getAttribute("categoriasIngreso");
                                if (categoriasIngreso != null) {
                                    boolean primero = true;
                                    for (Categoria c : categoriasIngreso) {
                                        if (!primero) sbIngreso.append(",");
                                        sbIngreso.append("{\"id\":").append(c.getIdCategoria())
                                               .append(",\"nombre\":\"").append(c.getNombreCategoria().replace("\"", "\\\"")).append("\"}");
                                        primero = false;
                                    }
                                }
                                sbIngreso.append("]");
                            %><%= sbIngreso.toString() %>'>
                    </select>
                </div>
            </div>

            <div>
                <label class="mb-1 block text-sm font-semibold">Fecha</label>
                <input type="date" name="fecha" required class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
            </div>

            <button type="submit" class="w-full rounded-xl px-4 py-3 font-bold" style="background:#38b6ff;color:white">
                Guardar transacción
            </button>
        </form>
    </aside>

    <article class="surface rounded-2xl p-5 xl:col-span-2">
        <h3 class="mb-4 text-lg font-bold">Historial</h3>

        <div class="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <input type="text" id="filtro-descripcion" placeholder="Buscar por descripción..."
                   class="filtro focus-ring w-full rounded-xl border px-3 py-2 text-sm">

            <select id="filtro-tipo" class="filtro focus-ring w-full rounded-xl border px-3 py-2 text-sm">
                <option value="">Todos los tipos</option>
                <option value="GASTO">Gasto</option>
                <option value="INGRESO">Ingreso</option>
            </select>

            <select id="filtro-categoria" class="filtro focus-ring w-full rounded-xl border px-3 py-2 text-sm">
                <option value="">Todas las categorías</option>
                <%
                    List<Categoria> categorias = (List<Categoria>) request.getAttribute("categorias");
                    if (categorias != null) {
                        for (Categoria c : categorias) {
                %>
                <option value="<%= c.getNombreCategoria() %>"><%= c.getNombreCategoria() %></option>
                <%
                        }
                    }
                %>
            </select>

            <input type="date" id="filtro-fecha" class="filtro focus-ring w-full rounded-xl border px-3 py-2 text-sm">
        </div>

        <%
            List<Movimiento> movimientos = (List<Movimiento>) request.getAttribute("movimientos");
        %>

        <% if (movimientos == null || movimientos.isEmpty()) { %>
        <p class="py-10 text-center text-sm" style="color:var(--muted)">Todavía no registraste movimientos.</p>
        <% } else { %>
        <div id="lista-movimientos" class="space-y-2 max-h-[420px] overflow-y-auto pr-1">
            <%
                for (Movimiento m : movimientos) {
                    boolean esGasto = (m instanceof Gasto);
                    boolean esHormiga = esGasto && Boolean.TRUE.equals(((Gasto) m).getEsGastoHormiga());
                    String nombreCategoria = "";
                    if (categorias != null) {
                        for (Categoria c : categorias) {
                            if (c.getIdCategoria() == m.getIdCategoria()) {
                                nombreCategoria = c.getNombreCategoria();
                                break;
                            }
                        }
                    }
            %>
            <div class="fila-movimiento flex items-center justify-between rounded-xl border px-4 py-3"
                 style="border-color:var(--line)"
                 data-tipo="<%= esGasto ? "GASTO" : "INGRESO" %>"
                 data-categoria="<%= nombreCategoria %>"
                 data-descripcion="<%= m.getDescripcion().toLowerCase() %>"
                 data-fecha="<%= m.getFecha() %>">
                <div>
                    <p class="font-semibold"><%= m.getDescripcion() %></p>
                    <p class="text-xs" style="color:var(--muted)">
                        <%= m.getFecha() %> · <%= nombreCategoria %><% if (esHormiga) { %> · <span style="color:var(--coral)">gasto hormiga</span><% } %>
                    </p>
                </div>
                <p class="font-bold" style="color: <%= esGasto ? "var(--coral)" : "var(--forest)" %>">
                    <%= esGasto ? "-" : "+" %> S/ <%= m.getMonto() %>
                </p>
            </div>
            <%
                }
            %>
        </div>
        <p id="sin-resultados-filtro" class="hidden py-10 text-center text-sm" style="color:var(--muted)">
            Ningún movimiento coincide con los filtros.
        </p>
        <% } %>
    </article>

</div>

<!-- Modal: nueva categoría -->
<div id="modal-nueva-categoria" class="hidden fixed inset-0 z-50 flex items-center justify-center p-4" style="background:rgba(0,0,0,.45)">
    <div class="surface w-full max-w-sm rounded-2xl p-6">
        <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-bold">Nueva categoría</h3>
            <button type="button" id="btn-cerrar-categoria" class="text-xl leading-none" style="color:var(--muted)">&times;</button>
        </div>

        <form action="MovimientoServlet" method="post" class="space-y-4">
            <input type="hidden" name="accion" value="agregarCategoria">

            <div>
                <label class="mb-1 block text-sm font-semibold">Nombre</label>
                <input type="text" name="nombreCategoria" required maxlength="40"
                       class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
            </div>

            <div>
                <label class="mb-1 block text-sm font-semibold">Tipo</label>
                <select name="tipoCategoria" class="focus-ring w-full rounded-xl border px-3 py-2.5 text-sm">
                    <option value="GASTO">Gasto</option>
                    <option value="INGRESO">Ingreso</option>
                </select>
            </div>

            <button type="submit" class="w-full rounded-xl px-4 py-3 font-bold" style="background:#38b6ff;color:white">
                Crear categoría
            </button>
        </form>
    </div>
</div>
