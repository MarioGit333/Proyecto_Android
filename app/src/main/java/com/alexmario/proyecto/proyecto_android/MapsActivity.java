package com.alexmario.proyecto.proyecto_android;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    Button botonmapa, botonsatelite, botonhibrido, botonInterior;
    LocationRequest mPeticionUbicacion;
    GoogleApiClient mGoogleApiClient;
    Location mUltimaUbicacion;
    Marker mMarcadorActual;

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

        botonmapa = findViewById(R.id.botonmapa);
        botonsatelite = findViewById(R.id.botonsatelite);
        botonhibrido = findViewById(R.id.botonhibrido);
        botonInterior = findViewById(R.id.botonInterior);

        botonmapa.setOnClickListener(this);
        botonsatelite.setOnClickListener(this);
        botonhibrido.setOnClickListener(this);
        botonInterior.setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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


        final PolylineOptions rectOptions = new PolylineOptions();


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                rectOptions.add(latLng).width(10)
                        .color(Color.BLUE)
                        .geodesic(true);
                Polyline polyline = mMap.addPolyline(rectOptions);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(getApplicationContext(), "Me pulsiaste jaj :V", Toast.LENGTH_LONG).show();
                return false;
            }
        });

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) { //Cambiar el tipo de mapa en funcion del boton pulsado.
            case R.id.botonmapa:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.botonsatelite:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.botonhibrido:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.botonInterior:
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-33.86997, 151.2089), 18));
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
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions(); //Ajustamos el LatLng a las coordenadas que devuelve el GPS.
        markerOptions.position(latLng);
        markerOptions.title("Posicion actual"); //Titulo para el marcador.
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)); //Icono del marcador.
        mMarcadorActual = mMap.addMarker(markerOptions); //Añadimos el marcador al fragment del mapa.

        //Centramos la camara en nuestra ubicacion actual.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
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