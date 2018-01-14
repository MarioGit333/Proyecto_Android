<?php
/**
 * Obtiene todas los datos de la ruta de la base de datos
 */

require 'Usuariosapp.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    // Manejar petici�n GET
    $usuariosapp = Usuariosapp::getAll();

    if ($usuariosapp) {

        $datos["estado"] = 1;
        $datos["usuariosapp"] = $usuariosapp;

        print json_encode($datos);
    } else {
        print json_encode(array(
            "estado" => 2,
            "mensaje" => "Ha ocurrido un error"
        ));
    }
}

