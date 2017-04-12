/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.peter.graddraft.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.peter.graddraft.MyNodes;
import com.example.peter.graddraft.R;
import com.example.peter.graddraft.data.DataContract;
import com.example.peter.graddraft.data.DataProvider;
import com.example.peter.graddraft.data.GradDbHelper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {

    public static String CHANGED_NODE=null;
    public static int NOTIFICATION_ID;
    public static Date d2;
    public static HashMap<Integer, String> NOTIFICATIONS_MAP = new HashMap<Integer, String>();

    //    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    SharedPreferences preferences;
    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM Demo";

    @Override
    protected void onHandleIntent(Intent intent) {

        preferences=getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);

//        preferences= PreferenceManager.getDefaultSharedPreferences(this);
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                // This loop represents the service doing some work.
//                for (int i = 0; i < 5; i++) {
//                    Log.i(TAG, "Working... " + (i + 1)
//                            + "/5 @ " + SystemClock.elapsedRealtime());
//                    try {
//
//                        Thread.sleep(5000);
//
//                    } catch (InterruptedException e) {
//
//                    }
//                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
//                GradSyncAdapter.syncImmediately(this);

                _( "Received: " + extras.toString());
                
                ///////////////////////READING FROM THE MESSAGE//////////////////////////////////////
                JSONObject object=null;
                GradDbHelper dbHelper=new GradDbHelper(this);
                ContentValues values = new ContentValues();
                SharedPreferences.Editor editor=preferences.edit();
                String messag=extras.getString("message","");
                int id=0;
                String type = null;
                String alarm=null;
                String active=null;
                String DateTimeString=null;
//                String date,time;

                _("YOUR MESSAGE "+messag);
                try {
                    object   =new JSONObject(messag);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    DateTimeString= extras.toString().substring(extras.toString().indexOf("Time: ") + 6,
                            extras.toString().indexOf(", android.support"));
                    id=object.getInt("serial");
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date d = df.parse(DateTimeString);
                    Calendar gc = new GregorianCalendar();
                    gc.setTime(d);
                    gc.add(Calendar.HOUR, 7);
                    d2 = gc.getTime();
                    _("D2 "+d2);
                    editor.putString(String.valueOf(id) + "#time", d2.toString());
                    editor.commit();




                    type=object.getString("type");
                     alarm=object.getString("alarm");
                    active=object.getString("active");
                    values.put(DataContract.ID, String.valueOf(object.getInt("serial")));
                    _("HERE " + String.valueOf(object.getInt("serial")));
                    values.put(DataContract.ACTIVE, String.valueOf(object.getBoolean("active")));
                    _("HERE " + String.valueOf(object.getBoolean("active")));
                    values.put(DataContract.ALARM, String.valueOf(object.getBoolean("alarm")));
                    _("HERE " + String.valueOf(object.getBoolean("alarm")));
                    values.put(DataContract.VALUE, object.getString("value"));

                if(!dbHelper.searchDbById(object.getInt("serial")) && MyNodes.OFFLINE_FLAG==false){
                    getContentResolver().insert(DataProvider.CONTENT_URI, values);
                }

                else if((!dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.ALARM,object.getString("alarm")) ||
                        !dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.ACTIVE,object.getString("active"))
                        ||!dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.TYPE,object.getString("type"))||
                        !dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.VALUE,object.getString("value")))
                                && MyNodes.OFFLINE_FLAG==false )
                {
                    _("ALARM "+(!dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.ALARM,object.getString("alarm"))+"ACTIVE "+
                            !dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.ACTIVE,object.getString("active"))+"TYPE "+
                            !dbHelper.searchDbByColumn(object.getInt("serial"),DataContract.ALARM,object.getString("type"))
                    )+"");
                    _("OBSERVER COLUMN IS CHANGED");
//                getContext().getContentResolver().update()
                    int m=getContentResolver().update(DataProvider.CONTENT_URI, values, DataContract.ID + "=?",
                            new String[]{object.getInt("serial")+""});

                    CHANGED_NODE=object.getString("type");
                    NOTIFICATION_ID=object.getInt("serial");
                    NOTIFICATIONS_MAP.put(NOTIFICATION_ID,CHANGED_NODE);
                    _("OBSERVER ROWS UPDATED "+m+" "+CHANGED_NODE+" "+ object.getString("type"));
                    _("OBSERVER ARRAY SIZE " + NOTIFICATIONS_MAP.size());
                }
                else
                    _("OBSERVER DATA IS NOT CHANGED");


                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                mNotificationManager = (NotificationManager)
                        this.getSystemService(Context.NOTIFICATION_SERVICE);

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, MyNodes.class), 0);
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.smartlogo)
                        .setContentTitle(type)
                        .setSound(soundUri)
                        .setAutoCancel(true).setVibrate(new long[]{700, 0, 0, 500});

//                if(type.equals(R.string.door))
                if(type.equals(getResources().getString(R.string.door)))
                {
                    String name = preferences.getString(String.valueOf(id),type+" #"+id);
                    _("NAME ----- " + name);
                    if(alarm.equals("true"))
                    mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+" has been opened")).setContentText(name + " has been opened");
                    else
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+" has been closed")).setContentText(name + " has been closed");

                    mBuilder.setContentIntent(contentIntent);
                    if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                }
                else if(type.equals(getResources().getString(R.string.plug)))
                {
                    String name = preferences.getString(String.valueOf(id),type+" #"+id);
                    if(active.equals("true"))
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+" is now active")).setContentText(name + " is now active");
                    else
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+" is now inactive")).setContentText(name + " is now inactive");

                    mBuilder.setContentIntent(contentIntent);
                    if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                }


                else if(type.equals(getResources().getString(R.string.gas_sensor)) || type.equals(getResources().getString(R.string.smoke_sensor)))
                {
                    String name = preferences.getString(String.valueOf(id),type+" #"+id);
                    if(alarm.equals("true"))
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+" alarm")).setContentText(name + " alarm");
                    else
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(name+": alarm ended")).setContentText(name + ": alarm ended");
                    mBuilder.setContentIntent(contentIntent);
                    if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

                }

                else if(type.equals(getResources().getString(R.string.intrusion_detector)))
                {
                    String name = preferences.getString(String.valueOf(id),type+" #"+id);
                    if(alarm.equals("true"))
                    {mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Intrusion at "+name)).setContentText("Intrusion at " + name);
                        mBuilder.setContentIntent(contentIntent);
                        if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());}


                    else{}
//                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
//                                .bigText(name+" is now inactive")).setContentText(name + " is now inactive");


                }
                else if(type.equals(getResources().getString(R.string.temperature)))
                {
                    String name = preferences.getString(String.valueOf(id),type+" #"+id);
                    {
                        try {
                            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(name + "'s reading : " + object.getString("value"))).setContentText(name+"'s reading : "+object.getString("value"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mBuilder.setContentIntent(contentIntent);
                        if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());}
                }



                else
                {mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(type+" #"+id+" has been changed"))
                        .setContentText(type + " #" + id + " has been changed");
                    mBuilder.setContentIntent(contentIntent);
                    if(MyNodes.OFFLINE_FLAG==false)mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                }















//                sendNotification(messag);


                ///////////////////////READING FROM THE MESSAGE//////////////////////////////////////
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {

//        mNotificationManager = (NotificationManager)
//                this.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MyNodes.class), 0);
//
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.smartlogo)
//        .setContentTitle("GCM Notification")
//        .setStyle(new NotificationCompat.BigTextStyle()
//                .bigText(msg))
//        .setContentText(msg).setAutoCancel(true).setVibrate(new long[]{700,0,0,500});
//
//        mBuilder.setContentIntent(contentIntent);
//        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    public void _(String s){
        Log.d("MY APPLICATION", "INTENT SERVICE" + "######" + s);
    }
}
