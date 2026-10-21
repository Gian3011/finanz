document.addEventListener('DOMContentLoaded', function () {
    if (window.lucide) {
        lucide.createIcons();
    }

    const contenido = document.getElementById('contenido');
    const encabezado = document.getElementById('page-heading');

    function activarEnlaces(url) {
        document.querySelectorAll('.nav-item[data-url]').forEach(function (a) {
            a.classList.toggle('active', a.getAttribute('data-url') === url);
        });
    }

    function cargarSeccion(url, titulo) {
        contenido.innerHTML = '<p class="py-10 text-center text-sm" style="color:var(--muted)">Cargando...</p>';

        fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
            .then(function (respuesta) {
                if (!respuesta.ok) {
                    throw new Error('Error del servidor: ' + respuesta.status);
                }
                return respuesta.text();
            })
            .then(function (html) {
                contenido.innerHTML = html;
                activarEnlaces(url);
                if (titulo && encabezado) {
                    encabezado.textContent = titulo;
                }
                inicializarTransacciones();
                if (window.lucide) {
                    lucide.createIcons();
                }
            })
            .catch(function (error) {
                contenido.innerHTML = '<p class="mensaje-error">No se pudo cargar la sección. Intenta de nuevo.</p>';
                console.error(error);
            });
    }

    // ===== Transacciones: categoría dinámica según tipo, modal de nueva
    // categoría y filtros del historial. Se vuelve a invocar cada vez que
    // se carga/recarga el fragmento (puede no haber nada de esto en el DOM
    // en otras secciones, por eso cada función comprueba que el elemento exista). =====
    function actualizarCategoriasPorTipo() {
        const selectTipo = document.getElementById('tipo-movimiento');
        const selectCategoria = document.getElementById('select-categoria');
        if (!selectTipo || !selectCategoria) {
            return;
        }

        const atributo = selectTipo.value === 'INGRESO' ? 'data-ingreso' : 'data-gasto';
        let categorias = [];
        try {
            categorias = JSON.parse(selectCategoria.getAttribute(atributo) || '[]');
        } catch (error) {
            categorias = [];
        }

        selectCategoria.innerHTML = '';
        categorias.forEach(function (c) {
            const opcion = document.createElement('option');
            opcion.value = c.id;
            opcion.textContent = c.nombre;
            selectCategoria.appendChild(opcion);
        });
    }

    function abrirModalCategoria() {
        const modal = document.getElementById('modal-nueva-categoria');
        if (modal) {
            modal.classList.remove('hidden');
        }
    }

    function cerrarModalCategoria() {
        const modal = document.getElementById('modal-nueva-categoria');
        if (modal) {
            modal.classList.add('hidden');
        }
    }

    // ===== Presupuestos: modal de "presupuesto mensual" y de "límite por
    // categoría". Mismo criterio: solo actúan si el elemento existe. =====
    function abrirModalPorId(id) {
        const modal = document.getElementById(id);
        if (modal) {
            modal.classList.remove('hidden');
        }
    }

    function cerrarModal(modal) {
        if (modal) {
            modal.classList.add('hidden');
        }
    }

    function aplicarFiltros() {
        const lista = document.getElementById('lista-movimientos');
        if (!lista) {
            return;
        }

        const descripcion = (document.getElementById('filtro-descripcion') || {}).value || '';
        const tipo = (document.getElementById('filtro-tipo') || {}).value || '';
        const categoria = (document.getElementById('filtro-categoria') || {}).value || '';
        const fecha = (document.getElementById('filtro-fecha') || {}).value || '';

        const textoDescripcion = descripcion.trim().toLowerCase();
        let visibles = 0;

        lista.querySelectorAll('.fila-movimiento').forEach(function (fila) {
            const coincide =
                (!tipo || fila.getAttribute('data-tipo') === tipo) &&
                (!categoria || fila.getAttribute('data-categoria') === categoria) &&
                (!fecha || fila.getAttribute('data-fecha') === fecha) &&
                (!textoDescripcion || fila.getAttribute('data-descripcion').indexOf(textoDescripcion) !== -1);

            fila.classList.toggle('hidden', !coincide);
            if (coincide) {
                visibles++;
            }
        });

        const sinResultados = document.getElementById('sin-resultados-filtro');
        if (sinResultados) {
            sinResultados.classList.toggle('hidden', visibles !== 0);
        }
    }

    function inicializarTransacciones() {
        actualizarCategoriasPorTipo();
        aplicarFiltros();
    }

    contenido.addEventListener('click', function (e) {
        if (e.target.closest('#btn-nueva-categoria')) {
            abrirModalCategoria();
        }
        if (e.target.closest('#btn-cerrar-categoria') || e.target.id === 'modal-nueva-categoria') {
            cerrarModalCategoria();
        }

        if (e.target.closest('#btn-presupuesto-mensual')) {
            abrirModalPorId('modal-presupuesto-mensual');
        }

        const botonAbrirLimite = e.target.closest('#btn-limite-categoria');
        if (botonAbrirLimite && !botonAbrirLimite.disabled) {
            abrirModalPorId('modal-limite-categoria');
        }

        const botonCerrarModal = e.target.closest('.btn-cerrar-modal');
        if (botonCerrarModal) {
            cerrarModal(botonCerrarModal.closest('.fixed.inset-0'));
        }

        if (e.target.id === 'modal-presupuesto-mensual' || e.target.id === 'modal-limite-categoria') {
            cerrarModal(e.target);
        }
    });

    contenido.addEventListener('change', function (e) {
        if (e.target.id === 'tipo-movimiento') {
            actualizarCategoriasPorTipo();
        }
        if (e.target.classList.contains('filtro')) {
            aplicarFiltros();
        }
    });

    contenido.addEventListener('input', function (e) {
        if (e.target.classList.contains('filtro')) {
            aplicarFiltros();
        }
    });

    function enviarFormulario(form) {
        // URLSearchParams envía application/x-www-form-urlencoded en vez de
        // multipart/form-data: así request.getParameter() en los Servlets
        // sigue funcionando sin necesitar @MultipartConfig.
        const datos = new URLSearchParams(new FormData(form));

        fetch(form.getAttribute('action'), {
            method: form.getAttribute('method') || 'POST',
            body: datos,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(function (respuesta) {
                if (!respuesta.ok) {
                    throw new Error('Error del servidor: ' + respuesta.status);
                }
                return respuesta.text();
            })
            .then(function (html) {
                contenido.innerHTML = html;
                inicializarTransacciones();
                if (window.lucide) {
                    lucide.createIcons();
                }
            })
            .catch(function (error) {
                contenido.innerHTML = '<p class="mensaje-error">Ocurrió un error al guardar. Intenta de nuevo.</p>';
                console.error(error);
            });
    }

    // Navegación del menú (sidebar de escritorio + barra inferior en móvil)
    document.querySelectorAll('.nav-item[data-url]').forEach(function (enlace) {
        enlace.addEventListener('click', function (e) {
            e.preventDefault();
            cargarSeccion(enlace.getAttribute('data-url'), enlace.getAttribute('data-titulo'));
        });
    });

    // Delegación de eventos: cualquier <form> que aparezca dentro de #contenido
    // (incluso los que se cargan después, dinámicamente) se envía por AJAX.
    contenido.addEventListener('submit', function (e) {
        if (e.target.tagName === 'FORM') {
            e.preventDefault();
            enviarFormulario(e.target);
        }
    });

    // Modo oscuro (se recuerda en el navegador con localStorage).
    // Cualquier botón con la clase "theme-toggle-btn" alterna el tema,
    // incluso si aparece dentro de un fragmento cargado por AJAX (ej. Ajustes).
    if (localStorage.getItem('finanz-modo') === 'oscuro') {
        document.body.classList.add('dark');
    }
    document.addEventListener('click', function (e) {
        const boton = e.target.closest('.theme-toggle-btn');
        if (boton) {
            document.body.classList.toggle('dark');
            localStorage.setItem(
                'finanz-modo',
                document.body.classList.contains('dark') ? 'oscuro' : 'claro'
            );
        }
    });
});


