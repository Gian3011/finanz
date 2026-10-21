package com.mycompany.finanz.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    //PARAMETROS DE CONEXION
    private static final String URL="jdbc:mysql://localhost:3306/db_finanz"
            + "?useSSL=false&serverTimezone=UTC";
    private static final String CLAVE=System.getenv().getOrDefault("FINANZ_DB_PASSWORD", "");
    private static final String USUARIO="root";
    
    //CREA MÉTODO QUE RETORNA CONEXIÓN A BD
     public static Connection conectar() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (ClassNotFoundException e) {
            throw new SQLException("DRIVER MYSQL NO ENCONTRADO", e);
        }
    }
}
