package com.bulan_baru.surf_forecast;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ErrorDialogFragment extends AppCompatDialogFragment {

    private int errorType;
    private String errorMessage;

    ErrorDialogFragment(int errorType, String errorMessage){
        this.errorType = errorType;
        this.errorMessage = errorMessage;
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

        View contentView = inflater.inflate(R.layout.error_dialog, null);
        TextView details = (TextView)contentView.findViewById(R.id.errorDetails);
        details.setText(errorType + ": " + errorMessage);

        builder.setView(contentView)
                .setTitle(getString(R.string.app_name) + " error!");

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
