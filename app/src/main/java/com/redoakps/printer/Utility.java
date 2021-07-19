package com.redoakps.printer;

import java.lang.reflect.Method;

import saioapi.base.Misc;

public final class Utility {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String binToStr(byte[] bData) {
        return binToStr(bData, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String binToStr(byte[] bData, boolean includeSpace) {
        if (bData == null) {
            return "null";
        }

        final StringBuilder builder = new StringBuilder();
        for (byte b : bData) {
            if (includeSpace) {
                builder.append(String.format("%02X ", b));
            }
            else {
                builder.append(String.format("%02X", b));
            }
        }
        return builder.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String binToStr(byte[] bData, boolean includeSpace, int maxLength) {
        if (bData == null) {
            return "null";
        }

        final StringBuilder builder = new StringBuilder();
        for (byte b : bData) {
            if (includeSpace) {
                builder.append(String.format("%02X ", b));
            }
            else {
                builder.append(String.format("%02X", b));
            }

            if (builder.length() >= maxLength) {
                builder.append("...");
                break;
            }
        }
        return builder.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static void delay(int timeout) {
        try {
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public enum Device {DEVICE_200T, DEVICE_200I, DEVICE_200D, DEVICE_200CP, DEVICE_200NP, DEVICE_A3, DEVICE_AT100, DEVICE_AT150, DEVICE_AT170, DEVICE_AP10, DEVICE_AP210S, DEVICE_SC200Y_JP, DEVICE_UNKNOWN}

    private static final String XAC_DEVICE_200T = "xCE-200T";
    private static final String XAC_DEVICE_200I = "xCE-200I";
    private static final String XAC_DEVICE_200D = "xCE-200D";
    private static final String XAC_DEVICE_200CP = "xCL_E200CP";
    private static final String XAC_DEVICE_200NP = "xCL_E200NP";
    private static final String XAC_DEVICE_A3 = "PRESTO-A3";
    private static final String XAC_DEVICE_AT100 = "AT-100";
    private static final String XAC_DEVICE_AT150 = "AT-150";
    private static final String XAC_DEVICE_AT170 = "AT-170";
    private static final String XAC_DEVICE_AT100R = "AT100";
    private static final String XAC_DEVICE_AT150R = "AT150";
    private static final String XAC_DEVICE_AT170R = "AT170";
    private static final String XAC_DEVICE_AP10 = "AP-10";
    private static final String XAC_DEVICE_AP210S = "AP-210S";  //saiocfg
    private static final String XAC_DEVICE_AP210S_1 = "ap210s"; //property
    private static final String XAC_DEVICE_SC200Y_JP = "XAC_SC200Y_JP";

    public static Device getDeviceType() {
        byte[] systemInfo = new byte[100];
        Misc.getSystemInfo(Misc.INFO_PRODUCT, systemInfo);
        String value = new String(systemInfo);

        if (value.startsWith(XAC_DEVICE_200T)) {
            return Device.DEVICE_200T;
        }
        else if (value.startsWith(XAC_DEVICE_200I)) {
            return Device.DEVICE_200I;
        }
        else if (value.startsWith(XAC_DEVICE_200D)) {
            return Device.DEVICE_200D;
        }
        else if (value.startsWith(XAC_DEVICE_200CP)) {
            return Device.DEVICE_200CP;
        }
        else if (value.startsWith(XAC_DEVICE_200NP)) {
            return Device.DEVICE_200NP;
        }
        else if (value.startsWith(XAC_DEVICE_A3)) {
            return Device.DEVICE_A3;
        }
        else if (value.contains(XAC_DEVICE_AT100) || value.contains(XAC_DEVICE_AT100R)) {
            return Device.DEVICE_AT100;
        }
        else if (value.contains(XAC_DEVICE_AT150) || value.contains(XAC_DEVICE_AT150R)) {
            return Device.DEVICE_AT150;
        }
        else if (value.contains(XAC_DEVICE_AT170) || value.contains(XAC_DEVICE_AT170R)) {
            return Device.DEVICE_AT170;
        }
        else if (value.contains(XAC_DEVICE_AP10)) {
            return Device.DEVICE_AP10;
        }
        else if (value.contains(XAC_DEVICE_AP210S)) {    //saiocfg
            return Device.DEVICE_AP210S;
        }
        else if (value.contains(XAC_DEVICE_SC200Y_JP)) {
            return Device.DEVICE_SC200Y_JP;
        }
        else {
            try {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                Method get = cls.getDeclaredMethod("get", String.class, String.class);
                value = (String) (get.invoke(null, new Object[]{"ro.product.name", ""}));
            } catch (Exception ignored) {}
            //
            if (value.contains(XAC_DEVICE_AP210S_1)) {  //property
                return Device.DEVICE_AP210S;
            }

            return Device.DEVICE_UNKNOWN;
        }
    }
    
}
