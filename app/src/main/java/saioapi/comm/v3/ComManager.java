package saioapi.comm.v3;

import android.content.Context;
import android.util.Log;
import saioapi.comm.v3.OnComEventListener;
import java.lang.reflect.Method;

public class ComManager {
    static
    {
        System.loadLibrary("SaioBase");
    }

    //
    //  Constants (Error code and Events)
    //
    /** Indicates to get detail error code by using {@link #lastError} method. */
    public static final int ERR_OPERATION       = 0xFFFFFFFF;

    /** The device is no longer available. The device is in use by another. */
    public static final int ERR_NOT_READY       = 0x0000E000;

    /** The device does not exist. */
    public static final int ERR_NOT_EXIST       = 0x0000E002;

    /** The communication device has not been opened. */
    public static final int ERR_NOT_OPEN        = 0x0000E004;

    /** The inputted parameters are not valid. */
    public static final int ERR_INVALID_PARAM   = 0x0000E006;

    /** Unable to get method reference of the class listener. */
    public static final int ERR_NO_LISTENER     = 0x0000E008;

    /** An I/O error occurred while making a service operation. */
    public static final int ERR_IO_ERROR        = 0x0000E00A;

    /** The specified connection has not been established. */
    public static final int ERR_NO_CONNECTED    = 0x0000E00C;

    /** Fails to establish a XPD connection due to synchronization error. */
    public static final int ERR_XPD_SYNC        = 0x0000E00E;

    /** A communication timeout occurs on XPD connection. */
    public static final int ERR_XPD_TIMEOUT     = 0x0000E010;

    /** XPD Device is busy and unable to process the command now. */
    public static final int ERR_XPD_BUSY        = 0x0000E012;

    /** XPD Device is connected with Raw data and then diagnostic functions is unavailable*/
    public static final int ERR_DIAG_UNABAILABLE= 0x0000E100;

    /** Indicates new data comes from incoming stream. */
    public static final int EVENT_DATA_READY    = 0x00000001;

    /** Signal the End Of Transmission event is received (VNG only). */
    public static final int EVENT_EOT           = 0x00000002;

    /** Signals the connection with host has been lost. */
    public static final int EVENT_DISCONNECT    = 0x00000003;

    /** Signals the device just gets connection with host. */
    public static final int EVENT_CONNECT       = 0x00000004;

    /** Raw communication. */
    public static final int PROTOCOL_RAW_DATA    = 0x00000000;

    /** XAC legacy VNG device. */
    public static final int PROTOCOL_XAC_VNG     = 0x00000004;

    /** XAC VISA3 chip type contactless reader device. */
    public static final int PROTOCOL_XAC_VISA3   = 0x00000005;

    /** The id for communication device at USB 0. */
    public static final int DEVICE_USB0          = 0x00000000;

    /** The id for communication device at COM 0. */
    public static final int DEVICE_COM0          = 0x00000004;

    /** The id for communication device at COM 1. */
    public static final int DEVICE_COM1          = 0x00000005;

    /** The id for communication device at COM 2. */
    public static final int DEVICE_COM2          = 0x00000006;

    /** The id for communication device at COM 3. */
    public static final int DEVICE_COM3          = 0x00000007;

    /** The id for communication device at COM 4. */
    public static final int DEVICE_COM4          = 0x00000008;

    /** The id for extension board USB 0. */
    public static final int DEVICE_EXT_USB0      = 0x00000010;

    /** The id for extension board USB 1. */
    public static final int DEVICE_EXT_USB1      = 0x00000011;

    /** The id for printer at USB 0. */
    public static final int DEVICE_PRINTER0      = 0x00000012;

    /** The id for printer at USB 1. */
    public static final int DEVICE_PRINTER1      = 0x00000013;

    /** The id for printer at USB 2. */
    public static final int DEVICE_PRINTER2      = 0x00000014;

    /** The id for printer at USB 3. */
    public static final int DEVICE_PRINTER3      = 0x00000015;

    /** The id for printer at USB 4. */
    public static final int DEVICE_PRINTER4      = 0x00000016;

    /** The id for gadget serial port 0. */
    public static final int DEVICE_GS0           = 0x00000017;

    /** The id for communication device at ttyACM0. */
    public static final int DEVICE_ACM0           = 0x00000018;

    /** The id for communication device at ttyACM1. */
    public static final int DEVICE_ACM1           = 0x00000019;

    /** The id for communication device at ttyACM2. */
    public static final int DEVICE_ACM2           = 0x0000001A;

    /** The id for communication device at ttyACM3. */
    public static final int DEVICE_ACM3           = 0x0000001B;

    /** The id for communication device at ttyACM4. */
    public static final int DEVICE_ACM4           = 0x0000001C;

    /** The id for communication device at ttyACM5. */
    public static final int DEVICE_ACM5           = 0x0000001D;

    /** The id for communication device at ttyACM6. */
    public static final int DEVICE_ACM6           = 0x0000001E;

    /** The id for communication device at ttyACM7. */
    public static final int DEVICE_ACM7           = 0x0000001F;

    /** The id for communication device at ttyVSP0. */
    public static final int DEVICE_VSP0           = 0x00000020;

    /** The id for communication device at ttyVSP1. */
    public static final int DEVICE_VSP1           = 0x00000021;

    /** The id for communication device at ttyVSP2. */
    public static final int DEVICE_VSP2           = 0x00000022;

    /** The id for communication device at ttyVSP3. */
    public static final int DEVICE_VSP3           = 0x00000023;

    /** The id for extension board USB 2. */
    public static final int DEVICE_EXT_USB2      = 0x00000025;

    /** The id for extension board USB 3. */
    public static final int DEVICE_EXT_USB3      = 0x00000026;

    /** The id for extension board USB 4. */
    public static final int DEVICE_EXT_USB4      = 0x00000027;

    /** The id for extension board USB 5. */
    public static final int DEVICE_EXT_USB5      = 0x00000028;

    /** The device id for spi-interface printer (for SC20-series products with built-in printer only). */
    public static final int DEVICE_PRINTER_SPI0  = 0x00000029;

    /** The id for qualcomm smd11 (Shared Memory Driver). */
    public static final int DEVICE_SMD11         = 0x0000002A;

    /** The id for qualcomm smd8 (Shared Memory Driver). */
    public static final int DEVICE_SMD8          = 0x0000002B;

    /** The id for usb accessory */
    public static final int DEVICE_USB_ACCESSORY = 0x0000002C;

    /** The control flag specifies that default number of bits per byte is CS8 */
    public static final int CFLAG_CSIZE_DEFAULT   = 0x00000000;

    /** The control flag specifies 5 bits per byte. This feature is not supported for DEVICE_COM0, DEVICE_COM1, DEVICE_COM2, DEVICE_COM3 of iMX6 series products. */
    public static final int CFLAG_CS5             = 0x00000001;

    /** The control flag specifies 6 bits per byte. This feature is not supported for DEVICE_COM0, DEVICE_COM1, DEVICE_COM2, DEVICE_COM3 of iMX6 series products. */
    public static final int CFLAG_CS6             = 0x00000010;

    /** The control flag specifies 7 bits per byte. */
    public static final int CFLAG_CS7             = 0x00000020;

    /** The control flag specifies 8 bits per byte. */
    public static final int CFLAG_CS8             = 0x00000030;

    /** The control flag specifies sending two stop bits rather than one.*/
    public static final int CFLAG_CSTOPB          = 0x00000040;

    /** The control flag specifies that Hang up the modem connection when the last process with the port open closes it */
    public static final int CFLAG_HUPCL           = 0x00000400;

    /** The control flag specifies enable parity generation and detection. */
    public static final int CFLAG_PARENB          = 0x00000100;

    /** The control flag specifies that use odd parity rather than even if CFLAG_PARENB is set. */
    public static final int CFLAG_PARODD          = 0x00000200;

    /** The control flag specifies that enable RTS/CTS (hardware) flow control. */
    public static final int CFLAG_CRTSCTS         = 0x80000000;

    /** Vendor id of XAC Peripheral Devices. */
    public static final int XAC_DEVICE_VID      = 0x2182;//8578

    /** Product id of XAC Peripheral Devices. */
    public static final int XAC_DEVICE_PID      = 0x8000;//32768

    /** Vendor id of serial converter PL2303. */
    public static final int USB_VID_PL2303      = 0x067B;//1659

    /** Product id of serial converter PL2303 */
    public static final int USB_PID_PL2303      = 0x2303;//8963

    /** Vendor id of barcode reader V10. */
    public static final int USB_VID_V10         = 0x040B;//1035

    /** Product id of barcode reader V10 */
    public static final int USB_PID_V10         = 0x2043;//8259

    /** Vendor id of XAC Peripheral Devices. */
    public static final int USB_VID_XAC         = 0x2182;//8578

    /** Product id of XAC Pinpads. */
    public static final int USB_PID_XAC_PINPAD  = 0x8000;//32768

    /** Product id of XAC Printers. */
    public static final int USB_PID_XAC_PRINTER = 0x7000;//28672

    /** Product id of XAC CDC interface Devices. */
    public static final int USB_PID_XAC_CDC     = 0xA000;//40960

    /** BcdDevice value of XAC key readers. */
    public static final int USB_BCDDEVICE_XAC_KEY_READER  = 0x0001;

    /** Diagnostic command RM1. */
    public static final int CMD_DIAG_RM1 = 0;

    /** Diagnostic command RM2. */
    public static final int CMD_DIAG_RM2 = 1;

    private saioapi.comm.v2.ComManager mV2Commanager;
    private Context mContext;
    final String TAG = "SaioComManager-V3";
    private ServiceManager mServiceManager;
    private boolean mServiceOpenPort = false;
    private saioapi.comm.v2.OnComEventListener V2OnComEventListener = null;
    private int[] portNums = new int[] {-1};
    public static final String ACTION_COMMANAGER_SERVICE_REBIND = "android.xac.commanager_service_rebind";
    private boolean isV3Enabled;

    public ComManager(Context context)
    {
        mContext = context;
        mV2Commanager = new saioapi.comm.v2.ComManager(mContext);
        isV3Enabled = isV3ComManagerEnabled();
        if (isV3Enabled) {
            mServiceManager = new ServiceManager(mContext);
            portNums = getV3ComManagerEnabledPort();
        }
    }

    /**
     * The listener that receives notifications when an COMM event is triggered.
     */
    private OnComEventListener OnComEventListener = null;
    private OnComEventListener OnDiagEventListener = null;

    //

    //
    //    Methods
    //

    /**
     * Get the registered Listener that handle the COM event.
     * @return The callback to be invoked with a COM event is triggered,
     *         or null id no callback has been set.
     */
    public final OnComEventListener getOnComEventListener()
    {
        return OnComEventListener;
    }
    public final OnComEventListener getOnDiagEventListener()
    {
        return OnDiagEventListener;
    }

    /**
     * Register a callback to be invoked when a COM event is triggered.
     * @param listener The callback that will be invoked.
     */
    public void setOnComEventListener(OnComEventListener listener)
    {
        OnComEventListener = listener;
        if (isV3Enabled && mServiceOpenPort) {
            mServiceManager.setOnComEventListener(_onComEventListener);
        }else {
            mV2Commanager.setOnComEventListener(_onV2ComEventListener);
        }
    }

    /**
     * This method gets the usb device id that sort with parameter vid and pid.(for Usb device only)
     * @param vid Vendor ID of usb device
     * @param pid Product id of usb device.
     * @return Return a int array containing device id that sort with parameter vid and pid.
     */
    public int[] getUsbDevId(int vid, int pid){
        return getUsbDevId(vid, pid, -1);
    }

    /**
     * This method gets the usb device id that sort with parameter vid, pid and bcdDevice.(for Usb device only)
     * @param vid Vendor ID of usb device
     * @param pid Product id of usb device.
     * @param bcdDevice The device-defined revision number. If set to -1, bcdDevice value will be ignored in this function.
     * @return Return a int array containing device id that sort with parameter vid, pid and bcdDevice.
     */
    public int[] getUsbDevId(int vid, int pid, int bcdDevice){
        return mV2Commanager.getUsbDevId(vid,pid,bcdDevice);
    }

    /**
     * This method gets Build-In Epp device id.
     * @return Return Build-In Epp device id.
     */
    public static int getBuiltInEppDevId(){
        return saioapi.comm.Com.getBuiltInEppDevId();
    }

    /**
     * This method gets Build-In printer device id.
     * @return Return Build-In printer device id.
     */
    public static int getBuiltInPrinterDevId(){
        return saioapi.comm.Com.getBuiltInPrinterDevId();
    }

    /**
     * This method gets card reader device id.
     * @return Return card reader device id.
     */
    public static int getCardReaderDevId(){
        return saioapi.comm.Com.getCardReaderDevId();
    }

    /**
     * This method gets key reader device id.
     * @return Return key reader device id.
     */
    public int[] getKeyReaderDevId(){
        return getUsbDevId(USB_VID_XAC,USB_PID_XAC_CDC,USB_BCDDEVICE_XAC_KEY_READER);
    }

    /**
     * This method gets PL2303 device id.
     * @return Return PL2303 device id.
     */
    public int[] getPL2303DevId(){
        return getUsbDevId(USB_VID_PL2303,USB_PID_PL2303,-1);
    }

    private boolean isV3ComManagerEnabled(){
        Method get = null;
        String value = "false";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.commanager.v3.enabled", "false"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Log.i(TAG,"isV3ComManagerEnabled:"+value);
        return value.equals("true");
    }

    private int[] getV3ComManagerEnabledPort(){
        Method get = null;
        String value = "-1";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.commanager.v3.enabled.port", "-1"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        String[] ports = value.split(",");
        int[] devIds = new int[ports.length];;
        devIds = StringArrToIntArr(ports);

        Log.i(TAG,"getV3ComManagerEnabledPort:"+value);
        return devIds;
    }

    private int[] StringArrToIntArr(String[] s) {
        int[] result = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            result[i] = Integer.parseInt(s[i]);
        }
        return result;
    }

    private boolean isV3ComManagerEnabledPort(int dev){
        for (int i=0; i<portNums.length; i++) {
            Log.i(TAG, "portNums[" + i + "]="+portNums[i]);
            if (dev == portNums[i]){
                return true;
            }
        }
        return false;
    }

    /**
     * This method tries to open the communication service. It always implements an exclusive open.
     * @param dev_id  Logical ID of the selected communication device where the connection to be established.
     *               The ID should be one element value of {@link #getUsbDevId} return array or
     *               the following predefined values defines available ID.
     * <table border=1>
     *   <thead><tr><th>Values</th><th>Status</th></tr></thead>
     *   <tbody>
     *     <tr><td>0 to 31</td><td>serial communication device</td></tr>
     *     <tr><td>other</td><td>Unused</td></tr>
     *   </tbody>
     * </table>
     * @return Return zero if the function succeeds. Otherwise a nonzero error code defined in class constants.
     */
    public int open(int dev_id) {
        int ret;
        if (isV3Enabled && isV3ComManagerEnabledPort(dev_id)) {
            Log.i(TAG,"============V3=============");
            ret=mServiceManager.open(dev_id);
            if(ret != ERR_NOT_OPEN) {
                mServiceOpenPort = true;
            }else{
                mServiceOpenPort = false;
            }
            if (OnComEventListener != null)
                mServiceManager.setOnComEventListener(_onComEventListener);
            return ret;
        }else{
            Log.i(TAG,"============V2=============");
            return mV2Commanager.open(dev_id);
        }
    }

    /**
     * This method tries to open the communication service with the usb device.
     * @param dev_id  Logical ID of the selected communication device where the connection to be established.
     *               The ID should be one element value of {@link #getUsbDevId} return array.
     * @param interface_id The interface's bInterfaceNumber field of the usb device.
     * @return Return zero if the function succeeds. Otherwise a nonzero error code defined in class constants.
     */
    public int openUsbDev(int dev_id,int interface_id) {
        return mV2Commanager.openUsbDev(dev_id, interface_id);
    }

    /**
     * This method tries to close an opened communication service. If there is some operation is not completed, it will wait for all
     * done.
     * @return Return zero if the function succeeds else nonzero error code defined in class constants.
     */
    public int close(){
        if (isV3Enabled && mServiceOpenPort) {
            mServiceOpenPort = false;
            return mServiceManager.close();
        }else{
            return mV2Commanager.close();
        }
    }

    /**
     * This method establishes a connection to device over a specified communication device (serial, USB etc.) with a given protocol.
     * @param baud Specifies the baud rate at which the serial communication device operation. It is an actual baud rate value.
     * @param data_size The number of bits in the bytes transmitted and received. Ignored by USB device.
     * @param stop_bit The number of stop bits to be used. Ignored by USB device.
     * @param parity Specifies the parity scheme to be used. Ignored by USB device.
     * @param flow_control Specifies the flow control. Now only support HW flow control {@link #CFLAG_CRTSCTS}, default is 0. Ignored by USB device.
     * @param protocol The communication protocol family defined in below class constants:
     * <table border=1>
     *   <thead><tr><th>Class Constants</th><th>Protocols</th></tr></thead>
     *   <tbody>
     *     <tr><td>PROTOCOL_RAW_DATA</td><td>Raw communication</td></tr>
     *     <tr><td>PROTOCOL_XAC_VNG</td><td>XAC legacy VNG device</td></tr>
     *     <tr><td>PROTOCOL_XAC_VISA3</td><td>XAC VISA3 chip type contactless reader device. This protocol is not supported for USB device.</td></tr>
     *   </tbody>
     * </table>
     * @param extra The data array containing extra connect information, ignored by now.
     * @return Return zero if the function succeeds else nonzero error code defined in class constants.
     */
    public int connect(int baud, int data_size, int stop_bit, int parity, int flow_control, int protocol, byte[] extra) {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.connect(baud,data_size,stop_bit,parity,flow_control,protocol,extra);
        } else {
            return mV2Commanager.connect(baud,data_size,stop_bit,parity,flow_control,protocol,extra);
        }
    }

    /**
     * This method establishes a connection to device over a specified communication device (serial, USB etc.) with a given protocol.
     * @param protocol The communication protocol family defined in below class constants:
     * <table border=1>
     *   <thead><tr><th>Class Constants</th><th>Protocols</th></tr></thead>
     *   <tbody>
     *     <tr><td>PROTOCOL_RAW_DATA</td><td>Raw communication</td></tr>
     *     <tr><td>PROTOCOL_XAC_VNG</td><td>XAC legacy VNG device</td></tr>
     *     <tr><td>PROTOCOL_XAC_VISA3</td><td>XAC VISA3 chip type contactless reader device. This protocol is not supported for USB device.</td></tr>
     *   </tbody>
     * </table>
     * @return Return zero if the function succeeds else nonzero error code defined in class constants.
     */
    public int connect(int protocol){
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.connect(115200, 0, 0, 0, 0, protocol, null);
        } else {
            return mV2Commanager.connect(protocol);
        }
    }

    /**
     * Read data from the open input stream, the thread will be blocked until something is received before timer has expired.
     * @param data The byte array to receive data from to the device.
     * @param len The number of byte to be read from the communication device.
     * @param timeout The maximum time (in milliseconds) to finish the read operation, if the timeout is set to zero the timer will never
     *        expire else, the timeout will begin when the request begins to be processed.
     * @return If successful, the number of bytes actually read is returned, otherwise a nonzero error code defined in class constants.
     */
    public int read(byte[] data,int len,int timeout) {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.read(data,len,timeout);
        } else {
            return mV2Commanager.read(data,len,timeout);
        }
    }

    /**
     * This method is writing data from the specified byte array to the opened output stream before the timer is expired. It is asynchronous
     * and results in an event sent to the {@link #listener} method.
     * For LA platform products, writing to DEVICE_GS0 may take up to 10ms to complete transmitting the whole package which is larger than 500 bytes.
     * Note: data queued in system buffer could not be flushed away when flow control is enabled.
     *
     * @param data The data to be written to the output device.
     * @param len The number of byte to be written to the communication device.
     * @param timeout The maximum time (in milliseconds) to finish the write operation, if the timeout is set to zero the timer will never
     *         expire else, the timeout will begin when the request begins to be processed.
     * @return Upon successful completion, the number of bytes which were written is return, Otherwise a nonzero error code is returned.
     */
    public int write(byte[] data,int len,int timeout) {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.write(data, len, timeout);
        } else {
            return mV2Commanager.write(data,len,timeout);
        }
    }

    /**
     * The method gets a value indicating the opened or closed status of the communication service.
     * @return  true if the communication service is open; otherwise, false.
     */
    public boolean isOpened() {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.isOpened();
        } else {
            return mV2Commanager.isOpened();
        }
    }

    /**
     * The method sets a value to enable usb reconnect feature. Must set by this method before open device. After opening usb device, not allow to change setting by the method.
     * Default is disable that opened communication service will be closed as unplugged.
     * If enable, the usb device will reconnect after unplug /re-plug.(for Usb device only)
     * @param enable true to enable reconnect feature; otherwise, false. The default is false.
     * @return Return zero if there is no error else {@link #ERR_INVALID_PARAM} that opened communication service not allow to change setting.
     */
    public int enableUsbReconnect(boolean enable){
        if (isV3Enabled && mServiceOpenPort) {
            return 0;
        } else {
            return mV2Commanager.enableUsbReconnect(enable);
        }
    }

    /**
     * Retrieves the last error occurs on communication operation.
     * @return Return zero if there is no error else nonzero error code defined in class constants.
     */
    public int lastError(){
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.lastError();
        } else {
            return mV2Commanager.lastError();
        }
    }

    /**
     * Get the status of the given communication device.
     * <table border=1>
     *   <thead><tr><th>BIT</th><th>Status</th><th>BIT=0</th><th>BIT=1</th></tr></thead>
     *   <tbody>
     *     <tr><td>0</td><td>Communication device connection status</td><td>Offline</td><td>Online</td></tr>
     *     <tr><td>1</td><td>Incoming data is available</td><td>No</td><td>Yes</td></tr>
     *     <tr><td>2-31</td><td>Unused</td><td></td><td></td></tr>
     *   </tbody>
     * </table>
     * @return Return {@link #ERR_INVALID_PARAM} if USB device is opened else the device status is returned for success or a {@link #ERR_OPERATION} is returned.
     *         The method {@link #lastError} can be used to indicate the error.
     */
    public int status(){
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.status();
        } else {
            return mV2Commanager.status();
        }
    }

    /**
     * Get CTS signal
     * @return Return {@link #ERR_INVALID_PARAM} if USB device is opened else the device status is returned for success or a {@link #ERR_OPERATION} is returned.
     *         The method {@link #lastError} can be used to indicate the error.
     */
    public int getCts() {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.getCts();
        } else {
            return mV2Commanager.getCts();
        }
    }

    /**
     * Set RTS signal (only works when hardware flow control is supported and enabled)
     * @param rts 1 to enable RTS; 0 to disable RTS
     * @return Return {@link #ERR_INVALID_PARAM} if USB device is opened else the device status is returned for success or a {@link #ERR_OPERATION} is returned.
     *         The method {@link #lastError} can be used to indicate the error.
     */
    public int setRts(int rts) {
        if (isV3Enabled && mServiceOpenPort) {
            return mServiceManager.setRts(rts);
        } else {
            return mV2Commanager.setRts(rts);
        }
    }

    /**
     * The method get called when the class received a notification event of the given communication device and the register method has
     * been enabled.
     * @param event Indicates the event defined in class constants.
     */
    public void listener(int event)
    {
        //
        //  Call your real function to handle event here
        //
        if (null != OnComEventListener)
        {
            OnComEventListener.onEvent(event);
        }
    }

    private saioapi.comm.v2.OnComEventListener _onV2ComEventListener = new saioapi.comm.v2.OnComEventListener(){
        @Override
        public void onEvent(int event) {
            // TODO Auto-generated method stub
            listener(event);
        }
    };

    private OnComEventListener _onComEventListener = new OnComEventListener(){
        @Override
        public void onEvent(int event) {
            // TODO Auto-generated method stub
            listener(event);
        }
    };

    private OnDiagEventListener _diagEventListener = new OnDiagEventListener() {
        @Override
        public void onEvent(int cmd, int event) {
            // TODO Auto-generated method stub
            if (null != OnDiagEventListener)
            {
                OnDiagEventListener.onEvent(event);
            }
        }
    };

    /**
     * (Only used for diagnostic feature, if you need to use it, please contact xac for technical support.)
     * Register a callback to be invoked when a Diag event is triggered.
     * @param diaglistener The callback that will be invoked.
     */
    public void setOnDiagEventListener(OnComEventListener diaglistener)
    {
        OnDiagEventListener = diaglistener;
        if (isV3Enabled) {
            mServiceManager.setOnDiagEventListener(_diagEventListener);
        }
    }

    /**
     * (Only used for diagnostic feature, if you need to use it, please contact xac for technical support.)
     * This method is writing diagnostic command and results in an event sent to the {@link #listener} method.
     * @param dev The output device.
     * @param cmd The diagnostic command to the output device.
     * @param module {ModuleIDs} (Please See FW spec.)
     * @return Return zero if there is no error else nonzero error code defined in class constants.
     */
    public int diagWrite(int dev,int cmd,byte[] module) {
        if (isV3Enabled) {
            return mServiceManager.diagWrite(dev, cmd, module);
        } else {
            return ERR_OPERATION;
        }
    }

    /**
     * (Only used for diagnostic feature, if you need to use it, please contact xac for technical support.)
     * Read Diagnostic commands response.
     * @param dev The input device.
     * @param data The byte array to receive data from to the device.
     * @param len The number of byte to be read from the communication device.
     * @return If successful, the number of bytes actually read is returned, otherwise a nonzero error code defined in class constants.
     */

    public int diagRead(int dev, byte[] data,int len) {
        if (isV3Enabled) {
            return mServiceManager.diagRead(dev,data,len);
        } else {
            return ERR_OPERATION;
        }
    }
}
