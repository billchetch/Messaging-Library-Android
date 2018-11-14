package com.bulan_baru.surf_forecast;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bulan_baru.surf_forecast_data.ClientDevice;
import com.bulan_baru.surf_forecast_data.SurfForecastRepository;

public class AboutDialogFragment extends AppCompatDialogFragment {
    private SurfForecastRepository repository;

    AboutDialogFragment(SurfForecastRepository repository){
        this.repository = repository;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        ClientDevice device = repository.getClientDevice();
        String lf = "\n";
        String blurb = "API Base URL: " + repository.getServiceManager().getApiBaseURL() + lf;
        blurb += "Client device ID: " + device.getDeviceID() + lf;
        blurb += "Using location from device: " + device.getLocationDeviceID() + lf;


        View contentView = inflater.inflate(R.layout.about_dialog, null);
        TextView btv = (TextView)contentView.findViewById(R.id.aboutBlurb);
        btv.setText(blurb);

        builder.setView(contentView)
                .setTitle(R.string.app_name);

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
