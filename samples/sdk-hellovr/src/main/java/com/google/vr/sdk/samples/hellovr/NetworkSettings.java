package com.google.vr.sdk.samples.hellovr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

public class NetworkSettings extends Activity implements View.OnClickListener {

    Button BApplayAndStart = null;
    Button BApplySettings = null;
    Button BRestore = null;
    SeekBar SBHeadX = null;
    SeekBar SBHeadY = null;
    EditText EDRobotIP = null;
    EditText EDStreamURL = null;
    EditText EDSBHeadX = null;
    EditText EDSBHeadY = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);
        BApplayAndStart = (Button) findViewById(R.id.BApplayAndStart);
        BApplayAndStart.setOnClickListener(this);

        BApplySettings = (Button) findViewById(R.id.BApplySettings);
        BApplySettings.setOnClickListener(this);

        BRestore = (Button) findViewById(R.id.BRestore);
        BRestore.setOnClickListener(this);

        EDRobotIP = (EditText)findViewById(R.id.EDRobotIP);
        EDStreamURL = (EditText)findViewById(R.id.EDStreamURL);

        EDSBHeadX = (EditText) findViewById(R.id.EDSBHeadX);
        EDSBHeadY = (EditText) findViewById(R.id.EDSBHeadY);

        SBHeadX = (SeekBar) findViewById(R.id.SBHeadX);

        SBHeadX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (progress < 10) {
                    seekBar.setProgress(10);
                    progress = 10;
                }
                EditText EDSBHeadX = (EditText) findViewById(R.id.EDSBHeadX);
                EDSBHeadX.setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {}

        });

        SBHeadY = (SeekBar) findViewById(R.id.SBHeadY);

        SBHeadY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (progress < 10) {
                    progress = 10;
                    seekBar.setProgress(10);
                }
                EditText EDSBHeadY = (EditText) findViewById(R.id.EDSBHeadY);
                EDSBHeadY.setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        LoadPreferencesAndSetControls(getApplicationContext());
    }

    private void LoadPreferencesAndSetControls(Context context)
    {
        //AppSettings.SaveToSharedPreferences(context);
        AppSettings.ReadFromSharedPreferences(context);
        EDRobotIP.setText(AppSettings.RobotIp);
        EDStreamURL.setText(AppSettings.MJPEGstreamURL);
        SBHeadX.setProgress(AppSettings.MotionAngle);
        SBHeadY.setProgress(AppSettings.TurningAngle);
        EDSBHeadX.setText(String.valueOf(AppSettings.MotionAngle));
        EDSBHeadY.setText(String.valueOf(AppSettings.TurningAngle));
    }

    private void SavePreferences(Context context)
    {
        AppSettings.RobotIp = EDRobotIP.getText().toString();
        AppSettings.MJPEGstreamURL = EDStreamURL.getText().toString();
        AppSettings.MotionAngle = SBHeadX.getProgress();
        AppSettings.TurningAngle = SBHeadY.getProgress();
        AppSettings.SaveToSharedPreferences(context);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BApplayAndStart: {
                SavePreferences(getApplicationContext());
                Intent intent = new Intent(this, HelloVrActivity.class);
                //EditText editText = (EditText) findViewById(R.id.editText);
                //String message = editText.getText().toString();
                //intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
            }
            case R.id.BRestore: {
                LoadPreferencesAndSetControls(getApplicationContext());
            }
            case R.id.BApplySettings: {
                SavePreferences(getApplicationContext());
            }

        }
    }
}
