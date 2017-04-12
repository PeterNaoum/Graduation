package com.example.peter.graddraft.data;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * Created by Peter on 5/9/2016.
 */
public class DataObserver extends ContentObserver {
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DataObserver(Handler handler) {
        super(handler);
    }
   @Override
    public void onChange(boolean selfChange, Uri uri) {
       Log.d("OBSERVER","OBSERVERD CHANGE");
    }
}
