package com.alexmario.proyecto.proyecto_android;

import android.Manifest;
import android.app.Service;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
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
    private PolylineOptions rectOptions;
    private Polyline polyline;
    private boolean rutaIniciada, sonido, vibracion;
    private SharedPreferences prefs;
    private double distancia;
    private Vibrator vibrador;
    private ArrayList<LatLng> latLngList;
    private LatLng latLng;
    private Toast toast;
    private String regex, temporizadorTxt;
    private int temporizador;
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

        rutaIniciada = false;
        regex = "\\d+";
        temporizador = 30;
        latLngList = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrador = (Vibrator) (getSystemService(Service.VIBRATOR_SERVICE));
        cronometro = findViewById(R.id.chronometer2);
        textoDistancia = findViewById(R.id.distanciaView);


        btnRuta = findViewById(R.id.btnRuta);


        btnRuta.setOnClickListener(this);
        sonido = prefs.getBoolean("sonido", false);
        vibracion = prefs.getBoolean("vibracion", false);
        temporizadorTxt = prefs.getString("temporizador", "30");

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


    @Override
    public void onClick(View view) {
        switch (view.getId()) { //Cambiar el tipo de mapa en funcion del boton pulsado.

            case R.id.btnRuta:
                if (rutaIniciada) {
                    cronometro.stop();
                    if (latLngList.size() > 1) {
                        //Mediante el SphericalUtil, se calcula la distancia total dada por los LatLng guardados.
                        //distancia = SphericalUtil.computeLength(latLngList) / 1000;//Dividido entre 1000 para mostrar en km
                        Toast.makeText(getApplicationContext(),
                                "Distancia recorrida: " + String.format("%.2f", distancia) + " kilómetros en "
                                        + cronometro.getText(),
                                Toast.LENGTH_LONG).show();
                    }
                    rutaIniciada = false;
                    btnRuta.setText("Iniciar Ruta");
                } else {
                    if (vibracion || sonido) {
                        if (temporizadorTxt.matches(regex)) {
                            temporizador = Integer.parseInt(temporizadorTxt);
                        }
                    }
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
            rectOptions.add(latLng).width(10)
                    .color(Color.BLUE)
                    .geodesic(false);
            polyline = mMap.addPolyline(rectOptions);

            distancia = SphericalUtil.computeLength(latLngList) / 1000;
            textoDistancia.setText("DISTANCIA: " + String.format("%.2f", distancia) + " km");
        }
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
}