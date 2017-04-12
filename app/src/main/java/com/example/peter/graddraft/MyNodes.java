package com.example.peter.graddraft;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peter.graddraft.data.DataContract;
import com.example.peter.graddraft.data.DataProvider;
import com.example.peter.graddraft.data.GradDbHelper;
import com.example.peter.graddraft.data.URLs;
import com.example.peter.graddraft.gcm.GcmIntentService;
import com.example.peter.graddraft.sync.GradSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.peter.graddraft.R.layout.nodes;

/**
 * Created by Peter on 4/14/2016.
 */
public class MyNodes extends AppCompatActivity implements MqttCallback{

    public Context c = this;
    public static boolean OFFLINE_FLAG;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder mBuilder;
    public static MqttClient client;
    public SharedPreferences preferencesForMqtt;
    public static ListView listView;
    public static String MQTT_BROKER="tcp://192.168.105.1:1883";
    public static String MQTT_IP="192.168.105.1";//"tcp://test.mosquitto.org:1883"
    String user;
    public static CustomListAdapter adapter;
    public static Switch offline_switch;
    public static boolean CONNECTED_FLAG=false;

    ////////////////////////////GCM VARIABLES //////////////////////////////////////////
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String TAG = "GCM Demo";
    String SENDER_ID = "265970664615";
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    String regid;

    ////////////////////////////GCM VARIABLES //////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(nodes);
        OFFLINE_FLAG=false;
        try {
            client = new MqttClient(MQTT_BROKER, "my_Client", new MemoryPersistence());
        } catch (MqttException e) {
            _("FAILED TO CREATE CLIENT");
            e.printStackTrace();
        }
        ;
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {

            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(c);

            if (regid.isEmpty()) {
                registerInBackground();
            }

        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////


        ContentResolver resolver;
        GradSyncAdapter syncAdapter = new GradSyncAdapter(this.getApplicationContext(), true);
        resolver = getContentResolver();
        DataObserver dataObserver = new DataObserver(new Handler());
        resolver.registerContentObserver(DataProvider.CONTENT_URI, true,
                dataObserver);

        mBuilder = new NotificationCompat.Builder(this);

        listView = (ListView) findViewById(R.id.mylist);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.header, listView, false);
        ViewGroup footer= (ViewGroup) inflater.inflate(R.layout.footer, listView, false);
        LinearLayout footer_lo=(LinearLayout)footer.findViewById(R.id.logout);
        TextView headerTitle = (TextView) header.findViewById(R.id.usernode);
        TextView footerTitle= (TextView) footer.findViewById(R.id.logout_text);
        offline_switch=(Switch)header.findViewById(R.id.offline_switch);
        offline_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    OFFLINE_FLAG = true;
                    _("Offline Flag Set");
                    NewClient();
//            adapter.notifyDataSetChanged();
                } else {
                    OFFLINE_FLAG = false;
                    _("OFFLINE FLAG NOT SET");
                    try {

                        client.disconnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        Typeface custom_font = Typeface.createFromAsset(getAssets(), "BodoniFLF-Bold.ttf");
        headerTitle.setTypeface(custom_font);
        footerTitle.setTypeface(custom_font);
        footer_lo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(c);
                    alert.setTitle("Logout");
                    alert.setMessage("Logging out will remove all saved data \nAre you sure ?");

                    alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            Intent intent = new Intent(c,MainActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            SharedPreferences preferences;
                            preferences= getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);

                            SharedPreferences.Editor editor = preferences.edit();
                            editor.clear();
                            editor.commit();
                            Toast.makeText(c, "Logged Out",
                                    Toast.LENGTH_LONG).show();
                            startActivity(intent);

                        }
                    });

                    alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    alert.show();


                }



            }
        });
        offline_switch.setTypeface(custom_font);
        listView.addHeaderView(header, null, false);
        listView.addFooterView(footer, null, false);
        if (ContentResolver.isSyncPending(syncAdapter.getSyncAccount(c), c.getString(R.string.content_authority))||
                ContentResolver.isSyncActive(syncAdapter.getSyncAccount(c), c.getString(R.string.content_authority))) {
           _("ContentResolver SyncPending, canceling");
            ContentResolver.cancelSync(syncAdapter.getSyncAccount(c), c.getString(R.string.content_authority));
        }

        GradSyncAdapter.syncImmediately(this);
        Uri uri = DataProvider.CONTENT_URI;
        Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);
        adapter = new CustomListAdapter(c, cursor);
        _("CURSOR " + String.valueOf(cursor.moveToFirst()));
        _("CURSOR COUNT " + String.valueOf(cursor.getCount()));
        listView.setAdapter(adapter);

///////////////////////////SYNC CODE/////////////////////////////////////
        resolver.setIsSyncable(syncAdapter.getSyncAccount(this.getApplicationContext()), DataProvider.PROVIDER_NAME, 1);
///////////////////////////SYNC CODE/////////////////////////////////////


        Intent resultIntent = new Intent(this, MyNodes.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MyNodes.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        ////////////////////////FLOATING BUTTON/////////////////////

        final List<String> NodeNames = new ArrayList<String>();
        NodeNames.add(getResources().getString(R.string.door));
        NodeNames.add(getResources().getString(R.string.plug));
        NodeNames.add(getResources().getString(R.string.smoke_sensor));
        NodeNames.add(getResources().getString(R.string.gas_sensor));
        NodeNames.add(getResources().getString(R.string.temperature));
        NodeNames.add(getResources().getString(R.string.intrusion_detector));
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.myFAB);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(c);
                    alert.setTitle("Add Node");
                    alert.setMessage("Enter Node Details");

                    LayoutInflater inflater = getLayoutInflater();
                    View dialoglayout = inflater.inflate(R.layout.add_new_node, null);
                    final Spinner spinner = (Spinner) dialoglayout.findViewById(R.id.node_spinner);
                    final EditText idEntry = (EditText) dialoglayout.findViewById(R.id.id_entry);
                    alert.setView(dialoglayout);
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(c, R.layout.spinneritem, NodeNames);
                    spinner.setAdapter(dataAdapter);
                    alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String selection = spinner.getSelectedItem().toString();
                            String serial = idEntry.getText().toString();
                            String newNodeJSON = "";
                            JSONObject newJson = new JSONObject();
                            try {
                                newJson.accumulate("type", selection);
                                newJson.accumulate("active", false);
                                newJson.accumulate("value", 0);
                                newJson.accumulate("serial", serial);
                                newJson.accumulate("alarm", false);
                                newNodeJSON = newJson.toString();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            new AddNewNode(c).execute(newNodeJSON);
                            Toast.makeText(c, "Node Added Successfully",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    alert.show();


                }

            }
        });


        ////////////////////////FLOATING BUTTON/////////////////////


    }

    private void _(String s) {
        Log.d("MY APPLICATION", "NODES" + "######" + s);
    }
    public ListView getList(){
        return listView;
    }


    public static Account getAccount(Context c) {
        Account account = new Account(DataProvider.ACCOUNT, DataProvider.ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) c.getSystemService(Context.ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(account, null, null);
        return account;
    }
 ////////////////////MQTT CALLBACK METHODS////////////////
    @Override
    public void connectionLost(Throwable throwable) {
        _("MQTT CONNECTION LOST");
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        _("MQTT MESSAGE 664446 = " + mqttMessage.toString() + " topic " + s);
        ContentValues values=new ContentValues();
        preferencesForMqtt= getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);
        int id=Integer.valueOf(s.substring(1,s.length()));
        String alarm=new String();
        String value=new String();
        String type;
        if(mqttMessage.toString().equals("1"))
            alarm="true";
        else if(mqttMessage.toString().equals("0"))
            alarm="false";
        else
        {
            value=mqttMessage.toString();//.substring(2,mqttMessage.toString().length()-1);
            values.put(DataContract.VALUE,value);
        }
        values.put(DataContract.ID,id);
        values.put(DataContract.ALARM, alarm);

        getContentResolver().update(DataProvider.CONTENT_URI, values, DataContract.ID + "=?",
                new String[]{id + ""});

        GradDbHelper gradDbHelper=new GradDbHelper(c);
        Cursor cursor=gradDbHelper.getFromDbById(id);
        type=cursor.getString(cursor.getColumnIndex("type"));
        if(cursor.moveToFirst()){
                GcmIntentService.CHANGED_NODE= type;
                GcmIntentService.NOTIFICATION_ID=cursor.getInt(cursor.getColumnIndex("serial"));
                GcmIntentService.NOTIFICATIONS_MAP.put(GcmIntentService.NOTIFICATION_ID, GcmIntentService.CHANGED_NODE);
            mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MyNodes.class), 0);

            android.support.v4.app.NotificationCompat.Builder mBuilder = new android.support.v4.app.NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.smartlogo)
                    .setContentTitle(type)
                    .setAutoCancel(true).setVibrate(new long[]{700, 0, 0, 500});

            ///////////////////////////////// MQTT NOTIFICATIONS /////////////////////////////////////////
            if(type.equals(getResources().getString(R.string.door)))
            {
                String name = preferencesForMqtt.getString(String.valueOf(id),type+" #"+id);
                _("NAME ----- " + name);
                if(alarm.equals("true"))
                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                            .bigText(name+" has been opened")).setContentText(name + " has been opened");
                else
                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                            .bigText(name+" has been closed")).setContentText(name + " has been closed");

                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());
            }
//            else if(type.equals(getResources().getString(R.string.plug)))
//            {
//                String name = preferencesForMqtt.getString(String.valueOf(id),type+" #"+id);
//                if(active.equals("true"))
//                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
//                            .bigText(name+" is now active")).setContentText(name + " is now active");
//                else
//                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
//                            .bigText(name+" is now inactive")).setContentText(name + " is now inactive");
//
//                mBuilder.setContentIntent(contentIntent);
//                mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());
//            }


            else if(type.equals(getResources().getString(R.string.gas_sensor)) || type.equals(getResources().getString(R.string.smoke_sensor)))
            {
                String name = preferencesForMqtt.getString(String.valueOf(id),type+" #"+id);
                if(alarm.equals("true"))
                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                            .bigText(name+" alarm")).setContentText(name + " alarm");
                else
                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                            .bigText(name+": alarm ended")).setContentText(name + ": alarm ended");
                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());

            }

            else if(type.equals(getResources().getString(R.string.intrusion_detector)))
            {
                String name = preferencesForMqtt.getString(String.valueOf(id),type+" #"+id);
                if(alarm.equals("true"))
                {mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                        .bigText("Intrusion at "+name)).setContentText("Intrusion at " + name);
                    mBuilder.setContentIntent(contentIntent);
                    mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());}


                else{}

            }
            else if(type.equals(getResources().getString(R.string.temperature)))
            {
                String name = preferencesForMqtt.getString(String.valueOf(id),type+" #"+id);
                {
                    mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                            .bigText(name + "'s reading : " + value)).setContentText(name + "'s reading : " + value);
                    mBuilder.setContentIntent(contentIntent);
                    mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());}
            }



            else
            {mBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                    .bigText(type+" #"+id+" has been changed"))
                    .setContentText(type + " #" + id + " has been changed");
                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(GcmIntentService.NOTIFICATION_ID, mBuilder.build());
            }


            ///////////////////////////////// MQTT NOTIFICATIONS /////////////////////////////////////////
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
    ////////////////////MQTT CALLBACK METHODS////////////////
    class DataObserver extends ContentObserver {

        public DataObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("OBSERVER", "OBSERVERD CHANGE");
            Uri myuri = DataProvider.CONTENT_URI;
            Cursor cursor = c.getContentResolver().query(myuri, null, null, null, null);
            _("CURSORRR " + cursor.getCount());
            CustomListAdapter adapter = new CustomListAdapter(c, cursor);
            mBuilder.setSmallIcon(R.drawable.smartlogo);
            listView.deferNotifyDataSetChanged();
            listView.setAdapter(adapter);
            Set set = GradSyncAdapter.NOTIFICATIONS_MAP.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Map.Entry mentry = (Map.Entry) iterator.next();
                mBuilder.setContentTitle(mentry.getValue().toString());
                mBuilder.setAutoCancel(true);
                mBuilder.setContentText(mentry.getValue().toString() + " : " + mentry.getKey() + " has been changed!");
                mBuilder.setVibrate(new long[]{700, 0, 0, 500});
                Notification notification = mBuilder.build();
//                if(OFFLINE_FLAG==true)mNotificationManager.notify(Integer.parseInt(mentry.getKey().toString()),notification);
            }
            GradSyncAdapter.NOTIFICATIONS_MAP.clear();
        }
    }

    //////////////////////////////////GCM FUNCTIONS/////////////////////////////////////////////////////////
    private boolean checkPlayServices() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();

            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }

        return true;
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        Log.d("DEMO ACT ", regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private String getRegistrationId(Context context) {

        final SharedPreferences prefs = getGcmPreferences(context);

        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private void registerInBackground() {


        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                HttpURLConnection connection = null;
                String finalJsonString = "";
                JSONObject newJson = new JSONObject();

                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(c);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    _("REG ID " + regid);

                    ///////////////////////////////////////

                    SharedPreferences sharedpreferences = null;
                    _("doInBackG,called");
                    _(params[0] + "");

                    try {
//                        newJson.accumulate("name",sharedpreferences.getString("User",""));
                        newJson.accumulate("registration_id", regid);
                        newJson.accumulate("active", true);

                        finalJsonString = newJson.toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        URL url = new URL(URLs.GCM_URL);
                        _(url + " URL");
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setRequestProperty("authorization", "Basic " + GradSyncAdapter.AUTHENTICATION_KEY);
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                        _("SENT JSON " + finalJsonString);
                        bufferedWriter.write(finalJsonString);
                        bufferedWriter.flush();
                        bufferedWriter.close();
                        String mssg = connection.getResponseMessage();
                        _(mssg);
                        int responseCode = connection.getResponseCode();
                        StringBuilder sb = new StringBuilder();
                        int HttpResult = connection.getResponseCode();
                        _("Code now is " + HttpResult);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    storeRegistrationId(c, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }
        }.execute(null, null, null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MyNodes.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

//////////////////////////////////GCM FUNCTIONS/////////////////////////////////////////////////////////
    public class AddNewNode extends AsyncTask<String, Void, Void> {
        SharedPreferences preferences;
        String jsonResponseString;
        public String AUTHENTICATION_KEY = null;
        public Context context;

        public AddNewNode(Context c) {
            context = c;
        }

        @Override
        protected Void doInBackground(String... params) {
            preferences = context.getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);
            AUTHENTICATION_KEY = preferences.getString("Key", "");
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(URLs.NODES_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("authorization", "Basic " + AUTHENTICATION_KEY);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                bufferedWriter.write(params[0]);
                bufferedWriter.flush();
                bufferedWriter.close();
                String msg = connection.getResponseMessage();
                _(msg);
                int responseCode = connection.getResponseCode();
                StringBuilder sb = new StringBuilder();
                int HttpResult = connection.getResponseCode();
                _("Code now is " + HttpResult);
                if (HttpResult == 200) {
                    _("HTTP IS OK");
                    _(responseCode + "");
                }


                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
    public void NewClient() {
        try {
            MqttConnectOptions connOpts = new MqttConnectOptions();

            connOpts.setCleanSession(true);
//            connOpts.setConnectionTimeout(300);
//            connOpts.setKeepAliveInterval(300);

            /////CHECK CONNECTIVITY//////
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {

                        if( InetAddress.getByName(MQTT_IP).isReachable(2000))
                        { CONNECTED_FLAG=true;
                            _("SOCKET CONNECTED");}
                        else
                        {
                            CONNECTED_FLAG=false;
                            _("SOCKET not CONNECTED");}
                    }catch(Exception e){
                        _("SOCKET EXCEPTION " +e);
                    }
                    return null;
                }
            }.execute().get();

            /////CHECK CONNECTIVITY//////

            if(!CONNECTED_FLAG){
                NotConnected();
            }
            if(OFFLINE_FLAG==true){
                _("CONNECTING MQTT");
                client.connect(connOpts);
            client.setCallback(this);
            Uri myuri = DataProvider.CONTENT_URI;
            Cursor cursor = c.getContentResolver().query(myuri, null, null, null, null);
            if(cursor.moveToFirst()){
                do{
                    client.subscribe("s"+cursor.getInt((cursor.getColumnIndex("serial"))));
                    _("SUBSCRIBED TO "+"s"+cursor.getInt((cursor.getColumnIndex("serial"))));
                }while (cursor.moveToNext());
                cursor.close();
            }
            }
        } catch (MqttException e) {
            _("ERROR "+e);

            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
    public void NotConnected(){
        OFFLINE_FLAG=false;
        offline_switch.setChecked(false);
        Toast.makeText(c,"Cannot reach gateway \n Offline mode is unavailable",Toast.LENGTH_LONG).show();
    }
}