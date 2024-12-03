package com.barber.myapplication;

public class Cita {
    private String id;
    private String nombre;
    private String fecha;
    private String hora;

    public Cita() {
        // Constructor vacío necesario para Firebase
    }

    public Cita(String id, String nombre, String fecha, String hora) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.hora = hora;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }
}
