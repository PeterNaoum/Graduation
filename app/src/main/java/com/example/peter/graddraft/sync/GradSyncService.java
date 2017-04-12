package com.example.peter.graddraft.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class GradSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static GradSyncAdapter sGradSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("GradSyncService", "onCreate - GradSyncService");
        synchronized (sSyncAdapterLock) {
            if (sGradSyncAdapter == null) {
                sGradSyncAdapter = new GradSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sGradSyncAdapter.getSyncAdapterBinder();
    }
}