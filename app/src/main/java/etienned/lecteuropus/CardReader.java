/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package etienned.lecteuropus;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
public class CardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "CardReader";

    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "94a40800042000";
    private static final String READRECORD_APDU_HEADER = "94b20";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        void onAccountReceived(Card myCard);
    }

    public CardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
    }

    public static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.v(TAG, "New tag discovered");
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.
        String sIsoDep = IsoDep.class.getName();
        String sNfcB = NfcB.class.getName();
        String sMifare = MifareUltralight.class.getName();
        String sNfcA = NfcA.class.getName();
        if (tag.toString().contains(sMifare) || tag.toString().contains(sNfcA)) {
            MifareUltralight mifareCard = MifareUltralight.get(tag);
            if (mifareCard != null) {
                try {
                    Log.i(TAG, "Occa detected");
                    mifareCard.connect();
                    byte[] page0to3 = mifareCard.readPages(0);
                    byte[] page4to7 = mifareCard.readPages(4);
                    byte[] page8to11 = mifareCard.readPages(8);
                    byte[] page12to15 = mifareCard.readPages(12);

                    byte[][] cardData = new byte[][]{
                            page0to3,
                            page4to7,
                            page8to11,
                            page12to15};

                    Mifare mainCard = new Mifare(cardData);

                    mAccountCallback.get().onAccountReceived(mainCard.getCard());

                } catch (IOException e) {
                    Log.e(TAG, "Error communicating with card: " + e.toString());
                }
            }
        } else if (tag.toString().contains(sNfcB) || tag.toString().contains(sIsoDep)) {
            IsoDep OpusCard = IsoDep.get(tag);
            if (OpusCard != null) {
                Log.i(TAG, "Opus detected");
                //byte[] select_apdu = Utils.HexStringToByteArray(SELECT_APDU);
                //byte[] read_apdu = Utils.HexStringToByteArray(READRECORD_APDU);

                byte[][] dataTransit = new byte[3][];
                byte[][] dataSubscription = new byte[4][];
                byte[][] dataTicket = new byte[4][];

                try {
                    OpusCard.connect();

                    // ICC (Card ID)
                    OpusCard.transceive(Utils.HexStringToByteArray("94A4000002000219"));
                    byte[] dataID = OpusCard.transceive(Utils.HexStringToByteArray("94B201041D"));

                    // Enviroment (Expiration)
                    OpusCard.transceive(getSelectApdu("2001"));
                    byte[] dataExp = OpusCard.transceive(getReadApdu(1));

                    // Transit
                    OpusCard.transceive(getSelectApdu("2010"));
                    for(int i = 0; i < 3; i++){
                        dataTransit[i] = OpusCard.transceive(getReadApdu(i+1));
                    }
                    //Fake Transit, uncomment next block
//                    dataTransit[0] = Utils.HexStringToByteArray("70AA3500000CF000282040014877B85515C622");
//                    dataTransit[1] = Utils.HexStringToByteArray("70AA2B80000CF0002835F0016077B85515C620");
//                    dataTransit[2] = Utils.HexStringToByteArray("70923380000CF00028B338013877B849150622");

                    // Subscription
                    OpusCard.transceive(getSelectApdu("2020"));
                    for(int i = 0; i < 4; i++){
                        dataSubscription[i] = OpusCard.transceive(getReadApdu(i+1));
                    }
                    //Fake Subs, uncomment next block
//                    dataSubscription[0] = Utils.HexStringToByteArray("1E0A092B80000007F48DB66826E0");
//                    dataSubscription[1] = Utils.HexStringToByteArray("1E0A0043B5F8D8D7F48DAF966A00");
//                    dataSubscription[2] = Utils.HexStringToByteArray("1E0A0043B2A4CB87F48D94E6D600");
//                    dataSubscription[3] = Utils.HexStringToByteArray("1E0A079BB2E6CBD7F48D9736E800");

                    // Ticket
                    OpusCard.transceive(getSelectApdu("202A"));
                    dataTicket[0] = OpusCard.transceive(getReadApdu(1));
                    OpusCard.transceive(getSelectApdu("202B"));
                    dataTicket[1] = OpusCard.transceive(getReadApdu(1));
                    OpusCard.transceive(getSelectApdu("202C"));
                    dataTicket[2] = OpusCard.transceive(getReadApdu(1));
                    OpusCard.transceive(getSelectApdu("202D"));
                    dataTicket[3] = OpusCard.transceive(getReadApdu(1));

                    Opus mainCard = new Opus(dataID, dataExp, dataTransit, dataSubscription, dataTicket);

                    mAccountCallback.get().onAccountReceived(mainCard.getCard());


                }
                catch (IOException e) {
                    Log.e(TAG, "Error communicating with card: " + e.toString());
                }

            }
        }
    }

    byte[] getReadApdu(int i){ // i must be between 1 and 9
        String readString = READRECORD_APDU_HEADER + i + "0400";
        return Utils.HexStringToByteArray(readString);
    }

    byte[] getSelectApdu(String HexEnd){ // HexEnd must be 4 characters (Ex. 20A0)
        String selectString = SELECT_APDU_HEADER + HexEnd;
        return Utils.HexStringToByteArray(selectString);
    }

}
