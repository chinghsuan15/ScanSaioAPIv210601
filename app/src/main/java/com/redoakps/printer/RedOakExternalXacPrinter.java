package com.redoakps.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import saioapi.comm.v2.ComManager;
import saioapi.comm.v2.OnComEventListener;

public class RedOakExternalXacPrinter extends RedOakPrinter {
    private final int TIMEOUT = 1000;
    private final int MAX_PACKET = 3072;

    // External Printer
    private final int DEVICE_VENDOR_ID = 1561;
    private final int DEVICE_PRODUCT_ID = 278;

    private ComManager mPrinterCom;
    private int mPrinterHandle = ComManager.ERR_OPERATION;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final byte[] PRINT_AND_FEED_ONE = {0x1B, 0x64, 0x01};
    private static final byte[] PRINT_AND_FEED = {0x1B, 0x64, 0x03};
    private static final byte[] ENABLE_AUTO_CUTTER = {0x12, 0x77, 0x01, (byte) 0xFE, 0x40, 0x00};
    private static final byte[] FULL_CUT = {0x1D, 0x56, 0x00};
    private static final byte[] PARTIAL_CUT = {0x1D, 0x56, 0x01};
    private static final byte[] ENABLE_ASB = {0x1D, 0x61, 0x1F};
    private static final byte[] PRINTER_INIT = {0x1B, 0x40};
    private static final byte[] REVERSE_ON = {0x1D, 0x42, 0x01};
    private static final byte[] REVERSE_OFF = {0x1D, 0x42, 0x00};
    private static final byte[] BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] DOUBLE_HEIGHT_ON = {0x1D, 0x21, 0x01};
    private static final byte[] DOUBLE_HEIGHT_OFF = {0x1D, 0x21, 0x00};
    private static final byte[] UNDERLINE_ON = {0x1B, 0x2D, 0x01};
    private static final byte[] UNDERLINE_OFF = {0x1B, 0x2D, 0x00};
    private static final byte[] CHECK_STATUS = {0x1D, 0x72, 0x01};
    private static final byte[] STANDARD_MODE_SELECT = {0x1B, 0x53};
    private static final byte[] PAGE_MODE_SELECT = {0x1B, 0x4C};
    private static final byte[] PAGE_MODE_PRINT = {0x0C};
    private static final byte[] PAGE_MODE_CANCEL = {0x18};

    // Printer Errors
    // Byte 1 - Printer Unit Information
    private final byte PRINTER_ERROR_COVER_OPEN = 0x20;
    // Byte 2 - Error Information
    private final byte PRINTER_ERROR_JAMMED = 0x04;
    private final byte PRINTER_ERROR_AUTOCUTTER = 0x08;
    private final byte PRINTER_ERROR_HEAD_ERROR = 0x20;
    private final byte PRINTER_ERROR_HEAD_TEMP = 0x40;
    // Byte 3 - Paper Sensor Information
    private final byte PRINTER_ERROR_PAPER_NEAR_OUT = 0x01;
    private final byte PRINTER_ERROR_PAPER_OUT = 0x04;
    // Byte 4 - Presenter Information
    private final byte PRINTER_ERROR_PRESENTER_PAPER_SENSOR = 0x01;
    private final byte PRINTER_ERROR_PRESENTER_FEED = 0x04;
    private final byte PRINTER_ERROR_PRESENTER_PAPER_JAM = 0x20;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public RedOakExternalXacPrinter() {
        TAG = "RedOakExternalXacPrinter";
        mPrintWidth = PrintWidth.TP_72;
        mInterval = 20;
        mPrintJobTimeout = 500;
        mRunnable = new PrintRunnable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public RedOakExternalXacPrinter(int interval, int printJobTimeout) {
        TAG = "RedOakExternalXacPrinter";
        mPrintWidth = PrintWidth.TP_72;
        mInterval = interval;
        mPrintJobTimeout = printJobTimeout;
        mRunnable = new PrintRunnable();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    boolean mPaperHasBeenOut = false;

    @Override
    public PrintResult open(Context context) {
        // Do not attempt to open USB reader if we do not know what device this is as it could be a phone
        if (Utility.getDeviceType() == Utility.Device.DEVICE_UNKNOWN) {
            return PrintResult.FAILED;
        }

        mPrinterCom = new ComManager(context);

        mPrinterCom.setOnComEventListener(new OnComEventListener() {
            @Override
            public void onEvent(int event) {
                switch (event) {
                    case ComManager.EVENT_DATA_READY:
                        byte[] response = new byte[10];
                        int length = mPrinterCom.read(response, response.length, TIMEOUT);

                        if (length > 0) {
                            boolean mFailed = false;
                            boolean mPaperOut = false;

                            if (response[0] == 0x72) {
                                // Invalid Command Sent to Printer
                                Log.d(TAG, "Invalid Command Sent to Printer");

                                setPrintJobResult(PrintResult.FAILED);
                                return;
                            }
                            else if (((response[0] & 0x10) == 0x10) &&
                                    ((response[0] & 0x80) == 0x00) &&
                                    ((response[0] & 0x01) == 0x00)) {
                                // Auto Status Back Command
                                Log.d(TAG, "Auto Status Back: " + Utility.binToStr(response));

                                // Check if Error Condition Occurred

                                // Byte 3 - Paper Sensor Information
                                if ((response[2] & PRINTER_ERROR_PAPER_NEAR_OUT) == PRINTER_ERROR_PAPER_NEAR_OUT) {
                                    // Do not stop printing if paper is just near out
                                    Log.d(TAG, "Paper Near End");
                                }
                                if ((response[2] & PRINTER_ERROR_PAPER_OUT) == PRINTER_ERROR_PAPER_OUT) {
                                    Log.d(TAG, "Paper Out");
                                    mPaperOut = true;
                                    mPaperHasBeenOut = true;
                                }
//                            if ((response[2] & 0x40) == 0x40) {
//                                // Mark Sensor
//                            }

                                // Byte 1 - Printer Unit Information
                                if ((response[0] & 0x02) == 0x02) {
                                    Log.d(TAG, "Device is Printing");
                                }
                                else {
                                    Log.d(TAG, "Device is not Printing");
                                }
//                                if ((response[0] & 0x04) == 0x04) {
//                                    // Is Drawer High
//                                }
                                if ((response[0] & PRINTER_ERROR_COVER_OPEN) == PRINTER_ERROR_COVER_OPEN) {
                                    Log.d(TAG, "Printer Cover Open");

                                    // Due to the order of paper out / print cover open errors, have to have
                                    // some special processing to prevent an error when paper has been re-added
                                    // and cover has been closed
                                    if (mPaperHasBeenOut && !mPaperOut) {
                                        mPaperHasBeenOut = false;
                                    }
                                    else {
                                        mFailed = true;
                                    }
                                }
//                            if ((response[0] & 0x40) == 0x40) {
//                                // Paper Feed
//                            }

                                // Byte 2 - Error Information
                                if ((response[1] & PRINTER_ERROR_JAMMED) == PRINTER_ERROR_JAMMED) {
                                    Log.d(TAG, "Printer Jammed");
                                    mFailed = true;
                                }
                                if ((response[1] & PRINTER_ERROR_AUTOCUTTER) == PRINTER_ERROR_AUTOCUTTER) {
                                    Log.d(TAG, "Autocutter Error");
                                    mFailed = true;
                                }
                                if ((response[1] & PRINTER_ERROR_HEAD_ERROR) == PRINTER_ERROR_HEAD_ERROR) {
                                    Log.d(TAG, "Head Error");
                                    mFailed = true;
                                }
                                if ((response[1] & PRINTER_ERROR_HEAD_TEMP) == PRINTER_ERROR_HEAD_TEMP) {
                                    Log.d(TAG, "Automatic recovery error (Head temperature error)");
                                    mFailed = true;
                                }

                                // Byte 4 - Presenter Information
                                if ((response[3] & PRINTER_ERROR_PRESENTER_PAPER_SENSOR) == PRINTER_ERROR_PRESENTER_PAPER_SENSOR) {
                                    Log.d(TAG, "Presenter Paper Sensor");
                                    mFailed = true;
                                }
                                if ((response[3] & PRINTER_ERROR_PRESENTER_FEED) == PRINTER_ERROR_PRESENTER_FEED) {
                                    Log.d(TAG, "Presenter Feed Error");
                                    mFailed = true;
                                }
                                if ((response[3] & PRINTER_ERROR_PRESENTER_PAPER_JAM) == PRINTER_ERROR_PRESENTER_PAPER_JAM) {
                                    Log.d(TAG, "Presenter Paper Jam");
                                    mFailed = true;
                                }

                                if (mPaperOut) {
                                    setPrintJobResult(PrintResult.PAPER_OUT);
                                    return;
                                }
                                else if (mFailed) {
                                    setPrintJobResult(PrintResult.FAILED);
                                    return;
                                }
                            }
                        }

                        break;
                }
            }
        });

        // Find the list of devices connected with the printer vendor / product ID
        int[] device = mPrinterCom.getUsbDevId(DEVICE_VENDOR_ID, DEVICE_PRODUCT_ID);
        if (device.length == 0) {
            return PrintResult.FAILED;
        }

        // Open the first device returned
        mPrinterHandle = mPrinterCom.open(device[0]);

        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            if (mPrinterCom.connect(ComManager.PROTOCOL_RAW_DATA) == 0) {
                Log.d(TAG, "External Printer Connected");

                // Send command to cancel any ongoing page mode
                addToArrayList(PAGE_MODE_CANCEL);

                // Send command to return device to standard mode
                addToArrayList(STANDARD_MODE_SELECT);

                // Send command to reset the printer
                addToArrayList(PRINTER_INIT);

                // Send command to turn on auto cutter
                addToArrayList(ENABLE_AUTO_CUTTER);

                // Send command to turn on auto status back
                addToArrayList(ENABLE_ASB);

                // Explicitly check printer status
                addToArrayList(CHECK_STATUS);

                Log.d(TAG, "Printer Opened - Print Job Started");
                return PrintResult.OK;
            }
            else {
                Log.d(TAG, "Error connecting to External Printer");
            }
        }
        else {
            int error = mPrinterCom.lastError();
            Log.d(TAG, "Error opening External Printer: " + error);
        }

        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public PrintResult printLine(String textLeft, String textCenter, String textRight, int printStyle) {
        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            byte[] command = alignText(textLeft, textCenter, textRight);
            if (command != null) {
                // Send commands to enable requested styles
                if ((printStyle & STYLE_ITALIC) == STYLE_ITALIC) {
                    // Not Supported by Stand Alone Printer
                    Log.d(TAG, "Unsupported Style Used: Italics");
                }

                if ((printStyle & STYLE_BOLD) == STYLE_BOLD) {
                    addToArrayList(BOLD_ON);
                }

                // Underline only works if reverse is not enabled
                if ((printStyle & STYLE_REVERSE) == STYLE_REVERSE) {
                    addToArrayList(REVERSE_ON);
                }
                else if ((printStyle & STYLE_UNDERLINE) == STYLE_UNDERLINE) {
                    addToArrayList(UNDERLINE_ON);
                }

                if ((printStyle & STYLE_LARGE_FONT) == STYLE_LARGE_FONT) {
                    addToArrayList(DOUBLE_HEIGHT_ON);
                }

                // Process Commands
                addToArrayList(command);
                addToArrayList(PRINT_AND_FEED_ONE);

                // Send commands to disable requested styles
                if ((printStyle & STYLE_ITALIC) == STYLE_ITALIC) {
                    // Not Supported by Stand Alone Printer
                }

                if ((printStyle & STYLE_BOLD) == STYLE_BOLD) {
                    addToArrayList(BOLD_OFF);
                }

                // Underline only works if reverse is not enabled
                if ((printStyle & STYLE_REVERSE) == STYLE_REVERSE) {
                    addToArrayList(REVERSE_OFF);
                }
                else if ((printStyle & STYLE_UNDERLINE) == STYLE_UNDERLINE) {
                    addToArrayList(UNDERLINE_OFF);
                }

                if ((printStyle & STYLE_LARGE_FONT) == STYLE_LARGE_FONT) {
                    addToArrayList(DOUBLE_HEIGHT_OFF);
                }

                return PrintResult.OK;
            }
        }
        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final int MAX_CHARACTERS = 48;

    private byte[] alignText(String leftText, String centerText, String rightText) {
        try {
            // Validation of string lengths to ensure we don't have overwrite issues
            if (leftText.length() + centerText.length() + rightText.length() > MAX_CHARACTERS) {
                Log.d(TAG, "Invalid Cumulative Text Length");
                return null;
            }

            int leftLength;
            int rightLength;

            if (centerText.length() > 0) {
                leftLength = (MAX_CHARACTERS - centerText.length()) / 2;
                rightLength = MAX_CHARACTERS - centerText.length() - leftLength;

                if (leftText.length() > leftLength) {
                    Log.d(TAG, "Invalid Left Aligned Text");
                    return null;
                }

                if (rightText.length() > rightLength) {
                    Log.d(TAG, "Invalid Right Aligned Text");
                    return null;
                }
            }
            else {
                leftLength = MAX_CHARACTERS - rightText.length();
                rightLength = rightText.length();
            }

            StringBuilder builder = new StringBuilder();

            builder.append(leftText);
            for (int i = 0; i < leftLength - leftText.length(); i++) {
                builder.append(" ");
            }

            builder.append(centerText);

            for (int i = 0; i < rightLength - rightText.length(); i++) {
                builder.append(" ");
            }
            builder.append(rightText);

            return builder.toString().getBytes("UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public PrintResult printImage(Bitmap bitmap, Alignment align) {
        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            if (null == bitmap) {
                Log.d(TAG, "Invalid Bitmap");
                return PrintResult.FAILED;
            }

            byte[] command = bitmapToCommands(bitmap, 0);

            if (null == command) {
                Log.d(TAG, "Invalid Bitmap");
                return PrintResult.FAILED;
            }

            // Put the device into page mode
            addToArrayList(PAGE_MODE_SELECT);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                // Generate Command: Print Area Set in Page Mode
                stream.write(new byte[] {0x1B, 0x57, 0x00, 0x00, 0x00, 0x00});

                // Page width is 576
                stream.write(mPrintWidth.intValue() % 255); // dxL
                stream.write(mPrintWidth.intValue() / 255); // dxH

                stream.write(bitmap.getHeight() % 255); // dyL
                stream.write(bitmap.getHeight() / 255); // dyH

                addToArrayList(stream.toByteArray());

                // Generate Command: Absolute Position Specify
                int position = 0;
                if (align == Alignment.CENTER) {
                    position = (mPrintWidth.intValue() - bitmap.getWidth()) / 2;
                }
                else if (align == Alignment.RIGHT) {
                    position = mPrintWidth.intValue() - bitmap.getWidth();
                }

                stream  = new ByteArrayOutputStream();
                stream.write(new byte[] {0x1B, 0x24});
                stream.write(position % 255); // nl
                stream.write(position / 255); // nh

                addToArrayList(stream.toByteArray());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Break the bitmap command up and send to the printer in smaller pieces
            int srcPos = 0;

            if (command.length <= MAX_PACKET) {
                addToArrayList(command, true); // true = indicates this is part of a bitmap
            }
            else {
                while (srcPos < command.length) {
                    byte[] packet;

                    if ((command.length - srcPos) <= MAX_PACKET) {
                        packet = new byte[command.length - srcPos];
                        System.arraycopy(command, srcPos, packet, 0, packet.length);
                        srcPos += packet.length;
                    }
                    else {
                        packet = new byte[MAX_PACKET];
                        System.arraycopy(command, srcPos, packet, 0, MAX_PACKET);
                        srcPos += MAX_PACKET;
                    }

                    addToArrayList(packet, true); // true = indicates this is part of a bitmap
                }
            }

            // Prints page mode data and returns printer to the standard mode
            addToArrayList(PAGE_MODE_PRINT);

            return PrintResult.OK;
        }

        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void feed() {
        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            // Send the feed command
            addToArrayList(PRINT_AND_FEED);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void cut(boolean fullCut) {
        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            // Send cut command to printer
            if (fullCut) {
                addToArrayList(FULL_CUT);
            }
            else {
                addToArrayList(PARTIAL_CUT);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void close() {
        super.close();

        if (mPrinterHandle != ComManager.ERR_OPERATION) {
            Log.d(TAG, "Shutting down External Printer");

            // Delay required to prevent error similar to this:
            // JNI ERROR (app bug): accessed deleted global reference 0x1d20054a
            // or this:
            // JNI ERROR (app bug): accessed stale global reference 0x1d20043a (index 270 in a table of size 270)
            Utility.delay(250);

            mPrinterCom.setOnComEventListener(null);

            mPrinterCom.close();

            mPrinterHandle = ComManager.ERR_OPERATION;

            clearArrayList();


        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class PrintRunnable implements Runnable {
        @Override
        public void run() {
            PrintResult printResult;
            Command command;

            while (true) {
                Utility.delay(mInterval);

                command = readFromArrayList();
                // If we don't have a current command, continue the while loop
                if (command == null) {
                    //Log.d(TAG, "Current command list is exhausted");
                    continue;
                }

                if (!command.isEndOfPrintJob()) {
                    if ((printResult = getPrintJobResult()) != PrintResult.OK && !command.isBitmapCommand()) {
                        decrementIndex();
                        Log.d(TAG, "Print Result Error Occurred: " + printResult);
                        return;
                    }

                    if ((printResult = sendCommand(command.getCommand())) != PrintResult.OK) {
                        setPrintJobResult(printResult);
                        return;
                    }
                }
                else {
                    Log.d(TAG, "Print Job Complete");
                    setPrintJobResult(PrintResult.COMPLETE);
                    return;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private PrintResult sendCommand(byte[] command) {
        try {
//            Log.d(TAG, "Attempting Command Len : " + command.length + ", Data: " + Utility.binToStr(command, true, 10));
            int length = mPrinterCom.write(command, command.length, TIMEOUT);

            if (length != ComManager.ERR_OPERATION && length != ComManager.ERR_NOT_READY) {
//                Log.d(TAG, "Successful");
                return PrintResult.OK;
            }
            else {
                Log.d(TAG, "Failed. Write Error = " + mPrinterCom.lastError());
            }
        }
        catch (Exception e) {
            Log.d(TAG, "sendCommand Exception");
            e.printStackTrace();
        }
        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final boolean _isDebug = false;

    private byte[] bitmapToCommands(Bitmap bmp, int mode)
    {
        if(null == bmp)
        {
            Log.e(TAG, "Bitmap is null");
            return null;
        }

        if((mode < 0 || 3 < mode) && (mode < 48 || 51 < mode))
        {
            Log.e(TAG, "Invalid mode: " + mode);
            return null;
        }

        long t = 0L;
        int w = bmp.getWidth() >> 3;
        if(bmp.getWidth() % 8 != 0)
            w++;
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        if(_isDebug) Log.v(TAG, "bmp_size=" + bmp.getWidth() + "x" + bmp.getHeight());

        t = System.currentTimeMillis();
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        if(_isDebug) Log.v(TAG, "Spend " + (System.currentTimeMillis() - t) + " ms to get bmp pixels");

        //ARGB pixels to Monochrome bytes
        t = System.currentTimeMillis();
        byte[] raw = _pixelsARGB2Mono(pixels, bmp.getWidth(), bmp.getHeight(), w, Alignment.NONE, false);
        if(_isDebug) Log.v(TAG, "Spend " + (System.currentTimeMillis() - t) + " ms to convert pixels from ARGB to Mono");

        t = System.currentTimeMillis();
        //
        //max width of image is (256 * 256 + 256) * 8 = 526336 dots, so don't care about if the width is out of bound
        //max height of image is (16 * 256 + 256) = 4352 dots, only mind the height
        int copy_size = raw.length;
        int copy_h = bmp.getHeight();
        if(bmp.getHeight() >= 4352)
        {
            copy_size = bmp.getHeight() * 4352;
            copy_h = 4351;
        }
        byte[] cmd = new byte[copy_size + 8];
        cmd[0] = 0x1D;
        cmd[1] = 0x76;
        cmd[2] = 0x30;
        cmd[3] = (byte)mode;
        cmd[4] = (byte)(w & 0xFF);
        cmd[5] = (byte)(w >> 8);
        cmd[6] = (byte)(copy_h & 0xFF);
        cmd[7] = (byte)(copy_h >> 8);
        System.arraycopy(raw, 0, cmd, 8, copy_size);
        if(_isDebug) Log.v(TAG, "Spend " + (System.currentTimeMillis() - t) + " ms to copy raster bit image to cmd");
        return cmd;
    }
}
