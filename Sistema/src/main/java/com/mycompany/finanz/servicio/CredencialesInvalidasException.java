package com.mycompany.finanz.servicio;

/**
 * Se lanza cuando el correo no existe o la contraseña no coincide.
 * A propósito no se distingue cuál de los dos casos fue, para no
 * darle pistas a un atacante sobre qué correos existen en el sistema.
 */
public class CredencialesInvalidasException extends Exception {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
