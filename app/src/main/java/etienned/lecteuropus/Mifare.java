package etienned.lecteuropus;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by etienned on 9/28/16.
 */
public class Mifare {


    public static final String TAG = "Mifare";

    private byte[][] m_Id;
    private byte[][] m_pagesData;
    private Card m_Card;



    public Mifare(byte[][] cardData){

        // Keep only the 4 first bytes
        m_pagesData = new byte[16][4];
        for (int i = 0; i < 4; i++){
            m_pagesData[i+0] = Arrays.copyOfRange(cardData[0], i*4, 4+i*4);
            m_pagesData[i+4] = Arrays.copyOfRange(cardData[1], i*4, 4+i*4);
            m_pagesData[i+8] = Arrays.copyOfRange(cardData[2], i*4, 4+i*4);
            m_pagesData[i+12] = Arrays.copyOfRange(cardData[3], i*4, 4+i*4);
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
        m_Card = new Card(Card.CardType.OCCASIONNELLE, tripList, contractList);

    }

    public Card getCard(){
        return m_Card;
    }
}
