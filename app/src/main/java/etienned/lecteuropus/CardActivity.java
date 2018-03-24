package etienned.lecteuropus;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static etienned.lecteuropus.Utils.isInternetAvailable;

public class CardActivity extends AppCompatActivity {
    public static final String TAG = "CardActivity";
    Card m_Card;
    int m_OperatorId;
    int m_BusId;
    String m_TimeHash;
    File m_PreviousAddTrip;
    List<ImageButton> m_AddButtons;

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
                int logoId = m_Card.getContract().get(0).getLogoId();
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
                    int logoId = contract.get(i).getLogoId();
                    if (logoId != 0) logo.setImageResource(logoId);
                    LayoutSubscription.addView(customSubs);
                }
                else if(contract.get(i).getNbTicket() > 0){
                    // Ticket
                    contract.get(i).setLogoFromXml(this);
                    View customTicket = inflater.inflate(R.layout.ticket, null);
                    TextView textTicket = (TextView) customTicket.findViewById(R.id.NbTicket);
                    ImageView logo = (ImageView) customTicket.findViewById(R.id.logoTicket);
                    int nbTicket = contract.get(i).getNbTicket();
                    String ticket = "";
                    if (nbTicket == 0 || nbTicket == 1) {
                        ticket = nbTicket + " " + getString(R.string.ticket_remaining);
                    } else {
                        ticket = nbTicket + " " + getString(R.string.tickets_remaining);
                    }
                    textTicket.setText(ticket);
                    int logoId = contract.get(i).getLogoId();
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

            File path = this.getFilesDir();
            m_PreviousAddTrip = new File(path, "cardsAdd.txt");
            List<String> list = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(m_PreviousAddTrip));
                String str;
                while((str = br.readLine()) != null){
                    list.add(str);
                }
            }
            catch (IOException e){
                Log.e(TAG, "File not found");
            }

            m_AddButtons = new ArrayList<>();
            for (int i = 0; i < trip.size(); i++) {
                trip.get(i).setBusFromXml(this); // Set bus name and operator
                View customTransit = inflater.inflate(R.layout.transit, null);
                TextView textDate = (TextView) customTransit.findViewById(R.id.textDate);
                TextView textTime = (TextView) customTransit.findViewById(R.id.textTime);
                TextView textBus = (TextView) customTransit.findViewById(R.id.textBus);
                ImageView imageBus = (ImageView) customTransit.findViewById(R.id.imageBus);
                TextView textTransfer = (TextView) customTransit.findViewById(R.id.textTransfer);
                ImageButton addButton = (ImageButton) customTransit.findViewById(R.id.addButton);
                m_AddButtons.add(addButton);

                Calendar tempo = trip.get(i).getDateTime();
                textTime.setText(Utils.TimeToString(tempo));
                textDate.setText(Utils.DateToString(tempo));

                // Transfer
                if(trip.get(i).getTransfer() != true){
                    textTransfer.setVisibility(View.GONE);
                }
                imageBus.setImageResource(trip.get(i).getLogoId());

                String name = trip.get(i).getBusName();
                if(name.equals(""))
                    name = getString(R.string.unknown);

                String tripId = trip.get(i).getTripId();
                if (!list.contains(tripId)) {
                    addButton.setTag(i);
                }
                else{
                    addButton.setVisibility(View.GONE);
                }
                textBus.setText(name);

                LayoutTransit.addView(customTransit);
            }
        }

        if (BuildConfig.DEBUG) {
            File path = this.getFilesDir();
            File cardRaw = new File(path, "card.xml");
            File expected = new File(path, "expected.xml");
            try {
                FileOutputStream streamRaw = new FileOutputStream(cardRaw);
                FileOutputStream streamExpected = new FileOutputStream(expected);
                streamRaw.write(m_Card.getRawSerialize().getBytes());
                streamRaw.close();
                streamExpected.write(m_Card.serialize().getBytes());
                streamExpected.close();
            }
            catch (IOException e){
                Log.e(TAG, "Error writing file");
            }
        }
    }

    public void addUnknown(View v) {
        if(!isInternetAvailable()){
            Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final int index =  Integer.valueOf(v.getTag().toString());
        m_OperatorId = m_Card.getTrip().get(index).getOperatorId();
        m_BusId = m_Card.getTrip().get(index).getBusId();
        final String operatorName = m_Card.getTrip().get(index).getOperatorName();
        final String busName = m_Card.getTrip().get(index).getBusName();
        m_TimeHash = m_Card.getTrip().get(index).getTripId();

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.help_us);

        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        if(busName.equals("")){
            final LayoutInflater layoutInflater = LayoutInflater.from(this);
            View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
            alertDialogBuilder.setView(promptView);

            final Spinner operatorSpinner = (Spinner) promptView.findViewById(R.id.operatorInput);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                    Utils.getOperatorNames(this));
            operatorSpinner.setAdapter(adapter);

            operatorSpinner.setSelection(adapter.getPosition(operatorName));

            final Spinner busSpinner = (Spinner) promptView.findViewById(R.id.busInput);
            final String[] items = new String[]{""};
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
            busSpinner.setAdapter(adapter);

            final EditText busInputText = (EditText) promptView.findViewById(R.id.busText);
            final EditText operatorInputText = (EditText) promptView.findViewById(R.id.operatorText);
            final TextView busLabel = (TextView) promptView.findViewById(R.id.busLabel);
            final LinearLayout busLayout = (LinearLayout) promptView.findViewById(R.id.busLayout);

            operatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> spinner, View container,
                                           int position, long id) {
                    String operator = (String) spinner.getSelectedItem();
                    String items[];
                    if (operator.equals("")){
                        busLabel.setVisibility(View.GONE);
                        busLayout.setVisibility(View.GONE);
                        operatorInputText.setVisibility(View.GONE);
                        items = new String[]{""};
                    }
                    else if(operator.equals(getString(R.string.other))){
                        operatorInputText.setVisibility(View.VISIBLE);
                        busLabel.setVisibility(View.VISIBLE);
                        busLayout.setVisibility(View.VISIBLE);
                        items = new String[]{getString(R.string.other)};
                    }
                    else{
                        operatorInputText.setVisibility(View.GONE);
                        busLabel.setVisibility(View.VISIBLE);
                        busLayout.setVisibility(View.VISIBLE);
                        items = Utils.getBusNames(spinner.getContext(), operator);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(),
                            android.R.layout.simple_spinner_dropdown_item, items);
                    busSpinner.setAdapter(adapter);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub
                }
            });

            busSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> spinner, View container,
                                           int position, long id) {
                    String bus = (String) spinner.getSelectedItem();
                    if(bus.equals(getString(R.string.other))) {
                        busInputText.setVisibility(View.VISIBLE);
                    }
                    else{
                        busInputText.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub
                }
            });

            alertDialogBuilder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean missing = false;
                    String operator = (String) operatorSpinner.getSelectedItem();
                    if (operator.equals(getString(R.string.other))) {
                        operator = operatorInputText.getText().toString();
                        missing = true;
                    }

                    String bus = (String) busSpinner.getSelectedItem();
                    if (bus.equals(getString(R.string.other))) {
                        bus = busInputText.getText().toString();
                        missing = true;
                    }

                    if(sendData(bus, operator, true, missing))
                        m_AddButtons.get(index).setVisibility(View.GONE);
                }
            });
            alertDialogBuilder.show();
        }
        else{ // Vote
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View promptView = layoutInflater.inflate(R.layout.vote_dialog, null);
            alertDialogBuilder.setView(promptView);

            TextView text = (TextView) promptView.findViewById(R.id.operatorBusText);
            text.setText(operatorName + " - " + busName);

            final AlertDialog alert = alertDialogBuilder.show();

            Button good = (Button) promptView.findViewById(R.id.button_good);
            good.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(sendData(operatorName, busName, true, false))
                        m_AddButtons.get(index).setVisibility(View.GONE);
                    alert.cancel();
                }
            });

            Button bad = (Button) promptView.findViewById(R.id.button_bad);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(sendData(operatorName, busName, false, false))
                        m_AddButtons.get(index).setVisibility(View.GONE);
                    alert.cancel();
                }
            };
            bad.setOnClickListener(listener);
        }
    }

    public boolean sendData(String busName, String operatorName, boolean add, boolean missing){
        if((operatorName.equals("") || busName.equals("")) && !missing){
            return false;
        }
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        // Send POST request
        // "http://ec2-54-149-222-116.us-west-2.compute.amazonaws.com/"
        String data;
        String[] params;

        if(m_OperatorId == 0 || m_BusId == 0){
            data = "id=" + androidId + "&time=" + m_TimeHash + "&busName=" + busName
                    + "&operatorName=" + operatorName + "&cardData="
                    + m_Card.getRawSerialize().replaceAll("\n", "");
            params = new String[]{"http://lecteuropus.duckdns.org/card.php", data};
        }
        else{
            data = "id=" + androidId + "&time=" + m_TimeHash + "&busId=" + m_BusId + "&operatorId=" + m_OperatorId
                    + "&busName=" + busName + "&operatorName=" + operatorName + "&add=" + Boolean.toString(add);
            params = new String[]{"http://lecteuropus.duckdns.org/add.php", data};
        }

        if(missing)
            params[0] = "http://lecteuropus.duckdns.org/missing.php";
        SendHttpRequestTask t = new SendHttpRequestTask();
        t.execute(params);

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(m_PreviousAddTrip, true));
            bw.write(m_TimeHash);
            bw.newLine();
            bw.flush();
        }
        catch (IOException e){
            Log.e(TAG, "Error writing file");
        }
        finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
        Toast.makeText(this, R.string.thank_contribution, Toast.LENGTH_SHORT).show();
        return true;
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String name = params[1];

            String data = sendHttpRequest(url, name);
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
