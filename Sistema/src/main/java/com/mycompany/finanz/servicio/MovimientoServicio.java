package com.mycompany.finanz.servicio;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.dao.MovimientoDAO;
import com.mycompany.finanz.modelo.Categoria;
import com.mycompany.finanz.modelo.Gasto;
import com.mycompany.finanz.modelo.Ingreso;
import com.mycompany.finanz.modelo.Movimiento;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro y clasificación de ingresos y gastos. Al registrar un gasto evalúa
 * si es gasto hormiga y si genera alertas de presupuesto.
 */
public class MovimientoServicio {

    private final MovimientoDAO movimientoDAO;
    private final CategoriaDAO categoriaDAO;
    private final GastoHormigaServicio gastoHormigaServicio;
    private final PresupuestoServicio presupuestoServicio;

    public MovimientoServicio() {
        this.movimientoDAO = new MovimientoDAO();
        this.categoriaDAO = new CategoriaDAO();
        this.gastoHormigaServicio = new GastoHormigaServicio(movimientoDAO);
        this.presupuestoServicio = new PresupuestoServicio();
    }

    /**
     * Valida y guarda un movimiento. Devuelve los avisos para el usuario
     * (gasto hormiga detectado, alertas de presupuesto nuevas).
     */
    public List<String> registrar(int idUsuario, String tipo, int idCategoria, BigDecimal monto,
                                  LocalDate fecha, String descripcion, String fuente)
            throws ValidacionException, SQLException {
        if (!"INGRESO".equals(tipo) && !"GASTO".equals(tipo)) {
            throw new ValidacionException("Selecciona si es un ingreso o un gasto.");
        }
        Categoria categoria = categoriaDAO.buscarDisponible(idCategoria, idUsuario);
        if (categoria == null || !tipo.equals(categoria.getTipo())) {
            throw new ValidacionException("Selecciona una categoría válida para el tipo de movimiento.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha no puede ser posterior a hoy.");
        }
        if (fecha.isBefore(LocalDate.now().minusYears(5))) {
            throw new ValidacionException("La fecha es demasiado antigua.");
        }

        List<String> avisos = new ArrayList<>();
        if ("INGRESO".equals(tipo)) {
            String fuenteFinal = fuente == null ? categoria.getNombreCategoria() : fuente;
            movimientoDAO.insertar(new Ingreso(idUsuario, idCategoria, monto, fecha, descripcion, fuenteFinal));
            return avisos;
        }

        Gasto gasto = new Gasto(idUsuario, idCategoria, monto, fecha, descripcion);
        boolean esHormiga = gastoHormigaServicio.evaluarGastoHormiga(gasto);
        gasto.setEsGastoHormiga(esHormiga);
        movimientoDAO.insertar(gasto);

        if (esHormiga) {
            gastoHormigaServicio.marcarPatron(gasto);
            avisos.add("Este gasto se marcó como posible gasto hormiga: ya registraste "
                    + GastoHormigaServicio.UMBRAL_FRECUENCIA + " o más gastos pequeños en "
                    + categoria.getNombreCategoria() + " en los últimos " + GastoHormigaServicio.DIAS_PERIODO + " días.");
        }
        avisos.addAll(presupuestoServicio.verificarAlertas(idUsuario, fecha));
        return avisos;
    }

    public Categoria crearCategoria(int idUsuario, String nombre, String tipo) throws ValidacionException, SQLException {
        String nombreLimpio = Validar.texto(nombre, "nombre de la categoría", 50);
        if (!"INGRESO".equals(tipo) && !"GASTO".equals(tipo)) {
            throw new ValidacionException("Indica un tipo válido (Gasto o Ingreso) para la categoría.");
        }
        if (categoriaDAO.existeNombre(idUsuario, nombreLimpio, tipo)) {
            throw new ValidacionException("Ya tienes una categoría con ese nombre.");
        }
        Categoria categoria = new Categoria(idUsuario, nombreLimpio, tipo);
        categoriaDAO.insertar(categoria);
        return categoria;
    }

    public List<Movimiento> listar(int idUsuario) throws SQLException {
        return movimientoDAO.listarPorUsuario(idUsuario);
    }

    public List<Categoria> categorias(int idUsuario) throws SQLException {
        return categoriaDAO.listarDisponibles(idUsuario);
    }

    public List<Categoria> categoriasPorTipo(int idUsuario, String tipo) throws SQLException {
        return categoriaDAO.listarPorTipo(idUsuario, tipo);
    }

    public boolean eliminar(int idUsuario, int idMovimiento) throws SQLException {
        return movimientoDAO.eliminar(idMovimiento, idUsuario);
    }

    public boolean clasificarHormiga(int idUsuario, int idMovimiento, boolean esHormiga) throws SQLException {
        return movimientoDAO.actualizarFlagHormiga(idMovimiento, idUsuario, esHormiga);
    }
}
