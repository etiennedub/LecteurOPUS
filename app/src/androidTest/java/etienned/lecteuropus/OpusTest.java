package etienned.lecteuropus;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import android.util.Xml;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

import static etienned.lecteuropus.Trip.TAG;
import static etienned.lecteuropus.Utils.DateToStringNumber;
import static etienned.lecteuropus.Utils.HexStringToByteArray;
import static etienned.lecteuropus.Utils.TimeToString;

/**
 * Created by etienned on 3/11/18.
 */
@RunWith(Theories.class)
public class OpusTest extends TestCase {

    // List all cards in src/androidTest/assets/cards/
    public static @DataPoints String[] cards() throws Exception{
        Context ctx = InstrumentationRegistry.getContext();
        return ctx.getResources().getAssets().list("expected");
    }

    @Test
    public void testOneCard() throws Exception {
        testCardsInformation("opus-01.xml");
    }

    @Theory
    // Compare `.xml` with the matching name in `cards` and `expected` folders.
    public void testCardsInformation(String cardInput) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        Context ctx = InstrumentationRegistry.getContext();
        InputStream input = ctx.getResources().getAssets().open("cards/" + cardInput);
        parser.setInput(input , null);
        parser.nextTag();

        Card card = new Card();
        if(parser.getName().equals("Mifare")){
            byte[][] pagesData = new byte[16][4];
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if(name.equals("List")) {
                    int index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                    pagesData[index] = HexStringToByteArray(parser.nextText());
                }
            }

            Mifare mifareCard = new Mifare(pagesData);
            card = mifareCard.getCard();
        }
        else if(parser.getName().equals("Opus")){
            byte[][] dataTransit = new byte[3][];
            byte[][] dataSubscription = new byte[4][];
            byte[][] dataTicket = new byte[4][];
            byte[] dataID = null;
            byte[] dataExp = null;

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if(name.equals("ID")) {
                    dataID = HexStringToByteArray(parser.nextText());
                }
                else if(name.equals("Expiration")) {
                    dataExp = HexStringToByteArray(parser.nextText());
                }
                else if(name.equals("Transit")) {
                    int index = 0;
                    while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("Transit")) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                        dataTransit[index] = HexStringToByteArray(parser.nextText());
                    }
                    assertEquals(2, index);
                }
                else if(name.equals("Subscription")) {
                    int index = 0;
                    while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("Subscription")) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                        dataSubscription[index] = HexStringToByteArray(parser.nextText());
                    }
                    assertEquals(2, index);
                }
                else if(name.equals("Ticket")) {
                    int index = 0;
                    while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("Ticket")) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                        dataTicket[index] = HexStringToByteArray(parser.nextText());
                    }
                    assertEquals(2, index);
                }
            }
            Opus opusCard = new Opus(dataID, dataExp, dataTransit, dataSubscription, dataTicket);
            card = opusCard.getCard();
        }
        else{
            fail();
        }

        input = ctx.getResources().getAssets().open("expected/" + cardInput);
        parser.setInput(input , null);
        parser.nextTag();

        Assert.assertEquals("Card", parser.getName());
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if(name.equals("Type")) {
                Assert.assertEquals(parser.nextText(), card.getType().toString());
            }
            else if(name.equals("ID")) {
                Assert.assertEquals(Long.parseLong(parser.nextText()), card.getID());
            }
            else if(name.equals("ExpirationDate")) {
                Assert.assertEquals(parser.nextText(), DateToStringNumber(card.getExpirationDate()));
            }
            else if(name.equals("Trip")) {
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("Trip")) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    int index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                    Trip trip = card.getTrip().get(index);
                    trip.setBusFromXml(InstrumentationRegistry.getTargetContext());

                    String date = parser.getAttributeValue(null, "Date");
                    if(date != null)
                        assertEquals(date, DateToStringNumber(trip.getDateTime()));
                    String time = parser.getAttributeValue(null, "Time");
                    if(time != null)
                        assertEquals(time, TimeToString(trip.getDateTime()));
                    String transfert = parser.getAttributeValue(null, "Transfert");
                    if(transfert != null)
                        assertEquals(Boolean.parseBoolean(transfert), trip.getTransfer());
                    String operatorId = parser.getAttributeValue(null, "OperatorId");
                    if(operatorId != null)
                        assertEquals(Integer.parseInt(operatorId), trip.getOperatorId());
                    String busId = parser.getAttributeValue(null, "BusId");
                    if(busId != null)
                        assertEquals(Integer.parseInt(busId), trip.getBusId());
                    String operatorName = parser.getAttributeValue(null, "OperatorName");
                    if(operatorName != null)
                        assertEquals(operatorName, trip.getOperatorName());
                    String busName = parser.getAttributeValue(null, "BusName");
                    if(busName != null)
                        assertEquals(busName, trip.getBusName());

                }
            }
            else if(name.equals("Contract")) {
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("Contract")) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    int index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                    Contract contract = card.getContract().get(index);
                    contract.setLogoFromXml(InstrumentationRegistry.getTargetContext());

                    String subscription = parser.getAttributeValue(null, "Subscription");
                    if (subscription != null)
                        assertEquals(Boolean.parseBoolean(subscription), contract.isSubscription());
                    String nbTicket = parser.getAttributeValue(null, "NbTicket");
                    if (nbTicket != null)
                        assertEquals(Integer.parseInt(nbTicket), contract.getNbTicket());
                    String operatorId = parser.getAttributeValue(null, "OperatorId");
                    if(operatorId != null)
                        assertEquals(Integer.parseInt(operatorId), contract.getOperatorId());
                    String operatorName = parser.getAttributeValue(null, "OperatorName");
                    if(operatorName != null)
                        assertEquals(operatorName, contract.getOperatorName());
                    String date = parser.getAttributeValue(null, "ValidityDate");
                    if(date != null)
                        assertEquals(date, DateToStringNumber(contract.getValidityDate()));
                }
            }
        }
    }

}