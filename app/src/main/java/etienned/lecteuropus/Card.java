package etienned.lecteuropus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by etienned on 10/7/16.
 */
public class Card implements Serializable {

    public static final String TAG = "CARD";

    public enum SubscriptionType {
        DAY,
        WEEK,
        MONTH,
        TICKET,
    }

     public enum CardType {
            OPUS,
            OCCASIONNELLE,
        }

    private SubscriptionType m_Subscription; // Passe d'un jour, mois...
    private ArrayList<Trip> m_Trip;
    private ArrayList<Contract> m_Contract;
    private CardType m_Type;
    private Calendar m_ExpirationDate;
    private long m_ID;

    public Card(){
        // do nothing
    }

    public Card(CardType p_Type, long p_ID, Calendar p_ExpirationDate,
                ArrayList<Trip> p_Trip,  ArrayList<Contract> p_Contract) {
        m_Type = p_Type;
        m_ID = p_ID;
        m_ExpirationDate = p_ExpirationDate;
        m_Trip = sortTrips(p_Trip);
        m_Contract = p_Contract;
    }

    public Card(CardType p_Type, ArrayList<Trip> p_Trip, ArrayList<Contract> p_Contract) {
        m_Type = p_Type;
        m_Contract = p_Contract;
        m_Trip = sortTrips(p_Trip);
    }


    public ArrayList<Trip> getTrip(){
        return m_Trip;
    }

    public ArrayList<Contract> getContract(){
        return m_Contract;
    }

    public void setSubscription(SubscriptionType p_Subscription){
        m_Subscription = p_Subscription;
    }

    public ArrayList<Trip> sortTrips(ArrayList<Trip> p_Trip){
        // Sorting

        Collections.sort(p_Trip, new Comparator<Trip>() {
            @Override
            public int compare(Trip trip2, Trip trip1)
            {

                return  trip1.getDateTime().compareTo(trip2.getDateTime());
            }
        });
        return p_Trip;
    }
    public void setExpirationDate(Calendar p_ExpirationDate){
        m_ExpirationDate = p_ExpirationDate;
    }
    public void setType(CardType p_Type){
        m_Type = p_Type;
    }
    public Calendar getExpirationDate(){
        return m_ExpirationDate;
    }

    public CardType getType(){
        return m_Type;
    }
    public long getID() {
        return m_ID;
    }
    public void setID(long p_ID){
        m_ID = p_ID;
    }

}