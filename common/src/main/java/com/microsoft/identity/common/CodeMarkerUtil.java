package com.microsoft.identity.common;

import java.util.List;

public class CodeMarkerUtil {
    public static String getCSVContent(List<CodeMarker> codeMarkers) {
        StringBuilder stringToWrite = new StringBuilder("TimeStamp,Marker,Time,Thread,CpuUsed,CpuTotal,ResidentSize,VirtualSize,WifiSent,WifiRecv,WwanSent,WwanRecv,AppSent,AppRecv,Battery,SystemDiskRead,SystemDiskWrite");
        for(CodeMarker codeMarker : codeMarkers) {
            stringToWrite.append("\n");
            stringToWrite.append(codeMarker.getCSVString());
        }
        return stringToWrite.toString();
    }
}
