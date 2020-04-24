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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class ArcCentView extends AbstractCentView {
    private static final float MAX_ANGLE = 22.0f / 180 * (float) java.lang.Math.PI;

    // Constants as ratio of View height
    private static final float NEEDLE_LEN = 0.85f;
    private static final float NEEDLE_WIDTH = 0.005f;
    private static final float NEEDLE_BASE_Y = 0.1f;
    private static final float NEEDLE_BASE_DISC_RADIUS = 0.015f;
    private static final float NEEDLE_BASE_CIRC_RADIUS = 0.03f;

    private float needle_len;
    private float needle_base_x;
    private float needle_base_y;

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
        needle_len = h * NEEDLE_LEN;
        needle_base_x = width / 2;
        needle_base_y = height * (1 - NEEDLE_BASE_Y);
    }

    @Override
    protected void drawNeedle(Canvas canvas) {
        float c;
        if (animating)
            c = animCents;
        else
            c = cents;

        paint.setColor(needleColor);
        paint.setStrokeWidth(height * NEEDLE_WIDTH);

        float theta = MAX_ANGLE * c / 50;

        canvas.drawLine(needle_base_x,
                        needle_base_y,
                        needle_base_x + needle_len * (float) java.lang.Math.sin(theta),
                        needle_base_y - needle_len * (float) java.lang.Math.cos(theta),
                        paint);
    }

    @Override
    protected void drawStatic(Canvas canvas) {
        float sw = height * NEEDLE_WIDTH * 2.7f;
        paint.setColor(Color.RED);
        paint.setStrokeWidth(sw);

        canvas.drawLine(needle_base_x, height * 0.1f, needle_base_x, sw, paint);

        drawNeedleBase(canvas);
    }

    private void drawNeedleBase(Canvas canvas) {
        paint.setColor(needleColor);
        canvas.drawCircle(needle_base_x, needle_base_y, NEEDLE_BASE_DISC_RADIUS * height, paint);

        paint.setStrokeWidth(height * NEEDLE_WIDTH * 1.5f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(needle_base_x, needle_base_y, NEEDLE_BASE_CIRC_RADIUS * height, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
