package com.saharw.nfcUtil;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * Created by sahar on 8/8/15.
 */
public class NdefReaderAsyncTask extends AsyncTask<Tag, Void, String> {

    private final String TAG = "NdefReaderAsyncTask";
    private final String UTF_8_ENCODE = "UTF-8";
    private final String UTF_16_ENCODE = "UTF-16";
    private final ITagListener mListener;

    public NdefReaderAsyncTask(ITagListener listener){
        mListener = listener;
    }

    @Override
    protected String doInBackground(Tag... params) {
        Tag tag = params[0];
        Ndef ndef = Ndef.get(tag);
        if(ndef == null){
            Log.d(TAG, "tag is not in valid NDEF format!");
            return null;
        }

        NdefMessage msg = ndef.getCachedNdefMessage();
        if(msg == null){// tag in "initialized" state (empty)
            Log.d(TAG, "ndef tag is empty!");
        }
        else {
            NdefRecord[] records = msg.getRecords();
            for (int i = 0; i < records.length; i++) {
                NdefRecord currRecord = records[i];
                if (currRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                    try {
                        return readText(currRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, " unsupported encoding!", e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(mListener != null){
            mListener.onTagRead(result);
        }
    }

    /**
     * here we read the data from the nfc record according to the NFC forum specifications of
     * "Text Record Type Definition"
     * @param record
     * @return
     * @throws UnsupportedEncodingException
     */
    private String readText(NdefRecord record) throws UnsupportedEncodingException{
        byte[] payload = record.getPayload();

        // get encoding
        String encoding = ((payload[0] & 128) == 0) ? UTF_8_ENCODE : UTF_16_ENCODE;

        // get language code (e.g. "en")
        int langCodeLength = payload[0] & 0063;

        return new String(payload, langCodeLength + 1, payload.length - langCodeLength - 1, encoding);
    }
}
