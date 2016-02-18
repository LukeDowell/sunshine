package org.lukedowell.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.v(getClass().getSimpleName(), "Creating detail view with forecast : " + forecast);
            TextView textView = (TextView) rootView.findViewById(R.id.definitelyUniqueId);
            textView.setText(forecast);

        } else {
            Toast.makeText(getContext(), "No forecast data received!", Toast.LENGTH_LONG).show();
        }
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }
}
