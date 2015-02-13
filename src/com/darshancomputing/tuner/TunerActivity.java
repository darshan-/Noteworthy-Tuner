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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
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

    private static final int SAMPLE_RATE = 48000;
    private static final int SAMPLES = SAMPLE_RATE / 5;

    // AMDF and FFT_PITCH are unworkably slow or don't work at all
    private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.FFT_YIN;
    //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
    //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.MPM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home);

        res = getResources();
        context = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        HorizontalCentView centView = (HorizontalCentView) findViewById(R.id.cent_view);
        centView.setAnimationDuration(1000 / (SAMPLE_RATE / SAMPLES));

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
                        // TODO: Show name in one color when "close enough" (abs(cents) < n) and another otherwise
                        TextView text = (TextView) findViewById(R.id.pitchInHz);
                        text.setText("" + (java.lang.Math.round(hz * 10) / 10.0) + " Hz");
                        text = (TextView) findViewById(R.id.note);
                        text.setText("" + n.getName());
                        //text = (TextView) findViewById(R.id.cents);
                        //text.setText("" + n.getCents());
                        HorizontalCentView centView = (HorizontalCentView) findViewById(R.id.cent_view);
                        centView.setCents(n.getCents());
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

        detector.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        detector.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }
}
