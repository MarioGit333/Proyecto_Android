package com.alexmario.proyecto.proyecto_android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends AppCompatActivity {

    TextView resultado;
    private ObtenerWebService hiloConexion;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<Ruta> rutas=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        String IP = "http://servicioandroid.000webhostapp.com";
        // Rutas de los Web Services
        String GETRUTAS = IP + "/obtener_distanciastiempos.php";

        if (hayConexion(getApplicationContext())) {//Si estamos conectados a internet
            hiloConexion = new ObtenerWebService();
            hiloConexion.execute(GETRUTAS, "1");
        }
    }

    private boolean hayConexion(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE); //Comprobamos que estamos conectados a internet
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected() || !info.isAvailable()) {
            return false;
        }
        return true;
    }


    public class ObtenerWebService extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... params) {

            String cadena = params[0];
            URL url = null; // Url de donde queremos obtener información
            String devuelve = "";


            if (params[1] == "1") {    // Consulta de todos las rutas

                try {
                    url = new URL(cadena);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Abrir la conexión
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0" +
                            " (Linux; Android 1.5; es-ES) Ejemplo HTTP");
                    //connection.setHeader("content-type", "application/json");

                    int respuesta = connection.getResponseCode();

                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {


                        InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                        // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                        // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                        // StringBuilder.

                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);        // Paso toda la entrada al StringBuilder
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                        //Accedemos al vector de resultados

                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON


                        System.out.println(resultJSON);
                        if (resultJSON.equals("1")) {      // hay rutas a mostrar

                            JSONArray rutasJSON = respuestaJSON.getJSONArray("distanciastiempo");   // estado es el nombre del campo en el JSON

                            for (int i = 0; i < rutasJSON.length(); i++) {


                                int segundos=Integer.parseInt(rutasJSON.getJSONObject(i).getString("tiempo"));
                                int minutos=0;
                                int horas=0;
                                if (segundos>=3600){
                                    horas=segundos/3600;
                                    segundos=segundos%3600;
                                }
                                if (segundos>=60){
                                    minutos=segundos/60;
                                    segundos=segundos%60;
                                }
                                String tiempo;
                                String fecha=rutasJSON.getJSONObject(i).getString("fecha");
                                String muestraMinutos;
                                if (minutos<10)
                                    muestraMinutos="0"+minutos;
                                else
                                    muestraMinutos=""+minutos;

                                if (segundos<10){
                                    tiempo=horas+":"+muestraMinutos+":0"+segundos;
                                }else{
                                    tiempo=horas+":"+muestraMinutos+":"+segundos;
                                }

                                Ruta ruta=new Ruta(rutasJSON.getJSONObject(i).getString("distancia"),
                                        tiempo,fecha);

                                rutas.add(ruta);

                            }

                            List<Ruta> input = rutas;

                            mAdapter = new MyAdapter(input);


                        } else if (resultJSON.equals("2")) {
                            devuelve = "No hay rutas";
                        }


                    }


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return devuelve;


            }
            return "1";
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        @Override
        protected void onPostExecute(String s) {
            //super.onPostExecute(s);
            recyclerView.setAdapter(mAdapter);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }
}
