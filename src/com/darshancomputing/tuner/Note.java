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

/*
  TarsosDSP's apparent range of what it can recognize:
  Low:  F1      @ ~43.6535
  High: G♯7/A♭7 @ ~3322.44

  (TODO: Look into this and tweak library code?)
*/
public class Note {
    private String name;
    private float cents;
    private boolean isNull;
    private float lastHz;
    private float lastCents; // Last actual measured cents, so we don't keep averaging with last average

    private static String[] notes = {"A", "A♯ / B♭", "B", "C", "C♯ / D♭", "D", "D♯ / E♭", "E", "F", "F♯ / G♭", "G", "G♯ / A♭"};

    public Note() {
        fromHz(-1);
    }

    public Note(float hz) {
        fromHz(hz);
    }

    public String getName() {
        return name;
    }

    public float getCents() {
        return cents;
    }

    public boolean isNull() {
        return isNull;
    }

    // Sets / updates note; if same note (same name and same octave), averages last and current cents
    public void fromHz(float hz) {
        if (hz < 0) {
            isNull = true;
            name = "—";//"☺";//"//";//"∅";//"N/A";
            cents = 0;
            return;
        }

        isNull = false;

        float semi = log2(java.lang.Math.pow(hz / 440.0, 12.0));
        int roundedSemi = java.lang.Math.round(semi);
        int note = (roundedSemi % 12 + 12) % 12; // Modules can be negative in Java
        String newName = notes[note];
        float newCents = (semi - roundedSemi) * 100;

        if (newName.equals(name) && java.lang.Math.abs(hz - lastHz) / hz < 0.5) {
            cents = (lastCents + newCents) / 2;
        } else {
            cents = newCents;
        }

        name = newName;
        lastHz = hz;
        lastCents = newCents;
    }

    private float log2(double n) {
        return (float) (java.lang.Math.log(n) / java.lang.Math.log(2));
    }

}
