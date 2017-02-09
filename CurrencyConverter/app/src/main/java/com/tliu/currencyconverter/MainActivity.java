package com.tliu.currencyconverter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import android.os.Handler;


public class MainActivity extends AppCompatActivity implements TextWatcher{
    //json data for debugging
    private String str = "{\"base\":\"CAD\",\"date\":\"2017-01-30\",\"rates\":{\"AUD\":1.009,\"BGN\":1.3987,\"BRL\":2.3828,\"CHF\":0.763,\"CNY\":5.228,\"CZK\":19.325,\"DKK\":5.319,\"GBP\":0.60742,\"HKD\":5.8983,\"HRK\":5.3474,\"HUF\":222.27,\"IDR\":10136.0,\"ILS\":2.8769,\"INR\":51.657,\"JPY\":87.077,\"KRW\":895.59,\"MXN\":15.794,\"MYR\":3.3679,\"NOK\":6.3476,\"NZD\":1.049,\"PHP\":37.831,\"PLN\":3.0973,\"RON\":3.2188,\"RUB\":45.612,\"SEK\":6.7503,\"SGD\":1.0854,\"THB\":26.813,\"TRY\":2.9007,\"USD\":0.76021,\"ZAR\":10.335,\"EUR\":0.71515}}";
    private String jsonStr = "";
    private JSONObject jsonObj = new JSONObject();
    private EditText editText = null;
    private ListView listView = null;

    private List<String> keyList = new ArrayList<String>();
    private List<String> valueList = new ArrayList<String>();
    private List<String> rateList = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Start background thread, to fetch rates from fixer.io
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                String url = "http://api.fixer.io/latest?base=CAD";
                new JsonTask().execute(url);
                Log.d("AsyncTask.execute","executed");
                new Handler().postDelayed(this, (1000 * 60 * 30));
                Looper.loop();
            }
        });

        editText = (EditText) findViewById(R.id.editText);
        editText.setSelection(editText.length());
        editText.addTextChangedListener(this);

        //Extract currency rates info from local file.
        try {
            //jsonObj = new JSONObject(str);
            InputStream inputStream = this.openFileInput("cc_rates.json");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                jsonStr = stringBuilder.toString();
                Log.d("ReadFile", ">>>  "+jsonStr);
            }
            jsonObj = new JSONObject(jsonStr);
            JSONObject ratesJsonObj = jsonObj.getJSONObject("rates");

            Iterator<?> keys = ratesJsonObj.keys();
            List<String> spinnerList = new ArrayList<String>();
            spinnerList.add(jsonObj.getString("base"));
            while( keys.hasNext() ) {
                String key = (String)keys.next();
                keyList.add(key);
                spinnerList.add(key);
                valueList.add(ratesJsonObj.getString(key));
                rateList.add(ratesJsonObj.getString(key));
            }
            //Add base rate
            keyList.add(jsonObj.getString("base"));
            valueList.add("1");
            rateList.add("1");

            Log.i("keyList", ">>>  "+keyList.toString());
            Log.i("valueList", ">>>  "+valueList.toString());

            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            ArrayAdapter spinnerAdapter = new ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, spinnerList.toArray());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                    String itemKey = parent.getItemAtPosition(pos).toString();
                    double newRateBase = Double.parseDouble(rateList.get(keyList.indexOf
                            (itemKey)));
                    ListIterator<String> iter0 = rateList.listIterator();
                    while(iter0.hasNext()) {
                        //update rates
                        iter0.set(Double.toString(Double.parseDouble(iter0.next()) / newRateBase));

                    }
                    editText.setText(editText.getText());
                    listView.invalidateViews();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //
                }
            });

            listView = (ListView) findViewById(R.id.listView);
            ArrayAdapter listAdapter = new ArrayAdapter(this,
                    android.R.layout.simple_list_item_2, android.R.id.text1, keyList){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                    text1.setText(keyList.get(position));
                    text2.setText(valueList.get(position));
                    return view;
                }
            };
            listView.setAdapter(listAdapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String times = editText.getText().toString();
        if (times.isEmpty()) {
            times = "0.00";
            editText.setText(times);
        }
        ListIterator<String> iter = valueList.listIterator();
        ListIterator<String> iter0 = rateList.listIterator();
        while(iter.hasNext() & iter0.hasNext()) {
            iter.next();
            iter.set(Double.toString(Double.parseDouble(iter0.next()) * Double.parseDouble
                    (times)));
        }
        listView.invalidateViews();
        Log.d("afterTextChanged","Done");
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();

                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response", "> " + line);

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("JsonTask.onPostExecute",result);
            //Save to local file
            String filename = "cc_rates.json";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(result.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("JsonTask.onPostExecute","Done");
        }
    }
}
