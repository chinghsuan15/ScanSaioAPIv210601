package com.redoakps.printer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
//import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;

import saioapi.comm.Com;
import saioapi.util.SaioService;
import saioapi.util.Sys;

public class RedOakInternalPrinter extends RedOakPrinter {
    private final int WRITE_TIMEOUT = 12000;
    private final int READ_TIMEOUT = 12000;
    private final int MINIMUM_BATTERY_PERCENTAGE = 10;
    private final int MAXIMUM_BATTERY_TEMPERATURE = 60;

    private Com mPrinterCom;
    private int mPrinterHandle = Com.ERR_OPERATION;
    private Utility.Device mDevice;
    private float mTemperature;

    private final byte[] FEED_PAPER = {0x70, 0x00, 0x03, 0x1B, 0x4A, (byte) 0x80, (byte) 0xA2};
    private final byte[] LINE_FEED = {0x70, 0x00, 0x03, 0x1B, 0x4A, (byte) 0x10, (byte) 0x32};
    private final byte[] CANCEL = {0x70, 0x00, 0x02, 0x1D, 0x18, 0x77};
    private final byte[] RESET_PRINTER = {0x70, 0x00, 0x02, 0x1B, 0x40, 0x29};
    //private final byte[] CHECK_STATUS = {0x1D, 0x72};
    private final byte[] CHECK_STATUS = {0x70, 0x00, 0x02, 0x1D, 0x76, 0x19};

    private boolean mHasFlagKeepScreenOn = false;

    private Context mContext;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public PrintResult getStatus() {

        //non-print-job-thread should not invake write/read() (in)directly, it may cause rx data racing with print-job-thread!!!
        /*
        if(null == mPrinterCom)
            return PrintResult.FAILED;

        int length = mPrinterCom.write(mPrinterHandle, CHECK_STATUS, CHECK_STATUS.length, WRITE_TIMEOUT);

        if (length != Com.ERR_OPERATION) {
            byte[] response = new byte[10];
            length = mPrinterCom.read(mPrinterHandle, response, response.length, READ_TIMEOUT);

            byte[] st = {0x0, 0x0};
            //
            if (length == 0x02) {       //1D 72
                st[0] = response[0];
                st[1] = response[1];
            }
            else if (length == 0x07)    //1D 76 (with protocol)
            {
                st[0] = response[4];
                st[1] = response[5];
            }
            else
            {
                StringBuffer sb = new StringBuffer();
                sb.append("Unknown status returned!!! (len=").append(length).append(")");
                if(length != Com.ERR_OPERATION) {
                    sb.append(" - ");
                    for (int i = 0; i < length; i++)
                        sb.append(String.format("%02X ", response[i]));
                }
                Log.w(TAG, sb.toString());
            }
            Log.i(TAG, String.format("getStatus(): {%02X %02X}", st[0], st[1]));

            if (st[1] == (byte)0x88) {

                //0 Head temperature too high or too low
                //1 Power supply too high or too low
                //2 Buffer Full
                //3 Paper out
                //4 Printer in printing
                //5 Cutter in using
                if ((st[0] & 0x08) != (byte) 0x00)
                    return PrintResult.PAPER_OUT;

                if (st[0] == 0)
                    return PrintResult.OK;
            }
        }

        return PrintResult.FAILED;
        //*/

        //*
        addToArrayList(CHECK_STATUS);

        //delay 200ms to await for status response
        try{Thread.sleep(200);}catch(InterruptedException e){}

        return getPrintJobResult();
        //*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public RedOakInternalPrinter() {
        TAG = "RedOakInternalPrinter";
        mPrintWidth = PrintWidth.TP_48;
        mInterval = 20;
        mPrintJobTimeout = 500;
        mRunnable = new PrintRunnable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public RedOakInternalPrinter(int interval, int printJobTimeout) {
        TAG = "RedOakInternalPrinter";
        mPrintWidth = PrintWidth.TP_48;
        mInterval = interval;
        mPrintJobTimeout = printJobTimeout;
        mRunnable = new PrintRunnable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public PrintResult open(Context context) {
        mContext = context;

        mDevice = Utility.getDeviceType();

        // Do not attempt to open USB reader if we do not know what device this is as it could be a phone
        if (mDevice == Utility.Device.DEVICE_UNKNOWN) {
            return PrintResult.FAILED;
        }

        // Check the battery status - fail if battery is less than 5% charged
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mDevice == Utility.Device.DEVICE_AT100 ||
                    mDevice == Utility.Device.DEVICE_AT150 ||
                    mDevice == Utility.Device.DEVICE_AT170) {
                BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (manager != null) {
                    long percentage = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    Log.d(TAG, "Battery Percentage: " + percentage);

                    // Get the battery charging status
                    Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    Log.d(TAG, "Battery Status: " + status);

                    if (percentage < MINIMUM_BATTERY_PERCENTAGE &&
                            status != BatteryManager.BATTERY_STATUS_CHARGING) {
                        return PrintResult.LOW_BATTERY;
                    }
                }
            }
        }

        // Check the battery temperature
        mTemperature = 0;
        if (mDevice == Utility.Device.DEVICE_AT100 ||
                mDevice == Utility.Device.DEVICE_AT150 ||
                mDevice == Utility.Device.DEVICE_AT170) {
            if (context instanceof Activity) {
                mTemperature = getBatteryTemperature(mContext);
                Log.d(TAG, "Starting Battery Temperature: " + mTemperature);

                if (mTemperature > MAXIMUM_BATTERY_TEMPERATURE) {
                    return PrintResult.OVERHEATED_BATTERY;
                }
            }
        }

        mPrinterCom = new Com();
        mPrinterHandle = mPrinterCom.open((short) Com.getBuiltInPrinterDevId());  //Com.DEVICE_PRINTER0

        if (mPrinterHandle != Com.ERR_OPERATION && (mPrinterHandle < Com.ERR_NOT_READY || mPrinterHandle > Com.ERR_XPD_BUSY)) {
            if (mPrinterCom.connect(mPrinterHandle, 115200, (byte) 0, (byte) 0, (byte) 0, (byte) Com.PROTOCOL_RAW_DATA, null) == 0) {
                Log.d(TAG, "Internal Printer Connected");

                if (mDevice != Utility.Device.DEVICE_AT100 &&
                        mDevice != Utility.Device.DEVICE_AT150 &&
                        mDevice != Utility.Device.DEVICE_AT170) {
                    // Reset the printer device
                    addToArrayList(RESET_PRINTER);
                }
                else {
                    // Send battery temperature to the printer FW
                    sendBatteryTemperature();

                    // Adjust the device CPU governor
                    Sys.setCpuFreqGov(Sys.CPUFREQ_SCALING_MAX_FREQ_MASK | Sys.CPUFREQ_GOVERNOR_USERSPACE_480000);
                }

                // Set default typeset to arial
                //Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/arial.ttf");
                //setTypeface(typeface);

                // Add FLAG_KEEP_SCREEN_ON
                if (mContext instanceof Activity) {
                    final Activity activity = (Activity) mContext;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check to see if FLAG_KEEP_SCREEN_ON is already set.  If not, set it
                            int flags = activity.getWindow().getAttributes().flags;
                            if ((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                            else {
                                mHasFlagKeepScreenOn = true;
                            }
                        }
                    });
                }

                // Disable power button during print job
                SaioService saioService = new SaioService(mContext, null);
                saioService.setPowerkeyEnabled(false);
                saioService.release();

                Log.d(TAG, "Printer Opened - Print Job Started");
                return PrintResult.OK;
            }
            else {
                Log.d(TAG, "Error connecting to Internal Printer");
            }
        }
        else {
            int error = mPrinterCom.lastError();
            Log.d(TAG, "Error opening Internal Printer: " + error);
        }

        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private float getBatteryTemperature(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendBatteryTemperature() {
        byte[] formattedCommand = {0x70, 0x00, 0x08, 0x12, 0x77, 0x04, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00};

        // Set temperature byte
        formattedCommand[10] = (byte)((int) mTemperature);

        // Checksum
        byte checkSum = 0;
        for (int i = 0; i < formattedCommand.length; i++) {
            checkSum = (byte) (checkSum ^ formattedCommand[i]);
        }
        formattedCommand[formattedCommand.length - 1] = checkSum;

        Log.d(TAG, "Temperature Command: [" + Utility.binToStr(formattedCommand) + "]");

        addToArrayList(formattedCommand);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public PrintResult printLine(String textLeft, String textCenter, String textRight, int printStyle) {
        if ((textLeft == null || textLeft.isEmpty()) &&
                (textCenter == null || textCenter.isEmpty()) &&
                (textRight == null || textRight.isEmpty())) {
            lineFeed();
            return PrintResult.OK;
        }
        Log.i(TAG, "Line Start");
        Bitmap bitmap = generateBitmap(textLeft, textCenter, textRight, printStyle);
        return printImage(bitmap, Alignment.NONE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public PrintResult printImage(Bitmap bitmap, Alignment align) {
        if (null == bitmap) {
            Log.d(TAG, "Invalid Bitmap");
            return PrintResult.FAILED;
        }

        Collection<byte[]> collection = bitmapToCommands(bitmap, align);

        if (collection.size() == 0) {
            Log.d(TAG, "Invalid Bitmap");
            return PrintResult.FAILED;
        }

        for (byte[] command : collection) {
            byte[] formattedCommand = new byte[command.length + 4];

            // Protocol Command Format
            formattedCommand[0] = 0x70;

            // Data Length
            formattedCommand[1] = (byte) (command.length >> 8);
            formattedCommand[2] = (byte) command.length;

            // Instruction Field
            System.arraycopy(command, 0, formattedCommand, 3, command.length);

            // Checksum
            byte checkSum = 0;
            for (int i = 0; i < formattedCommand.length; i++) {
                checkSum = (byte) (checkSum ^ formattedCommand[i]);
            }
            formattedCommand[formattedCommand.length - 1] = checkSum;

            addToArrayList(formattedCommand);
        }
        Log.i(TAG, "Line End");
        return PrintResult.OK;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void lineFeed() {
        addToArrayList(LINE_FEED);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void feed() {
        addToArrayList(FEED_PAPER);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void cut(boolean fullCut) {
        // Placeholder - Internal Printer does not support cutter
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void close() {
        super.close();

        if (mPrinterHandle != Com.ERR_OPERATION) {
            Log.d(TAG, "Shutting down Internal Printer");

            // Shutdown the printer handle
            mPrinterCom.cancel(mPrinterHandle);

            // Delay required to prevent error similar to this:
            // JNI ERROR (app bug): accessed deleted global reference 0x1d20054a
            // or this:
            // JNI ERROR (app bug): accessed stale global reference 0x1d20043a (index 270 in a table of size 270)
            Utility.delay(250);

            mPrinterCom.close(mPrinterHandle);

            mPrinterHandle = Com.ERR_OPERATION;

            clearArrayList();

            if (mDevice == Utility.Device.DEVICE_AT100 ||
                    mDevice == Utility.Device.DEVICE_AT150 ||
                    mDevice == Utility.Device.DEVICE_AT170) {
                // Restore original CPU governor settings
                Sys.setCpuFreqGov(Sys.CPUFREQ_SCALING_MAX_FREQ_MASK | Sys.CPUFREQ_GOVERNOR_USERSPACE_1152000);
            }
        }

        // Clear FLAG_KEEP_SCREEN_ON
        if (mContext instanceof Activity) {
            final Activity activity = (Activity) mContext;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Only clear the flag if the printer API added it
                    if (!mHasFlagKeepScreenOn) {
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            });
        }

        // Enable power button after print job
        SaioService saioService = new SaioService(mContext, null);
        saioService.userActivity();
        saioService.setPowerkeyEnabled(true);
        saioService.release();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class PrintRunnable implements Runnable {
        @Override
        public void run() {
            PrintResult printResult;
            Command command;

            while (true) {
                command = readFromArrayList();
                // If we don't have a current command, continue the while loop
                if (command == null) {
                    //Log.v(TAG, "Current command list is exhausted");
                    continue;
                }

                // Monitor the battery temperature
                if (mDevice == Utility.Device.DEVICE_AT100 ||
                        mDevice == Utility.Device.DEVICE_AT150 ||
                        mDevice == Utility.Device.DEVICE_AT170) {
                    if (mContext != null && mContext instanceof Activity) {
                        float newTemperature = getBatteryTemperature(mContext);

                        if ((newTemperature >= mTemperature + 2) || (newTemperature <= mTemperature - 2)){
                            Log.d(TAG, "New Battery Temperature: " + newTemperature);
                            mTemperature = newTemperature;
                            sendBatteryTemperature();
                        }
                    }
                }

                if (!command.isEndOfPrintJob()) {
                    if ((printResult = getPrintJobResult()) != PrintResult.OK) {
                        decrementIndex();
                        Log.d(TAG, "Print Result Error Occurred: " + printResult);
                        return;
                    }

                    if ((printResult = sendCommand(command.getCommand())) != PrintResult.OK) {
                        if (printResult != PrintResult.PAPER_OUT) {
                            cancelPrintJob();
                        }
                        setPrintJobResult(printResult);
                        return;
                    }
                }
                else {
                    Log.d(TAG, "Print Job Complete");
                    setPrintJobResult(PrintResult.COMPLETE);
                    //
                    broadcastDiagIntent(SaioService.DIAG_PRIORITY_LOW, mContext, SaioService.ACTION_DIAG_DEVICE, SaioService.DIAG_STATUS_OK, null);

                    return;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final byte PRINTER_COMMAND_ERROR_TAG = 0x01;
    private final byte PRINTER_COMMAND_ERROR_CHECKSUM = 0x02;
    private final byte PRINTER_COMMAND_ERROR_COMMAND = 0x03;
    private final byte PRINTER_COMMAND_ERROR_PARITY = 0x04;
    private final byte PRINTER_COMMAND_ERROR_OVERRUN = 0x05;
    private final byte PRINTER_COMMAND_ERROR_FRAME = 0x06;
    private final byte PRINTER_COMMAND_ERROR_TIMEOUT = 0x07;
    private final byte PRINTER_COMMAND_ERROR_OVERFLOW = 0x08;

    private final byte PRINTER_ERROR_HEAD_TEMP = 0x01;
    private final byte PRINTER_ERROR_POWER = 0x02;
    private final byte PRINTER_ERROR_BUFFER_FULL = 0x04;
    private final byte PRINTER_ERROR_PAPER_OUT = 0x08;
    private final byte PRINTER_ERROR_BUSY = (byte)0x80;
    private final byte PRINTER_ERROR_EXISTS = (byte)0x8F;

    private static final byte[] response = new byte[10];

    private PrintResult sendCommand(byte[] command) {
        try {
            int length = mPrinterCom.write(mPrinterHandle, command, command.length, WRITE_TIMEOUT);

            if (length != Com.ERR_OPERATION) {
                //Log.d(TAG, "Successfully Wrote Packet Length: " + length + " bytes");
                //Log.d(TAG, Utility.binToStr(command));

                length = mPrinterCom.read(mPrinterHandle, response, response.length, READ_TIMEOUT);

                if (length != Com.ERR_OPERATION) {
                    if (response[0] == 0x71) {
                        //Log.d(TAG, "Successfully Read Packet Length: " + length + " bytes");
                        //Log.d(TAG, Utility.binToStr(response));

                        //response of set_temperature_cmd, skip this!!!
                        if(response[3] == 0x77)
                        {
                            return PrintResult.OK;
                        }

                        /*
                            0 Head temperature too high or too low
                            1 Power supply too high or too low
                            2 Buffer Full
                            3 Paper out
                            4 Printer in printing
                            5 Cutter in using
                            7 Busy
                        */
                        if (
                                ((response[4] & 0x01) != (byte)0x00)
                             || ((response[4] & 0x02) != (byte)0x00)
                             || ((response[4] & 0x08) != (byte)0x00)
                             || ((response[4] & 0x80) != (byte)0x00)
                        ){
                            byte[] diagStatus = new byte[]{response[4], response[5], (byte)0};
                            broadcastDiagIntent(SaioService.DIAG_PRIORITY_HIGH, mContext, SaioService.ACTION_DIAG_DEVICE, SaioService.DIAG_STATUS_ERROR, diagStatus);
                        }
                        else //normal case
                        {
                            broadcastDiagIntent(SaioService.DIAG_PRIORITY_LOW, mContext, SaioService.ACTION_DIAG_DEVICE, SaioService.DIAG_STATUS_OK, null);
                        }

                        if ((response[4] & PRINTER_ERROR_EXISTS) == 0) {
                            return PrintResult.OK;
                        }
                        else if ((response[4] & PRINTER_ERROR_HEAD_TEMP) == PRINTER_ERROR_HEAD_TEMP) {
                            Log.d(TAG, "PRINTER_ERROR_HEAD_TEMP");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_ERROR_POWER) == PRINTER_ERROR_POWER) {
                            Log.d(TAG, "PRINTER_ERROR_POWER");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_ERROR_BUFFER_FULL) == PRINTER_ERROR_BUFFER_FULL) {
//                            Log.d(TAG, "PRINTER_ERROR_BUFFER_FULL - Resending Previous Command");
                            decrementIndex();
                            return PrintResult.OK;
                        }
                        else if ((response[4] & PRINTER_ERROR_PAPER_OUT) == PRINTER_ERROR_PAPER_OUT) {
                            Log.d(TAG, "PRINTER_ERROR_PAPER_OUT");
                            decrementIndex();
                            return PrintResult.PAPER_OUT;
                        }
                        else if ((response[4] & PRINTER_ERROR_BUSY) == PRINTER_ERROR_BUSY) {
                            Log.d(TAG, "PRINTER_ERROR_BUSY");
                            return PrintResult.FAILED;
                        }

                        return PrintResult.OK;
                    }
                    else if (response[0] == 0x72) {
                        Log.d(TAG, "Successfully Read Error Packet Length: " + length + " bytes");

                        if ((response[4] & PRINTER_COMMAND_ERROR_TAG) == PRINTER_COMMAND_ERROR_TAG) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_TAG");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_CHECKSUM) == PRINTER_COMMAND_ERROR_CHECKSUM) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_CHECKSUM");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_COMMAND) == PRINTER_COMMAND_ERROR_COMMAND) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_COMMAND");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_PARITY) == PRINTER_COMMAND_ERROR_PARITY) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_PARITY");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_OVERRUN) == PRINTER_COMMAND_ERROR_OVERRUN) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_OVERRUN");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_FRAME) == PRINTER_COMMAND_ERROR_FRAME) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_FRAME");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_TIMEOUT) == PRINTER_COMMAND_ERROR_TIMEOUT) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_TIMEOUT");
                            return PrintResult.FAILED;
                        }
                        else if ((response[4] & PRINTER_COMMAND_ERROR_OVERFLOW) == PRINTER_COMMAND_ERROR_OVERFLOW) {
                            Log.d(TAG, "PRINTER_COMMAND_ERROR_OVERFLOW");
                            return PrintResult.FAILED;
                        }
                    }
                }
                else {
                    Log.d(TAG, "Read Error = " + mPrinterCom.lastError());
                }
            }
            else {
                Log.d(TAG, "Write Error = " + mPrinterCom.lastError());
            }
        }
        catch (Exception e) {
            Log.d(TAG, "sendCommand Exception");
            e.printStackTrace();
        }
        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //!!! DO NOT grant to public/protected because write/read() only must be called by print-job-thread
    private void cancelPrintJob() {
        try {
            int length = mPrinterCom.write(mPrinterHandle, CANCEL, CANCEL.length, WRITE_TIMEOUT);
            if (length != Com.ERR_OPERATION) {
                Log.d(TAG, "Successfully Wrote Cancel Packet Length: " + length + " bytes");

                byte[] response = new byte[1000];
                length = mPrinterCom.read(mPrinterHandle, response, response.length, READ_TIMEOUT);

                if (length != Com.ERR_OPERATION) {
                    Log.d(TAG, "Successfully Read Cancel Packet Length: " + length + " bytes");
                }
                else {
                    Log.d(TAG, "Read Error = " + mPrinterCom.lastError());
                }
            }
            else {
                Log.d(TAG, "Write Error = " + mPrinterCom.lastError());
            }
        }
        catch (Exception e) {
            Log.d(TAG, "cancelPrintJob Exception");
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static int[] _pixels = null;

    private Collection<byte[]> bitmapToCommands(Bitmap bmp, Alignment align)
    {
        if(null == bmp)
            return null;

        ArrayList<byte[]> cmds;
        int nMaxCmdLen = 1152;//0x0480: default for TP_48 (48x24)
        if(PrintWidth.TP_72 == mPrintWidth)
            nMaxCmdLen = 1728;//0x06C0: for TP_72 (72x24)

        int height = bmp.getHeight();

        if(null == _pixels || (bmp.getWidth() * height > _pixels.length))
        {
            _pixels = new int[bmp.getWidth() * height];
        }

        bmp.getPixels(_pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), height);

        //ARGB pixels to Monochrome bytes
        byte[] raw = _pixelsARGB2Mono(_pixels, bmp.getWidth(), height, mPrintWidth.nativeInt, align, false);

        if(null != raw)
        {
            cmds = new ArrayList<>();
            int nb;
            byte[] cmd;
            int nCmdLen;

            //split monochrome bytes to print commands
            while(true)
            {
                nb = cmds.size() * nMaxCmdLen;
                if(nb >= raw.length)
                    break;

                if((raw.length - nb) >= nMaxCmdLen)
                {
                    cmd = newGraphicCommand(mPrintWidth, nMaxCmdLen);
                    nCmdLen = nMaxCmdLen;
                    System.arraycopy(raw, nb, cmd, nCommandHeader, nCmdLen);
                }
                //reminder data
                else
                {
                    int r = raw.length % nMaxCmdLen;
                    cmd = newGraphicCommand(mPrintWidth, r);
                    System.arraycopy(raw, nb, cmd, nCommandHeader, r);
                }

                cmds.add(cmd);
            }
        }
        else
            return null;

        return cmds;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static int nCommandHeader = 8;

    private static byte[] newGraphicCommand(PrintWidth printWidth, int len) {
        byte[] cmd = new byte[len + nCommandHeader];

        cmd[0] = 0x1B;
        cmd[1] = 0x2A;
        cmd[2] = (byte)(len & 0xFF);
        cmd[3] = (byte)(len >> 8);
        cmd[4] = 0x00;
        cmd[5] = 0x00;
        cmd[6] = 0x00;
        cmd[7] = (byte)printWidth.nativeInt;

        return cmd;
    }
}
