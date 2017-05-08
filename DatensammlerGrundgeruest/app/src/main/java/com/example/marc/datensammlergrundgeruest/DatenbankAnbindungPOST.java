package com.example.marc.datensammlergrundgeruest;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/*

 Mit dieser Klasse soll eine Verbindung zum REST-Interface aufgebaut werden. Mithilfe dieses
 REST-Interface sollen Sensorwerte oder Positionierungen an das REST-Interface als JSON geschickt werden.
 Wichtig ist hierbei, das Erben von der Klasse AsyncTask damit die Datenbank-Verbindung im Hintergrund
 durchgeführt wird.

*/

public class DatenbankAnbindungPOST extends AsyncTask {

    URL url;
    boolean beendet = false;
    String temp = "";

    @Override
    protected Object doInBackground(Object[] params) {
        String urlstring = "http://pi-bo.dd-dns.de:8080/ContextAware/api/v1/data"; // URL der REST-API

        String requestBody = (String) params[0]; // Das JSON das übertragen werden soll ist der ausgelesene Übergabeparameter.

        try {
            url = new URL(urlstring);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Aufbauen der URL-Verbindung
            conn.setRequestMethod("POST");                                     // Festlegen des HTTP-Request
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");        // Festlegen, dass ein JSON übertragen wird
            OutputStream outputStream = new BufferedOutputStream(conn.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"));
            writer.write(requestBody);   // JSON wird übertragen
            writer.flush();
            writer.close();
            outputStream.close();

            InputStream inputStream;
            // get stream
            if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }
            conn.disconnect(); // Trennung der Verbindung

            // Fehler - und Ergebnisausgabe im Log.d()
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while ((temp = bufferedReader.readLine()) != null) {
              Log.d("Ergebnis: ",temp);
            }

            beendet = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
