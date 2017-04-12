package com.example.peter.graddraft;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.peter.graddraft.data.DataContract;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Peter on 4/26/2016.
 */
public class CustomListAdapter extends BaseAdapter {
    Cursor cursor;

    public static HashMap<Integer, String> TYPES_MAP = new HashMap<Integer, String>();
    public static HashMap<Integer, Boolean> ACTIVE_MAP = new HashMap<Integer, Boolean>();
    public static HashMap<Integer, Boolean> ALARM_MAP= new HashMap<Integer, Boolean>();
    Context myContext;
    public MyNodes myNodes=new MyNodes();
    Boolean activFlag,openFlag;
    SharedPreferences sharedpreferences;
    public static String type;
    static LayoutInflater inflater=null;
    public CustomListAdapter(Context context,Cursor mycursor){
        myContext=context;
        cursor=mycursor;
        inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return cursor.getString(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = null;
//        if (convertView == null) {
            if(cursor.moveToPosition(position)) {
                _("POSITION " + position);
                view = inflater.inflate(R.layout.singlenode, null);
                ImageView image = (ImageView) view.findViewById(R.id.nodeimage);
                ImageView history = (ImageView) view.findViewById(R.id.history_logo);
                TextView recent = (TextView)view.findViewById(R.id.recent_change);
                TextView temperature =(TextView)view.findViewById(R.id.node_temp);
                TextView title = (TextView) view.findViewById(R.id.nodetitle);
                TextView nodeId =(TextView)view.findViewById(R.id.nodeshowid);
                Typeface custom_font1 = Typeface.createFromAsset(myContext.getAssets(), "TheanoDidot-Regular.ttf");
                Typeface custom_font2 = Typeface.createFromAsset(myContext.getAssets(), "MontereyFLF-Italic.ttf");
                title.setTypeface(custom_font1);
                final CheckBox open = (CheckBox) view.findViewById(R.id.open);
                open.setTypeface(custom_font2);
                nodeId.setTypeface(custom_font2);
                recent.setTypeface(custom_font2);
                temperature.setTypeface(custom_font1);
                final int id = cursor.getInt((cursor.getColumnIndex("serial")));
                nodeId.setText(String.valueOf(id));
                type = cursor.getString(cursor.getColumnIndex("type"));



                sharedpreferences= myContext.getSharedPreferences(DataContract.PREFERENCES_MAIN, Context.MODE_PRIVATE);
//                sharedpreferences= PreferenceManager.getDefaultSharedPreferences(myContext);
//                if (sharedpreferences.contains(String.valueOf(id) + "#time"))
                    recent.setText("Last Change: " + sharedpreferences.getString(String.valueOf(id)+ "#time", " "));
//                else
//                    recent.setText("Last Change: ");

                if(sharedpreferences.contains(String.valueOf(id)))
                    title.setText(sharedpreferences.getString(String.valueOf(id),cursor.getString(cursor.getColumnIndex("type"))));
                else
                title.setText(cursor.getString(cursor.getColumnIndex("type")));
//                _("title " + cursor.getString(cursor.getColumnIndex("type")));

                openFlag = Boolean.valueOf(cursor.getString(cursor.getColumnIndex("alarm")));
                activFlag = Boolean.valueOf(cursor.getString(cursor.getColumnIndex("active")));
                ///////////////////////////////
                if (type.equals(myContext.getResources().getString(R.string.plug))) {
                    image.setImageResource(R.drawable.plug);
                    open.setText("Active");
                    open.setClickable(true);
                    temperature.setVisibility(View.INVISIBLE);
                    open.setChecked(activFlag);
                } else if (type.equals(myContext.getResources().getString(R.string.door))) {
                    image.setImageResource(R.drawable.door);
                    temperature.setVisibility(View.INVISIBLE);
                    open.setText("Open");
                    open.setClickable(false);
                    open.setChecked(openFlag);
                } else if (type.equals(myContext.getResources().getString(R.string.intrusion_detector)) || type.equals(myContext.getResources().getString(R.string.gas_sensor))
                        || type.equals(myContext.getResources().getString(R.string.smoke_sensor))) {
                    image.setImageResource(R.drawable.intruder);
                    open.setClickable(false);
                    open.setChecked(openFlag);
                    temperature.setVisibility(View.INVISIBLE);
                    open.setText("Alarm");
                }
                else if (type.equals(myContext.getResources().getString(R.string.temperature))) {
                    image.setImageResource(R.mipmap.temper);
                    open.setClickable(false);
                    temperature.setText(cursor.getString(cursor.getColumnIndex("value")) + "°C");
//                    myNodes.setTemp(cursor.getString(cursor.getColumnIndex("value")) + "°C");
                    open.setVisibility(View.INVISIBLE);

                }


                TYPES_MAP.put(id, type);
                ACTIVE_MAP.put(id, activFlag);
                ALARM_MAP.put(id, openFlag);

                if(type.equals(myContext.getResources().getString(R.string.plug))||type.equals(myContext.getResources().getString(R.string.solenoid_lock)))
                {
                    open.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (MyNodes.OFFLINE_FLAG == false) {
                                String finalJsonString = "";
                                JSONObject newJson = new JSONObject();
                                try {
                                    newJson.accumulate("value", 0);
                                    newJson.accumulate("type", TYPES_MAP.get(id));
                                    newJson.accumulate("serial", id);
                                    newJson.accumulate("active", !ACTIVE_MAP.get(id));
                                    newJson.accumulate("alarm", ALARM_MAP.get(id));
                                    activFlag = !activFlag;
                                    ACTIVE_MAP.put(id, activFlag);
                                    finalJsonString = newJson.toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                SendToServer sendToServer = new SendToServer();
                                sendToServer.setData(finalJsonString);
                                _("DATA " + sendToServer.data);
                                sendToServer.execute(id);
                            }
                            else
                            {
                                try {

//                                    MyNodes.client.connect();
                                    MqttMessage message = new MqttMessage();
                                    byte[] msg;
                                    _("!ACTIVE MAP "+String.valueOf(!ACTIVE_MAP.get(id)));
//                                    if (String.valueOf(!ACTIVE_MAP.get(id)).equals("true"))
                                    if(!open.isChecked())
                                        msg=String.valueOf(1).getBytes();
                                    else
                                        msg=String.valueOf(0).getBytes();
                                    _("!ACTIVE MAP2 "+String.valueOf(!ACTIVE_MAP.get(id)));
                                    message.setPayload(msg);
                                    _("MQTT MESSAGE " + message.toString());
                                    activFlag = !activFlag;

                                    ACTIVE_MAP.put(id, activFlag);
                                    MyNodes.client.publish(String.valueOf(id), message);
                                    ContentValues values=new ContentValues();
                                    values.put(DataContract.ID, id);
                                    _("AVTIVE FLAG " + activFlag);
                                    values.put(DataContract.ACTIVE, activFlag);
//                                    myContext.getContentResolver().update(DataProvider.CONTENT_URI, values, DataContract.ID + "=?",
//                                            new String[]{id + ""});
//                                    open.setChecked(true);
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }

                ////////////////////////////////View Rename/////////////////////////////////////
//                if (view != null) {
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(myContext);
                            alert.setTitle("Rename Node");
                            alert.setMessage("Enter a new name for " + TYPES_MAP.get(id) + " #" + id);
                            final EditText input = new EditText(myContext);
                            alert.setView(input);

                            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    if (!input.getText().toString().isEmpty()) {
                                        editor.putString(String.valueOf(id), input.getText().toString());
                                        editor.commit();
                                    } else
//                                 editor.putString(String.valueOf(id),type);
                                    {
                                        editor.remove(String.valueOf(id));
                                        editor.commit();
                                    }

                                }
                            });

                            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Canceled.
                                }
                            });

                            alert.show();

                            return true;
                        }
                    });
                history.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        new GetLogs(myContext).execute(id);
                        Intent i = new Intent(myContext, MyLogs.class);
                        i.putExtra("serial", id);
                        i.putExtra("position", position);
                        i.putExtra("type", TYPES_MAP.get(id));
                        myContext.startActivity(i);
                    }
                });
//                }
            }

        ////////////////////////////////View Rename/////////////////////////////////////




        return view;
    }


    private void _(String s){
        Log.d("MY APPLICATION", "LIST ADAPTER" + "######" + s);
    }


}

