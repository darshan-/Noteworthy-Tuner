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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class HorizontalCentView extends ImageView {
    private Context context;
    private Paint paint;
    private int width, height;
    private float cents;
    private float animCents;

    private final Handler mHandler = new Handler();
    private int animDuration;
    private long animStartMs, animEndMs;
    private float animStartCents, animEndCents;
    private boolean animating;

    private final Runnable animate = new Runnable() {
        public void run() {
            long now = System.currentTimeMillis();
            System.out.println("........... " + now + ": old animCents: " + animCents);
            float timePos = (float) (now - animStartMs) / (animEndMs - animStartMs);
            System.out.println("  ...... timePos: " + timePos);
            System.out.println("  ...... animStartCents: " + animStartCents);
            System.out.println("  ...... animEndCents: " + animEndCents);
            animCents = animStartCents + (animEndCents - animStartCents) * timePos;
            System.out.println("  ...... new animCents: " + animCents);
            invalidate();

            if (now < animEndMs)
                mHandler.postDelayed(animate, 15);
            else
                endAnim();
        }
    };

    public HorizontalCentView(Context c) {
        super(c);
        context = c;
        initialize();
    }

    /* This and the next constructor are necessary for inflating from XML */
    public HorizontalCentView(Context c, android.util.AttributeSet attrs) {
        super(c, attrs);
        context = c;
        initialize();
    }

    public HorizontalCentView(Context c, android.util.AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
        context = c;
        initialize();
    }

    private void initialize() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.FILL);
        paint.setDither(true);

        setCents(0);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
    }

    @Override
    protected void onDraw (Canvas canvas) {
        super.onDraw(canvas);

        drawCenter(canvas);
        drawNeedle(canvas); // Draw second, so it's on top
    }

    private void drawNeedle(Canvas canvas) {
        float c;
        if (animating)
            c = animCents;
        else
            c = cents;

        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);

        float x = width / 2;
        x += c / 100 * width;

        canvas.drawLine(x, 0 + 15, x, height - 15, paint);
    }

    private void drawCenter(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);

        canvas.drawLine(width / 2, 0 + 10, width / 2, height - 10, paint);
    }

    private void startAnim(float c) {
        endAnim();
        animating = true;
        animStartMs = System.currentTimeMillis();
        animEndMs = animStartMs + animDuration;
        animStartCents = cents;
        animEndCents = c;
        animCents = cents; // In case View is invalidated before animate.run() is called
        System.out.println("********* Animating from " + animStartCents + " to " + animEndCents);
        mHandler.post(animate);
    }

    private void endAnim() {
        animating = false;
        mHandler.removeCallbacks(animate);
    }

    public void setCents(float c) {
        if (c == cents) return;

        if (animDuration <= 0)
            invalidate();
        else
            startAnim(c);

        // Don't set until after calling startAnim(); it needs old value of cents
        cents = c;
    }

    public void setAnimationDuration(int ms) {
        animDuration = ms;
    }
}
