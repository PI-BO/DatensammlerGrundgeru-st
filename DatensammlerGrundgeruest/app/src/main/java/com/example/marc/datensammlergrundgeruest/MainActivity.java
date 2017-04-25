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
import java.util.List;

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


    int intervall;
    LinkedList<Sensordaten> sensoren;

    boolean datensammlungAktiv;  // true bzw. false je nachdem ob die Datensammlung gestartet ist oder nicht
    Handler handler;             // wird benötigt um
    Runnable werteAnDBschicken;      // dieses Runnable immer in einem bestimmten Intervall auszuführen
                                     // Dieses Runnable soll genutzt werden um die Sensorwerte an die Datenbank in einem bestimmten Intervall zu schicken.

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
        sbIntervall = (SeekBar) findViewById(R.id.sbIntervall);

        datensammlungAktiv = false; // Zu Beginn findet noch keine Datensammlung statt
        handler = new Handler();

        // Anlegen eines Sensormanagers
        // Dieser wird genutzt um später die einzelnen Sensoren am Sensor-Listener zu registrieren
        SensorManager sensorman = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensoren = sensorenErstellen(sensorman); // Füllen der Sensordatenliste mit den Sensoren, deren Werte ausgelesen werden sollen
        LinkedList<String> sensornamen = new LinkedList<String>();

        for(Sensordaten s: sensoren){
            sensornamen.add(s.name);
           // sensorman.registerListener(this,s.sensor,sensorman.SENSOR_DELAY_NORMAL);
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sensornamen);   // Der Spinner für die Sensorauswahl erhält alle Sensornamen als Inhalt
        spinnerSensoren.setAdapter(dataAdapter);

        spinnerSensoren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Hier muss programmiert werden was passieren soll, wenn der Nutzer den Sensor im Spinner wechselt

                //cbDatensammeln.setChecked(sensoren.get(position).aufzeichnung);
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
                // Hier wird programmiert, was passieren soll, falls die CheckBox mit der Option, ob für einen Sensor
                // Daten aufgezeichnet werden sollen, betätigt wird.

             /* int position = spinnerSensoren.getSelectedItemPosition();   // Position des in der Sensorauswahl ausgewählten Sensor herausfinden
                sensoren.get(position).aufzeichnung = isChecked;    // Position wird genutzt um den Sensor aus der Sensordatenliste zu erhalten und das Attribut aufzeichnung zu ändern
               */
            }
        });


        //Starten bzw. Stoppen der Datensammlung
        bttnDatensammlung.setOnClickListener(new View.OnClickListener() {
         @Override
             public void onClick(View v) {
                    // Hier muss programmiert werden, was passieren soll wenn die Datensammlung gestartet, bzw. später wieder gestoppt wird.
             }
        });

        // Hiermit kann der Nutzer das Intervall mithilfe einer verschiebaren SeekBar einstellen.
        sbIntervall.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Hier wird programmiert, was passieren soll wenn der Nutzer mit der SeekBar ein neues Intervall einstellt.

               /* intervall = progress + 1; // Wird benötigt, da es ansonsten ein Intervall von 0 Sekunden gibt, dass für Fehler sorgt.
                tvIntervallanzeige.setText("Intervall: " + intervall + " Sekunden"); */
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

        sbIntervall.setProgress(2);

    }

    @Override
    public void onSensorChanged(SensorEvent event) { // In dieser Methode wird festgelegt was passieren soll falls sich der Wert eines Sensors verändert

        /*int listenposition = spinnerSensoren.getSelectedItemPosition();        // Der in der Sensorauswahl ausgewählte Sensor wird gspeichert
        String ausgabe = "";

        if (event.sensor.getType() == sensoren.get(listenposition).sensor.getType()) {
            if (sensoren.get(listenposition).anzahlwerte == 3) {   // Wenn der ausgewählte Sensor, ein Sensor mit 3 Achsen ist...
                ausgabe = "Werte: \n" + sensoren.get(listenposition).prefix[0] + ": " + event.values[0] + "\n" +
                        sensoren.get(listenposition).prefix[1] + ": " + event.values[1] + "\n" +                // Ausgabe für einen Sensor mit 3 Achsen
                        sensoren.get(listenposition).prefix[2] +": "+ event.values[2];
            } else {                                                       // Wenn der ausgewählte Sensor, ein Sensor mit nur 1 Achse ist...
                ausgabe = "Wert: \n" + sensoren.get(listenposition).prefix[0]+": " + event.values[0];               // Ausgabe für einen Sensor mit 1 Achse
            }
            tvSensordaten.setText(ausgabe); // Ausgabe wird dargestellt
        }

        Sensordaten neuewerte = sensordatenFinden(event.sensor.getType());   // Der Sensor für den neue Werte ausgelesen wurden, wird ermittelt mithilfe der Funktion SensordatenFinden()
        if(neuewerte.anzahlwerte == 3){     // Wenn der Sensor ein Sensor mit 3 Achsen ist...
            neuewerte.erhaltenewerteX.add((double) event.values[0]);        //
            neuewerte.erhaltenewerteY.add((double) event.values[1]);        // Die Werte für alle 3 Achse werden den zugehörigen Listen hinzugefügt.
            neuewerte.erhaltenewerteZ.add((double) event.values[2]);        //
        }
        else{
            neuewerte.erhaltenewerteX.add((double) event.values[0]);        // Der Wert für eine Achse wird der zugehörigen Liste hinzugefügt.
        } */


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // wird nicht benötigt
    }

    public LinkedList<Sensordaten> sensorenErstellen(SensorManager sensorman){    // Hinzufügen der Sensoren zur Sensorliste, als Objekte der Klasse Sensordaten
        LinkedList<Sensordaten> sensordatenliste = new LinkedList<Sensordaten>();
        // Objekte der Klasse Sensordaten mit dem Konstruktor Sensordaten(Sensor,Name für den Sensor,Anzahl der Werte, Prefix bei Sensoren mit nur einem Wert)

        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_LIGHT),"Lichtsensor",1,"Lichteinfall"));
        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),"Beschleunigung",3,null));
        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),"Lineare Beschleunigung",3,null));
        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_PRESSURE),"Luftdruck",1,"Luftdruck"));
        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),"Magnetisches Feld",3,null));
        sensordatenliste.add(new Sensordaten(sensorman.getDefaultSensor(Sensor.TYPE_GYROSCOPE),"Gyroskop",3,null));
        return sensordatenliste;
    }

    public Sensordaten sensordatenFinden(int sensortyp){
        for(Sensordaten sd : sensoren){        // Zu der ID gehörender Sensor wird aus der Liste herausgesucht
            if(sd.sensor.getType() == sensortyp){
                return sd;
            }
        }
        return null;
    }
}
