package etienned.lecteuropus;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import static etienned.lecteuropus.Utils.DateToStringNumber;
import static etienned.lecteuropus.Utils.DateToStringShort;
import static etienned.lecteuropus.Utils.TimeToString;
import static etienned.lecteuropus.Utils.bytesToHex;

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
    private String m_serializeRawCard;

    public Card(){
        // do nothing
    }

    public Card(CardType p_Type, long p_ID, Calendar p_ExpirationDate,
                ArrayList<Trip> p_Trip,  ArrayList<Contract> p_Contract, String serialize) {
        m_Type = p_Type;
        m_ID = p_ID;
        m_ExpirationDate = p_ExpirationDate;
        m_Trip = sortTrips(p_Trip);
        m_Contract = p_Contract;
        m_serializeRawCard = serialize;
    }

    public Card(CardType p_Type, ArrayList<Trip> p_Trip, ArrayList<Contract> p_Contract, String serialize) {
        m_Type = p_Type;
        m_Contract = p_Contract;
        m_Trip = sortTrips(p_Trip);
        m_serializeRawCard = serialize;
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

    public String getRawSerialize(){
        // This return the raw data in xml form
        return m_serializeRawCard;
    }

    public String serialize() {
        // This return the data in xml form

        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();

            xmlSerializer.setOutput(writer);

            //Start Document
            xmlSerializer.startDocument("UTF-8", true);
            //Open Tag <file>
            xmlSerializer.startTag("", "Card");

            xmlSerializer.startTag("", "Type");
            xmlSerializer.text(m_Type.name());
            xmlSerializer.endTag("", "Type");

            xmlSerializer.startTag("", "ID");
            xmlSerializer.text(Long.toString(m_ID));
            xmlSerializer.endTag("", "ID");

            if (m_ExpirationDate != null) {
                xmlSerializer.startTag("", "ExpirationDate");
                xmlSerializer.text(DateToStringNumber(m_ExpirationDate));
                xmlSerializer.endTag("", "ExpirationDate");
            }

            xmlSerializer.startTag("", "Trip");
            for (int i = 0; i < m_Trip.size(); i++) {
                Trip trip = m_Trip.get(i);
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));

                if (trip.getDateTime() != null) {
                    xmlSerializer.attribute("", "Date", DateToStringNumber(trip.getDateTime()));
                    xmlSerializer.attribute("", "Time", TimeToString(trip.getDateTime()));
                }
                xmlSerializer.attribute("", "Transfert", Boolean.toString(trip.getTransfer()));
                xmlSerializer.attribute("", "OperatorId", Integer.toString(trip.getOperatorId()));
                xmlSerializer.attribute("", "BusId", Integer.toString(trip.getBusId()));
                if (trip.getOperatorName() != null)
                    xmlSerializer.attribute("", "OperatorName", trip.getOperatorName());
                if (trip.getBusName() != null)
                    xmlSerializer.attribute("", "BusName", trip.getBusName());

                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Trip");

            xmlSerializer.startTag("", "Contract");
            for (int i = 0; i < m_Contract.size(); i++) {
                Contract contract = m_Contract.get(i);
                xmlSerializer.startTag("", "List");
                xmlSerializer.attribute("", "index", Integer.toString(i));

                xmlSerializer.attribute("", "Subscription", Boolean.toString(contract.isSubscription()));
                xmlSerializer.attribute("", "Valid", Boolean.toString(contract.isValid()));
                xmlSerializer.attribute("", "NbTicket", Integer.toString(contract.getNbTicket()));
                xmlSerializer.attribute("", "OperatorId", Integer.toString(contract.getOperatorId()));
                if (contract.getOperatorName() != null)
                    xmlSerializer.attribute("", "OperatorName", contract.getOperatorName());
                if (contract.getValidityDate() != null)
                    xmlSerializer.attribute("", "ValidityDate", DateToStringNumber(contract.getValidityDate()));

                xmlSerializer.endTag("", "List");
            }
            xmlSerializer.endTag("", "Contract");

            xmlSerializer.endTag("", "Card");
            xmlSerializer.endDocument();

            return writer.toString();
        }
        catch (IOException e){
            Log.e(TAG, "Error serialize");
            return "";
        }
    }
}