package com.mycompany.finanz.dao;

import java.sql.Connection;
import java.sql.SQLException;

public class ConexionTest {
    public static void main(String[] args) throws SQLException {
        //INVOCA MÉTODO DE COMPONENTE DE CONEXIÓN A BD
        Connection cn = ConexionDB.conectar();
        System.out.println("CONEXION EXITOSA");
    }
    
}
