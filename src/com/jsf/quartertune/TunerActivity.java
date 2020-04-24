/*
    Copyright (c) 2015-2017 Darshan Computing, LLC

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.jsf.quartertune;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class TunerActivity extends Activity {
    private SharedPreferences settings;
    private SharedPreferences sp_store;

    private LayoutInflater mInflater;

    private Resources res;

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

    private static final String P_RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO;
    private static final int PR_RECORD_AUDIO = 1;

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PR_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(this, res.getString(R.string.need_mic_permission), Toast.LENGTH_SHORT).show();
                    checkMicPermission();
                }

                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_wrapper);

        res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setTitle(R.string.app_full_name);
    }

    private boolean checkMicPermission() {
        if (android.os.Build.VERSION.SDK_INT < 23)
            return true;

        if (checkSelfPermission(P_RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{P_RECORD_AUDIO}, PR_RECORD_AUDIO);
            return false;
        }

        return true;
    }

    private void init() {
        final AbstractCentView centView = (AbstractCentView) findViewById(R.id.cent_view);
        centView.setAnimationDuration(1000 / (SAMPLE_RATE / SAMPLES));
        centView.setNeedleColor(NULL_NEEDLE_COLOR);

        String default_a4_hz = res.getString(R.string.default_a4_hz);
        String a4_hz = settings.getString(SettingsActivity.KEY_A4_HZ, default_a4_hz);
        if ("other".equals(a4_hz))
            a4_hz = settings.getString(SettingsActivity.KEY_A4_HZ_OTHER, default_a4_hz);
        final float a4f = java.lang.Float.parseFloat(a4_hz);
        final Note n = new Note(a4f);

        TextView text = (TextView) findViewById(R.id.a4Hz);
        if (a4f != 440.0) {
            text.setText("A4=" + a4f + " Hz");
            text.setVisibility(View.VISIBLE);
        } else {
            text.setVisibility(View.INVISIBLE);
        }

        final TextView pitchInHz_tv = (TextView) findViewById(R.id.pitchInHz);
        final TextView note_tv = (TextView) findViewById(R.id.note);
        final TextView tooFlat_tv = (TextView) findViewById(R.id.tooFlat);
        final TextView tooSharp_tv = (TextView) findViewById(R.id.tooSharp);

        tooFlat_tv.setVisibility(View.INVISIBLE);
        tooSharp_tv.setVisibility(View.INVISIBLE);

        // Initially based on http://0110.be/posts/TarsosDSP_on_Android_-_Audio_Processing_in_Java_on_Android
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
                            tooFlat_tv.setVisibility(View.INVISIBLE);
                            tooSharp_tv.setVisibility(View.INVISIBLE);
                        } else {
                            hzStr = "" + (java.lang.Math.round(hz * 10) / 10.0) + " Hz";
                            if (java.lang.Math.abs(n.getCents()) < IN_TUNE_CENTS) {
                                color = Color.GREEN;
                                tooFlat_tv.setVisibility(View.INVISIBLE);
                                tooSharp_tv.setVisibility(View.INVISIBLE);
                            } else {
                                color = Color.YELLOW;

                                if (settings.getBoolean(SettingsActivity.KEY_FLAT_SHARP_HINT, false)) {
                                    if (n.getCents() < 0) {
                                        tooFlat_tv.setVisibility(View.VISIBLE);
                                        tooSharp_tv.setVisibility(View.INVISIBLE);
                                    } else {
                                        tooFlat_tv.setVisibility(View.INVISIBLE);
                                        tooSharp_tv.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }

                        //text = (TextView) findViewById(R.id.pitchInHz);
                        pitchInHz_tv.setText(hzStr);
                        //text = (TextView) findViewById(R.id.note);
                        note_tv.setText("" + n.getName());
                        note_tv.setTextColor(color);

                        centView.setCents(n.getCents());
                        centView.setNeedleColor(color);
                    }
                });                        
            }
        };

        // 1.0 multiplier
        // Normal buffer; should be medium response speed, medium ability to detect low pitch
        //   Seems like a very good default still.

        // 2.0 multiplier
        // Large buffer; should be slow response, more able to detect low pitch
        //   Does seem somewhat more accurate at very low pitches.  May be favorable in some
        //    circumstances to have needle move less frequently.  So a worthwhile option to
        //    have while sticking with normal value by default.

        float bufMult = 1.0f;
//         if (settings.getBoolean(SettingsActivity.KEY_LARGER_BUFFER, false))
//             bufMult = 2.0f;
        int bufSize = (int) (SAMPLES * bufMult);
        pp = new PitchProcessor(ALGORITHM, SAMPLE_RATE, bufSize, pdh);
        detector = new PitchDetector(SAMPLE_RATE, bufSize, 0 /* SAMPLES / 2 */, pp);

        checkMicAvailable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (checkMicPermission())
            init();

        checkMicAvailable();
    }

    private void checkMicAvailable() {
        if (detector != null && !detector.started() && !detector.start()) {
            showDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE);
            micUnavailableDialogShowing = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (micUnavailableDialogShowing) {
            dismissDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE);
            micUnavailableDialogShowing = false;
        }

        if (detector != null)
            detector.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
      case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
            return true;
        case R.id.menu_help:
            mStartActivity(HelpActivity.class);
            return true;
        case R.id.menu_rate_and_review:
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                                         Uri.parse("market://details?id=com.jsf.quartertune")));
            } catch (Exception e) {
                Toast.makeText(this, "Sorry, can't launch Play Store!", Toast.LENGTH_SHORT).show();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
                        finishActivity(1);
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

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(getPackageName(), c.getName());
        startActivityForResult(new Intent().setComponent(comp), 1);
    }
}
