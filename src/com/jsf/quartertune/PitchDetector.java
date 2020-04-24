/*
    Copyright (c) 2015 Darshan Computing, LLC

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

import android.media.AudioRecord;
import android.media.MediaRecorder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;

public class PitchDetector {
    public AudioDispatcher dispatcher;

    private AudioRecord audioInputStream;
    private PitchProcessor mPp;
    private int mSampleRate;
    private int mAudioBufferSize;
    private int mBufferOverlap;
    private boolean started;

    /* sampleRate:      The requested sample rate.
     * audioBufferSize: The size of the audio buffer (in samples).
     * bufferOverlap:   The size of the overlap (in samples).
     */
    public PitchDetector(int sampleRate, int audioBufferSize, int bufferOverlap, PitchProcessor pp) {
        mSampleRate = sampleRate;
        mAudioBufferSize = audioBufferSize;
        mBufferOverlap = bufferOverlap;
        mPp = pp;
    }

    // Initially based on AudioDispatcherFactory.java from TarsosDSP
    private boolean initMic() {
        if (AudioRecord.getMinBufferSize(mSampleRate, android.media.AudioFormat.CHANNEL_IN_MONO,
                                         android.media.AudioFormat.ENCODING_PCM_16BIT) < 0) {
            return false;
        }

        audioInputStream = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                                           android.media.AudioFormat.CHANNEL_IN_MONO,
                                           android.media.AudioFormat.ENCODING_PCM_16BIT,
                                           mAudioBufferSize * 2);

        if (audioInputStream.getState() != AudioRecord.STATE_INITIALIZED) {
            return false;
        }

        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(mSampleRate, 16, 1, true, false);
		
        TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, format);
        dispatcher = new AudioDispatcher(audioStream, mAudioBufferSize, mBufferOverlap);

        return true;
    }

    public boolean start() {
        started = false;

        if (! initMic()) return started;

        dispatcher.addAudioProcessor(mPp);

        audioInputStream.startRecording();

        if (audioInputStream.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            return started;
        }

        new Thread(dispatcher,"Audio Dispatcher").start();

        started = true;
        return started;
    }

    public boolean started() {
        return started;
    }

    public void stop() {
        started = false;

        if (dispatcher == null) return;

        dispatcher.stop();

        try {
            audioInputStream.stop();
        } catch(Exception e) {
            /* Sometimes AudioRecord.stop() throws an exception.  I don't know why, but it seems safe
                 to ignore it here since we're done with audioInputStream anyway.
             */
        }
        audioInputStream.release();
        audioInputStream = null;
    }
}
