package com.example.peter.graddraft;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peter.graddraft.data.DataContract;
import com.example.peter.graddraft.data.DataObserver;
import com.example.peter.graddraft.data.DataProvider;
import com.example.peter.graddraft.data.GradDbHelper;
import com.example.peter.graddraft.data.URLs;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;

/**
 * Created by Peter on 7/5/2016.
 */
public class MyLogs extends AppCompatActivity {
    public Context context;
    public SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logs);
        final int id=getIntent().getExtras().getInt("serial");
        final int position=getIntent().getExtras().getInt("position");
        _("POSITION "+position);
        try {
            new GetLogs(this).execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        String type =getIntent().getExtras().getString("type");
        _(String.valueOf(id));
        context = this;
        TextView logTitle = (TextView) findViewById(R.id.log_item_title);
//        ImageView logImage = (ImageView) findViewById(R.id.log_item_image);
        LinearLayout clear = (LinearLayout)findViewById(R.id.clear_history);
        clear.setClickable(true);
        final TableLayout tableLayout=(TableLayout)findViewById(R.id.logs_table);
        Typeface custom_font1 = Typeface.createFromAsset(context.getAssets(), "TheanoDidot-Regular.ttf");
//        Typeface custom_font2 = Typeface.createFromAsset(context.getAssets(), "MontereyFLF-Italic.ttf");
        Typeface custom_font2 = Typeface.createFromAsset(context.getAssets(), "TheanoDidot-Regular.ttf");
        GradDbHelper helper = new GradDbHelper(context);
        Cursor cursor = helper.getLogs(id);
        logTitle.setTypeface(custom_font1);
        SharedPreferences preferences=getSharedPreferences(DataContract.PREFERENCES_MAIN,Context.MODE_PRIVATE);
        logTitle.setText(preferences.getString(String.valueOf(id),type));

        ///////////////////// Header Row //////////////////////
        TextView timeTitle=new TextView(context);
        TextView statusTitle=new TextView(context);
        statusTitle.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, .5f));
        timeTitle.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, .5f));
        timeTitle.setPadding(10, 10, 10, 10);
        statusTitle.setPadding(10, 10, 10, 10);
        timeTitle.setTextSize(30);
        statusTitle.setTextSize(30);
        timeTitle.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        statusTitle.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        timeTitle.setGravity(Gravity.CENTER);
        statusTitle.setGravity(Gravity.CENTER);
        timeTitle.setText("Time");
        if(type.equals(getResources().getString(R.string.temperature)))
            statusTitle.setText("Temp");
        else
            statusTitle.setText("Status");
        timeTitle.setTypeface(custom_font2);
        statusTitle.setTypeface(custom_font2);
        TableRow headerRow=new TableRow(context);
        headerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        headerRow.addView(timeTitle);
        headerRow.addView(statusTitle);
        tableLayout.addView(headerRow);
        ///////////////////// Header Row //////////////////////
        if (cursor.moveToLast()) {

            do {

                TableRow row = new TableRow(context);
                TextView timeView = new TextView(context);
                TextView statusView = new TextView(context);
                row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                timeView.setPadding(10, 10, 10, 10);
                statusView.setPadding(10, 10, 10, 10);
                timeView.setTextSize(25);
                timeView.setGravity(Gravity.CENTER);
                statusView.setGravity(Gravity.CENTER);
                statusView.setTextSize(25);
                timeView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,.5f));
                statusView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,.5f));


                String[] DateTimeString=cursor.getString(cursor.getColumnIndex(DataContract.TIME)).split("T");
                String date=DateTimeString[0];
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                String time=DateTimeString[1].substring(0,DateTimeString[1].indexOf("."));
                Date d = null;
                try {
                    d = df.parse(date+" "+time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Calendar gc = new GregorianCalendar();
                gc.setTime(d);
                gc.add(Calendar.HOUR, 2);
                Date d2 = gc.getTime();
                timeView.setText(d2.toString().substring(0, d2.toString().indexOf("GMT")));
//                timeView.setText(time);
                if(type.equals(getResources().getString(R.string.plug))||type.equals(getResources().getString(R.string.solenoid_lock)))
                    statusView.setText(cursor.getString(cursor.getColumnIndex(DataContract.ACTIVE)));
                else if(type.equals(getResources().getString(R.string.temperature)))
                    statusView.setText(cursor.getString(cursor.getColumnIndex(DataContract.VALUE)));
                else
                    statusView.setText(cursor.getString(cursor.getColumnIndex(DataContract.ALARM)));
//                row.addView(dateView);
                row.addView(timeView);
                row.addView(statusView);
                timeView.setTypeface(custom_font2);
                statusView.setTypeface(custom_font2);
                tableLayout.addView(row);
            }while (cursor.moveToPrevious());
        }

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Clear History");
                alert.setMessage("Are you sure ?");
                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            if(new DeleteLogs().execute(id).get());
                            {int count = tableLayout.getChildCount();
                                for (int i = 1; i < count; i++) {
                                    View child = tableLayout.getChildAt(i);
                                    if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
                                }

                                Toast.makeText(context, "History cleared successfully!",
                                        Toast.LENGTH_LONG).show();}
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });

                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                
                
                
            }
        });

        LinearLayout deleteNode = (LinearLayout)findViewById(R.id.delete_node);
        deleteNode.setClickable(true);
        deleteNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Delete Node");
                alert.setMessage("Are you sure ?");
                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            if(new DeleteNode().execute(id).get());
                            {

//                                   ListView list =new MyNodes().getList();
////                                list.removeViewAt(position);
//                                        View child = list.getChildAt(position);
////                                _(position + " position ");
//                                            ((ViewGroup) child).removeAllViews();
//                                CustomListAdapter adapter=new MyNodes().getAdapter();
//                                adapter.notifyDataSetChanged();
                                GradDbHelper helper=new GradDbHelper(context);
                                helper.deleteNode(id);
                                SharedPreferences.Editor editor=sharedpreferences.edit();
                                editor.remove(String.valueOf(id));
                                editor.commit();

                                ContentResolver resolver;
                                resolver = getContentResolver();
                                DataObserver dataObserver = new DataObserver(new Handler());
                                resolver.registerContentObserver(DataProvider.CONTENT_URI, true,
                                        dataObserver);

                                resolver.notifyChange(DataProvider.CONTENT_URI,dataObserver,true);

//                                ListView list =new MyNodes().getList();
//                                View child = list.getChildAt(position);
//                                ((ViewGroup) child).removeAllViews();

//                                CustomListAdapter adapter=new MyNodes().getAdapter();
//                                adapter.notifyDataSetChanged();

                                _("EXECUTED");
                                Toast.makeText(context, "Node Deleted",
                                        Toast.LENGTH_LONG).show();
                            finish();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });

                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();





            }
        });
        
    }
    public class DeleteLogs extends AsyncTask<Integer,Void,Boolean>
    {
        @Override
        protected Boolean doInBackground(Integer... params) {
            sharedpreferences=context.getSharedPreferences(DataContract.PREFERENCES_MAIN,Context.MODE_PRIVATE);
            HttpURLConnection connection = null;
            boolean ret=false;
            BufferedReader reader = null;
            try {
                URL url = new URL(URLs.DELETE_LOGS_URL+params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("authorization", "Basic " + sharedpreferences.getString("Key",""));
                connection.connect();
                _("CONNECTED     " + connection.getResponseMessage());
                if(connection.getResponseCode()==connection.HTTP_OK)
                {_("CONN OK");
                    GradDbHelper helper=new GradDbHelper(context);
                    helper.deleteLogs(params[0]);
                    ret=true;
                }

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


            return ret;
        }
        }


    public class DeleteNode extends AsyncTask<Integer,Void,Boolean>
    {
        @Override
        protected Boolean doInBackground(Integer... params) {
            sharedpreferences=context.getSharedPreferences(DataContract.PREFERENCES_MAIN,Context.MODE_PRIVATE);
            HttpURLConnection connection = null;
            boolean ret=false;
            BufferedReader reader = null;
            try {
                URL url = new URL(URLs.NODES_URL+params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("authorization", "Basic " + sharedpreferences.getString("Key",""));
                connection.connect();
                _("CONNECTED     " + connection.getResponseMessage());
                if(connection.getResponseCode()==connection.HTTP_OK)
                {_("CONN OK");


                    ret=true;

                }

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


            return ret;
        }
    }


    public void _(String s) {
        Log.d("MY APPLICATION", "MY_LOGS" + "######" + s);
    }

}