package etienned.lecteuropus;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;

import static etienned.lecteuropus.Utils.bytesToHex;

/**
 * Created by etienned on 10/8/16.
 */
public class Opus {

    public static final String TAG = "OPUS";


    byte[] m_dataID;
    byte[] m_dataExp;
    private byte[][] m_dataTransit;
    private byte[][] m_dataSubscription;
    private byte[][] m_dataTicket;
    private Card m_Card;


    public Opus(byte[] p_dataID, byte[] p_dataExp, byte[][] p_dataTransit, byte[][] p_dataSubscription, byte[][] p_dataTicket){

        m_dataTicket = p_dataTicket;
        m_dataSubscription = p_dataSubscription;
        m_dataTransit = p_dataTransit;
        m_dataID = p_dataID;
        m_dataExp = p_dataExp;

        m_Card = new Card();

        // Expiration Date
        int daysExpiration = Utils.bytesToInt(m_dataExp, 45, 14);
        Calendar ExpirationDate = Utils.intToDateTime(daysExpiration,0);

        // Card ID
         long ID = Utils.bytesToInt(m_dataID, 128, 32) & 0x00000000ffffffffL;

        // Trip
        ArrayList<Trip> tripList = new ArrayList();
        for(int i = 0; i < 3; i++){
            try{
                Trip tempo = new Trip(p_dataTransit[i]);
                tripList.add(tempo);
            }
            catch (IllegalArgumentException e){
                Log.e(TAG, "Empty transit");
            }
        }

        ArrayList<Contract> contractList = new ArrayList();
        // Contracts
        for(int i = 0; i < 3; i++){
            Contract tempo = new Contract(m_dataSubscription[i], m_dataTicket[i]);
            contractList.add(tempo);
        }

        m_Card = new Card(Card.CardType.OPUS, ID, ExpirationDate, tripList, contractList, this.serialize());
    }

    public Card getCard(){
        return m_Card;
    }

    public String serialize(){
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {
            xmlSerializer.setOutput(writer);

            //Start Document
            xmlSerializer.startDocument("UTF-8", true);
            //Open Tag <file>
            xmlSerializer.startTag("", "Opus");

            xmlSerializer.startTag("", "ID");
            xmlSerializer.text(bytesToHex(m_dataID));
            xmlSerializer.endTag("", "ID");

            xmlSerializer.startTag("", "Expiration");
            xmlSerializer.text(bytesToHex(m_dataExp));
            xmlSerializer.endTag("", "Expiration");

            xmlSerializer.startTag("", "Transit");
            for (int i = 0; i < 3; i++) {
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));
                xmlSerializer.text(bytesToHex(m_dataTransit[i]));
                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Transit");

            xmlSerializer.startTag("", "Subscription");
            for (int i = 0; i < 3; i++) {
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));
                xmlSerializer.text(bytesToHex(m_dataSubscription[i]));
                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Subscription");

            xmlSerializer.startTag("", "Ticket");
            for (int i = 0; i < 3; i++) {
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));
                xmlSerializer.text(bytesToHex(m_dataTicket[i]));
                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Ticket");

            xmlSerializer.endTag("", "Opus");

            xmlSerializer.endDocument();

            return writer.toString();
        }
        catch (IOException e){
            Log.e("Opus", "Error serialize");
            return "";
        }
    }
}

