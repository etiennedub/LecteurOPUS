package etienned.lecteuropus;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

import static etienned.lecteuropus.Utils.bytesToHex;

/**
 * Created by etienned on 9/28/16.
 */
public class Mifare {


    public static final String TAG = "Mifare";

    private byte[][] m_Id;
    private byte[][] m_pagesData;
    private Card m_Card;



    public Mifare(byte[][] cardData){

        if(cardData.length == 4) {
            // Keep only the 4 first bytes
            m_pagesData = new byte[16][4];
            for (int i = 0; i < 4; i++) {
                m_pagesData[i + 0] = Arrays.copyOfRange(cardData[0], i * 4, 4 + i * 4);
                m_pagesData[i + 4] = Arrays.copyOfRange(cardData[1], i * 4, 4 + i * 4);
                m_pagesData[i + 8] = Arrays.copyOfRange(cardData[2], i * 4, 4 + i * 4);
                m_pagesData[i + 12] = Arrays.copyOfRange(cardData[3], i * 4, 4 + i * 4);
            }
        }
        else if (cardData.length == 16){
            m_pagesData = cardData;
        }

        m_Id = new byte[2][4];
        m_Id[0] = m_pagesData[0];
        m_Id[1] = m_pagesData[1];

        m_Card = new Card();

        ArrayList<Contract> contractList = new ArrayList();
        Contract tempo = new Contract(m_pagesData[3], m_pagesData[4], m_pagesData[5], m_pagesData[10]);
        contractList.add(tempo);

        ArrayList<Trip> tripList = new ArrayList();
        for(int i = 0; i < 2; i++){
            try{

                Trip tempoTrip = new Trip(m_pagesData[8+(i*4)], m_pagesData[9+(i*4)],
                        m_pagesData[11+(i*4)], tempo.isSubscription());
                tripList.add(tempoTrip);
            }
            catch (IllegalArgumentException e){
                Log.e(TAG, "Empty transit");
            }
        }
        m_Card = new Card(Card.CardType.OCCASIONNELLE, tripList, contractList, this.serialize());
    }

    public Card getCard(){
        return m_Card;
    }

    public String serialize(){

        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();

            xmlSerializer.setOutput(writer);

            //Start Document
            xmlSerializer.startDocument("UTF-8", true);
            //Open Tag <file>
            xmlSerializer.startTag("", "Mifare");

            xmlSerializer.startTag("", "Pages");
            for (int i = 0; i < 16; i++) {
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));
                xmlSerializer.text(bytesToHex(m_pagesData[i]));
                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Pages");
            xmlSerializer.endTag("", "Mifare");

            xmlSerializer.endDocument();

            return writer.toString();
        }
        catch (IOException e){
            Log.e("Mifare", "Error serialize");
            return "";
        }
    }
}
