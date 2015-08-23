package com.saharw.nfcUtil;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sahar on 8/8/15.
 */
public class NFCActivity extends Activity implements ITagListener, View.OnClickListener {

    private static final String EMPTY_MSG = "";
    private static final String NFC_STATE_OFF = "OFF";
    private static final String NFC_STATE_ON = "ON";
    private static final String NFC_STATE_TURNING_ON = "TURNING ON";
    private static final String NFC_STATE_TURNING_OFF = "TURNING OFF";
    private final String TAG = "NFCActivity";
    private final String MIME_TEXT_PLAIN = "text/plain";
    private final long NFC_STATE_SAMPLE_DELAY = 1000 * 4; // between nfc state changes
    private NfcAdapter mNfcAdapter;
    private Button mBtnWrite;
    private Tag mTag;
    private EditText mEdTxt;
    private TextView mReadTxt;
    private Button mBtnClearRead, mBtnClearWrite;
    private TextView mTxtVNfcState;
    private NfcStateListener mNfcStateListener;
    private Timer mNfcStateTimer;
    private TimerTask mNfcStateTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        initUIComponents();
        if(mNfcAdapter == null){
            Log.e(TAG, "nfc adapter is null!");
            shutdown();
        }

        // currently support only portrait display
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        setupForegroundDispatch(this, mNfcAdapter);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // listen to nfc state changed
            setNfcStateListener();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();

        // unregister broadcast receiver
        unregisterNfcStateListener();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // update nfc state for api < 18
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!hasFocus) {
                Log.d(TAG, "onWindowFocusChanged: false");
                cancelTimer();
            }
            if (hasFocus) {
                Log.d(TAG, "onWindowFocusChanged: true");
                scheduleNfcStateTimer();
            }
        }
    }

    private void cancelTimer() {
        if(mNfcStateTimer != null){
            mNfcStateTimer.cancel();
            mNfcStateTimer.purge();
            mNfcStateTimer = null;
            mNfcStateTimerTask.cancel();
            mNfcStateTimerTask = null;
        }
    }

    @Override
    public void onTagRead(String text) {
        String txtToShow = text == null ? "emtpy!" : text;
        Log.d(TAG, "onTagRead: " + txtToShow);
        Toast.makeText(this, "onTagRead: " + text, Toast.LENGTH_SHORT).show();
        if(!TextUtils.isEmpty(text)){
            mReadTxt.setText(text);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        if(intent != null) {
            handleIntent(intent);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if(v != null){
            switch (v.getId()){
                case R.id.btn_write:{
                    Log.d(TAG, "case btn_write");
                    if(mEdTxt.getText() != null && !TextUtils.isEmpty(mEdTxt.getText().toString())){
                        String textToWrite = mEdTxt.getText().toString();
                        try {
                            boolean writeSuccessful = writeTxtToTag(textToWrite, mTag);
                            if(writeSuccessful) {
                                Toast.makeText(this, "write successful!!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "write failed! (IOException)", Toast.LENGTH_SHORT).show();
                        } catch (FormatException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "write failed! (FormatException)", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(this, "Empty!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case R.id.btn_clear_read:{
                    Log.d(TAG, "case btn_clear_read");
                    mReadTxt.setText(EMPTY_MSG);
                    mReadTxt.requestLayout();
                    Toast.makeText(this, "read string cleared!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.btn_clear_write:{
                    Log.d(TAG, "case btn_clear_write");
                    mEdTxt.setText(EMPTY_MSG);
                    mReadTxt.requestLayout();
                    Toast.makeText(this, "write string cleared!", Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;
            }
        }
    }

    private boolean writeTxtToTag(String textToWrite, Tag tag) throws IOException, FormatException{
        if(tag == null){
            Log.d(TAG, "writeTxtToTag: tag is null!");
            Toast.makeText(this, "Tag is null", Toast.LENGTH_SHORT).show();
            return false;
        }
        NdefRecord record = createRecord(textToWrite);
        if(record == null){
            Log.d(TAG, "writeTxtToTag: record is null!");
            return false;
        }else {
            NdefRecord[] records = {record};
            NdefMessage message = new NdefMessage(records);
            Ndef ndef = Ndef.get(tag);
            ndef.connect();
            ndef.writeNdefMessage(message);
            ndef.close();
        }
        return true;
    }

    /**
     * creates a NDEF record containing the given text (according to NDEF standard in the NFC forum)
     * @param text
     * @return ndef record containing given text
     * @throws UnsupportedEncodingException
     */
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC;
        try{
            recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            Toast.makeText(this, "cannot create NDEF record", Toast.LENGTH_SHORT).show();
            recordNFC = null;
        }
        return recordNFC;
    }

    /**
     * here is where we define & enable foreground dispatch of 'ndef_discovered' intents to this activity
     * notice: this must be called while activity is in foreground!
     * @param activity
     * @param adapter
     */
    private void setupForegroundDispatch(Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        IntentFilter[] filters = new IntentFilter[2];
        String[][] techList = new String[][]{};

        // detect ndef messages
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        // detect other types of nfc tags
        filters[1] = new IntentFilter();
        filters[1].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);

        try{
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.d(TAG, "bad mime type", e);
            shutdown();
        }

        // enable foreground dispatch to this activity
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * this is where we stop the foreground dispatch of intents to this activity
     * important: this must be called before activity was paused!
     * @param activity
     * @param adapter
     */
    private void stopForegroundDispatch(Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void shutdown() {
        Log.d(TAG, "shutdown");
        finish();
    }


    // this is where we handle intents that might contain 'EXTRA_TAG' parcelable data
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if(!TextUtils.isEmpty(action)) {

            // Read ndef tag
            if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                Log.d(TAG, "got intent with action = " + NfcAdapter.ACTION_NDEF_DISCOVERED);
                String mimeType = intent.getType();
                if (!TextUtils.isEmpty(mimeType) && mimeType.equals(MIME_TEXT_PLAIN)) {
                    Log.d(TAG, "got intent with mimeType = " + MIME_TEXT_PLAIN);
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    new NdefReaderAsyncTask(this).execute(tag);
                } else {
                    Log.d(TAG, "mime type is not " + MIME_TEXT_PLAIN);
                }
            }

            // empty & other technology (then ndef) tags
            else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) || action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Log.d(TAG, "got intent with action = " + NfcAdapter.ACTION_TECH_DISCOVERED);

                // in case we'd like to use the 'Tech Discovered' intent
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                String[] techList = tag.getTechList();
                String searchedTech = Ndef.class.getName();
                for(int i = 0; i < techList.length; i++){
                    String tech = techList[i];
                    if(searchedTech.equals(tech)){
                        new NdefReaderAsyncTask(this).execute(tag);
                        break;
                    }
                }
            }

            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }


    private void initUIComponents() {
        mBtnWrite = (Button)findViewById(R.id.btn_write);
        mBtnWrite.setOnClickListener(this);

        mEdTxt = (EditText)findViewById(R.id.edTxt_write);
        mReadTxt = (TextView)findViewById(R.id.txtV_read_msg);
        mReadTxt.setText(EMPTY_MSG);
        mBtnClearRead = (Button)findViewById(R.id.btn_clear_read);
        mBtnClearWrite = (Button)findViewById(R.id.btn_clear_write);
        mBtnClearRead.setOnClickListener(this);
        mBtnClearWrite.setOnClickListener(this);

        mTxtVNfcState = (TextView)findViewById(R.id.nfc_state);
    }


    private void setNfcStateListener() {
        mNfcStateListener = new NfcStateListener() {
            @Override
            void onStateOff() {
                if(mTxtVNfcState != null){
                    mTxtVNfcState.setText(NFC_STATE_OFF);
                }
            }

            @Override
            void onStateOn() {
                if(mTxtVNfcState != null){
                    mTxtVNfcState.setText(NFC_STATE_ON);
                }
            }

            @Override
            void onStateTurningOn() {
                if(mTxtVNfcState != null){
                    mTxtVNfcState.setText(NFC_STATE_TURNING_ON);
                }
            }

            @Override
            void onStateTurningOff() {
                if(mTxtVNfcState != null){
                    mTxtVNfcState.setText(NFC_STATE_TURNING_OFF);
                }
            }
        };
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        registerReceiver(mNfcStateListener, filter);
    }


    private void unregisterNfcStateListener() {
        try{
            if(mNfcStateListener != null) {
                unregisterReceiver(mNfcStateListener);
            }
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }


    private void scheduleNfcStateTimer() {

        if(mNfcStateTimer == null){
            mNfcStateTimer = new Timer();
        }

        if(mNfcStateTimerTask == null){
            mNfcStateTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if(mNfcAdapter != null && mTxtVNfcState != null){
                        boolean isEnabled = mNfcAdapter.isEnabled();
                        if(isEnabled){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtVNfcState.setText(NFC_STATE_ON);
                                }
                            });
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtVNfcState.setText(NFC_STATE_OFF);
                                }
                            });
                        }
                    }
                }
            };
        }
        try {
            mNfcStateTimer.schedule(mNfcStateTimerTask, NFC_STATE_SAMPLE_DELAY);
        }catch (IllegalStateException e){
            Log.e(TAG, "unable to schedule timer task!");
        }
    }
}
