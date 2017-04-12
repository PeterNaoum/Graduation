package com.example.peter.graddraft;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.example.peter.graddraft.data.DataContract;
import com.example.peter.graddraft.data.GradDbHelper;
import com.example.peter.graddraft.data.URLs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class GetLogs extends AsyncTask <Integer,Void,String>{
    SharedPreferences preferences;
    String jsonResponseString;
    public static String AUTHENTICATION_KEY=null;
    public Context context;
    public GetLogs(Context c){context=c;}

    @Override
    protected String doInBackground(Integer... params) {

        preferences = context.getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);

        AUTHENTICATION_KEY=preferences.getString("Key", "");
        _("KEY= "+AUTHENTICATION_KEY);
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(URLs.LOGS_URL+params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("authorization", "Basic " + AUTHENTICATION_KEY);
            connection.connect();
            _("CONNECTED     " + connection.getResponseMessage());
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = "";
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            jsonResponseString = buffer.toString();
            _("LOGS REPLY" + jsonResponseString);

//            return jsonResponseString;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            updateDatabase(jsonResponseString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
//////////////////////////////////////////////////////////////////////////

    public void updateDatabase(String string) throws JSONException {

        GradDbHelper dbHelper=new GradDbHelper(context);
        JSONArray jsonArray= null;
        ContentValues values = new ContentValues();
        if(string!=null)jsonArray = new JSONArray(string);
        if(jsonArray!=null){
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject myObject = null;
                try {
                    myObject = jsonArray.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                values.put(DataContract.ID, myObject.getInt("node"));
                values.put(DataContract.ALARM, String.valueOf(myObject.getBoolean("alarm")));
                values.put(DataContract.ACTIVE, String.valueOf(myObject.getBoolean("active")));
                values.put(DataContract.TIME, myObject.getString("time"));
                values.put(DataContract.VALUE, myObject.getString("value"));

                if(!dbHelper.searchLogs(myObject.getInt("node"), myObject.getString("time"))){
                    dbHelper.updateLogs(values);
                    _("VALUES ADDED");
                }
            }
        }
    }
    public void _(String s){
        Log.d("MY APPLICATION", "DATA LOGS" + "######" + s);
    }
}