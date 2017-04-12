package com.example.peter.graddraft.data;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;

/**
 * Created by Peter on 5/6/2016.
 */
public class DataProvider extends ContentProvider {
    public static final String PROVIDER_NAME="com.example.peter.graddraft";
    public static final String ACCOUNT = "default";
    public static final String ACCOUNT_TYPE = "default_account";
    public static final String ACCOUNT_PASSWORD= "default_password";
    public static final Account account=new Account(ACCOUNT,ACCOUNT_TYPE);
    static final String URL ="content://"+PROVIDER_NAME+"/nodes";
    public static final Uri CONTENT_URI=Uri.parse(URL);
    static final int MY_NODES=1;
//    private static HashMap<String,String> values;
    static final UriMatcher uriMatcher;
    static {uriMatcher= new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(PROVIDER_NAME,"nodes",MY_NODES);}
    private SQLiteDatabase sqlDB;
    private DataObserver dataObserver=new DataObserver(new Handler());

    @Override
    public boolean onCreate() {
        GradDbHelper dbHelper=new GradDbHelper(getContext());
        sqlDB = dbHelper.getWritableDatabase();
        if(sqlDB!=null){return true;}
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder=new SQLiteQueryBuilder();
        queryBuilder.setTables(DataContract.TABLE_NAME);
        switch (uriMatcher.match(uri)){
            case MY_NODES:
                queryBuilder.appendWhere(DataContract._ID+ " = "+ uri.getLastPathSegment());
//                queryBuilder.setProjectionMap(values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI "+uri);
        }
//        Cursor cursor=queryBuilder.query(sqlDB,projection,selection,selectionArgs,
//                null,null,sortOrder);
        Cursor cursor=sqlDB.query(DataContract.TABLE_NAME,projection,selection,selectionArgs,
                null,null,sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)){
            case MY_NODES:
                return "vnd.android.dir/nodes";

            default:
                throw new IllegalArgumentException("Unsupported URI "+uri);
        }

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID=sqlDB.insert(DataContract.TABLE_NAME,null,values);
        if(rowID>0){
            Uri _uri= ContentUris.withAppendedId(CONTENT_URI,rowID);
            getContext().getContentResolver().notifyChange(uri,dataObserver);
//        getContext().getContentResolver().notifyChange(_uri,null);
//            this.getContext().sendBroadcast(new Intent(DatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
        return _uri;}
        else
        {
//            sqlDB.update(DataContract.TABLE_NAME,values,DataContract.ID,null);
        return null;}
    }
//    public Uri insert(Uri uri, ContentValues values) {
//        long rowID=sqlDB.insert(DataContract.TABLE_NAME,null,values);
//        if(rowID>0){Uri _uri= ContentUris.withAppendedId(CONTENT_URI,rowID);
//            getContext().getContentResolver().notifyChange(_uri,null);
//            this.getContext().sendBroadcast(new Intent(DatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
//            return _uri;}
//        else return null;
//    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted=0;
        switch (uriMatcher.match(uri)){
            case MY_NODES:
               rowsDeleted= sqlDB.delete(DataContract.TABLE_NAME,selection,selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI "+uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
       int rowsUpdated=0;

        switch (uriMatcher.match(uri)){
            case MY_NODES:
                rowsUpdated=sqlDB.update(DataContract.TABLE_NAME,values,selection,selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI "+uri);

        }
//        getContext().getContentResolver().notifyChange(uri,null);
        getContext().getContentResolver().notifyChange(uri,dataObserver);
//        this.getContext().sendBroadcast(new Intent(DatabaseChangedReceiver.ACTION_DATABASE_CHANGED));
        return rowsUpdated;
    }
}
