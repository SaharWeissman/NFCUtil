package com.saharw.nfcUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.util.Log;

/**
 * Created by sahar on 8/22/15.
 */
public abstract class NfcStateListener extends BroadcastReceiver {
    private final String TAG = "NfcStateListener";

    abstract void onStateOff();
    abstract void onStateOn();
    abstract void onStateTurningOn();
    abstract void onStateTurningOff();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null){
            Log.d(TAG, "onReceive");
            if(intent.getAction() != null && intent.getAction().equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)){
                Log.d(TAG, "got action: " + NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
                int nfcState = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);
                switch (nfcState){
                    case NfcAdapter.STATE_OFF:{
                        Log.d(TAG, "case STATE_OFF");
                        onStateOff();
                        break;
                    }
                    case NfcAdapter.STATE_ON:{
                        Log.d(TAG, "case STATE_ON");
                        onStateOn();
                        break;
                    }
                    case NfcAdapter.STATE_TURNING_ON:{
                        Log.d(TAG, "case STATE_TURNING_ON");
                        onStateTurningOn();
                        break;
                    }
                    case NfcAdapter.STATE_TURNING_OFF:{
                        Log.d(TAG, "case STATE_TURNING_OFF");
                        onStateTurningOff();
                        break;
                    }
                    default:{
                        Log.d(TAG, "case default");
                        break;
                    }
                }
            }
        }
    }
}
