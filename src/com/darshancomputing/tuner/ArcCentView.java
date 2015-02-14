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
import android.util.AttributeSet;

public class ArcCentView extends AbstractCentView {
    private static final float MAX_ANGLE = 30.0f / 180 * (float) java.lang.Math.PI;
    private float needle_len;

    public ArcCentView(Context c) {
        super(c);
    }

    /* This and the next constructor are necessary for inflating from XML */
    public ArcCentView(Context c, android.util.AttributeSet attrs) {
        super(c, attrs);
    }

    public ArcCentView(Context c, android.util.AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        needle_len = h * 0.9f;
    }

    @Override
    protected void drawNeedle(Canvas canvas) {
        float c;
        if (animating)
            c = animCents;
        else
            c = cents;

        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);

        float theta = MAX_ANGLE * c / 50;

        canvas.drawLine(width / 2,
                        height,
                        (width / 2) + needle_len * (float) java.lang.Math.sin(theta),
                        height -      needle_len * (float) java.lang.Math.cos(theta),
                        paint);
    }

    @Override
    protected void drawStatic(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);

        canvas.drawLine(width / 2, height / 10, width / 2, height / 100, paint);
    }
}
