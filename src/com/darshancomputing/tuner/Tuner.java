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


    Based on code from TarsosDSP
*/

package com.darshancomputing.tuner;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;

public class Tuner {
    public AudioDispatcher dispatcher;

    private AudioRecord audioInputStream;
    private PitchProcessor mPp;
    private int mSampleRate;
    private int mAudioBufferSize;
    private int mBufferOverlap;

    /* sampleRate:      The requested sample rate.
     * audioBufferSize: The size of the audio buffer (in samples).
     * bufferOverlap:   The size of the overlap (in samples).
     */
    public Tuner(int sampleRate, int audioBufferSize, int bufferOverlap, PitchProcessor pp) {
        mSampleRate = sampleRate;
        mAudioBufferSize = audioBufferSize;
        mBufferOverlap = bufferOverlap;
        mPp = pp;
    }

    // From TarsosDSP
    private void initMic() {
        int minAudioBufferSize = AudioRecord.getMinBufferSize(mSampleRate,
                                                              android.media.AudioFormat.CHANNEL_IN_MONO,
                                                              android.media.AudioFormat.ENCODING_PCM_16BIT);
        int minAudioBufferSizeInSamples =  minAudioBufferSize / 2;

        if (minAudioBufferSizeInSamples <= mAudioBufferSize){
            audioInputStream = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                                               android.media.AudioFormat.CHANNEL_IN_MONO,
                                               android.media.AudioFormat.ENCODING_PCM_16BIT,
                                               mAudioBufferSize * 2);

            TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(mSampleRate, 16, 1, true, false);
		
            TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, format);
            dispatcher = new AudioDispatcher(audioStream, mAudioBufferSize, mBufferOverlap);
        } else {
            new IllegalArgumentException("Buffer size too small; should be at least " + (minAudioBufferSize *2));
            dispatcher = null;
        }
    }

    public void start() {
        initMic();
        dispatcher.addAudioProcessor(mPp);

        audioInputStream.startRecording();
        new Thread(dispatcher,"Audio Dispatcher").start();
    }

    public void stop() {
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
