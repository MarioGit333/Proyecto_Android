package com.alexmario.proyecto.proyecto_android;

/**
 * Created by Alex on 14/01/2018.
 */

public class Usuario {
    private String nombre;
    private String numero;

    public Usuario(String nombre, String numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    public Usuario(String[] contacto){
        nombre=contacto[1];
        numero=contacto[2];
    }

    public String getNombre() {
        return nombre;
    }

    public String getNumero() {
        return numero;
    }
}
