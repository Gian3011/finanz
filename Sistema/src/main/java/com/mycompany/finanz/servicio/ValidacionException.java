package com.mycompany.finanz.servicio;

/**
 * Error de validación de datos ingresados por el usuario. Su mensaje se
 * muestra tal cual en pantalla, por eso debe estar redactado para el usuario.
 */
public class ValidacionException extends Exception {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
