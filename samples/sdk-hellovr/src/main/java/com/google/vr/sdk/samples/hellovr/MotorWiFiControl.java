package com.google.vr.sdk.samples.hellovr;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MotorWiFiControl {
    public enum Command {
        Forward, Backward, Stop, Left, Right;
    }

    public static final String []CommandString = {"fw", "bk", "st", "lt", "rt"};

    public static void SendRequest(String url, Command command, Context context)
    {
        String com = url + "/" + CommandString[command.ordinal()];
        SendRequest(com, context);
    }

    private static long MaxTimeSpan = 2000;
    private static long MinTimeSpan = 500;

    private static long CurrentTimeMillis = 0;
    private static RequestQueue queue = null;
    public static void SendRequest(String url, Context context)
    {
        long t = System.currentTimeMillis();
        //if (Math.abs(CurrentTimeMillis - t) > MaxTimeSpan)
        if (t - CurrentTimeMillis  > MaxTimeSpan)
        {
            CurrentTimeMillis = t;
        }
        else
        {
            return;
        }
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
            //String url ="http://www.google.com";
        }
// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        CurrentTimeMillis = 0;//System.currentTimeMillis() + MaxTimeSpan - MinTimeSpan;
                        // Display the first 500 characters of the response string.
                        //mTextView.setText("Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                        CurrentTimeMillis = 0;//System.currentTimeMillis() + MaxTimeSpan - MinTimeSpan;
                //mTextView.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
