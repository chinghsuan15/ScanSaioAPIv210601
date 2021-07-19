
package saioapi.comm.v2;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbConstants;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

class NativeUsb extends Connection{
    private final String TAG = "CM-NativeUsb";
    private UsbDeviceConnection mUsbConnection;
    private UsbEndpoint mUsbOutEndpoint;
    private UsbEndpoint mUsbInEndpoint;
    private UsbEndpoint mUsbEndpoint;
    private UsbInterface mUsbInterface;
    private boolean mOpened;
    private boolean mEndpointIn = false;
    private boolean mEndpointOut = false;
    private UsbManager mUsbManager;
    private UsbDevice usbDevice;
    private int device_id[]= new int[10];
    private byte[] mRawDescs;
    private int mBcdDevice = -1;
    private OnComEventListener mOnComEventListener = null;
    private Context mContext;
    private boolean mReconnectEvent;
    private boolean mReconnet = false;
    private int LOG_MAX_LENGTH = 256;
    private static final int USB_WRITE_TIMEOUT_MILLIS = 100;
    private int usbInterfaceId;

    NativeUsb(Context context,OnComEventListener listener, boolean reconnect)
    {
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mOnComEventListener = listener;
        mContext = context;
        mReconnectEvent = reconnect;
    }

    void listener(int event)
    {
        //
        //  Call your real function to handle event here
        //
        if (null != mOnComEventListener)
        {
            mOnComEventListener.onEvent(event);
        }
    }

    private boolean openEndpoint(int interface_id) {
        //Log.i(TAG,"XAC ID: " + usbDevice.getDeviceId());
        // Attempt to connect to each USB device and send a API_VERSION message to the device until pinpad is found
        // Obtain the interface for the USB device

        int index = 0 ;
        int interfaceCount = usbDevice.getInterfaceCount();
        if (interface_id != -1) {
            if (interface_id < usbDevice.getInterfaceCount()) {
                index = interface_id;
                interfaceCount = interface_id + 1;
            } else {
                close();
                return false;
            }
        }
        Log.i("openUsbDev", "interface_id:" + interface_id+" / index:" + index+" / interfaceCount:" + interfaceCount);
        for (int i = index; i < interfaceCount; i++) {
            mUsbInterface = usbDevice.getInterface(i);
            // Endpoint Configuration
            for (int j = 0; j < mUsbInterface.getEndpointCount(); j++) {
                mUsbEndpoint = mUsbInterface.getEndpoint(j);
                switch(mUsbEndpoint.getType()) {
                    case UsbConstants.USB_ENDPOINT_XFER_BULK:
                        if (mUsbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                            mUsbInEndpoint = mUsbEndpoint;
                            mEndpointIn = true;
                        } else if (mUsbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                            mUsbOutEndpoint = mUsbEndpoint;
                            mEndpointOut = true;
                        }
                        break;
                }
            }
            if (mEndpointIn && mEndpointOut){
                break;
            }else{
                mEndpointIn=false;
                mEndpointOut=false;
            }
        }

        // Return an error if the endpoints are not properly found
        if (mEndpointIn && mEndpointOut) {
            // Setup the USB connection
            try {
                mUsbConnection = mUsbManager.openDevice(usbDevice);
            } catch (Exception e) {
                Log.e(TAG, "exception in UsbManager.openDevice :" + e.getMessage());
                e.printStackTrace();
            }
            if (mUsbConnection != null) {
                // Force claim the interface
                if (mUsbConnection.claimInterface(mUsbInterface, true)) {
                    mRawDescs = mUsbConnection.getRawDescriptors();
                    mBcdDevice = ((mRawDescs[13] << 8) + mRawDescs[12]);
                    Log.d(TAG, "BcdDevice:" + String.format("0x%04X", mBcdDevice));
                    mOpened = true;
                    return true;//Open success!!
                } else {
                    Log.d(TAG, usbDevice.getDeviceName() + " - Claim Interface Failed");
                }
            } else {
                Log.d(TAG, usbDevice.getDeviceName() + " - Open Device Failed");
            }
        }
        close();
        return false;
    }

    void setup(int baudRate, int data_size, int stop_bit, int parity, int flow_control, byte[] extra) {
        Log.d(TAG, "VID:" + String.format("0x%04X", usbDevice.getVendorId())+" / PID:" + String.format("0x%04X", usbDevice.getProductId()));
        if (usbDevice.getVendorId() == ComManager.USB_VID_PL2303
                && usbDevice.getProductId() == ComManager.USB_PID_PL2303)//PL2303 settings
        {
           initPL2303Chip();   
           ctrlOut(baudRate);
        }else if(usbDevice.getVendorId() == ComManager.USB_VID_V10 && usbDevice.getProductId() == ComManager.USB_PID_V10){
		   ctrlOut(baudRate);
		}
    }
    private final void ctrlOut(int baudRate)  {
        byte[] lineRequestData = new byte[7];

        lineRequestData[0] = (byte) (baudRate & 0xff);
        lineRequestData[1] = (byte) ((baudRate >> 8) & 0xff);
        lineRequestData[2] = (byte) ((baudRate >> 16) & 0xff);
        lineRequestData[3] = (byte) ((baudRate >> 24) & 0xff);

        lineRequestData[4] = 0;
        lineRequestData[5] = 0;
        lineRequestData[6] = (byte) 8;
        outControlTransfer(UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_CLASS | 0x01, 0x20, 0, 0, lineRequestData);
    }

    private final void outControlTransfer(int requestType, int request, int value, int index, byte[] data){
        int length = (data == null) ? 0 : data.length;
        int result = mUsbConnection.controlTransfer(requestType, request, value, index, data, length, USB_WRITE_TIMEOUT_MILLIS);
        if (result != length) {
            Log.d(TAG, String.format("ControlTransfer with value 0x%x failed: %d", value, result));
        }
    }
    private void initPL2303Chip() {
        int mDeviceType = getDeviceType();
        vendorIn(0x8484, 0, 1);
        vendorOut(0x0404, 0, null);

        vendorIn(0x8484, 0, 1);
        vendorIn(0x8383, 0, 1);
        vendorIn(0x8484, 0, 1);

        vendorOut(0x0404, 1, null);
        vendorIn(0x8484, 0, 1);
        vendorIn(0x8383, 0, 1);

        vendorOut(0, 1, null);
        vendorOut(1, 0, null);
        vendorOut(2, (mDeviceType == 0) ? 0x44 : 0x24, null);
    }
    private int getDeviceType(){
        int mDeviceType = 0;
        try {
            if (usbDevice.getDeviceClass() == 0x02) {
                mDeviceType = 1;
            } else{
                Method getRawDescriptorsMethod = mUsbConnection.getClass().getMethod("getRawDescriptors");
                byte[] rawDescriptors = (byte[]) getRawDescriptorsMethod.invoke(mUsbConnection);
                byte maxPacketSize0 = rawDescriptors[7];
                if (maxPacketSize0 == 64) {
                    mDeviceType = 0;
                } else if ((usbDevice.getDeviceClass() == 0x00) || (usbDevice.getDeviceClass() == 0xff)) {
                    mDeviceType = 2;
                } else {
                    mDeviceType = 0;
                }
            }
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return mDeviceType;
    }
    private final byte[] inControlTransfer(int requestType, int request, int value, int index, int length)  {
        byte[] buffer = new byte[length];
        int result = mUsbConnection.controlTransfer(requestType, request, value, index, buffer, length, 5000);
        /*if (result != length) {
            throw new IOException(String.format("ControlTransfer with value 0x%x failed: %d", value, result));
        }*/
        return buffer;
    }
    private final byte[] vendorIn(int value, int index, int length) {
        return inControlTransfer(UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR, 0x01, value, index, length);
    }

    private final void vendorOut(int value, int index, byte[] data) {
        outControlTransfer(UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR, 0x01, value, index, data);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    int open(int dev , int interface_id) {
        if (mUsbManager == null) {
            return ComManager.ERR_NOT_OPEN;
        }

        int id = dev;

        HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceHashMap.values().iterator();

        while (deviceIterator.hasNext()) {
            usbDevice = deviceIterator.next();
            if (usbDevice.getDeviceId() == id) {
                if (openEndpoint(interface_id)) {
                    if(mReconnectEvent) {
                        mReconnet = true;
                        usbInterfaceId = interface_id ;
                        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                        mContext.registerReceiver(mUsbReceiver, filter);
                    }
                    Log.d(TAG, "open() dev is : " + dev);
                    return 0;
                }
            }
        }

        return ComManager.ERR_NOT_OPEN;
    }

    int[] getUsbDevId(int vid, int pid, int bcdDevice){
        HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator_for_num = deviceHashMap.values().iterator();
        int device_num=0;
        Arrays.fill(device_id, -1);
        while (deviceIterator_for_num.hasNext()) {
            usbDevice = deviceIterator_for_num.next();
            if (usbDevice.getVendorId() == vid && usbDevice.getProductId() == pid) {
                if(bcdDevice == -1 || getBcdDeviceVal(usbDevice) == bcdDevice) {
                    device_id[device_num] = usbDevice.getDeviceId();
                    Log.i(TAG, "device_id[" + device_num + "]:" + device_id[device_num]);
                    device_num++;
                }
            }
        }
        Log.i(TAG, "device_num:" + device_num);
        int[] sorted_dev = Arrays.copyOf(device_id, device_num);
        Arrays.sort(sorted_dev);
        /*for(int i=0; i < sorted_dev.length; i++) {
            Log.i(TAG, "sorted_dev["+i+"]:" + sorted_dev[i]);
        }*/
        return sorted_dev;
    }

    private int getBcdDeviceVal(UsbDevice mUsbDevice) {
        UsbDeviceConnection usbConnection = null;
        try {
            usbConnection = mUsbManager.openDevice(mUsbDevice);
        } catch (Exception e) {
            Log.e(TAG, "exception in UsbManager.openDevice :" + e.getMessage());
            e.printStackTrace();
        }
        if (usbConnection != null) {
            byte[] rawDescs = usbConnection.getRawDescriptors();
            int bcdDeviceVal = ((rawDescs[13] << 8) + rawDescs[12]);
            usbConnection.close();
            return bcdDeviceVal;
        }else{
            Log.d(TAG,"getBcdDeviceVal: "+ mUsbDevice.getDeviceName() + " - Open Device Failed");
            return -1;
        }
    }

    int close() {

        if (mUsbConnection != null) {
            mUsbConnection.releaseInterface(mUsbInterface);
            mUsbConnection.close();
            mUsbConnection = null;
        }

        if (mUsbOutEndpoint != null) {
            mUsbOutEndpoint = null;
        }

        if (mUsbInEndpoint != null) {
            mUsbInEndpoint = null;
        }

        if (mUsbInterface != null) {
            mUsbInterface = null;
        }

        mOpened = false;
        mBcdDevice = -1;
        if(mReconnet) {
            mContext.unregisterReceiver(mUsbReceiver);
        }
        Log.d(TAG, "close()");
        return 0;
    }

    boolean isOpened() {
        return mOpened;
    }

    String printLog(byte[] data,int dataLen)
    {
        StringBuilder log = new StringBuilder();
        if (dataLen > LOG_MAX_LENGTH) {
            for (int h = 0; h < LOG_MAX_LENGTH/2; h++) {
                log.append(String.format("%02X ", data[h] & 0xFF));
            }
            log.append("... ");
            for (int t = dataLen-(LOG_MAX_LENGTH/2); t < dataLen; t++) {
                log.append(String.format("%02X ", data[t] & 0xFF));
            }
        }else{
            for (int a = 0; a < dataLen; a++) {
                log.append(String.format("%02X ", data[a] & 0xFF));
            }
        }

        return log.toString();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected int bulkWrite(byte[] data, int timeout) {
        try {
            if (mUsbConnection.claimInterface(mUsbInterface, true)) {
                Log.d(TAG, "TX["+data.length+"] = "+ printLog(data,data.length));
                int writelen = mUsbConnection.bulkTransfer(mUsbOutEndpoint, data, data.length, timeout);
                if (writelen > 0)
                    Log.d(TAG, "Write: done!!, written data length:"+writelen);
                return writelen;
            } else {
                mOpened = false;
                return -1;
            }
        }catch(Exception e) {
            mOpened = false;
            return -1;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected int bulkRead(byte[] data, int dataLength, int timeout) {
        try{
            if (mUsbConnection.claimInterface(mUsbInterface, true)) {
                int readlen = mUsbConnection.bulkTransfer(mUsbInEndpoint, data, dataLength, timeout);
                if (readlen > 0) {
                    Log.d(TAG, "RX[" + readlen + "] = "+printLog(data,readlen));
                    Log.d(TAG, "Read: done!!, read data length:"+readlen);
                }
                return readlen;
            }
            else {
                mOpened = false;
                return ComManager.ERR_NO_CONNECTED;
            }
        }
        catch(Exception e) {
            mOpened = false;
            return ComManager.ERR_NO_CONNECTED;
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (device != null) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    if (device.getVendorId() == usbDevice.getVendorId() &&
                            device.getProductId() == usbDevice.getProductId() &&
                            getBcdDeviceVal(device) == mBcdDevice) {
                        usbDevice = device;
                        if (openEndpoint(usbInterfaceId)) {
                            mOpened = true;
                            mOnComEventListener.onEvent(ComManager.EVENT_CONNECT);
                        }
                    }
                }
                else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    // call your method that cleans up and closes communication with the device
                    if (device == usbDevice) {
                        mOpened = false;
                        mOnComEventListener.onEvent(ComManager.EVENT_DISCONNECT);
                    }
                }
            }
        }
    };
}
