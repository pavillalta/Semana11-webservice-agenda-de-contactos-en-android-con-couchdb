package com.example.semana11_webservice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    ProgressDialog progreso; //variable para barra de progreso
    JSONArray datosJSON; //guardar datos con formato de arreglo
    JSONObject jsonObject;
    Bundle parametros = new Bundle();
    int posicion = 0;

    //Leer texto input stream
    InputStreamReader isReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtenerDatos myAsync = new obtenerDatos();
        myAsync.execute();

        FloatingActionButton btn = (FloatingActionButton) findViewById(R.id.btnAgregar);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parametros.putString("accion", "nuevo");
                nuevo_agenda();
            }
        });
    }

    public void nuevo_agenda() {
        Intent agregar_agenda = new Intent(MainActivity.this, Agregar_agenda.class);
        agregar_agenda.putExtras(parametros);
        startActivity(agregar_agenda);
    }

     public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        try {
            datosJSON.getJSONObject(info.position);
            menu.setHeaderTitle(datosJSON.getJSONObject(info.position).getJSONObject("value").getString("nombre").toString() );
            posicion = info.position;
        }catch (Exception ex) {
            //error....
            Toast.makeText(MainActivity.this,"Error: " + ex.getMessage().toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnxAgregar:
                parametros.putString("accion", "nuevo");
                nuevo_agenda();
                return true;

            case R.id.mnxModificar:
                parametros.putString("accion", "modificar");
                try {
                    parametros.putString("valores", datosJSON.getJSONObject(posicion).getJSONObject("value").toString());
                    nuevo_agenda();
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this,"Error: " + ex.getMessage().toString(),Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.mnxEliminar:
                JSONObject miData = new JSONObject();
                try {
                    miData.put("_id", datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id"));
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this,"Error: " + ex.getMessage().toString(),Toast.LENGTH_LONG).show();
                }
                eliminarDatos objEliminar = new eliminarDatos();
                objEliminar.execute(miData.toString() );
                return true;
        }
        return super.onContextItemSelected(item);
    }


     private class obtenerDatos extends AsyncTask<Void,Void,String> {
        HttpURLConnection urlConnection;
         @Override
         protected String doInBackground( Void... params) {
             StringBuilder result = new StringBuilder();

            try {
                //conectarse con el servidor
                URL url=new URL("http://127.0.0.1:5984/agenda/_design/agenda/_view/mi-agenda");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                //URL url =new URL("http://pro2:pro2@10.0.2.2:5984/db_agenda/_design/agenda/_view/mi-agenda");
                //URL url =new URL("http://192.168.0.103:5984/db_agenda/_design/agenda/_view/mi_agenda");
                //URL url=new URL("http://10.0.2.2:5984/db_agenda");
                //URL url=new URL("http://127.0.0.1:5984/db_agenda");
                //URL url =new URL("http://192.168.1.8:5984/db_agenda/_design/agenda/_view/mi-agenda");
                //URL url =new URL("http://192.168.1.8:5984/db_agenda/");

                InputStream in = new BufferedInputStream ( urlConnection.getInputStream () );
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null){
                    result.append(line);
                }
            }catch (Exception ex){
                Log.e("Mi Error", "Error", ex);
                ex.printStackTrace();
            }finally{
                urlConnection.disconnect();
            }
            return result.toString();
    }


    @Override
    public void onPostExecute(String s){
        super.onPostExecute(s);
        try {
            jsonObject = new JSONObject(s);
            datosJSON = jsonObject.getJSONArray("rows");

            ListView lstAgenda = (ListView)findViewById(R.id.ltsagenda);

            final ArrayList<String> alAgenda = new ArrayList<String>();
            final ArrayAdapter<String> aaAgenda = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, alAgenda);
            lstAgenda.setAdapter(aaAgenda);

            for(int i = 0; i<datosJSON.length(); i++){
                alAgenda.add(datosJSON.getJSONObject(i).getJSONObject("value").getString("nombre").toString() );
            }
            aaAgenda.notifyDataSetChanged();
            registerForContextMenu(lstAgenda);
        }catch (Exception ex){
            Toast.makeText(MainActivity.this,"Error: " + ex.getMessage().toString(),Toast.LENGTH_LONG).show();
        }
    }
    } //cierre del método obtener datos


private class eliminarDatos extends AsyncTask<String, String,String> {
    HttpURLConnection urlConnection;
    @Override
    protected String doInBackground(String... params){
        StringBuilder result = new StringBuilder();
        String JsonResponse = null;
        String JsonDATA = params[0];
        BufferedReader reader = null;

        try {
            //conexion al servidor
            //String uri = "http://10.0.2.2:5984/db_agenda/"+
            String uri = "http://127.0.0.1:5984/agenda/"+
                    datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id")+"?rev="+
                    datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_rev");
            URL url = new URL(uri);

            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ( (line = reader.readLine()) != null ){
                result.append(line);
            }
        }catch (Exception ex){
                Log.e("Mi error", "Error", ex);
                ex.printStackTrace();
            } finally{
                urlConnection.disconnect();
            }
            return  result.toString();
        }

        @Override
        protected void onPostExecute(String s){
        super.onPostExecute(s);

        try {
            JSONObject jsonObject = new JSONObject(s);

            if(jsonObject.getBoolean("ok")){
                Toast.makeText(MainActivity.this, "Registro Eliminado con exito.", Toast.LENGTH_LONG).show();
                Intent regresar = new Intent(MainActivity.this, MainActivity.class);
                startActivity(regresar);
            }else{
                Toast.makeText(MainActivity.this, "Error al intentar eliminar registro.", Toast.LENGTH_LONG).show();
            }
        }catch (Exception ex){
            Toast.makeText(MainActivity.this, "Error al intentar conectar al servidor." + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
        }

        }
    }

//llave de cierre final
}