package com.microsoft.identity.common;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CodeMarkerManager {

    private static boolean enableCodeMarker = false;
    private static int MAX_SIZE_CODE_MARKER = 1000;
    private static volatile List<CodeMarker> codeMarkers = new ArrayList<CodeMarker>();
    private static long baseMilliSeconds = 0;
    public static void markCode(int marker) {
        if(enableCodeMarker) {
            if(codeMarkers.size() >= MAX_SIZE_CODE_MARKER) {
                clear();
            }
            long currentMilliSeconds = System.currentTimeMillis();
            if (codeMarkers.size() == 0) {
                baseMilliSeconds = currentMilliSeconds;
            }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            codeMarkers.add(new CodeMarker(marker, currentMilliSeconds - baseMilliSeconds, f.format(new Date()), Thread.currentThread().getId()));
        }
    }

    public static void setEnableCodeMarker(boolean enableCodeMarker) {
        CodeMarkerManager.enableCodeMarker = enableCodeMarker;
    }

    public static void clear(){
        codeMarkers.clear();
    }

    public static void writeToFile(String fileName) {
        String stringToWrite = getFileContent();
        File file = Environment.getExternalStorageDirectory();
        String strFilePath = file.getAbsolutePath() + "/" + fileName;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(strFilePath);
            FileWriter fileWriter = new FileWriter(fileOutputStream.getFD());
            fileWriter.write(stringToWrite);
            fileWriter.close();
            fileOutputStream.getFD().sync();
            fileOutputStream.close();
            System.out.println("Test");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFileContent() {
        String stringToWrite = "TimeStamp,Marker,Time,Thread,CpuUsed,CpuTotal,ResidentSize,VirtualSize,WifiSent,WifiRecv,WwanSent,WwanRecv,AppSent,AppRecv,Battery,SystemDiskRead,SystemDiskWrite";

        for(CodeMarker codeMarker : codeMarkers) {
            stringToWrite += "\n"
                            + codeMarker.getTimeStamp()
                            + "," + codeMarker.getMarker()
                            + "," + codeMarker.getTimeMilliseconds()
                            + "," + codeMarker.getThreadId()
                            + "," + ",NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA";
        }
        return stringToWrite;
    }
}
