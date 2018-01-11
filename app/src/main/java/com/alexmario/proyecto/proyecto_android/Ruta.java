package com.alexmario.proyecto.proyecto_android;

/**
 * Created by Alex on 11/01/2018.
 */

public class Ruta {
    private String distancia;
    private String tiempo;

    public Ruta(String distancia, String tiempo) {
        this.distancia = " Distancia: "+distancia;
        this.tiempo = " Tiempo: "+tiempo;
    }

    public String getDistancia() {
        return distancia;
    }

    public String getTiempo() {
        return tiempo;
    }
}
