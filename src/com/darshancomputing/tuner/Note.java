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
    public String name;
    public float cents;

    private static String[] notes = {"A", "A♯ / B♭", "B", "C", "C♯ / D♭", "D", "D♯ / E♭", "E", "F", "F♯ / G♭", "G", "G♯ / A♭"};

    public Note() {
        fromHz(-1);
    }

    public Note(float hz) {
        fromHz(hz);
    }

    public void fromHz(float hz) {
        if (hz < 0) {
            name = "N/A";
            cents = 0;
            return;
        }
        
        float semi = log2(java.lang.Math.pow(hz / 440.0, 12.0));
        int roundedSemi = java.lang.Math.round(semi);
        int note = (roundedSemi % 12 + 12) % 12; // Modules can be negative in Java
        name = notes[note];
        cents = (semi - roundedSemi) * 100;
    }

    private float log2(double n) {
        return (float) (java.lang.Math.log(n) / java.lang.Math.log(2));
    }

}
