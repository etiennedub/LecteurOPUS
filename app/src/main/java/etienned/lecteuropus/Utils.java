package etienned.lecteuropus;
/**
 * Created by etienned on 10/6/16.
 */

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseIntArray;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Utils {


    public static final String TAG = "Util";

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

   public static Calendar intToDateTime(int p_days, int p_mins){
       Calendar result = Calendar.getInstance();
       result.set(1997,Calendar.JANUARY ,1,0,0,0);
       result.add(Calendar.DATE, p_days);
       result.add(Calendar.MINUTE, p_mins);

       return result;
   }


    public static String TimeToString(Calendar p_time) {
        DateFormat df = new SimpleDateFormat("HH:mm");
        String reportTime = df.format(p_time.getTime());
        return reportTime;
    }


    public static String DateToString(Calendar p_date) {
        DateFormat day = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        String reportDate = day.format(p_date.getTime());
        return reportDate;
    }

    public static String DateToStringShort(Calendar p_date) {
        //DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        String reportDate = df.format(p_date.getTime());
        //reportDate = reportDate.substring(0, 1).toUpperCase() + reportDate.substring(1);
        return reportDate;
    }

    public static String DateToStringNumber(Calendar p_date) {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        String reportDate = df.format(p_date.getTime());
        return reportDate;
    }

    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static int bytesToInt(byte[] data, int indexFirstBit, int length) {
        int currentByte = indexFirstBit / 8; // First
        int firstBit = indexFirstBit % 8;
        int lastByte = (indexFirstBit + length - 1) / 8;
        int lastBit = (indexFirstBit + length - 1) % 8;
        int result = 0;
        int mask = 0;
        int offset = 0;

        while( currentByte < lastByte){
            offset = 8 * (lastByte - currentByte - 1) + lastBit + 1;
            mask = 0xFF >> firstBit;
            result += (data[currentByte] & mask) <<  offset;
            firstBit = 0;
            currentByte += 1;
        }

        mask = 0xFF >> firstBit;
        result += (data[currentByte] & mask) >> (7 - lastBit);
        return result;

    }

    public static String[] getOperatorNames(Context ctx){
        List<String> result = new ArrayList<>();
        XmlResourceParser operatorXml = ctx.getResources().getXml(R.xml.operators);
        try {
            int event = operatorXml.getEventType();
            outerloop:
            while (event != XmlPullParser.END_DOCUMENT){
                switch (event) {
                    case XmlPullParser.START_TAG:
                        if(operatorXml.getName().equals("operator")) {
                            result.add(operatorXml.getAttributeValue(null, "name"));
                        }
                        break;
                }
                event = operatorXml.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing stations XML file: " + e.getMessage());
        }
        result.add(ctx.getString(R.string.other));
        return result.toArray(new String[0]);
    }

    public static String[] getBusNames(Context ctx, String operator){
        List<String> result = new ArrayList<>();
        XmlResourceParser operatorXml = ctx.getResources().getXml(R.xml.operators);
        String file = "";
        try {
            int event = operatorXml.getEventType();
            outerloop:
            while (event != XmlPullParser.END_DOCUMENT){
                switch (event) {
                    case XmlPullParser.START_TAG:
                        if(operatorXml.getName().equals("operator")
                            && operator.equals(operatorXml.getAttributeValue(null, "name"))) {
                                file = operatorXml.getAttributeValue(null, "file");
                                break outerloop;
                        }
                        break;
                }
                event = operatorXml.next();
            }

            if (file.equals("")){
                return result.toArray(new String[0]);
            }

            // Read bus file
            int xmlId = ctx.getResources().getIdentifier(file, "xml", ctx.getPackageName());
            XmlResourceParser busXml = ctx.getResources().getXml(xmlId);
            event = busXml.getEventType();
            outerloop:
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        if (busXml.getName().equals("bus")) {
                            result.add(busXml.getAttributeValue(null, "name"));
                        }
                        break;
                }
                event = busXml.next();
            }
            result.add(ctx.getString(R.string.other));
            return result.toArray(new String[0]);

        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error parsing stations XML file: " + e.getMessage());
            return new String[] {""};
        }
    }

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error md5 " + e.getMessage());
        }
        return "";
    }

    public static boolean isInternetAvailable() {
        AsyncTask<String, Void, InetAddress> task = new AsyncTask<String, Void, InetAddress>()
        {

            @Override
            protected InetAddress doInBackground(String... params)
            {
                try
                {
                    return InetAddress.getByName(params[0]);
                }
                catch (UnknownHostException e)
                {
                    return null;
                }
            }
        };
        try
        {
            return !task.execute("lecteuropus.duckdns.org").get().equals("");
        }
        catch (InterruptedException | NullPointerException | ExecutionException e)
        {
            return false;
        }
    }
}

