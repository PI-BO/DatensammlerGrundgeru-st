package com.example.marc.datensammlergrundgeruest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;

/*
Innerhalb dieser Klasse sind alle Hauptfunktionen der Applikation zu finden.
Dazu werden alle Sensoren, deren Werte abgefragt werden sollen, innerhalb dieser Klasse
zusammen mit einem SensorManager an einem SensorEventListener angemeldet. Der SensorEventListener
bietet zwei wichtige Methoden an: onSensorChanged() und onAccuracyChanged()
Genutzt wird in dieser Applikation aber nur die Methode onSensorChanged().
Ergänzend zum SensorManager wird ein LocationManager verwendet um die aktuelle Position herauszufinden.
Alle Sensoren, deren Sensorwerte ausgelesen werden sollen, werden in der Methode SensorenErstellen() am
Ende dieser Klasse festgelegt. Für jeden Sensor wird eine Instanz der Klasse Sensordaten genutzt.

Für das Speichern der aktuellen Sensorwerte ist die Klasse Sensordaten verantwortlich. Für die
Speicherung der aktuellen Positionen ist die Klasse Lokalisierungsdaten zuständig.


 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // GUI Elemente
    Spinner spinnerSensoren;
    TextView tvSensordaten;
    TextView tvIntervallanzeige;
    CheckBox cbDatensammeln;
    Button bttnDatensammlung;
    SeekBar sbIntervall;
    Button bttnKarte;

    int intervall;


    boolean datensammlungAktiv;  // true bzw. false je nachdem ob die Datensammlung gestartet ist oder nicht
    Handler handler;             // wird benötigt um
    Runnable werteabfragen;      // dieses Runnable immer in einem bestimmten Intervall auszuführen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // GUI Elemente
        spinnerSensoren = (Spinner) findViewById(R.id.spinnerSensoren);
        tvSensordaten = (TextView) findViewById(R.id.tvSensordaten);
        tvIntervallanzeige = (TextView) findViewById(R.id.tvIntervallanzeige);
        cbDatensammeln = (CheckBox) findViewById(R.id.cbDatensammeln);
        bttnDatensammlung = (Button) findViewById(R.id.bttnDatensammlung);
        bttnKarte = (Button) findViewById(R.id.bttnKarte);
        sbIntervall = (SeekBar) findViewById(R.id.sbIntervall);

        datensammlungAktiv = false; // Zu Beginn findet noch keine Datensammlung statt
        handler = new Handler();

        // Anlegen eines Sensormanagers
        // Dieser wird genutzt um später die einzelnen Sensoren am Sensor-Listener zu registrieren
        SensorManager sensorman = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Anlegen eines Lokationmanagers
        LocationManager locationMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        SensorenErstellen(sensorman); // Füllen der Sensordatenliste mit den Sensoren, deren Werte ausgelesen werden sollen

        LinkedList<String> sensornamen = new LinkedList<String>();

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sensornamen);   // Der Spinner für die Sensorauswahl erhält alle Sensornamen als Inhalt
        spinnerSensoren.setAdapter(dataAdapter);

        spinnerSensoren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                    // wird nicht benötigt
            }
        });

        // Einstellung ob Daten aufgezeichnet werden sollen oder nicht
        cbDatensammeln.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        //Starten bzw. Stoppen der Datensammlung
        bttnDatensammlung.setOnClickListener(new View.OnClickListener() {
         @Override
             public void onClick(View v) {

             }
        });

        // Wenn auf den Button "Karte" gedrückt wird, öffnet sich eine neue Aktivität die eine Google Maps Karte beinhaltet.
        bttnKarte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Hiermit kann der Nutzer das Intervall mithilfe einer verschiebaren SeekBar einstellen.
        sbIntervall.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                    // wird nicht benötigt
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                    // wird nicht benötigt
            }
        });


        LocationListener locListener = new LocationListener() { // Wird benötigt um die aktuelle Position festzustellen, ein LocationManager meldet sich später an diesem Listener an
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        // Prüfung der Rechte für Lokalisierung
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) { // In dieser Methode wird festgelegt was passieren soll falls sich der Wert eines Sensors verändert

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // wird nicht benötigt
    }

    public void SensorenErstellen(SensorManager sensorman){    // Hinzufügen der Sensoren zur Sensorliste, als Objekte der Klasse Sensordaten

    }
}
