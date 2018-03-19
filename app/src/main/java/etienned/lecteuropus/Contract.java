package etienned.lecteuropus;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by etienned on 12/28/16.
 */
public class Contract implements Serializable {
    private boolean m_Subscription;
    private boolean m_Valid;
    private int m_nbTicket;
    private int m_operatorId;
    private String m_operatorName;
    private int m_logoId;
    private Calendar m_ValidityDate;

    public Contract(byte[] subscriptionData, byte[] ticketData){ // OPUS
        m_operatorId = Utils.bytesToInt(subscriptionData, 9, 8);
        if(Utils.bytesToInt(subscriptionData, 40, 16) == 0){
            // Ticket
            m_Subscription = false;
            m_nbTicket = Utils.bytesToInt(ticketData, 16, 8);
            m_Valid = (m_nbTicket > 0);
        }
        else{
            // Subscription
            m_Subscription = true;
            Calendar today = Calendar.getInstance();
            int days = Utils.bytesToInt(subscriptionData, 47, 14);
            m_ValidityDate = Utils.intToDateTime(days, 0);
            m_Valid = (m_ValidityDate.compareTo(today) > 0);
        }
    }

    public Contract(byte[] page3, byte[] page4, byte[] page5, byte[] page10) { // Mifare
        m_nbTicket = 0;
        m_Subscription = false;
        m_operatorId = Utils.bytesToInt(page5, 18, 8);
        if (Arrays.equals(page3, Utils.HexStringToByteArray("00000000"))) {
            // Carte neuve
            m_nbTicket = page10[2] & 0x80 >> 7 |
                    (page10[1] & 0x0F) << 1;
            if ((Arrays.equals(page4, Utils.HexStringToByteArray("00010001")))
                    &&  m_nbTicket != 0) {
                m_nbTicket -= 1;
            }
            m_Valid = true;
        }
        else if (Arrays.equals(page3, Utils.HexStringToByteArray("80000000"))){
            // Subscription
            m_Subscription = true; // TODO : Find validity Date in Mifare
            m_Valid = false;

        }
        else{
            // Card used
            m_Valid = true;
            while(Utils.bytesToInt(page3,31 - m_nbTicket, 1) == 0){
                m_nbTicket += 1;
            }
        }
    }

    public boolean isValid() {
        return m_Valid;
    }

    public void setLogoFromXml(Context ctx){
        String node = "";
        String logo = "";
        m_operatorName = "";
        XmlResourceParser operatorXml = ctx.getResources().getXml(R.xml.operators);
        try {
            int event = operatorXml.getEventType();
            outerloop:
            while (event != XmlPullParser.END_DOCUMENT){
                switch (event) {
                    case XmlPullParser.START_TAG:
                        node = operatorXml.getName();
                        if(node.equals("operator")) {
                            if (operatorXml.getAttributeValue(null, "id").equals("" + m_operatorId)) {
                                logo = operatorXml.getAttributeValue(null, "logo");
                                m_operatorName = operatorXml.getAttributeValue(null, "name");
                                break outerloop;
                            }
                        }
                        break;
                }
                event = operatorXml.next();
            }
        } catch (Exception e) {
            Log.e("CardActivity", "Error parsing stations XML file: " + e.getMessage());
        }

        m_logoId = 0;
        if (!logo.equals("")){
            m_logoId = ctx.getResources().getIdentifier(logo, "drawable", ctx.getPackageName());
        }
    }

    public int getNbTicket() {
        return m_nbTicket;
    }

    public int getOperatorId(){
        return m_operatorId;
    }

    public boolean isSubscription() {
        return m_Subscription;
    }

    public Calendar getValidityDate() {
        return m_ValidityDate;
    }

    public int getLogoId() {
        return m_logoId;
    }

    public String getOperatorName() {
        return m_operatorName;
    }
}
