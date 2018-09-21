package com.emre.wifibruter;

import java.io.BufferedReader;
import java.io.File;

public class FileReader {

    public FileReader(){


    }

    public String getFileContext(String fileString){
        File file = new File("/data/data/com.emre.wifibruter/files/"+fileString);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new java.io.FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                //text.append("\n");
            }
            br.close();
        }
        catch (Exception e) {

        }
        return text.toString();
    }
    public String getFileContextWithNextLine(String fileString){
        File file = new File("/data/data/com.emre.wifibruter/files/"+fileString);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new java.io.FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();
        }
        catch (Exception e) {

        }
        return text.toString();
    }
}