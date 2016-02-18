package org.lukedowell.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ArrayList<String> forecast = new ArrayList<>();

        mForecastAdapter =
                new ArrayAdapter<>(
                    getActivity(),
                    R.layout.list_item_forecast,
                    R.id.list_item_forecast_textview,
                    forecast);

        View view = inflater.inflate(R.layout.forecast_fragment, container, false);
        ListView forecastListView = (ListView) view.findViewById(R.id.list_view_forecast);

        forecastListView.setAdapter(mForecastAdapter);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String forecast = mForecastAdapter.getItem(position);

                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, forecast);

                startActivity(intent);
            }
        });

        return view;
    }

    private void updateWeather() {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String zipcodeString = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
            try {
                int zipcode = Integer.parseInt(zipcodeString);
                String[] forecast = new FetchWeatherTask().execute(zipcode).get();
                List<String> forecastList = Arrays.asList(forecast);
                mForecastAdapter.clear();
                mForecastAdapter.addAll(forecastList);
            } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Make sure to enter a valid zipcode!", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_refresh:
                updateWeather();
                break;
            case R.id.action_settings:
                Log.v("ForecastFragment", "SETTINGS");
                break;
        }
        return true;
    }

    public class FetchWeatherTask extends AsyncTask<Integer, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Integer... zipcodes) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String format = "json";
            String units = pref.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_default));

            int numDays = 7;

            try {

                Uri builtUri = Uri.parse(BuildConfig.API_URL).buildUpon()
                        .appendQueryParameter("q", zipcodes[0].toString())
                        .appendQueryParameter("mode", format)
                        .appendQueryParameter("units", units)
                        .appendQueryParameter("cnt", String.valueOf(numDays))
                        .appendQueryParameter("APPID", BuildConfig.API_KEY).build();

                Log.v("ForecastFragment", "Built URI: " + builtUri.toString());

                URL url = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();

                Log.v("ForecastFragment", "Connection opened...");

                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.v("ForecastFragment", "URL Connection success!");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("PlaceholderFragment", "Error ", e.fillInStackTrace());
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            Log.v("ForecastFragment", "JSON String: " + forecastJsonStr);
            String[] result;
            try {
                result = WeatherDataParser.getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (Exception e) {
                e.printStackTrace();
                result = new String[] {"ERROR"};
            }

            return result;
        }
    }
}
