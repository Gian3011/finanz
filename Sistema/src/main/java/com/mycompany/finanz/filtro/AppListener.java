package com.mycompany.finanz.filtro;

import com.mycompany.finanz.dao.ConexionDB;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Libera el pool de conexiones cuando el servidor detiene o redespliega la aplicación.
 */
@WebListener
public class AppListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ConexionDB.cerrar();
    }
}
