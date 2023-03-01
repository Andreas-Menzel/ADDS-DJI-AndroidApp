package com.andreasmenzel.adds_dji.Events;

import android.widget.Toast;

public class ToastMessage {

    public String message;
    public int toastLength;


    public ToastMessage(String message) {
        this.message = message;
        this.toastLength = Toast.LENGTH_LONG;
    }

    public ToastMessage(String message, int toastLength) {
        this.message = message;
        this.toastLength = toastLength;
    }

}
