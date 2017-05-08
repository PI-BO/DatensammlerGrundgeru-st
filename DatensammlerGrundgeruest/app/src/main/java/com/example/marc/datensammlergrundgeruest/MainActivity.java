package com.example.marc.datensammlergrundgeruest;

import android.Manifest;
import android.content.DialogInterface;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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


WICHTIG: Bevor gesammelte Daten an die REST-API gesendet und damit in die Datenbank geschrieben werden können,
         muss eine SessionID eingegeben und mit OK bestätigt werden.
         Eine SessionID kann am besten mit einem REST-Client erzeugt werden.
         Ein Tutorial dazu ist im moodle-Kurs zu finden:
         https://moodle.hs-bochum.de/pluginfile.php/73239/mod_resource/content/2/Anleitung_REST_API.pdf
         Für die Generierung einer SessionID sind Schritt 1 und Schritt 2 wichtig.



 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // GUI Elemente
    Spinner spinnerSensoren;
    TextView tvSensordaten;
    TextView tvIntervallanzeige;
    CheckBox cbDatensammeln;
    Button bttnDatensammlung;
    Button bttnOK;
    SeekBar sbIntervall;
    JSONArray jsonArrayMitWerten;
    JSONObject jsonObjekt;
    EditText txtSessionID;


    int intervall;
    int sessionid;
    LinkedList<Sensordaten> sensoren;

    boolean datensammlungAktiv;  // true bzw. false je nachdem ob die Datensammlung gestartet ist oder nicht
    Handler handler;             // wird benötigt um
    Runnable werteSpeichern;      // dieses Runnable immer in einem bestimmten Intervall auszuführen
                                     // Dieses Runnable soll genutzt werden um die Sensorwerte in einem bestimmten Intervall in einem JSON zu speichern.

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
        txtSessionID = (EditText) findViewById(R.id.txtSessionID);
        bttnOK = (Button) findViewById(R.id.bttnOK);


        datensammlungAktiv = false; // Zu Beginn findet noch keine Datensammlung statt
        handler = new Handler();

        jsonArrayMitWerten = new JSONArray();


        // Anlegen eines Sensormanagers
        // Dieser wird genutzt um später die einzelnen Sensoren am Sensor-Listener zu registrieren
        SensorManager sensorman = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensoren = sensorenErstellen(sensorman); // Füllen der Sensordatenliste mit den Sensoren, deren Werte ausgelesen werden sollen
        LinkedList<String> sensornamen = new LinkedList<String>();

        for(Sensordaten s: sensoren){
            sensornamen.add(s.name);
            sensorman.registerListener(this,s.sensor,sensorman.SENSOR_DELAY_NORMAL);
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sensornamen);   // Der Spinner für die Sensorauswahl erhält alle Sensornamen als Inhalt
        spinnerSensoren.setAdapter(dataAdapter);

        spinnerSensoren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Hier muss programmiert werden was passieren soll, wenn der Nutzer den Sensor im Spinner wechselt
                cbDatensammeln.setChecked(sensoren.get(position).aufzeichnung);
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

                int position = spinnerSensoren.getSelectedItemPosition();   // Position des in der Sensorauswahl ausgewählten Sensor herausfinden
                sensoren.get(position).aufzeichnung = isChecked;    // Position wird genutzt um den Sensor aus der Sensordatenliste zu erhalten und das Attribut aufzeichnung zu ändern

            }
        });

        bttnOK.setOnClickListener(new View.OnClickListener() { // Bestätigung der SessionID
                                      @Override
                                      public void onClick(View v) {
                                          try {
                                              sessionid = Integer.parseInt(txtSessionID.getText().toString()); // Session ID wird festgelegt.
                                          } catch (Exception ex) {
                                              Snackbar.make(v, "Session ID ist fehlerhaft!", Snackbar.LENGTH_LONG).show(); // Fehlerausgabe bei Parsing - Fehler.
                                          }
                                          if (sessionid <= 0) {
                                              Snackbar.make(v, "Session ID ist zu klein!", Snackbar.LENGTH_LONG).show();   // Fehlerausgabe bei zu  kleiner SessionID
                                          }
                                          else {
                                              Snackbar.make(v, "Session ID gespeichert!", Snackbar.LENGTH_LONG).show();    // Hinweis das die Session ID gespeichert wurde.
                                              werteSpeichern = new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      long zeit = System.currentTimeMillis();
                                                      try {
                                                          for (Sensordaten s : sensoren) {     // Für jeden Sensor in der Sensorenliste wird nachfolgendes abgefragt...
                                                              if (s.aufzeichnung == true) {    // Wenn Werte für diesen Sensor aufgezeichnet werden sollen...
                                                                  if (s.anzahlwerte == 3) {    // Wenn dieser Sensor 3 Achsen hat...
                                                                      if (s.name == "Beschleunigung") {    // Wenn dieser Sensor der Beschleunigungssensor (Accelerometer) ist ...
                                                                          jsonObjekt = new JSONObject();                       // ... dann werden die zuletzt ausgelesenen Werte des Accelerometer in das JSONArray geschrieben.
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 1);
                                                                          jsonObjekt.put("data", s.erhaltenewerteX.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 2);
                                                                          jsonObjekt.put("data", s.erhaltenewerteY.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 3);
                                                                          jsonObjekt.put("data", s.erhaltenewerteZ.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);

                                                                      } else if (s.name == "Gyroskop") {   // Wenn dieser Sensor das Gyroskop ist...
                                                                          jsonObjekt = new JSONObject();      // dann werden die zuletzt ausgelesenen Werte des Gyroskops in das JSONArray geschrieben.
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 4);
                                                                          jsonObjekt.put("data", s.erhaltenewerteX.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 5);
                                                                          jsonObjekt.put("data", s.erhaltenewerteY.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 6);
                                                                          jsonObjekt.put("data", s.erhaltenewerteZ.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                      }
                                                                      else if (s.name == "Magnetisches Feld") {   // Wenn dieser Sensor der Magnetfeldsensor ist ...
                                                                          jsonObjekt = new JSONObject();          // dann werden die zuletzt ausgelesenen Werte des Magnetfeldsensors in das JSONArray geschrieben.
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 10);
                                                                          jsonObjekt.put("data", s.erhaltenewerteX.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 11);
                                                                          jsonObjekt.put("data", s.erhaltenewerteY.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                          jsonObjekt = new JSONObject();
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 12);
                                                                          jsonObjekt.put("data", s.erhaltenewerteZ.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                      }

                                                                  } else {    // Wenn es ein Sensor mit einer Achse ist...
                                                                      if(s.name == "Lichtsensor") {             // Wenn dieser Sensor der Lichtsensor ist ...
                                                                          jsonObjekt = new JSONObject();        // dann wird der zuletzt ausgelesene Wert dieses Sensors in das JSONArray geschrieben.
                                                                          jsonObjekt.put("sid", sessionid);
                                                                          jsonObjekt.put("vid", 9);
                                                                          jsonObjekt.put("data", s.erhaltenewerteX.getLast());
                                                                          jsonObjekt.put("time", zeit);
                                                                          jsonArrayMitWerten.put(jsonObjekt);
                                                                      }
                                                                  }
                                                              }
                                                          }

                                                      } catch (JSONException e) {
                                                          e.printStackTrace();
                                                      } catch (Exception ex) {

                                                      }
                                                      Log.d("JSON",jsonArrayMitWerten.toString());
                                                      handler.postDelayed(werteSpeichern, intervall * 1000);
                                                  }
                                              };

                                          }
                                      }
                                  });

        //Starten bzw. Stoppen der Datensammlung
        bttnDatensammlung.setOnClickListener(new View.OnClickListener() {
         @Override
             public void onClick(View v) {
                    // Hier muss programmiert werden, was passieren soll wenn die Datensammlung gestartet, bzw. später wieder gestoppt wird.

                    if(datensammlungAktiv == false) {         // Wenn der Nutzer die Datensammlung startet...
                        jsonArrayMitWerten = new JSONArray();                 // Ein neues JSONArray wird angelegt, um dort die ausgelesenen Werte zu speichern.
                        bttnDatensammlung.setText("Datensammlung stoppen");   // Der Button-Text wird geändert.
                        handler.post(werteSpeichern);                         // Runnable werteSpeichern (im ,vom Nutzer eingegebenen Intervall, wird das JSONArray mit den aktuellsten Sensorwerten gefüllt) wird gestartet.
                        datensammlungAktiv = true;                            // Variable für die Unterscheidung zwischen aktiver und inaktiver Datensammlung wird auf true gesetzt
                    }
                    else{                                    // Wenn der Nutzer die Datensammlung stoppt...
                        handler.removeCallbacks(werteSpeichern);             // Runnable werteSpeichern wird gestoppt, Das JSONArray wird nicht weiter mit Werten gefüllt.
                        datensammlungAktiv = false;                          // Variable für die Unterscheidung zwischen aktiver und inaktiver Datensammlung wird auf false gesetzt.

                        bttnDatensammlung.setText("Datensammlung starten");        // Der Button-Text wird geändert.
                        DatenbankAnbindungPOST dbp = new DatenbankAnbindungPOST(); //Datenbankverbindung wird im Hintergrund aufgebaut.
                        dbp.execute(jsonArrayMitWerten.toString());                // JSONArray wird an die REST-API geschickt.
                    }


             }});

        // Hiermit kann der Nutzer das Intervall mithilfe einer verschiebaren SeekBar einstellen.
        sbIntervall.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Hier wird programmiert, was passieren soll wenn der Nutzer mit der SeekBar ein neues Intervall einstellt.

                intervall = progress + 1; // Wird benötigt, da es ansonsten ein Intervall von 0 Sekunden gibt, dass für Fehler sorgt.
                tvIntervallanzeige.setText("Intervall: " + intervall + " Sekunden"); // Intervall anzeigen
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

        sbIntervall.setProgress(2); // Die Seekbar ist auf 3 Sekunden bei Start der Applikation voreingestellt.

    }

    @Override
    public void onSensorChanged(SensorEvent event) { // In dieser Methode wird festgelegt was passieren soll falls sich der Wert eines Sensors verändert

        int listenposition = spinnerSensoren.getSelectedItemPosition();        // Der in der Sensorauswahl ausgewählte Sensor wird gspeichert
        String ausgabe = "";

        if (event.sensor.getType() == sensoren.get(listenposition).sensor.getType()) {
            if (sensoren.get(listenposition).anzahlwerte == 3) {   // Wenn der ausgewählte Sensor, ein Sensor mit 3 Achsen ist...

                // todo: Die ausgelesenen Werte für einen Sensor mit 3 Achsen müssen hier ausgegeben werden.
                ausgabe = "Werte: ";     // Ausgabe für einen Sensor mit 3 Achsen

            } else {// Wenn der ausgewählte Sensor, ein Sensor mit nur 1 Achse ist...

                // todo: Der ausgelesene Wert für einen Sensor mit einer Achse muss hier ausgegeben werden.
                ausgabe = "Wert: ";   // Ausgabe für einen Sensor mit 1 Achse

            }
            tvSensordaten.setText(ausgabe); // Ausgabe wird dargestellt
        }

        Sensordaten neuewerte = sensordatenFinden(event.sensor.getType());   // Der Sensor für den neue Werte ausgelesen wurden, wird ermittelt mithilfe der Funktion SensordatenFinden()

        if(neuewerte.anzahlwerte == 3){     // Wenn der Sensor ein Sensor mit 3 Achsen ist...

            // todo: Die ausgelesenen Werte des Sensors müssen den zugehörigen Listen (eine Liste pro Achse) in neuewerte hinzugefügt werden.

        }
        else{   // Wenn der Sensor ein Sensor mit einer Achse ist

            // todo: Der ausgelesene Wert des Sensors muss der zugehörigen Liste (Liste für die x-Achse) in neuewerte hinzugefügt werden.

        }


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
