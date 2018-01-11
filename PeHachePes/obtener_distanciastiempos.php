<?php
/**
 * Obtiene todas los datos de la ruta de la base de datos
 */

require 'Distanciastiempos.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    // Manejar petici�n GET
    $distanciastiempo = Distanciastiempos::getAll();

    if ($distanciastiempo) {

        $datos["estado"] = 1;
        $datos["distanciastiempo"] = $distanciastiempo;

        print json_encode($datos);
    } else {
        print json_encode(array(
            "estado" => 2,
            "mensaje" => "Ha ocurrido un error"
        ));
    }
}

