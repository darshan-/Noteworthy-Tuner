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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class HorizontalCentView extends ImageView {
    private Context context;
    private Paint paint;
    private int width, height;
    private float cents;

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
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);

        float x = width / 2;
        x += cents / 100 * width;

        canvas.drawLine(x, 0 + 15, x, height - 15, paint);
    }

    private void drawCenter(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);

        canvas.drawLine(width / 2, 0 + 10, width / 2, height - 10, paint);
    }

    public void setCents(float c) {
        cents = c;
        invalidate();
    }
}
