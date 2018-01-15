package com.alexmario.proyecto.proyecto_android;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

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
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private Button btnRuta;
    private LocationRequest mPeticionUbicacion;
    private GoogleApiClient mGoogleApiClient;
    private Location mUltimaUbicacion;
    private Marker mMarcadorActual;
    private SoundPool soundPool;
    private int carga;
    private PolylineOptions rectOptions;
    private Polyline polyline;
    private boolean rutaIniciada, sonido, vibracion;
    private SharedPreferences prefs;
    private Vibrator vibrador;
    private ArrayList<LatLng> latLngList;
    private LatLng latLng;
    private ObtenerWebService hiloConexion;
    private Toast toast;
    private String regex, temporizadorTxt, minutos;
    private double temporizador, distancia, minutoAux;
    private Chronometer cronometro;
    private TextView textoDistancia;
    private long pausaCronometro;
    private List<Polyline> polylineList;

    @Override
    protected void onPause() {
        super.onPause();
        //Detiene las actualizaciones del mapa cuando la activity no esta activa.
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        minutoAux = 0;
        rutaIniciada = false;
        regex = "\\d+";
        latLngList = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrador = (Vibrator) (getSystemService(Service.VIBRATOR_SERVICE));
        cronometro = findViewById(R.id.chronometer2);
        textoDistancia = findViewById(R.id.distanciaView);
        soundPool = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
        carga = soundPool.load(this, R.raw.stairs, 1);
        btnRuta = findViewById(R.id.btnRuta);


        btnRuta.setOnClickListener(this);
        sonido = prefs.getBoolean("sonido", false);
        vibracion = prefs.getBoolean("vibracion", true);
        temporizadorTxt = prefs.getString("tiempo", "15");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Comprobamos la version de Android, en caso de ser 6.0 o superior, pedimos permisos.
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Permisos de ubicacion/localizacion habilitados.
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            } else {
                //Al no estar habilitados los permisos, procedemos a pedirlos.
                checkLocationPermission();
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true); //Indicamos que queremos que se recoja nuestra ubicacion.
        }
        rectOptions = new PolylineOptions();
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17));
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) { //Cambiar el tipo de mapa en funcion del boton pulsado.

            case R.id.btnRuta:
                if (rutaIniciada) {
                    cronometro.stop();
                    if (latLngList.size() > 1) {
                        //Mediante el SphericalUtil, se calcula la distancia total dada por los LatLng guardados.
                        distancia = SphericalUtil.computeLength(latLngList) / 1000;//Dividido entre 1000 para mostrar en km
                        Toast.makeText(getApplicationContext(),
                                "Distancia recorrida: " + String.format("%.2f", distancia) + " kilómetros en "
                                        + cronometro.getText(),
                                Toast.LENGTH_LONG).show();
                        if (hayConexion(getApplicationContext())) {//Si estamos conectados a internet
                            hiloConexion = new ObtenerWebService();
                            hiloConexion.execute(
                                    "http://servicioandroid.000webhostapp.com/insertar_distanciatiempo.php",
                                    "1", String.valueOf(distancia), (String) cronometro.getText());
                        }
                    }
                    rutaIniciada = false;
                    btnRuta.setText("Iniciar Ruta");
                } else {
                    cronometro.setBase(SystemClock.elapsedRealtime());
                    cronometro.start();
                    rutaIniciada = true;
                    btnRuta.setText("Finalizar Ruta");
                    if (latLngList.size() > 0) { //Comprobamos que se ha realizado anteriormente una ruta.
                        mMap.clear();//Limpiamos el mapa de marcadores y/o lineas de recorrido.
                    }
                    latLngList.clear();
                }
                break;
        }
    }

    private void vibrarOSonar() {
        if (vibracion || sonido)
            if (temporizadorTxt.matches(regex)) {
                temporizador = Double.parseDouble(temporizadorTxt);
            }
        minutos = (String) cronometro.getText().subSequence(0, 2);
        if (Double.parseDouble(minutos) % temporizador == 0 && Double.parseDouble(minutos) != minutoAux) {
            if (vibracion) {
                vibrador.vibrate(500);
            }
            if (sonido) {
                soundPool.play(carga, 1, 1, 0, 0, 1);
            }
            minutoAux = Double.parseDouble(minutos);
        }
    }

    protected synchronized void buildGoogleApiClient() { //necesitamos la api de Google para ciertas funciones con los mapas.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)   //En este caso, necesitamos la api de google para recoger nuestra ubicacion
                .addOnConnectionFailedListener(this) // junto con la api FusedLocationProviderApi.
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mPeticionUbicacion = new LocationRequest();
        mPeticionUbicacion.setInterval(1000); //Ajustamos los parametros del LocationRequest para
        mPeticionUbicacion.setFastestInterval(1000); //Concretar el modo de uso del GPS y cada cuanto se actualiza.
        mPeticionUbicacion.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) { //Comprobamos que tenemos permisos de ubicacion/Localizacion.
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mPeticionUbicacion, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Metodo necesario.
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Metodo necesario.
    }

    @Override
    public void onLocationChanged(Location location) {
        mUltimaUbicacion = location;
        if (mMarcadorActual != null) {
            mMarcadorActual.remove(); //Borramos marcadores anteriores para no saturar el fragment del mapa.
        }

        //Colocamos el marcador en la ubicacion actual dada por la API de Google y nuestro GPS.
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions(); //Ajustamos el LatLng a las coordenadas que devuelve el GPS.
        markerOptions.position(latLng);
        markerOptions.title("Posicion actual"); //Titulo para el marcador.
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)); //Icono del marcador.
        mMarcadorActual = mMap.addMarker(markerOptions); //Añadimos el marcador al fragment del mapa.

        //Centramos la camara en nuestra ubicacion actual.
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Añadimos el LatLng actual a la lista para, al acabar, obtener una distancia total
        latLngList.add(latLng);

        //Dibujamos la ruta
        if (rutaIniciada) {
            rectOptions.add(latLng).width(15)
                    .color(Color.BLUE)
                    .geodesic(false);
            polyline = mMap.addPolyline(rectOptions);

            distancia = SphericalUtil.computeLength(latLngList) / 1000;
            textoDistancia.setText("DISTANCIA: " + String.format("%.2f", distancia) + " km");
        }
        vibrarOSonar();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    //Comprobacion de permisos.
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
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
                    int horas = 0;
                    int minutos = 0;
                    int segundos = 0;

                    if (strings[3].length() == 7) {
                        horas = Integer.parseInt(strings[3].substring(0, 1));
                        minutos = Integer.parseInt(strings[3].substring(2, 4));
                        segundos = Integer.parseInt(strings[3].substring(5, 7));
                    } else {
                        minutos = Integer.parseInt(strings[3].substring(0, 2));
                        segundos = Integer.parseInt(strings[3].substring(3, 5));
                    }

                    if (minutos > 0)
                        segundos += (minutos * 60) + (horas * 3600);

                    jsonParam.put("distancia", Double.parseDouble(strings[2]));
                    jsonParam.put("tiempo", segundos);
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

