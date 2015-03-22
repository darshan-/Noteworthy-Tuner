/*
    Copyright (c) 2015 Darshan-Josiah Barber

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.tuner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

// TODO: Keep screen on (past normal inactivity timeout)
//   (Will Activity still get paused on power button press?)
public class TunerActivity extends Activity {
    private SharedPreferences settings;
    private SharedPreferences sp_store;

    private LayoutInflater mInflater;

    private Resources res;
    private Context context;

    private final Handler mHandler = new Handler();

    private PitchDetector detector;
    private PitchProcessor pp;

    // 48000 and 41000 seem to work fine, too, but they don't really seem any better, and 22050 is
    //  noticeably faster on older devices
    private static final int SAMPLE_RATE_DEFAULT = 22050;
    private static final int SAMPLE_RATE = SAMPLE_RATE_DEFAULT;
    private static final int SAMPLES = SAMPLE_RATE / 5;

    private static final float IN_TUNE_CENTS = 4.0f;
    //private static final float MEDIUM_TUNE_CENTS = 12.0f;

    private static final int NULL_NEEDLE_COLOR = 0xffcccccc;

    // AMDF and FFT_PITCH are unworkably slow or don't work at all
    private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.FFT_YIN;
    //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
    //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.MPM;

    private static final int DIALOG_MUST_CLOSE_MIC_UNAVAILABLE = 0;
    private boolean micUnavailableDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home);

        res = getResources();
        context = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        setTitle(R.string.app_full_name);
        AbstractCentView centView = (AbstractCentView) findViewById(R.id.cent_view);
        centView.setAnimationDuration(1000 / (SAMPLE_RATE / SAMPLES));
        centView.setNeedleColor(NULL_NEEDLE_COLOR);

        // From http://0110.be/posts/TarsosDSP_on_Android_-_Audio_Processing_in_Java_on_Android

        final Note n = new Note();

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float hz = result.getPitch();
                n.fromHz(hz);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView text;
                        String hzStr;
                        int color;

                        if (n.isNull()) {
                            hzStr = "∅ Hz";
                            color = NULL_NEEDLE_COLOR;
                        } else {
                            hzStr = "" + (java.lang.Math.round(hz * 10) / 10.0) + " Hz";
                            if (java.lang.Math.abs(n.getCents()) < IN_TUNE_CENTS)
                                color = Color.GREEN;
                            else
                                color = Color.YELLOW;
                        }

                        text = (TextView) findViewById(R.id.pitchInHz);
                        text.setText(hzStr);
                        text = (TextView) findViewById(R.id.note);
                        text.setText("" + n.getName());
                        text.setTextColor(color);
                        AbstractCentView centView = (AbstractCentView) findViewById(R.id.cent_view);
                        centView.setCents(n.getCents());
                        centView.setNeedleColor(color);
                    }
                });                        
            }
        };

        pp = new PitchProcessor(ALGORITHM, SAMPLE_RATE, SAMPLES, pdh);
        detector = new PitchDetector(SAMPLE_RATE, SAMPLES, 0 /* SAMPLES / 2 */, pp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!detector.start()) {
            showDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE);
            micUnavailableDialogShowing = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (micUnavailableDialogShowing) {
            dismissDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE);
            micUnavailableDialogShowing = false;
        }

        detector.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_MUST_CLOSE_MIC_UNAVAILABLE:
            builder.setTitle(res.getString(R.string.mic_unavailable_title))
                .setMessage(res.getString(R.string.mic_unavailable_message))
                .setCancelable(false)
                .setPositiveButton(res.getString(R.string.okay), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        finish();
                        di.cancel();
                    }
                });

            dialog = builder.create();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

}
