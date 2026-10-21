package com.mycompany.finanz.modelo;

import java.time.LocalDateTime;

public class Alerta {

    public static final String CERCA = "CERCA";
    public static final String SUPERADO = "SUPERADO";

    private int idAlerta;
    private int idUsuario;
    private Integer idPresupuesto; // alerta del presupuesto mensual completo
    private Integer idLimite;      // alerta de un límite por categoría
    private String nivel;          // CERCA o SUPERADO
    private String mensaje;
    private LocalDateTime fecha;
    private String estado;         // PENDIENTE o LEIDO

    public Alerta() {
    }

    public Alerta(int idUsuario, Integer idPresupuesto, Integer idLimite, String nivel, String mensaje) {
        this.idUsuario = idUsuario;
        this.idPresupuesto = idPresupuesto;
        this.idLimite = idLimite;
        this.nivel = nivel;
        this.mensaje = mensaje;
        this.estado = "PENDIENTE";
    }

    public boolean isPendiente() {
        return "PENDIENTE".equals(estado);
    }

    public int getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(int idAlerta) {
        this.idAlerta = idAlerta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdPresupuesto() {
        return idPresupuesto;
    }

    public void setIdPresupuesto(Integer idPresupuesto) {
        this.idPresupuesto = idPresupuesto;
    }

    public Integer getIdLimite() {
        return idLimite;
    }

    public void setIdLimite(Integer idLimite) {
        this.idLimite = idLimite;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
