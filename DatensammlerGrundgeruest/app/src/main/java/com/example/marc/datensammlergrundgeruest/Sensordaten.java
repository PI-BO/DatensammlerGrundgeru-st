package com.example.marc.datensammlergrundgeruest;

import android.hardware.Sensor;

import java.util.LinkedList;

/*

  Diese Klasse soll dazu dienen die Sensoren zusammen mit den ausgelesenen Sensorwerten
  zu verwalten. Für jeden Sensor der später ausgelesen wird, soll eine Instanz dieser Klasse
  erstellt werden. Für Sensoren mit drei Sensorwerten werden drei Listen (erhalteneWerteX,erhalteneWerteY
  und erhalteneWerteZ) initialisert, für Sensoren mit nur einem Sensorwert wird nur die Liste erhalteneWerteX
  initialisiert. Zudem kann für jeden Sensorwert ein Prefix festgelegt werden. Im Fall der Sensoren
  mit 3 Sensorwerten sind dies die Achsenbeschriftungen für X,Y, Z-Achse. Dieser Prefix wird später für
  die Ausgabe der aktuellen Sensorwerte noch benötigt. Zudem erhält jeder Sensor einen Namen und eine boolean-
  Variable mit der für jeden Sensor festgelegt werden kann, ob die Sensorwerte in der Datenbank gespeichert
  werden sollen.

 */

public class Sensordaten {

    Sensor sensor;
    boolean aufzeichnung;
    int anzahlwerte;
    String prefix[];
    String name;
    LinkedList<Double> erhaltenewerteX;
    LinkedList<Double> erhaltenewerteY;
    LinkedList<Double> erhaltenewerteZ;

    public Sensordaten(Sensor s, String n, int a, String p){
        sensor = s;
        aufzeichnung = true;
        name = n;
        anzahlwerte = a;

        if(anzahlwerte == 3) {
            erhaltenewerteX = new LinkedList<Double>();
            erhaltenewerteY = new LinkedList<Double>();
            erhaltenewerteZ = new LinkedList<Double>();
            prefix = new String[3];
            prefix[0] = "X";
            prefix[1] = "Y";
            prefix[2] = "Z";
        }
        else{
            erhaltenewerteX = new LinkedList<Double>();
            prefix = new String[1];
            prefix[0] = p;
        }

    }

}
