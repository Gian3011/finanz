package com.mycompany.finanz.test;

import com.mycompany.finanz.dao.CategoriaDAO;
import com.mycompany.finanz.modelo.Categoria;

import java.util.List;

/**
 * Clase de prueba temporal. NO es parte del sistema final,
 * solo sirve para verificar que el DAO funciona antes de
 * conectarlo a un Servlet. Se puede borrar más adelante.
 */
public class PruebaCategoriaDAO {

    public static void main(String[] args) {
        CategoriaDAO categoriaDAO = new CategoriaDAO();

        try {
            // 1. Insertar categorías de prueba
            Categoria c1 = new Categoria("Alimentación", "GASTO");
            int id1 = categoriaDAO.insertar(c1);
            System.out.println("Categoria insertada con id: " + id1);

            Categoria c2 = new Categoria("Sueldo", "INGRESO");
            int id2 = categoriaDAO.insertar(c2);
            System.out.println("Categoria insertada con id: " + id2);

            // 2. Buscar por id
            Categoria encontrada = categoriaDAO.buscarPorId(id1);
            System.out.println("Encontrada por id: " + encontrada.getNombreCategoria() + " (" + encontrada.getTipo() + ")");

            // 3. Listar todas
            List<Categoria> todas = categoriaDAO.listarTodas();
            System.out.println("Total de categorías: " + todas.size());
            for (Categoria c : todas) {
                System.out.println(" - " + c.getIdCategoria() + ": " + c.getNombreCategoria() + " (" + c.getTipo() + ")");
            }

            // 4. Listar solo por tipo GASTO
            List<Categoria> soloGastos = categoriaDAO.listarPorTipo("GASTO");
            System.out.println("Categorías de tipo GASTO: " + soloGastos.size());

            // 5. Actualizar
            encontrada.setNombreCategoria("Alimentación y bebidas");
            categoriaDAO.actualizar(encontrada);
            Categoria actualizada = categoriaDAO.buscarPorId(id1);
            System.out.println("Nombre actualizado: " + actualizada.getNombreCategoria());

            // 6. Eliminar las categorías de prueba (para no dejar basura en la BD)
            categoriaDAO.eliminar(id1);
            categoriaDAO.eliminar(id2);
            System.out.println("Categorías de prueba eliminadas.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
