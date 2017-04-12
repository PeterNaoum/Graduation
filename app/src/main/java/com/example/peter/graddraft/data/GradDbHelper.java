package com.example.peter.graddraft.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Peter on 5/6/2016.
 */
public class GradDbHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME="favoritesDB";
        public static final int DATABASE_VERSION=7;
        public final ArrayList<NodeModel> myList= new ArrayList<>();


        public GradDbHelper(Context context) {
            super(context,DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String CREATE_NODES_TABLE = "CREATE TABLE " + DataContract.TABLE_NAME + "(" +
                    DataContract.KEY+ " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    DataContract.ID+ " REAL UNIQUE NOT NULL," +
                    DataContract.TYPE + " TEXT NOT NULL," +
                    DataContract.VALUE + " TEXT NOT NULL," +
                    DataContract.OWNER + " TEXT NOT NULL," +
                    DataContract.DATE_CREATED+ " TEXT NOT NULL," +
                    DataContract.ACTIVE + " TEXT NOT NULL," +
                    DataContract.ALARM + " TEXT NOT NULL" +
                    ");";
            final String CREATE_LOGS_TABLE = "CREATE TABLE " + DataContract.LOGS_TABLE + "(" +
                    DataContract.KEY+ " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    DataContract.ID+ " REAL NOT NULL," +
                    DataContract.TIME+ " TEXT NOT NULL," +
                    DataContract.VALUE + " TEXT NOT NULL," +
                    DataContract.ACTIVE + " TEXT NOT NULL," +
                    DataContract.ALARM + " TEXT NOT NULL" +
                    ");";
            db.execSQL(CREATE_LOGS_TABLE);
            db.execSQL(CREATE_NODES_TABLE);
            Log.d("DATABASE", "CREATED");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DataContract.TABLE_NAME);
            onCreate(db);

        }
    public void updateLogs(ContentValues values){
        SQLiteDatabase database=this.getWritableDatabase();
        database.insert(DataContract.LOGS_TABLE, null,values);
        Log.d("LOGS", "INSERTED");
        database.close();
    }

        public void addNode(NodeModel nodeModel){
            SQLiteDatabase database=this.getWritableDatabase();
            ContentValues contentValues= new ContentValues();
            contentValues.put(DataContract.DATE_CREATED,nodeModel.dateCreated);
            contentValues.put(DataContract.OWNER,nodeModel.owner);
            contentValues.put(DataContract.TYPE,nodeModel.type);
            contentValues.put(DataContract.ACTIVE,nodeModel.active);
            contentValues.put(DataContract.ALARM,nodeModel.alarm);
            contentValues.put(DataContract.ID,nodeModel.id);
            Log.d("TILL HERE", "OOK");
            database.insert(DataContract.TABLE_NAME, null, contentValues);
            Log.d("DATABASE", "INSERTED");
            database.close();

        }
        public ArrayList<NodeModel> getNode(){
            String selectAll="SELECT * FROM"+DataContract.TABLE_NAME;
            SQLiteDatabase db= this.getReadableDatabase();
            Cursor cursor=db.query(DataContract.TABLE_NAME,null,null,null,null,null,null);
            if(cursor.moveToFirst()){

                do{
                    NodeModel nodeModel =new NodeModel();
                    nodeModel.setActive(Boolean.valueOf(cursor.getString(cursor.getColumnIndex(DataContract.ACTIVE))));
                    nodeModel.setCreated(cursor.getString(cursor.getColumnIndex(DataContract.DATE_CREATED)));
                    nodeModel.setOwner(cursor.getString(cursor.getColumnIndex(DataContract.OWNER)));
                    nodeModel.setType(cursor.getString(cursor.getColumnIndex(DataContract.TYPE)));
                    nodeModel.setId(cursor.getInt(cursor.getColumnIndex(DataContract.ID)));
                    nodeModel.setAlarm(Boolean.valueOf(cursor.getString(cursor.getColumnIndex(DataContract.ALARM))));
                    myList.add(nodeModel);
                }while(cursor.moveToNext());
            }
            return myList;
        }
        public boolean searchDbById(int s){
            String query = "Select * FROM " + DataContract.TABLE_NAME + " WHERE " + DataContract.ID + " =  \"" + s + "\"";
            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);
            if(cursor.moveToFirst()==true)
            {db.close();
                return true;}
            else
            {db.close(); return false;}
        }
    public Cursor getFromDbById(int s){
        String query = "Select * FROM " + DataContract.TABLE_NAME + " WHERE " + DataContract.ID + " =  \"" + s + "\"";
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()==true)
        {db.close();
            return cursor;}
        else
        {db.close(); return null;}
    }




    public boolean searchDbByColumn(int s,String col,String value){
        String query = "Select * FROM " + DataContract.TABLE_NAME + " WHERE " + DataContract.ID + " =  \"" + s +"\""+" AND "+col+" =  \""
                +value + "\"";
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()==true)
        {db.close();return true;}
        else
        {db.close();    return false;}
    }

    public boolean searchLogs(int id,String value){

        String query = "Select * FROM " + DataContract.LOGS_TABLE + " WHERE " + DataContract.ID + " = " + id +" AND "+DataContract.TIME+" =  \""
                +value + "\"";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()==true)
        {db.close();return true;}
        else
        {db.close();    return false;}
    }


    public Cursor getLogs(int id){
        String query = "Select * FROM " + DataContract.LOGS_TABLE + " WHERE " + DataContract.ID + " = " + id ;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
//        if(cursor.moveToFirst()==true)
//        {
//           db.close();
        Log.d("CURSOR OF LOGS",String.valueOf(cursor.moveToFirst()));
            return cursor;
//        }
//        else
//        {db.close(); return null;}
    }

    public void deleteLogs(int id){
//        String query = "DELETE FROM " + DataContract.LOGS_TABLE + " WHERE " + DataContract.ID + " = " + id ;
        SQLiteDatabase db = this.getWritableDatabase();
//        db.execSQL(query, null);
        Log.d("DELETE LOGS","EXECUTED");
        db.delete(DataContract.LOGS_TABLE,DataContract.ID+"=?", new String[]{String.valueOf(id)});// > 0;

    }
    public void deleteNode(int id){
//        String query = "DELETE FROM " + DataContract.LOGS_TABLE + " WHERE " + DataContract.ID + " = " + id ;
        SQLiteDatabase db = this.getWritableDatabase();
//        db.execSQL(query, null);
        Log.d("DELETE NODES", "EXECUTED");
        db.delete(DataContract.TABLE_NAME, DataContract.ID + "=?", new String[]{String.valueOf(id)});
        db.delete(DataContract.LOGS_TABLE,DataContract.ID+"=?", new String[]{String.valueOf(id)});

    }


    public Cursor searchDbByName(String s){
        String query = "Select * FROM " + DataContract.TABLE_NAME + " WHERE " + DataContract.OWNER + " =  \"" + s +"\"";
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

//        public boolean deletenodeModel(int s) {
//
//            boolean result = false;
//
//            String query = "Select * FROM " + DataContract.TABLE_NAME + " WHERE " + DataContract.ID + " =  \"" + s + "\"";
//
//            SQLiteDatabase db = this.getWritableDatabase();
//
//            Cursor cursor = db.rawQuery(query, null);
//
//            if (cursor.moveToFirst()) {
//                db.delete(DataContract.TABLE_NAME, DataContract.ID + " = ?",
//                        new String[] { String.valueOf(s) });
//                cursor.close();
//                result = true;
//            }
//            db.close();
//            return result;
//        }
//
    }


