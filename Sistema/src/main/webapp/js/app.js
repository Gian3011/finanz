/*
 * FinanZ - lógica del dashboard (navegación sin recargar la página).
 *
 * Cada sección es un Servlet que devuelve un fragmento HTML. Este script:
 *  - carga la sección indicada en la URL (#resumen, #transacciones, ...)
 *  - envía los formularios por AJAX (con el token CSRF) y muestra la respuesta
 *  - dibuja los gráficos (Chart.js) a partir de atributos data-*
 *  - maneja filtros, ventanas modales, alertas, menú móvil y modo oscuro
 */
document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    var contenido = document.getElementById('contenido');
    var encabezado = document.getElementById('page-heading');
    var csrf = (document.querySelector('meta[name="csrf-token"]') || {}).content || '';
    var usuarioPagina = (document.querySelector('meta[name="usuario-id"]') || {}).content || '';
    var graficos = [];
    var SECCION_INICIAL = 'resumen';

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    function iconos() {
        if (window.lucide) {
            lucide.createIcons();
        }
    }

    function toast(mensaje) {
        var t = document.getElementById('toast');
        if (!t || !mensaje) {
            return;
        }
        t.textContent = mensaje;
        t.classList.add('show');
        clearTimeout(toast.timer);
        toast.timer = setTimeout(function () { t.classList.remove('show'); }, 3500);
    }

    function leerPreferencia(clave) {
        try { return localStorage.getItem(clave); } catch (e) { return null; }
    }

    function guardarPreferencia(clave, valor) {
        try { localStorage.setItem(clave, valor); } catch (e) { /* modo privado: se ignora */ }
    }

    function peticion(url, opciones) {
        opciones = opciones || {};
        opciones.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opciones.headers || {});
        opciones.credentials = 'same-origin';
        return fetch(url, opciones).then(function (r) {
            if (r.status === 401) {
                window.location.href = 'Login.jsp?sesion=cerrada';
                throw new Error('Sesión expirada');
            }
            // Un navegador guarda una sola sesión por sitio: si en otra pestaña se
            // inició sesión con otra cuenta, esta pestaña ya no es de su usuario.
            var usuarioServidor = r.headers.get('X-FinanZ-Usuario');
            if (usuarioPagina && usuarioServidor && usuarioServidor !== usuarioPagina) {
                window.alert('Se inició sesión con otra cuenta en este navegador (en otra pestaña).\n\n'
                    + 'La página se recargará con esa cuenta. Para usar dos cuentas a la vez, '
                    + 'abre la otra en otro navegador o en una ventana de incógnito.');
                window.location.reload();
                throw new Error('Cambio de usuario');
            }
            if (r.status === 403) {
                // El token de seguridad cambió (la sesión se renovó en otra pestaña)
                window.alert('Tu sesión se renovó en otra pestaña. La página se recargará.');
                window.location.reload();
                throw new Error('Sesión renovada');
            }
            return r.text().then(function (texto) {
                return { ok: r.ok, status: r.status, texto: texto };
            });
        });
    }

    // ------------------------------------------------------------------
    // Navegación por secciones (#seccion?parametros)
    // ------------------------------------------------------------------
    function rutaActual() {
        var hash = window.location.hash.replace(/^#/, '');
        return hash || SECCION_INICIAL;
    }

    function marcarMenu(seccion) {
        var titulo = null;
        document.querySelectorAll('.nav-item[data-seccion]').forEach(function (a) {
            var activo = a.getAttribute('data-seccion') === seccion;
            a.classList.toggle('active', activo);
            if (activo) {
                a.setAttribute('aria-current', 'page');
                titulo = a.getAttribute('data-titulo');
            } else {
                a.removeAttribute('aria-current');
            }
        });
        if (titulo && encabezado) {
            encabezado.textContent = titulo;
            document.title = 'FinanZ · ' + titulo;
        }
    }

    function mostrarHtml(html) {
        destruirGraficos();
        contenido.innerHTML = html;
        inicializarSeccion();
    }

    function cargarSeccion() {
        var ruta = rutaActual();
        var seccion = ruta.split('?')[0];
        marcarMenu(seccion);
        cerrarMenuMovil();
        contenido.innerHTML = '<p class="py-10 text-center text-sm muted">Cargando...</p>';

        peticion(ruta).then(function (r) {
            mostrarHtml(r.ok ? r.texto : (r.texto || '<p class="mensaje-error">No se pudo cargar la sección.</p>'));
            window.scrollTo(0, 0);
            actualizarContadorAlertas();
        }).catch(function (error) {
            console.error(error);
            contenido.innerHTML = '<p class="mensaje-error">No se pudo cargar la sección. Revisa tu conexión e intenta de nuevo.</p>';
        });
    }

    window.addEventListener('hashchange', cargarSeccion);

    // ------------------------------------------------------------------
    // Formularios: GET cambia la ruta, POST se envía por AJAX
    // ------------------------------------------------------------------
    contenido.addEventListener('submit', function (e) {
        var form = e.target;
        if (form.tagName !== 'FORM' || form.hasAttribute('data-nativo')) {
            return;
        }
        e.preventDefault();

        var confirmacion = form.getAttribute('data-confirmar');
        if (confirmacion && !window.confirm(confirmacion)) {
            return;
        }

        var datos = new URLSearchParams(new FormData(form));
        var accion = form.getAttribute('action');

        if ((form.getAttribute('method') || 'get').toLowerCase() === 'get') {
            var destino = '#' + accion + '?' + datos.toString();
            if (window.location.hash === destino) {
                cargarSeccion();
            } else {
                window.location.hash = destino;
            }
            return;
        }

        var boton = form.querySelector('[type="submit"]');
        if (boton) {
            boton.disabled = true;
        }
        // Se conservan los parámetros de la ruta actual (ej. el mes o la lección abierta)
        var query = rutaActual().split('?')[1];
        if (query) {
            new URLSearchParams(query).forEach(function (valor, clave) {
                if (!datos.has(clave)) {
                    datos.append(clave, valor);
                }
            });
        }

        peticion(accion, {
            method: 'POST',
            body: datos,
            headers: { 'X-CSRF-Token': csrf }
        }).then(function (r) {
            mostrarHtml(r.texto || '<p class="mensaje-error">Ocurrió un error al guardar. Intenta de nuevo.</p>');
            var exito = contenido.querySelector('.mensaje-exito');
            if (exito) {
                toast(exito.textContent.trim());
            }
            var primerMensaje = contenido.querySelector('.mensaje, .mensaje-error');
            if (primerMensaje) {
                primerMensaje.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
            actualizarContadorAlertas();
        }).catch(function (error) {
            console.error(error);
            toast('Ocurrió un error al guardar. Intenta de nuevo.');
            if (boton) {
                boton.disabled = false;
            }
        });
    });

    // Enlaces internos de las secciones: <a data-navegar="educacion?leccion=2">
    contenido.addEventListener('click', function (e) {
        var enlace = e.target.closest('[data-navegar]');
        if (enlace) {
            e.preventDefault();
            var destino = '#' + enlace.getAttribute('data-navegar');
            if (window.location.hash === destino) {
                cargarSeccion();
            } else {
                window.location.hash = destino;
            }
            return;
        }

        var abrir = e.target.closest('[data-abrir-modal]');
        if (abrir) {
            var modal = document.getElementById(abrir.getAttribute('data-abrir-modal'));
            if (modal) {
                // Copia data-campo-* del botón a los inputs del modal (ej. id de la meta)
                Object.keys(abrir.dataset).forEach(function (clave) {
                    if (clave.indexOf('campo') === 0) {
                        var nombre = clave.substring(5).charAt(0).toLowerCase() + clave.substring(6);
                        var campo = modal.querySelector('[data-rellenar="' + nombre + '"]');
                        if (campo) {
                            if ('value' in campo) {
                                campo.value = abrir.dataset[clave];
                            } else {
                                campo.textContent = abrir.dataset[clave];
                            }
                        }
                    }
                });
                modal.classList.remove('hidden');
                var primerCampo = modal.querySelector('input:not([type=hidden]), select');
                if (primerCampo) {
                    primerCampo.focus();
                }
            }
            return;
        }

        if (e.target.closest('[data-cerrar-modal]') || e.target.classList.contains('modal')) {
            var abierto = e.target.closest('.modal');
            if (abierto) {
                abierto.classList.add('hidden');
            }
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal:not(.hidden)').forEach(function (m) { m.classList.add('hidden'); });
            cerrarPanelAlertas();
            cerrarMenuMovil();
        }
    });

    // ------------------------------------------------------------------
    // Inicialización de cada sección cargada
    // ------------------------------------------------------------------
    function inicializarSeccion() {
        iconos();
        actualizarCategoriasPorTipo();
        aplicarFiltros();
        dibujarGraficos();
        mostrarModoSimulador();
    }

    // ----- Transacciones: categorías según tipo y filtros -----
    function actualizarCategoriasPorTipo() {
        var selectTipo = document.getElementById('tipo-movimiento');
        var selectCategoria = document.getElementById('select-categoria');
        if (!selectTipo || !selectCategoria) {
            return;
        }
        var esIngreso = selectTipo.value === 'INGRESO';
        var categorias = [];
        try {
            categorias = JSON.parse(selectCategoria.getAttribute(esIngreso ? 'data-ingreso' : 'data-gasto') || '[]');
        } catch (error) {
            categorias = [];
        }
        selectCategoria.innerHTML = '';
        categorias.forEach(function (c) {
            var opcion = document.createElement('option');
            opcion.value = c.id;
            opcion.textContent = c.nombre;
            selectCategoria.appendChild(opcion);
        });
        var fuente = document.getElementById('campo-fuente');
        if (fuente) {
            fuente.classList.toggle('hidden', !esIngreso);
        }
        var tipoCategoria = document.querySelector('#modal-nueva-categoria select[name="tipoCategoria"]');
        if (tipoCategoria) {
            tipoCategoria.value = selectTipo.value;
        }
    }

    function aplicarFiltros() {
        var lista = document.getElementById('lista-movimientos');
        if (!lista) {
            return;
        }
        var valor = function (id) { return (document.getElementById(id) || {}).value || ''; };
        var texto = valor('filtro-descripcion').trim().toLowerCase();
        var tipo = valor('filtro-tipo');
        var categoria = valor('filtro-categoria');
        var desde = valor('filtro-desde');
        var hasta = valor('filtro-hasta');
        var soloHormiga = (document.getElementById('filtro-hormiga') || {}).checked;

        var visibles = 0, ingresos = 0, gastos = 0;
        lista.querySelectorAll('.fila-movimiento').forEach(function (fila) {
            var fecha = fila.getAttribute('data-fecha');
            var coincide =
                (!tipo || fila.getAttribute('data-tipo') === tipo) &&
                (!categoria || fila.getAttribute('data-categoria') === categoria) &&
                (!desde || fecha >= desde) &&
                (!hasta || fecha <= hasta) &&
                (!soloHormiga || fila.getAttribute('data-hormiga') === 'true') &&
                (!texto || fila.getAttribute('data-descripcion').indexOf(texto) !== -1);

            fila.classList.toggle('hidden', !coincide);
            if (coincide) {
                visibles++;
                var monto = parseFloat(fila.getAttribute('data-monto')) || 0;
                if (fila.getAttribute('data-tipo') === 'INGRESO') { ingresos += monto; } else { gastos += monto; }
            }
        });

        var sinResultados = document.getElementById('sin-resultados-filtro');
        if (sinResultados) {
            sinResultados.classList.toggle('hidden', visibles !== 0);
        }
        var totales = document.getElementById('totales-filtro');
        if (totales) {
            var f = function (n) { return 'S/ ' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); };
            totales.textContent = visibles + ' movimiento(s) · Ingresos ' + f(ingresos) + ' · Gastos ' + f(gastos);
        }
    }

    contenido.addEventListener('change', function (e) {
        if (e.target.id === 'tipo-movimiento') {
            actualizarCategoriasPorTipo();
        }
        if (e.target.classList.contains('filtro')) {
            aplicarFiltros();
        }
        if (e.target.name === 'modo') {
            mostrarModoSimulador();
        }
        if (e.target.hasAttribute('data-enviar-al-cambiar')) {
            e.target.form.requestSubmit();
        }
    });

    contenido.addEventListener('input', function (e) {
        if (e.target.classList.contains('filtro')) {
            aplicarFiltros();
        }
    });

    contenido.addEventListener('click', function (e) {
        if (e.target.id === 'btn-limpiar-filtros') {
            contenido.querySelectorAll('.filtro').forEach(function (f) {
                if (f.type === 'checkbox') { f.checked = false; } else { f.value = ''; }
            });
            aplicarFiltros();
        }
    });

    // ----- Simulador: muestra los campos del modo elegido -----
    function mostrarModoSimulador() {
        var elegido = contenido.querySelector('input[name="modo"]:checked');
        if (!elegido) {
            return;
        }
        contenido.querySelectorAll('[data-modo]').forEach(function (bloque) {
            var visible = bloque.getAttribute('data-modo') === elegido.value;
            bloque.classList.toggle('hidden', !visible);
            bloque.querySelectorAll('input').forEach(function (i) { i.disabled = !visible; });
        });
    }

    // ------------------------------------------------------------------
    // Gráficos (Chart.js). Los datos vienen en atributos data-* del <canvas>.
    // ------------------------------------------------------------------
    var PALETA = ['#2f8fd8', '#176b4a', '#e9b949', '#d4553c', '#7c5cc4', '#1aa3a3', '#e07b39', '#5f6d66', '#c2417a', '#3d5a80'];

    function destruirGraficos() {
        graficos.forEach(function (g) { g.destroy(); });
        graficos = [];
    }

    function colorVar(nombre) {
        return getComputedStyle(document.body).getPropertyValue(nombre).trim();
    }

    function dibujarGraficos() {
        if (!window.Chart) {
            // Chart.js aún se está descargando: reintenta en un momento
            if (contenido.querySelector('canvas[data-grafico]')) {
                setTimeout(dibujarGraficos, 150);
            }
            return;
        }
        var texto = colorVar('--muted');
        var linea = colorVar('--line');
        Chart.defaults.font.family = '"DM Sans", sans-serif';
        Chart.defaults.color = texto;

        contenido.querySelectorAll('canvas[data-grafico]').forEach(function (canvas) {
            var tipo = canvas.getAttribute('data-grafico');
            var etiquetas = JSON.parse(canvas.getAttribute('data-etiquetas') || '[]');
            var series = JSON.parse(canvas.getAttribute('data-series') || '[]');
            var colores = (canvas.getAttribute('data-colores') || '').split(',').filter(Boolean);
            var soles = function (v) { return 'S/ ' + Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 }); };

            var config;
            if (tipo === 'dona') {
                config = {
                    type: 'doughnut',
                    data: {
                        labels: etiquetas,
                        datasets: [{ data: series[0].valores, backgroundColor: PALETA, borderColor: colorVar('--surface'), borderWidth: 2 }]
                    },
                    options: {
                        maintainAspectRatio: false,
                        cutout: '62%',
                        plugins: {
                            legend: { position: 'bottom', labels: { boxWidth: 12, padding: 12 } },
                            tooltip: { callbacks: { label: function (c) { return ' ' + c.label + ': ' + soles(c.parsed); } } }
                        }
                    }
                };
            } else {
                config = {
                    type: tipo === 'linea' ? 'line' : 'bar',
                    data: {
                        labels: etiquetas,
                        datasets: series.map(function (s, i) {
                            var color = colores[i] || PALETA[i % PALETA.length];
                            return {
                                label: s.nombre,
                                data: s.valores,
                                backgroundColor: tipo === 'linea' ? color + '33' : color,
                                borderColor: color,
                                borderRadius: 6,
                                fill: tipo === 'linea',
                                tension: 0.3,
                                pointRadius: tipo === 'linea' ? 2 : 0
                            };
                        })
                    },
                    options: {
                        maintainAspectRatio: false,
                        interaction: { mode: 'index', intersect: false },
                        plugins: {
                            legend: { display: series.length > 1, position: 'bottom', labels: { boxWidth: 12 } },
                            tooltip: { callbacks: { label: function (c) { return ' ' + c.dataset.label + ': ' + soles(c.parsed.y); } } }
                        },
                        scales: {
                            x: { grid: { display: false } },
                            y: { beginAtZero: true, grid: { color: linea }, ticks: { callback: function (v) { return 'S/ ' + v; } } }
                        }
                    }
                };
            }
            graficos.push(new Chart(canvas, config));
        });
    }

    // ------------------------------------------------------------------
    // Alertas (campana)
    // ------------------------------------------------------------------
    var botonAlertas = document.getElementById('btn-alertas');
    var panelAlertas = document.getElementById('panel-alertas');
    var contadorAlertas = document.getElementById('contador-alertas');

    function actualizarContadorAlertas() {
        peticion('alertas?contador=1').then(function (r) {
            var n = parseInt(r.texto, 10) || 0;
            contadorAlertas.textContent = n > 9 ? '9+' : String(n);
            contadorAlertas.classList.toggle('hidden', n === 0);
        }).catch(function () { /* sin conexión: se ignora */ });
    }

    function cerrarPanelAlertas() {
        if (panelAlertas && !panelAlertas.classList.contains('hidden')) {
            panelAlertas.classList.add('hidden');
            botonAlertas.setAttribute('aria-expanded', 'false');
        }
    }

    botonAlertas.addEventListener('click', function (e) {
        e.stopPropagation();
        if (!panelAlertas.classList.contains('hidden')) {
            cerrarPanelAlertas();
            return;
        }
        panelAlertas.innerHTML = '<p class="text-sm muted">Cargando...</p>';
        panelAlertas.classList.remove('hidden');
        botonAlertas.setAttribute('aria-expanded', 'true');
        peticion('alertas').then(function (r) {
            panelAlertas.innerHTML = r.texto;
            iconos();
            // Al abrir el panel, las alertas se dan por leídas
            return peticion('alertas', { method: 'POST', headers: { 'X-CSRF-Token': csrf } });
        }).then(function () {
            contadorAlertas.classList.add('hidden');
        }).catch(function (error) { console.error(error); });
    });

    document.addEventListener('click', function (e) {
        if (panelAlertas && !panelAlertas.contains(e.target) && e.target !== botonAlertas) {
            cerrarPanelAlertas();
        }
    });

    // ------------------------------------------------------------------
    // Menú lateral en celulares
    // ------------------------------------------------------------------
    var sidebar = document.getElementById('sidebar');
    var overlay = document.getElementById('sidebar-overlay');

    function abrirMenuMovil() {
        sidebar.classList.add('abierto');
        overlay.classList.add('visible');
    }

    function cerrarMenuMovil() {
        sidebar.classList.remove('abierto');
        overlay.classList.remove('visible');
    }

    document.getElementById('btn-abrir-menu').addEventListener('click', abrirMenuMovil);
    document.getElementById('btn-cerrar-menu').addEventListener('click', cerrarMenuMovil);
    overlay.addEventListener('click', cerrarMenuMovil);

    // ------------------------------------------------------------------
    // Modo oscuro (se recuerda en el navegador)
    // ------------------------------------------------------------------
    if (leerPreferencia('finanz-modo') === 'oscuro') {
        document.body.classList.add('dark');
    }
    document.addEventListener('click', function (e) {
        if (e.target.closest('.theme-toggle-btn')) {
            document.body.classList.toggle('dark');
            guardarPreferencia('finanz-modo', document.body.classList.contains('dark') ? 'oscuro' : 'claro');
            // Redibuja los gráficos con los colores del tema
            destruirGraficos();
            dibujarGraficos();
        }
    });

    // ------------------------------------------------------------------
    // Arranque
    // ------------------------------------------------------------------
    iconos();
    window.addEventListener('load', iconos);
    cargarSeccion();
});
