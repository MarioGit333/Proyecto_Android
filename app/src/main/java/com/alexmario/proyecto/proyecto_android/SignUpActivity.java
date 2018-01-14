package com.alexmario.proyecto.proyecto_android;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener{

    Button boton;
    EditText nombre,telefono;
    private SignUpActivity.ObtenerWebService hiloConexion;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        boton=findViewById(R.id.botonRegistro);
        telefono=findViewById(R.id.telefono);
        nombre=findViewById(R.id.nombre);

        boton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if (nombre.getText().length()!=0 && telefono.getText().length()==9){
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                    .putBoolean("isFirstRun", false).commit();
            startActivity(new Intent(this, PantallaPrincipal.class));

            String tel,name,id;
            tel=telefono.getText().toString();
            name=nombre.getText().toString();
            id = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            if (hayConexion(getApplicationContext())) {//Si estamos conectados a internet
                hiloConexion = new SignUpActivity.ObtenerWebService();
                hiloConexion.execute(
                        "http://servicioandroid.000webhostapp.com/insertar_usuariosapp.php",
                        "1", id, name,tel);
            }

        }else{
            Toast.makeText(this,
                    "Debes introducir un usuario y número correcto",Toast.LENGTH_LONG).show();
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


    public class ObtenerWebService extends AsyncTask<String, Void, String> {
        String devuelve = "";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        @Override
        protected String doInBackground(String... strings) {
            if (strings[1] == "1") { //insercion
                URL url;

                try {
                    url = new URL(strings[0]);
                    HttpURLConnection urlConn;
                    DataOutputStream printout;
                    DataInputStream input;
                    url = new URL(strings[0]);
                    urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setDoInput(true);
                    urlConn.setDoOutput(true);
                    urlConn.setUseCaches(false);
                    urlConn.setRequestProperty("Content-Type", "application/json");
                    urlConn.setRequestProperty("Accept", "application/json");
                    urlConn.connect();
                    //Creo el Objeto JSON
                    JSONObject jsonParam = new JSONObject();


                    jsonParam.put("id", strings[2]);
                    jsonParam.put("nombre", strings[3]);
                    jsonParam.put("telefono", strings[4]);
                    // Envio los parámetros post.
                    OutputStream os = urlConn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(jsonParam.toString());
                    writer.flush();
                    writer.close();

                    int respuesta = urlConn.getResponseCode();


                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {

                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            result.append(line);
                            //response+=line;
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                        //Accedemos al vector de resultados

                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON

                        if (resultJSON == "1") {      // hay una ruta que mostrar
                            devuelve = "Ruta insertada correctamente";

                        } else if (resultJSON == "2") {
                            devuelve = "La ruta no pudo insertarse";
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return devuelve;
        }
    }
}
