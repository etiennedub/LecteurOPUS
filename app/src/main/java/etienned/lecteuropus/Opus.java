package etienned.lecteuropus;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

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

        m_Card = new Card(Card.CardType.OPUS, ID, ExpirationDate, tripList, contractList);
//        Calendar todayOrHigher = Calendar.getInstance();
//        boolean isSubscribed = false;
//        for(int i = 0; i < 4; i++){
//            int days = Utils.bytesToInt(m_dataSubscription[i], 47, 14);
//            Calendar tempo = Utils.intToDateTime(days, 0);
//            if (tempo.compareTo(todayOrHigher) > 0){
//                todayOrHigher = tempo;
//                isSubscribed = true;
//            }
//        }
//        if (isSubscribed == true) {
//            m_Card.setValidityDate(todayOrHigher);
//            m_Card = new Card(Card.CardType.OPUS, ID, NbTicket, ExpirationDate, tripList, todayOrHigher);
//        }
//        else {
//            m_Card = new Card(Card.CardType.OPUS, ID, NbTicket, ExpirationDate, tripList);
//        }
    }


//    private int getTicketFromByte() {
//        int nbTicket = (m_dataTicket[0][2] & 0xFF);
//        nbTicket += (m_dataTicket[1][2] & 0xFF);
//
//        return nbTicket;
//    }

    public Card getCard(){
        return m_Card;
    }
}

