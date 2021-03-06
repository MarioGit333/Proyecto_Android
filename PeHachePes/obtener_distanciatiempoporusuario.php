<?php
/**
 * Obtiene el detalle de un alumno especificado por
 * su identificador "idalumno"
 */

require 'Distanciastiempos.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    if (isset($_GET['usuario'])) {

        // Obtener par�metro idalumno
        $parametro = $_GET['usuario'];

        // Tratar retorno
        $distanciastiempo = Distanciastiempos::getById($parametro);


        if ($distanciastiempo) {

            $datos["estado"] = 1;		// cambio "1" a 1 porque no coge bien la cadena.
            $datos["distanciastiempo"] = $distanciastiempo;
            // Enviar objeto json del alumno
            print json_encode($datos);
        } else {
            // Enviar respuesta de error general
            print json_encode(
                array(
                    'estado' => '2',
                    'mensaje' => 'No se obtuvo el registro'
                )
            );
        }

    } else {
        // Enviar respuesta de error
        print json_encode(
            array(
                'estado' => '3',
                'mensaje' => 'Se necesita un identificador'
            )
        );
    }
}

