package com.example.peter.graddraft.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.example.peter.graddraft.R;
import com.example.peter.graddraft.data.DataContract;
import com.example.peter.graddraft.data.DataProvider;
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
import java.util.HashMap;


public class GradSyncAdapter extends AbstractThreadedSyncAdapter {
//    public final String LOG_TAG = GradSyncAdapter.class.getSimpleName();
    String jsonResponseString;
    public static String AUTHENTICATION_KEY=null;
    public static Boolean UpdateFlag=false;
    public static Account newAccount;
//    public static Boolean accountFlag;
    String myUrl= URLs.NODES_URL;
    public static String CHANGED_NODE=null;
    public static int NOTIFICATION_ID;

    public static HashMap<Integer, String> NOTIFICATIONS_MAP = new HashMap<Integer, String>();

    public GradSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }



    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        _("onPerformSync Called.");

//////////////////////////////////////////////////////////////////////////
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(myUrl);
            _(myUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

//            try{
//                byte[] datanew = ("PeterRa:12345678").getBytes("UTF-8");
//                key = Base64.encodeToString(datanew, Base64.DEFAULT);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }


            _("FOUND HERER " + AUTHENTICATION_KEY);
            connection.setRequestProperty("authorization", "Basic " + AUTHENTICATION_KEY);
            connection.connect();
            _("CONNECTED     " + connection.getResponseMessage());
            InputStream stream = connection.getInputStream();
            _("STREAM");
            reader = new BufferedReader(new InputStreamReader(stream));
            _("READER");
            String line = "";
            _("LINE");
            StringBuffer buffer = new StringBuffer();
            _("BUFFER");
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");


            }
            jsonResponseString = buffer.toString();
            _("THIS THE JSON REPLY" + jsonResponseString);

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
////////////////////////////////////////////////////////////////////////////////////
//        JSONArray jsonArray= null;
//        try {
//            jsonArray = new JSONArray(jsonResponseString);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        for (int i = 0; i < jsonArray.length(); i++) {
//            NodeModel nodeModel = new NodeModel();
//            JSONObject myObject = null;
//            try {
//                myObject = jsonArray.getJSONObject(i);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
////////////////////////////////////////////////////////////////////////////////////

        try {
            updateDatabase(jsonResponseString);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
//////////////////////////////////////////////////////////////////////////

    public void updateDatabase(String string) throws JSONException {

//        ArrayList<NodeModel> nodeModelArrayList = new ArrayList<NodeModel>();
//        public SQLiteDatabase database=new SQLiteDatabase();
        GradDbHelper dbHelper=new GradDbHelper(this.getContext());
        JSONArray jsonArray= null;
        ContentValues values = new ContentValues();
        if(string!=null)jsonArray = new JSONArray(string);
        if(jsonArray!=null){
        for (int i = 0; i < jsonArray.length(); i++) {
//            NodeModel nodeModel=new NodeModel();
            JSONObject myObject = null;
            try {
                myObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            values.put(DataContract.ACTIVE, String.valueOf(myObject.getBoolean("active")));
            values.put(DataContract.ALARM, String.valueOf(myObject.getBoolean("alarm")));
            _("AFFFFF " + String.valueOf(myObject.getBoolean("alarm")));
            values.put(DataContract.DATE_CREATED, myObject.getString("created"));
            values.put(DataContract.OWNER, myObject.getString("owner"));
            values.put(DataContract.TYPE, myObject.getString("type"));
            values.put(DataContract.VALUE, myObject.getString("value"));
            values.put(DataContract.ID, myObject.getInt("serial"));
            _("aa ID " + myObject.getInt("serial"));
            _("aa TYPE " + myObject.getString("type"));
            _("aa ALARM " + myObject.getString("alarm"));

            if(!dbHelper.searchDbById(myObject.getInt("serial"))){

//                Uri uri=
                        getContext().getContentResolver().insert(DataProvider.CONTENT_URI, values);
            }

          else if(!dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.ALARM,myObject.getString("alarm")) ||
                    !dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.ACTIVE,myObject.getString("active"))
                    ||!dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.TYPE,myObject.getString("type")) ||
                    !dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.VALUE,myObject.getString("value")))

            //
            {
                _("ALARM "+(!dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.ALARM,myObject.getString("alarm"))+"ACTIVE "+
                        !dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.ACTIVE,myObject.getString("active"))+"TYPE "+
                        !dbHelper.searchDbByColumn(myObject.getInt("serial"),DataContract.ALARM,myObject.getString("type"))
                )+"");
               _("OBSERVER COLUMN IS CHANGED");
//                getContext().getContentResolver().update()
            int m=getContext().getContentResolver().update(DataProvider.CONTENT_URI, values, DataContract.ID + "=?",
                    new String[]{myObject.getInt("serial")+""});

                CHANGED_NODE=myObject.getString("type");
                NOTIFICATION_ID=myObject.getInt("serial");
                NOTIFICATIONS_MAP.put(NOTIFICATION_ID,CHANGED_NODE);
                _("OBSERVER ROWS UPDATED "+m+" "+CHANGED_NODE+" "+ myObject.getString("type"));
                _("OBSERVER ARRAY SIZE " + NOTIFICATIONS_MAP.size());
           }
            else
                _("OBSERVER DATA IS NOT CHANGED");

//
//            getApplicationContext().getContentResolver().insert(uri,values)
//            nodeModel.setType(myObject.getString("type"));
//            nodeModel.setActive(myObject.getBoolean("active"));
//            nodeModel.setCreated(myObject.getString("created"));
//            nodeModel.setAlarm(myObject.getBoolean("alarm"));
//            nodeModel.setOwner(myObject.getString("owner"));
//            nodeModelArrayList.add(i, nodeModel);
//            dbHelper.addNode(nodeModel);
            }}
//            ContentValues values=new ContentValues();
//            values.put(DataProvider.);
    }



    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Log.d("SYNC", "SYNC IMMID");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
//                DataProvider.PROVIDER_NAME, bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */

    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =(AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
//        Account
        if(newAccount==null)
                newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {
            if (!accountManager.addAccountExplicitly(newAccount, "password", null)) {
                return newAccount;
            }

        }
        return newAccount;


    }
//    public static Account getSyncAccount(Context context) {
//
//        AccountManager am = AccountManager.get(context);
//        accountFlag= am.addAccountExplicitly(newAccount, DataProvider.ACCOUNT_PASSWORD, null);
//        if ()
//        newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));
//
//            return newAccount;
//
////        Account account = new Account(DataProvider.ACCOUNT, context.getString(R.string.sync_account_type));
////            AccountManager am = AccountManager.get(context);
////            boolean accountCreated = am.addAccountExplicitly(account, DataProvider.ACCOUNT_PASSWORD, null);
//
//    }


    public void _(String s){
        Log.d("MY APPLICATION", "SYNC ADAPTER" + "######" + s);
    }
}