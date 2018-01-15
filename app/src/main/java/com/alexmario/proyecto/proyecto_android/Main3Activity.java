package com.alexmario.proyecto.proyecto_android;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main3Activity extends AppCompatActivity {

    private ObtenerWebService hiloConexion;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private static final int SOLICITUD_PERMISO_READ_CONTACTS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_contactos);
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        String IP = "http://servicioandroid.000webhostapp.com";
        String GETUSUARIOS = IP + "/obtener_usuariosapp.php";
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            if (hayConexion(getApplicationContext())) {//Si estamos conectados a internet
                hiloConexion = new Main3Activity.ObtenerWebService();
                hiloConexion.execute(GETUSUARIOS, "1");
            }
        } else {
            solicitarPermiso(Manifest.permission.READ_CONTACTS,
                    "No puedes leer contactos sin permiso", SOLICITUD_PERMISO_READ_CONTACTS, this);
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

    public void solicitarPermiso(final String permiso, String justificacion,
                                 final int requestCode, final Main3Activity actividad) {

        if (ActivityCompat.shouldShowRequestPermissionRationale(actividad,
                permiso)) {
            new AlertDialog.Builder(actividad)
                    .setTitle("Solicitud de permiso")
                    .setMessage(justificacion)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ActivityCompat.requestPermissions(actividad,
                                    new String[]{permiso}, requestCode);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(actividad,
                    new String[]{permiso}, requestCode);
        }
    }

    private List<Usuario> mostrarUsuariosApp() {

        List<Usuario> listaUsuarios = new ArrayList<>();

        String[] projeccion = new String[]{
                ContactsContract.Data._ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE};
        String selectionClause =
                ContactsContract.Data.MIMETYPE + "='" +
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND " +
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL";
        String sortOrder = ContactsContract.Data.DISPLAY_NAME + " ASC";

        Cursor c = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projeccion,
                selectionClause,
                null,
                sortOrder);

        while (c.moveToNext()) {
            Usuario u = new Usuario(c.getString(1), c.getString(2));
            listaUsuarios.add(u);
        }

        c.close();
        return listaUsuarios;
    }

    public class ObtenerWebService extends AsyncTask<String, Void, List<String[]>> {

        @Override
        protected List<String[]> doInBackground(String... params) {

            String cadena = "http://servicioandroid.000webhostapp.com/obtener_usuariosapp.php";
            URL url = null; // Url de donde queremos obtener información
            String[] contacto = new String[3];
            List<String[]> datos = new ArrayList<>();
            List<Usuario> usuarios = new ArrayList<>();
            List<Usuario> contactos = mostrarUsuariosApp();

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
                    // hay contactos a mostrar
                    JSONArray usuarioJSON = respuestaJSON.getJSONArray("usuariosapp");   // estado es el nombre del campo en el JSON
                    for (int i = 0; i < usuarioJSON.length(); i++) {
                        contacto[0] = usuarioJSON.getJSONObject(i).getString("id");
                        contacto[1] = usuarioJSON.getJSONObject(i).getString("nombre");
                        contacto[2] = usuarioJSON.getJSONObject(i).getString("telefono");
                        //datos.add(contacto);
                        Usuario u = new Usuario(contacto);
                        boolean esUsuario = false;
                        for (Usuario user : contactos) {
                            if (user.getNumero().equals(u.getNumero())) {
                                esUsuario = true;
                                usuarios.add(user);
                            }
                        }
                    }
                    List<Usuario> input = usuarios;
                    mAdapter = new UsuariosAdapter(input);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return datos;
        }

        @Override
        protected void onCancelled(List<String[]> s) {
            super.onCancelled(s);
        }

        @Override
        protected void onPostExecute(List<String[]> s) {
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


