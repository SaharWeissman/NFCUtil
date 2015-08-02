package com.example.saharw.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.saharw.nfcUtil.core.INfcCallback;
import com.saharw.nfcUtil.core.NFCHelperActivity;


public class MainActivity extends Activity implements INfcCallback {

    public static final int MENU_ITEM_READ_NFC_STRING = 0;
    public static final int MENU_ITEM_WRITE_NFC_STRING = 1;
    private static final String TAG = "MainActivity";
    private EditText mEdtxt;
    private boolean nfcHelperStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUiComponents();
    }

    private void initUiComponents() {
        mEdtxt = (EditText) findViewById(R.id.edTxt);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.add(0, MENU_ITEM_READ_NFC_STRING, MENU_ITEM_READ_NFC_STRING, "Read nfc tag");
        menu.add(0, MENU_ITEM_WRITE_NFC_STRING, MENU_ITEM_WRITE_NFC_STRING, "Write nfc tag");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        switch (id){
            case MENU_ITEM_READ_NFC_STRING:{
                Log.d(TAG, "in case MENU_ITEM_READ_NFC_STRING");
                if(!nfcHelperStarted){
                    startNfcHelperActivity();
                }
                break;
            }
            case MENU_ITEM_WRITE_NFC_STRING:{
                if(!nfcHelperStarted){
                    startNfcHelperActivity();
                }
                String text = mEdtxt.getText().toString();
                if(!TextUtils.isEmpty(text)){
                    NFCHelperActivity.writeNfcString(text);
                    Toast.makeText(this, "wrote string : " + text + " to nfc tag!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "string is empty!", Toast.LENGTH_SHORT).show();
                }
                NFCHelperActivity.writeNfcString(text);
                break;
            }
            default:{
                break;
            }
        }

        return super.onOptionsItemSelected(item);

    }

    private void startNfcHelperActivity() {
        nfcHelperStarted = true;
        Intent intent = new Intent(this, NFCHelperActivity.class);
        startActivity(intent);
        NFCHelperActivity.addReadCallback(this);
    }

    @Override
    protected void onPause() {
        NFCHelperActivity.removeReadCallback(this);
    }

    public void clearText(View view) {
        if(!TextUtils.isEmpty(mEdtxt.getText().toString())){
            mEdtxt.setText("");
        }
    }

    @Override
    public void onReceivedString(String s) {
        Toast.makeText(this, "read nfc string: " + s , Toast.LENGTH_SHORT).show();
    }
}
