package com.emre.wifibruter;

import android.content.Context;
import java.io.FileOutputStream;

public class FileWriter {
    //SharedPreferences sevmiyorum, buradaki metoda göre gereksiz process ve fazla yer kaplıyor
    public static void writeFile(String fileName, Context context, String content){
        try {
            FileOutputStream fOut = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fOut.write(content.getBytes());
            fOut.flush();
            fOut.close();
        }
        catch (Exception e) {

        }
    }
    public static void appendFile(String fileName, Context context, String content){
        try {
            FileOutputStream fOut = context.openFileOutput(fileName, Context.MODE_APPEND);
            fOut.write((content+"\n").getBytes());
            fOut.flush();
            fOut.close();
        }
        catch (Exception e) {

        }
    }
}