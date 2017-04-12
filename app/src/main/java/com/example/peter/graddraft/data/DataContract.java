package com.example.peter.graddraft.data;

import android.provider.BaseColumns;

/**
 * Created by Peter on 5/6/2016.
 */
public class DataContract implements BaseColumns {

    public static final String PROVIDER_NAME="com.example.peter.graddraft";
    public static final String ID="serial";
    public static final String VALUE="value";
    public static final String KEY="_id";
    public static final String TABLE_NAME="Nodes";
    public static final String LOGS_TABLE="Logs";
    public static final String TIME="time";
    public static final String DATE_CREATED="created";
    public static final String TYPE="type";
    public static final String ACTIVE="active";
    public static final String ALARM="alarm";
    public static final String OWNER="owner";
    public static final String PREFERENCES_MAIN = "my_pref" ;
}
