package com.google.vr.sdk.samples.hellovr;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    //192.168.1.101
    public static String RobotIp = "http://192.168.4.1";
    public static String MJPEGstreamURL = "http://192.168.1.39:5000/video_feed";
    public static int TurningAngle = 20;
    public static int MotionAngle = 20;
    public static void ReadFromSharedPreferences(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences("appsettings", Context.MODE_PRIVATE);
        RobotIp = prefs.getString("RobotIp",RobotIp);
        MJPEGstreamURL = prefs.getString("MJPEGstreamURL",MJPEGstreamURL);
        TurningAngle = prefs.getInt("TurningAngle",TurningAngle);
        MotionAngle = prefs.getInt("MotionAngle",MotionAngle);
    }

    public static void SaveToSharedPreferences(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences("appsettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("RobotIp",RobotIp);
        editor.putString("MJPEGstreamURL",MJPEGstreamURL);
        editor.putInt("TurningAngle",TurningAngle);
        editor.putInt("MotionAngle",MotionAngle);
        editor.commit();
    }

}
