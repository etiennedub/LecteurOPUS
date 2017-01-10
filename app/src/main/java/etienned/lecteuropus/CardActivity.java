package etienned.lecteuropus;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class CardActivity extends AppCompatActivity {
    Card m_Card;
    int m_OperatorId;
    int m_BusId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);

        Intent intent = getIntent();
        m_Card = (Card)intent.getSerializableExtra("Card");

        LinearLayout LayoutTransit = (LinearLayout) findViewById(R.id.layoutTransit);
        LinearLayout LayoutSubscription = (LinearLayout) findViewById(R.id.layoutSubcription);
        LinearLayout LayoutTicket = (LinearLayout) findViewById(R.id.layoutTicket);
        LinearLayout layoutCardId = (LinearLayout) findViewById(R.id.layoutCardId);



        TextView textType = (TextView) this.findViewById(R.id.CARTE_TYPE);
        ImageView imageType = (ImageView) this.findViewById(R.id.imageType);
        TextView textTransit = (TextView) this.findViewById(R.id.textTransit);
        TextView textExpiration = (TextView) this.findViewById(R.id.textExpiration);
        TextView textID = (TextView) this.findViewById(R.id.cardID);
        ImageView imageLogo = (ImageView) this.findViewById(R.id.imageLogo);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // TypeCard
        switch (m_Card.getType()){
            case OCCASIONNELLE:
                textType.setText(R.string.card_occa);
                imageType.setVisibility(View.GONE);
                textExpiration.setVisibility(View.GONE);
                layoutCardId.setVisibility(View.GONE);
                m_Card.getContract().get(0).setLogoFromXml(this);
                int logoId = m_Card.getContract().get(0).getlogoId();
                if (logoId != 0) imageLogo.setImageResource(logoId);

                break;
            case OPUS:
                String ID = "" + m_Card.getID();
                textID.setText("n° " + ID.substring(0,3) + " " + ID.substring(3,6) + " " + ID.substring(6));
                textType.setText(R.string.card_opus);
                imageType.setBackgroundResource(R.mipmap.opus);
                textExpiration.setText(getString(R.string.expiration) + " "
                        + Utils.DateToStringShort(m_Card.getExpirationDate()));
                break;
            default:
                // do nothing
                break;
        }

        // Contract
        int compteurTicket = 0;
        ArrayList<Contract> contract = m_Card.getContract();
        for (int i = 0; i < contract.size(); i++) {
            if (contract.get(i).isValid()){
                if(contract.get(i).isSubscription()) {
                    // Subscription
                    contract.get(i).setLogoFromXml(this);
                    View customSubs = inflater.inflate(R.layout.subscription, null);
                    LinearLayout LayoutSubsTime =
                            (LinearLayout) customSubs.findViewById(R.id.layoutSubsTime);
                    TextView textSubsTime =
                            (TextView) customSubs.findViewById(R.id.subscriptionTime);
                    TextView textSubsDate =
                            (TextView) customSubs.findViewById(R.id.subscriptionDate);
                    ImageView logo = (ImageView) customSubs.findViewById(R.id.logoSubs);
                    textSubsDate.setText(Utils.DateToString(contract.get(i).getValidityDate()));
                    LayoutSubsTime.setVisibility(View.GONE);
                    int logoId = contract.get(i).getlogoId();
                    if (logoId != 0) logo.setImageResource(logoId);
                    LayoutSubscription.addView(customSubs);
                }
                else if(contract.get(i).getnbTicket() > 0){
                    // Ticket
                    contract.get(i).setLogoFromXml(this);
                    View customTicket = inflater.inflate(R.layout.ticket, null);
                    TextView textTicket = (TextView) customTicket.findViewById(R.id.NbTicket);
                    ImageView logo = (ImageView) customTicket.findViewById(R.id.logoTicket);
                    int nbTicket = contract.get(i).getnbTicket();
                    String ticket = "";
                    if (nbTicket == 0 || nbTicket == 1) {
                        ticket = nbTicket + " " + getString(R.string.ticket_remaining);
                    } else {
                        ticket = nbTicket + " " + getString(R.string.tickets_remaining);
                    }
                    textTicket.setText(ticket);
                    int logoId = contract.get(i).getlogoId();
                    if (logoId != 0 && m_Card.getType() == Card.CardType.OPUS) logo.setImageResource(logoId);
                    LayoutTicket.addView(customTicket);
                    compteurTicket += 1;
                }
            }
        }

        if (compteurTicket == 0){ // 0 tickets
            View customTicket = inflater.inflate(R.layout.ticket, null);
            TextView textTicket = (TextView) customTicket.findViewById(R.id.NbTicket);
            textTicket.setText("0 " + getString(R.string.ticket_remaining));
            LayoutTicket.addView(customTicket);
        }


        // Transit
        ArrayList<Trip> trip = m_Card.getTrip();
        if (trip.size() == 0){
            LayoutTransit.setVisibility(View.GONE);
        }
        else {
            if (trip.size() == 1){
                textTransit.setText(R.string.last_trip);
            }
            for (int i = 0; i < trip.size(); i++) {
                trip.get(i).setBusFromXml(this); // Set bus name and operator
                View customTransit = inflater.inflate(R.layout.transit, null);
                TextView textDate = (TextView) customTransit.findViewById(R.id.textDate);
                TextView textTime = (TextView) customTransit.findViewById(R.id.textTime);
                TextView textBus = (TextView) customTransit.findViewById(R.id.textBus);
                ImageView imageBus = (ImageView) customTransit.findViewById(R.id.imageBus);
                TextView textTransfer = (TextView) customTransit.findViewById(R.id.textTransfer);
                ImageButton addButon = (ImageButton) customTransit.findViewById(R.id.addButton);

                Calendar tempo = trip.get(i).getDateTime();
                textTime.setText(Utils.TimeToString(tempo));
                textDate.setText(Utils.DateToString(tempo));

                // Transfer
                if(trip.get(i).getTransfer() != true){
                    textTransfer.setVisibility(View.GONE);
                }
                imageBus.setImageResource(trip.get(i).getLogoId());
                String name = trip.get(i).getBusName();
                if (name == "unknown") {
                    addButon.setTag(i);
                    name = getString(R.string.unknown);
                }
                else{
                    addButon.setVisibility(View.GONE);
                }
                textBus.setText(name);

                LayoutTransit.addView(customTransit);
            }
        }
    }

    public void addUnknown(View v) {
        int index =  Integer.valueOf(v.getTag().toString());
        m_OperatorId = m_Card.getTrip().get(index).getOperatorId();
        m_BusId = m_Card.getTrip().get(index).getBusId();

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setTitle(R.string.help_us);
        final EditText busInput = (EditText) promptView.findViewById(R.id.busInput);
        final EditText operatorInput = (EditText) promptView.findViewById(R.id.operatorInput);

        // Set up the buttons
        alertDialogBuilder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendData(busInput.getText().toString(), operatorInput.getText().toString());
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.show();
    }

    public void sendData(String busName, String operatorName){
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (busName != "" && operatorName != "") {
            Toast.makeText(this, R.string.thank_contribution, Toast.LENGTH_SHORT).show();

            // Send POST request
            // "http://ec2-54-149-222-116.us-west-2.compute.amazonaws.com/"
            SendHttpRequestTask t = new SendHttpRequestTask();
            String data = "id=" + androidId + "&busId=" + m_BusId + "&operatorId=" + m_OperatorId
                    + "&busName=" + busName + "&operatorName=" + operatorName;
            String[] params = new String[]{"http://ec2-54-149-222-116.us-west-2.compute.amazonaws.com/", data};

            t.execute(params);
        }
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String name = params[1];

            String data = sendHttpRequest(url, name);
            //System.out.println("Data ["+data+"]");
            return data;
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }

    private String sendHttpRequest(String url, String data) {
        StringBuffer buffer = new StringBuffer();
        try {
            System.out.println("URL ["+url+"] - Param ["+data+"]");

            HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();
            con.getOutputStream().write( (data).getBytes());

            InputStream is = con.getInputStream();
            byte[] b = new byte[1024];

            while ( is.read(b) != -1)
                buffer.append(new String(b));

            con.disconnect();
        }
        catch(Throwable t) {
            t.printStackTrace();
        }

        return buffer.toString();
    }


}
