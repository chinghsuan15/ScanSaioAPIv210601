package com.redoakps.printer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Looper;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.oned.CodaBarWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code93Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.ITFWriter;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.oned.UPCEWriter;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import saioapi.util.SaioService;

import static java.util.Calendar.SECOND;

public abstract class RedOakPrinter {
    protected PrintWidth mPrintWidth;
    protected int mInterval;
    protected int mPrintJobTimeout;
    protected String TAG;
    protected Runnable mRunnable;
    private Typeface mTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);

    public enum PrintResult {OK, PAPER_OUT, FAILED, COMPLETE, CANCELLED, LOW_BATTERY, OVERHEATED_BATTERY}
    public enum Alignment {NONE, LEFT, CENTER, RIGHT}

    public enum PrintWidth
    {
        /** Specify the print width is 48 x 8 dots. */
        TP_48   (48),

        /** Specify the print width is 72 x 8 dots. */
        TP_72   (72),

        /** The default print width is {@link #TP_72}. */
        DEFAULT (72);

        PrintWidth(int nativeInt)
        {
            this.nativeInt = nativeInt;
        }

        final int nativeInt;

        /**
         * Return number of dots per print line.
         * @return Return number of dots per print line.
         */
        public int intValue()
        {
            return nativeInt << 3;
        }
    }

    public static final int STYLE_NONE = 0x00;
    public static final int STYLE_UNDERLINE = 0x01;
    public static final int STYLE_ITALIC = 0x02;
    public static final int STYLE_BOLD = 0x04;
    public static final int STYLE_REVERSE = 0x08;
    public static final int STYLE_LARGE_FONT = 0x10;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public abstract PrintResult open(Context context);
    public abstract PrintResult printImage(Bitmap bitmap, Alignment align);
    public abstract void feed();
    public abstract void cut(boolean fullCut);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void close() {
        // Recycle bitmaps
        if (mLargeBitmap != null && !mLargeBitmap.isRecycled()) {
            Log.d(TAG, "Recycled Bitmap " + mLargeBitmap.getWidth() + " x " + mLargeBitmap.getHeight());
            mLargeBitmap.recycle();
            mLargeBitmap = null;

            mLargeCanvas = null;
        }

        if (mSmallBitmap != null && !mSmallBitmap.isRecycled()) {
            Log.d(TAG, "Recycled Bitmap " + mSmallBitmap.getWidth() + " x " + mSmallBitmap.getHeight());
            mSmallBitmap.recycle();
            mSmallBitmap = null;

            mSmallCanvas = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Thread mThread = null;
    private boolean mCancelled = false;

    private int mIndex = 0;

    private int getIndex() {
        synchronized (this) {
            return mIndex;
        }
    }

    protected void decrementIndex() {
        synchronized (this) {
            mIndex--;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ArrayList<Command> mCollection = new ArrayList<>();

    protected void addToArrayList(byte[] command) {
        addToArrayList(command, false);
    }

    protected void addToArrayList(byte[] command, boolean isBitmapCommand) {
        addToArrayList(new Command(command, isBitmapCommand));
    }

    protected void addToArrayList(Command command) {
        synchronized (this) {
            if (!mCancelled) {
                mCollection.add(command);

                if (mThread == null) {
                    mThread = new Thread(mRunnable);
                    mThread.start();
                }
            }
        }
    }

    protected Command readFromArrayList() {
        synchronized (this) {
            if (mCollection.size() > mIndex) {
                Command command = mCollection.get(mIndex);
                mIndex++;
                return command;
            }
            return null;
        }
    }

    protected void clearArrayList() {
        synchronized (this) {
            mCollection.clear();
        }
    }

    protected boolean isNextCommandBitmap() {
        synchronized (this) {
            if (mCollection.size() > mIndex) {
                return mCollection.get(mIndex).isBitmapCommand();
            }

            return false;
        }
    }

    protected void broadcastDiagIntent(int priority, Context context, String action, int status, byte[] data)
    {
        //get diag priority
        Method get = null;
        String prop = SaioService.DIAG_NOTIFY_PRIORITY_PROPERTY;
        String value = String.valueOf(SaioService.DIAG_PRIORITY_SILENT);
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            value = (String) (get.invoke(null, new Object[]{prop, value}));
        } catch (Throwable e) {
            Log.e(TAG, "Unable to get diagnostic priority - " + e.getMessage());
        }

        int os_diag_priority = Integer.valueOf(value);
        if(priority >= os_diag_priority)
            _broadcastDiagIntent(context, action, status, data);
    }

    private void _broadcastDiagIntent(Context context, String action, int status, byte[] data)
    {
        long t = System.currentTimeMillis();
        Intent intent = new Intent();

        intent.setAction(action);
        intent.putExtra(SaioService.EXTRA_DIAG_DEVICE, SaioService.DIAG_DEVICE_PRINTER);
        intent.putExtra(SaioService.EXTRA_DIAG_STATUS, status);
        if(null != data)
            intent.putExtra(SaioService.EXTRA_DIAG_DATA, data);
        intent.putExtra(SaioService.EXTRA_DIAG_TIMESTAMP, t);
        //intent.addFlags(0x01000000); //for sending an implicit bi to background service
        //
        context.sendBroadcast(intent);

        if(null != data)
            Log.i(TAG, String.format("broadcast diag intent - ACT=%s, ST=%d, DATA={%02X, %02X, %02X} (t=%d)",
                    action, status, data[0], data[1], data[2], t));
        else
            Log.i(TAG, String.format("broadcast diag intent - ACT=%s, ST=%d, DATA=null (t=%d)",
                    action, status, t));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private PrintResult mPrintResult = PrintResult.OK;

    protected PrintResult getPrintJobResult() {
        synchronized (this) {
            return mPrintResult;
        }
    }

    protected void setPrintJobResult(PrintResult newResult) {
        synchronized (this) {
            Log.d(TAG, "Setting Print Status: " + newResult);
            mPrintResult = newResult;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean mIsAlertDisplayed = false;

    private void setAlertDisplayed(boolean isDisplayed) {
        synchronized (this) {
            mIsAlertDisplayed = isDisplayed;
        }
    }

    private boolean isAlertDisplayed() {
        synchronized (this) {
            return mIsAlertDisplayed;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public PrintResult waitForResult(Activity activity) {
        PrintResult result;

        // Add null command to indicate end of print job
        Log.d(TAG, "Adding null message to indicate end of print job");
        addToArrayList(new Command());

        // If this is called from main UI thread, we can't prompt for paper out condition
        // so clear the activity setting
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d(TAG, "Current Thread is Main UI Thread");
            activity = null;
        }
        else {
            Log.d(TAG, "Current Thread IS NOT Main UI Thread");
        }
        final Activity myActivity = activity;

        while (true) {
            if (!isAlertDisplayed()) {
                result = getPrintJobResult();

                switch (result) {
                    case OK:
                        continue;

                    case PAPER_OUT:
                        if (myActivity != null) {
                            setAlertDisplayed(true);

                            myActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Display a paper out alert
                                    AlertDialog.Builder builder = new AlertDialog.Builder(myActivity);
                                    builder
                                            .setTitle("Paper Out")
                                            .setMessage("Load new paper roll and press continue button")
                                            .setCancelable(false)
                                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    resume();
                                                    Utility.delay(100);
                                                    setAlertDisplayed(false);
                                                }
                                            })
                                            .setNegativeButton("Cancel Print Job", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    cancel();
                                                    Utility.delay(100);
                                                    setAlertDisplayed(false);
                                                }
                                            });
                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();
                                }
                            });
                        }
                        else {
                            // We need to continue to process the bitmap commands even if an error occurs
                            if (isNextCommandBitmap()) {
                                continue;
                            }
                            return result;
                        }
                        break;

                    case CANCELLED:
                        // We need to continue to process the bitmap commands even if an error occurs
                        if (isNextCommandBitmap()) {
                            continue;
                        }
                        return result;

                    case OVERHEATED_BATTERY:
                    case FAILED:
                    case COMPLETE:
                        return result;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public PrintResult waitForResult(int timeoutSeconds, Activity activity) {
        PrintResult result;

        // Add null command to indicate end of print job
        Log.d(TAG, "Adding null message to indicate end of print job");
        addToArrayList(new Command());

        // If this is called from main UI thread, we can't prompt for paper out condition
        // so clear the activity setting
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d(TAG, "Current Thread is Main UI Thread");
            activity = null;
        }
        else {
            Log.d(TAG, "Current Thread IS NOT Main UI Thread");
        }
        final Activity myActivity = activity;

        // Setup timeout time
        Calendar timeout = Calendar.getInstance();
        timeout.add(SECOND, timeoutSeconds);

        while (true) {
            if (!isAlertDisplayed()) {
                Calendar current = Calendar.getInstance();
                if (timeout.before(current)) {
                    Log.d(TAG, "Print job has timed out");
                    setPrintJobResult(PrintResult.CANCELLED);
                }

                result = getPrintJobResult();

                switch (result) {
                    case OK:
                        continue;

                    case PAPER_OUT:
                        if (myActivity != null) {
                            setAlertDisplayed(true);

                            myActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Display a paper out alert
                                    AlertDialog.Builder builder = new AlertDialog.Builder(myActivity);
                                    builder
                                            .setTitle("Paper Out")
                                            .setMessage("Load new paper roll and press continue button")
                                            .setCancelable(false)
                                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    resume();
                                                    Utility.delay(100);
                                                    setAlertDisplayed(false);
                                                }
                                            })
                                            .setNegativeButton("Cancel Print Job", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    cancel();
                                                    Utility.delay(100);
                                                    setAlertDisplayed(false);
                                                }
                                            });
                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();
                                }
                            });
                        }
                        else {
                            // We need to continue to process the bitmap commands even if an error occurs
                            if (isNextCommandBitmap()) {
                                continue;
                            }
                            return result;
                        }
                        break;

                    case CANCELLED:
                        // We need to continue to process the bitmap commands even if an error occurs
                        if (isNextCommandBitmap()) {
                            continue;
                        }
                        return result;

                    case FAILED:
                    case COMPLETE:
                    case OVERHEATED_BATTERY:
                        return result;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void setTypeface(Typeface typeface) {
        if (typeface != null) {
            mTypeface = typeface;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void resume() {
        // Reset the print job status to OK
        setPrintJobResult(PrintResult.OK);

        // Resume the thread
        mThread = new Thread(mRunnable);
        mThread.start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public PrintResult printLine(String textLeft, String textCenter, String textRight, int printStyle) {
        Bitmap bitmap = generateBitmap(textLeft, textCenter, textRight, printStyle);
        return printImage(bitmap, Alignment.NONE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void cancel() {
        synchronized (this) {
            mCancelled = true;
        }

        setPrintJobResult(PrintResult.CANCELLED);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public PrintResult printBarCode(String data, BarcodeFormat barcodeFormat, Alignment alignment) {
        return printBarCode(data, barcodeFormat, alignment, null);
    }

    public PrintResult printBarCode(String data, BarcodeFormat barcodeFormat, Alignment alignment, Hashtable hints) {
        // Barcode
        BitMatrix bitMatrix;
        try {
            int width = 300;
            int height = 75;

            switch (barcodeFormat) {
                case UPC_A:
                    bitMatrix = new UPCAWriter().encode(data, barcodeFormat, width, height);
                    break;

                case UPC_E:
                    bitMatrix = new UPCEWriter().encode(data, barcodeFormat, width, height);
                    break;

                case CODABAR:
                    bitMatrix = new CodaBarWriter().encode(data, barcodeFormat, width, height);
                    break;

                case CODE_39:
                    bitMatrix = new Code39Writer().encode(data, barcodeFormat, width, height);
                    break;

                case CODE_93:
                    bitMatrix = new Code93Writer().encode(data, barcodeFormat, width, height);
                    break;

                case CODE_128:
                    bitMatrix = new Code128Writer().encode(data, barcodeFormat, width, height);
                    break;

                case EAN_8:
                    bitMatrix = new EAN8Writer().encode(data, barcodeFormat, width, height);
                    break;

                case EAN_13:
                    bitMatrix = new EAN13Writer().encode(data, barcodeFormat, width, height);
                    break;

                case PDF_417:
                    bitMatrix = new PDF417Writer().encode(data, barcodeFormat, width, height);
                    break;

                case DATA_MATRIX:
                    bitMatrix = new DataMatrixWriter().encode(data, barcodeFormat, width, height);
                    break;

                case ITF:
                    bitMatrix = new ITFWriter().encode(data, barcodeFormat, width, height);
                    break;

                case QR_CODE:
                    height = 300;
                    bitMatrix = new QRCodeWriter().encode(data, barcodeFormat, width, height, hints);
                    break;

                default:
                    return PrintResult.FAILED;
            }

            height = bitMatrix.getHeight();
            width = bitMatrix.getWidth();

            Bitmap imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < width; i++) {//width
                for (int j = 0; j < height; j++) {//height
                    imageBitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK: Color.WHITE);
                }
            }

            return printImage(imageBitmap, alignment);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return PrintResult.FAILED;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final int SMALL_FONT = 20;
    private final int LARGE_FONT = 30;
    private final float ITALIC_SKEW = -0.25f;

    private final boolean isAntiAlias = true;
    private final boolean isDither = false;

    private static Bitmap mLargeBitmap = null;
    private static Bitmap mSmallBitmap = null;
    private static Canvas mLargeCanvas = null;
    private static Canvas mSmallCanvas = null;

    protected Bitmap generateBitmap(String textLeft, String textCenter, String textRight, int style) {
        Paint p = new Paint();
        p.setTypeface(mTypeface);

        // Set paint styles
        p.setTextSize(((style & STYLE_LARGE_FONT) == STYLE_LARGE_FONT) ? LARGE_FONT : SMALL_FONT);
        p.setFakeBoldText((style & STYLE_BOLD) == STYLE_BOLD);
        p.setTextSkewX(((style & STYLE_ITALIC) == STYLE_ITALIC)? ITALIC_SKEW : 0);
        p.setUnderlineText((style & STYLE_UNDERLINE) == STYLE_UNDERLINE);
        p.setAntiAlias(isAntiAlias);
        p.setDither(isDither);

        // Set bitmap width
        int width = mPrintWidth.intValue();

        // y
        float baseline = (int) (-p.ascent() + 0.5f); //ascent() is negative

        // Set bitmap height
        int height = (int)(baseline + p.descent() + 0.5f);

        // x for each alignment
        int xLeft = 0;
        int xCenter = width >> 1;
        int xRight = width - 1;

        // These are reference variables used in drawing the text line further below
        Bitmap bitmap;
        Canvas canvas;

        // Only create new bitmaps / canvases if they haven't already been created
        // for this font size during this print job
        if ((style & STYLE_LARGE_FONT) == STYLE_LARGE_FONT) {
            if (mLargeBitmap == null || mLargeBitmap.isRecycled()) {
                mLargeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mLargeCanvas = new Canvas(mLargeBitmap);

                Log.d(TAG, "Created Bitmap " + mLargeBitmap.getWidth() + " x " + mLargeBitmap.getHeight());
            }

            bitmap = mLargeBitmap;
            canvas = mLargeCanvas;
        }
        else {
            if (mSmallBitmap == null || mSmallBitmap.isRecycled()) {
                mSmallBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mSmallCanvas = new Canvas(mSmallBitmap);

                Log.d(TAG, "Created Bitmap " + mSmallBitmap.getWidth() + " x " + mSmallBitmap.getHeight());
            }

            bitmap = mSmallBitmap;
            canvas = mSmallCanvas;
        }

        // Set the background and font colors
        if ((style & STYLE_REVERSE) == STYLE_REVERSE) {
            p.setColor(Color.BLACK);
            canvas.drawRect(0, 0, width, height, p);
            p.setColor(Color.WHITE);
        }
        else {
            p.setColor(Color.WHITE);
            canvas.drawRect(0, 0, width, height, p);
            p.setColor(Color.BLACK);
        }

        // Paint the text
        if (textLeft != null && !textLeft.isEmpty()) {
            p.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(textLeft, xLeft, baseline, p);
        }

        if (textCenter != null && !textCenter.isEmpty()) {
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(textCenter, xCenter, baseline, p);
        }

        if (textRight != null && !textRight.isEmpty()) {
            p.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(textRight, xRight, baseline, p);
        }

        return bitmap;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    static byte[] _raw = null;

    protected byte[] _pixelsARGB2Mono(int[] pixels, int width, int height, int monoWidth, Alignment align, boolean invert)
    {
        if(null == pixels)
            return null;

        //Log.d(TAG, "_pixelsARGB2Mono: pixels.len=" + pixels.length + ", w=" + width + ", h=" + height +
        //      ", pwidth=" + monoWidth + ", align=" + align + ", invert=" + invert);
        if(null == _raw || (monoWidth * height != _raw.length))
        {
            _raw = new byte[monoWidth * height];
        }

        //Arrays.fill(raw, (byte)0x0);
        int maxMonoWidth = monoWidth << 3;
        int k = 0;
        byte m = 0;//mono pixel

        if(Alignment.LEFT == align || Alignment.NONE == align)
        {
            for(int j = 0, pl = 0, rl = 0; j < height; j++, pl = pl + width, rl = rl + monoWidth)
            {
                //if(j < 35)Log.d(TAG, "==============j="+j);//DEBUG
                for(int i = 0, p = 0; i < monoWidth; i++)
                {
                    k = i << 3;
                    if(k + 8 <= width)
                        p = 8;
                    else if(k > width)
                        p = 0;
                    else
                        p = width - k;

                    m = 0;
                    //ARGB pixel 2 monochrome bit
                    for(int r = 0, pr = pl + k; r < p; r++)
                    {
                        m = (byte)((m << 1) + _argb2mono(pixels[pr + r], invert));

                        /*
                        //dump partial data for debug
                        if(j < 30 && i < 10)
                            Log.d(TAG, "Argb2Mono: " + String.format("%08X", pixels[pr + r]) + " => " + String.format("%08X", m));
                        //*/
                    }

                    //case: width of image is smaller width of print
                    if(p == 0)
                        m = 0;
                    else if(p < 8)
                        m = (byte)(m << (8 - p));

                    _raw[rl + i] = m;
                }
            }
        }
        else if(Alignment.CENTER == align)
        {
            //image is larger than or equals to paper
            if(width >= maxMonoWidth)
            {
                for(int j = 0, pl = (width - maxMonoWidth) >> 1, rl = 0; j < height; j++, pl = pl + width, rl = rl + monoWidth)
                {
                    for(int i = 0; i < monoWidth; i++)
                    {
                        k = i << 3;

                        m = 0;
                        //ARGB pixel 2 monochrome bit
                        for(int r = 0, pr = pl + k; r < 8; r++)
                        {
                            m = (byte)((m << 1) + _argb2mono(pixels[pr + r], invert));
                        }

                        _raw[rl + i] = m;
                    }
                }
            }
            //image is smaller than paper
            else
            {
                int emptyWidth = (maxMonoWidth - width) >> 1;
                for(int j = 0, pl = 0, rl = 0; j < height; j++, pl = pl + width, rl = rl + monoWidth)
                {
                    for(int i = 0, preByteEmpty = 0, postByteEmpty = 0, ps = 0; i < monoWidth; i++)
                    {
                        k = i << 3;
                        if(k + 8 <= emptyWidth)
                        {
                            preByteEmpty = 8;
                            ps = 0;
                            postByteEmpty = 0;
                        }
                        else if(k <= emptyWidth) //equals to if(k + 8 <= emptyWidth + 8)
                        {
                            preByteEmpty = emptyWidth - k;
                            ps = - preByteEmpty;

                            //image width is small than 8
                            postByteEmpty = 8 - preByteEmpty - width;
                            if(postByteEmpty < 0)
                                postByteEmpty = 0;
                        }
                        else if(k + 8 <= emptyWidth + width)
                        {
                            preByteEmpty = 0;
                            ps = ps + 8;
                            postByteEmpty = 0;
                        }
                        else if(k <= emptyWidth + width) //equals to if(k + 8 <= emptyWidth + width + 8)
                        {
                            preByteEmpty = 0;
                            ps = ps + 8;
                            postByteEmpty = k + 8 - emptyWidth - width;
                        }
                        else
                        {
                            preByteEmpty = 0;
                            //ps = ps + 8;
                            postByteEmpty = 8;
                        }

                        m = 0;
                        //pre-empty
                        if(preByteEmpty == 8)
                            m = 0;
                        else if(preByteEmpty > 0)
                            m = (byte)(m << preByteEmpty);

                        //ARGB pixel 2 monochrome bit
                        for(int r = preByteEmpty, pr = pl + ps; r < 8 - postByteEmpty; r++)
                        {
                            m = (byte)((m << 1) + _argb2mono(pixels[pr + r], invert));
                        }

                        //post-empty
                        if(postByteEmpty == 8)
                            m = 0;
                        else if(postByteEmpty > 0)
                            m = (byte)(m << postByteEmpty);

                        _raw[rl + i] = m;
                    }
                }
            }
        }
        else if(Alignment.RIGHT == align)
        {
            //image is larger than or equals to paper
            if(width >= maxMonoWidth)
            {
                for(int j = 0, pl = width - maxMonoWidth, rl = 0; j < height; j++, pl = pl + width, rl = rl + monoWidth)
                {
                    for(int i = 0; i < monoWidth; i++)
                    {
                        k = i << 3;

                        m = 0;
                        //ARGB pixel 2 monochrome bit
                        for(int r = 0, pr = pl + k; r < 8; r++)
                        {
                            m = (byte)((m << 1) + _argb2mono(pixels[pr + r], invert));
                        }

                        _raw[rl + i] = m;
                    }
                }
            }
            //image is smaller than paper
            else
            {
                for(int j = 0, pl = 0, rl = 0; j < height; j++, pl = pl + width, rl = rl + monoWidth)
                {
                    for(int i = 0, byteEmpty = 0, ps = 0; i < monoWidth; i++)
                    {
                        k = i << 3;
                        if(k + 8 + width <= maxMonoWidth)
                            byteEmpty = 8;
                        else if(k + width <= maxMonoWidth) //equals to if(k + 8 + width <= maxMonoWidth + 8)
                        {
                            byteEmpty = maxMonoWidth - width - k;
                            ps = - byteEmpty;
                        }
                        else
                        {
                            byteEmpty = 0;
                            ps = ps + 8;
                        }

                        m = 0;
                        //fill empty
                        if(byteEmpty == 8)
                            m = 0;
                        else if(byteEmpty > 0)
                            m = (byte)(m << byteEmpty);

                        //fill image
                        for(int r = byteEmpty, pr = pl + ps; r < 8; r++)
                        {
                            m = (byte)((m << 1) + _argb2mono(pixels[pr + r], invert));
                        }

                        _raw[rl + i] = m;
                    }
                }
            }
        }

        return _raw;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private int _argb2mono(int pixel, boolean invert)
    {
        int mono;

        if(pixel == -1) {
            if (invert)
                mono = 1;
            else
                mono = 0;
        }

        else {

            //
            int a = Color.alpha(pixel);
            int b = Color.blue(pixel);
            int g = Color.green(pixel);
            int r = Color.red(pixel);
            //
            if ((a >= 0x80 && (299 * r + 587 * g + 114 * b) <= 128000))
            {
                if (invert)
                    mono = 0;
                else
                    mono = 1;
            } else {
                if (invert)
                    mono = 1;
                else
                    mono = 0;
            }
        }

        return mono;
    }
}
