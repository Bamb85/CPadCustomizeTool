package com.saradabar.cpadcustomizetool;

import static com.saradabar.cpadcustomizetool.Common.getCrashLog;
import static com.saradabar.cpadcustomizetool.Common.saveCrashLog;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CrashDetection implements Thread.UncaughtExceptionHandler {

    Context mContext;
    Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public CrashDetection(Context context) {
        mContext = context;
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void uncaughtException(@NonNull Thread thread, Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();

        String[] str;
        if (getCrashLog(mContext) != null) {
            str = new String[]{String.join(",", getCrashLog(mContext)).replace("    ", "") + getNowDate() + stackTrace + "\n"};
        } else {
            str = new String[]{getNowDate() + stackTrace + "\n"};
        }
        saveCrashLog(str, mContext);

        mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
    }

    public String getNowDate(){
        DateFormat df = new SimpleDateFormat("MMM dd HH:mm:ss.SSS z yyyy :\n", Locale.ENGLISH);
        return df.format(System.currentTimeMillis());
    }
}