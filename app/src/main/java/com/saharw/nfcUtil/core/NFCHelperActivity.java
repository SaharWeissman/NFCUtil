package com.saharw.nfcUtil.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.saharw.nfcUtil.R;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sahar on 7/25/15.
 */
public class NFCHelperActivity extends Activity {

    private static final String TAG = "NFCHelperActivity";
    private static List<INfcCallback> mListeners;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilterWriteTag;
    private NfcAdapter mNfcAdapter;
    private static Tag sTag;
    private EditText mEdtxt;
    private Button mBtnWrite;
    private String[][] mTechListsArray = new String[][] { new String[] { NfcF.class.getName() } };
    private static int NUM_PAGES = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter mFilterTagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilterTagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        mFilterWriteTag = new IntentFilter[] {mFilterTagDetected};
        mListeners = new ArrayList<INfcCallback>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: enableForegroundDispatch");
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilterWriteTag, mTechListsArray);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: enableForegroundDispatch");
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            sTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String nfcString = readStringFromTag(sTag);
            if(!TextUtils.isEmpty(nfcString)){
                Toast.makeText(this, "read nfc string: " + nfcString, Toast.LENGTH_SHORT).show();
            }else{
                Log.d(TAG, "nfc string is empty or null!");
            }
        }
    }

    private String readStringFromTag(Tag mTag) {
        MifareUltralight mifare = MifareUltralight.get(mTag);
        try {
            mifare.connect();
            byte[] payload = mifare.readPages(4);
            String text = new String(payload, Charset.forName("US-ASCII"));
            notifyListenersOnReceive(text);
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight...", e);
        } finally {
            if (mifare != null) {
                try {
                    mifare.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }

    private void notifyListenersOnReceive(String text) {
        for(INfcCallback callback : mListeners){
            callback.onReceivedString(text);
        }
    }

    private static void write(String text, Tag tag) throws IOException, FormatException {
        MifareUltralight ultralight = MifareUltralight.get(tag);
        try {
            ultralight.connect();
            byte[] stringBites = text.getBytes();
            byte[] chunk = new byte[4];
            for(int i = 0; i < Math.min(stringBites.length / 4, NUM_PAGES); i++){
                System.arraycopy(stringBites, 4*i, chunk, 0,4);
                ultralight.writePage(NUM_PAGES + i, chunk);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while closing MifareUltralight...", e);
        } finally {
            try {
                ultralight.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException while closing MifareUltralight...", e);
            }
        }
    }

    public static void writeNfcString(String text) {
        String nfcString = text;
        if(!TextUtils.isEmpty(nfcString)){
            Log.d(TAG, "writeNfcString: nfcString = " + nfcString);
            try {
                write(nfcString, sTag);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addReadCallback(INfcCallback listener){
        mListeners.add(listener);
    }


    public static void removeReadCallback(INfcCallback listener){
        mListeners.remove(listener);
    }
}
