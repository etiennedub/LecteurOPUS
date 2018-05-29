package etienned.lecteuropus;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements CardReader.AccountCallback{

    public static final String TAG = "CardReaderFragment";
    // Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
    // activity is interested in NFC-A devices (including other Android devices), and that the
    // system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
    public static int FLAG_MIFARE =
            NfcAdapter.FLAG_READER_NFC_A;
    public static int FLAG_OPUS =
            NfcAdapter.FLAG_READER_NFC_B;
    public CardReader mCardReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCardReader = new CardReader(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        //disableReaderMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableReaderMode();
    }

    private void enableReaderMode() {
        Log.i(TAG, "Enabling reader mode");
        Activity activity = this;
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (!nfc.isEnabled()) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.disable_title));
            alertDialog.setMessage(getString(R.string.nfc_disable));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));

                        }
                    });
            alertDialog.show();
        }
        else if (nfc != null) {
            nfc.enableReaderMode(activity, mCardReader, FLAG_MIFARE | FLAG_OPUS, null);
        }
    }

    private void disableReaderMode() {
        Log.i(TAG, "Disabling reader mode");
        Activity activity = this;
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.disableReaderMode(activity);
        }
    }

    public void onAccountReceived(final Card myCard) {
        // This callback is run on a background thread, but updates to UI elements must be performed
        // on the UI thread.

        this.runOnUiThread(new Runnable() {
            public void run() {
                if(myCard != null) {
                    StartNewActivity(myCard);
                }

            }
        });
    }

    private final int ADD_TASK_REQUEST = 1;
    private void StartNewActivity(Card myCard){
        Intent intent = new Intent(MainActivity.this, CardActivity.class);
        intent.putExtra("Card", myCard);
        startActivityForResult(intent, ADD_TASK_REQUEST);
    }

}
