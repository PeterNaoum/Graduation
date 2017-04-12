package com.example.peter.graddraft.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

//package com.example.peter.graddraft.sync;
//
///**
// * Created by Peter on 5/10/2016.
// */
public class AccountAuthenticatorService extends Service {
    private static final String TAG = "AccountAuthenticatorService";
    private static final Object sSyncAdapterLock = new Object();
    private static AccountAuth sAccountAuthenticator = null;
    private static GradSyncAdapter gradSyncAdapter= null;

    public AccountAuthenticatorService() {
        super();
    }
    @Override
    public void onCreate() {
        super.onCreate();

//        Log.d(LOG_TAG, "onCreate");
        synchronized (sSyncAdapterLock) {
            if (gradSyncAdapter==null)
                gradSyncAdapter = new GradSyncAdapter(getBaseContext(),true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
            ret = getAuthenticator().getIBinder();
        return ret;
    }


    private AccountAuth getAuthenticator() {
        if (sAccountAuthenticator == null)
            sAccountAuthenticator = new AccountAuth(this);
        return sAccountAuthenticator;
    }
}