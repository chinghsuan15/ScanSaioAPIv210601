package saioapi.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import saioapi.base.Misc;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Calendar;

/**
 * Created by dennis_wang on 2015/10/22.
 */
public class SaioService {
    private static final String TAG = "SaioService";

    static
    {
        //
        //  Load the corresponding library
        //
        System.loadLibrary("SaioUtil");
    }

    /** The inputted parameters are not valid. */
    public static final int ERR_2ND_BRIGHTNESS_INVALID_PARAM    = 0x0000E000;

    /** No such device. */
    public static final int ERR_2ND_BRIGHTNESS_NO_SUCH_DEVICE   = 0x0000E001;

    /** No such device. */
    public static final int ERR_2ND_TOUCH_NO_SUCH_DEVICE        = 0x0000E002;

    /** Fail to switch to target usb device mode. (Curently usb device is disable mode.) */
    public static final int ERR_USBDEV_MODE_DISABLE             = 0x0000E003;

    /** Fail to switch to target usb device mode. (Curently usb device is cdc mode.) */
    public static final int ERR_USBDEV_MODE_CDC                 = 0x0000E004;

    /** Fail to switch to target usb device mode. (Curently usb device is unknow mode.) */
    public static final int ERR_USBDEV_MODE_UNKNOWN             = 0x0000E005;

    /** The sleep timeout 15 seconds. */
    public static final int PM_SLEEP_TIME_15_SEC        = 15000;

    /** The sleep timeout 30 seconds. */
    public static final int PM_SLEEP_TIME_30_SEC        = 30000;

    /** The sleep timeout 1 minute. */
    public static final int PM_SLEEP_TIME_1_MIN         = 60000;

    /** The sleep timeout 2 minutes. */
    public static final int PM_SLEEP_TIME_2_MIN         = 120000;

    /** The sleep timeout 5 minutes. */
    public static final int PM_SLEEP_TIME_5_MIN         = 300000;

    /** The sleep timeout 10 minutes. */
    public static final int PM_SLEEP_TIME_10_MIN        = 600000;

    /** The sleep timeout 30 minutes. */
    public static final int PM_SLEEP_TIME_30_MIN        = 1800000;

    /** The sleep timeout never, depend on platform setting */
    public static final int PM_SLEEP_TIME_NEVER         = 2147483647;

    /** The suspend timeout 30 seconds. */
    public static final int PM_SUSPEND_TIME_30_SEC      = 30000;

    /** The suspend timeout 1 minute. */
    public static final int PM_SUSPEND_TIME_1_MIN       = 60000;

    /** The suspend timeout 2 minutes. */
    public static final int PM_SUSPEND_TIME_2_MIN       = 120000;

    /** The suspend timeout 5 minutes. */
    public static final int PM_SUSPEND_TIME_5_MIN       = 300000;

    /** The suspend timeout 10 minutes. */
    public static final int PM_SUSPEND_TIME_10_MIN      = 600000;

    /** The suspend timeout 30 minutes. */
    public static final int PM_SUSPEND_TIME_30_MIN      = 1800000;

    /** The suspend timeout 60 minutes. */
    public static final int PM_SUSPEND_TIME_60_MIN      = 3600000;

    /** The poweroff minimum timeout 2 hours. */
    public static final int PM_POWEROFF_TIME_MIN_HOUR   = 2;

    /** The poweroff maximum timeout 108 hours. */
    public static final int PM_POWEROFF_TIME_MAX_HOUR   = 108;

    // input event enable/disable
    public static final int DEV_KEYBOARD    = 1;
    public static final int DEV_MOUSE       = 2;
    public static final int DEV_TOUCH       = 3;

    // ACM - A3/E200CP
    /** The LED id indicates the status bar led with blue light. */
    public static final int LED_STATUS_BAR_BLUE       = 0x00;

    /** The LED id indicates the status bar led with red light. */
    public static final int LED_STATUS_BAR_RED        = 0x01;

    /** The LED id indicates the status bar led with green light. */
    public static final int LED_STATUS_BAR_GREEN      = 0x02;

    /** The LED id indicates the status bar led with yellow light. */
    public static final int LED_STATUS_BAR_YELLOW     = 0x03;

    /** The LED id indicates the 1st led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT1        = 0x04;

    /** The LED id indicates the 2nd led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT2        = 0x05;

    /** The LED id indicates the 3rd led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT3        = 0x06;

    /** The LED id indicates the 4th led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT4        = 0x07;

    /** The LED id indicates the 5th led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT5        = 0x08;

    /** The LED id indicates the 6th led from the left in MSR slot. */
    public static final int LED_MSR_SLOT_LEFT6        = 0x09;

    /** The LED id indicates the logo led. */
    public static final int LED_LOGO                  = 0x0A;

    /** The LED id indicates the led in SCR slot. */
    public static final int LED_SCR_SLOT              = 0x0B;

    /** The LED id indicates the keypad led. */
    public static final int LED_KEY_PAD               = 0x0C;

    /** The LED id indicates the MSR led of product E200CP. */
    public static final int LED_MSR_SLOT_E200CP       = 0x0D;

    /** Switch USB mode to host for AP210S/AP220S */
    public static final int USB_HOST_MODE             = 0;

    /** Switch USB mode to device for AP210S/AP220S */
    public static final int USB_DEVICE_MODE           = 1;
    
    // T3
    public static final int ANTENNA_INTERNAL = 1;
    public static final int ANTENNA_EXTERNAL = 0;
    //
    public static final int USB_HIGH_SPEED  = 0;
    public static final int USB_FULL_SPEED  = 1;
    public static final int USB_NOT_SUPPORT = -1;

    //---- intent action ---------------------------------------------------------------------------
    // 24hr reboot
    public static final String SET_REBOOT_TIME  = "SaioService.SET_REBOOT_TIME";
    public static final String REBOOT_HOUR      = "SaioService.REBOOT_HOUR";
    public static final String REBOOT_MINUTE    = "SaioService.REBOOT_MINUTE";

    public static final String ACTION_TRIPLE_TAP_POWER_GESTURE = "SaioService.SYSTEM_TRIPLE_TAP_POWER_GESTURE";

    // device andmin
    public static final String ACTION_DEVICE_ADMIN_SET      = "SaioService.SET_DEVICE_ADMIN";
    public static final String ACTION_DEVICE_ADMIN_REMOVE   = "SaioService.REMOVE_DEVICE_ADMIN";
    public static final String DEVICE_ADMIN_PACKAGE_NAME    = "SaioService.DEVICE_ADMIN_PKG";

    // diagnostic property
    public static final String DIAG_NOTIFY_PRIORITY_PROPERTY  = "persist.sys.diag.notify.priority";
    // diagnostic intent action
    public static final String ACTION_DIAG_DEVICE             = "commanager.DIAGNOSTIC_DEVICE";
    public static final String ACTION_DIAG_API                = "commanager.DIAGNOSTIC_API";
    public static final String ACTION_DIAG_NOTIFY_PRIORITY    = "commanager.DIAGNOSTIC_NOTIFY_PRIORITY";

    public static final String ACTION_CRADLE_API_CALLBACK_RESPONSE = "com.xac.cradle.api.response";

    /** The Cradle action for of get info for call back */
    public static final int ACTION_CRADLE_GETINFO       = 0x01;
    /** The Cradle action for of get Log for call back */
    public static final int ACTION_CRADLE_GETLOG       = 0x02;
    /** The Cradle action for of reboot for call back */
    public static final int ACTION_CRADLE_REBOOT       = 0x03;
    /** The Cradle action for of reset for call back */
    public static final int ACTION_CRADLE_RESET       = 0x04;
    /** The Cradle action for of preupgrade for call back */
    public static final int ACTION_CRADLE_PREUPGRADLE = 0x05;
    /** The Cradle action for of transfer for call back */
    public static final int ACTION_CRADLE_TRANSFER = 0x06;
    /** The Cradle action for of update for call back */
    public static final int ACTION_CRADLE_UPDATE = 0x07;

    /** The Cradle action for of action tag for call back */
    public static final String EXTRA_CRADLE_ACTION = "ACTION";
    /** The Cradle action for of info tag for call back */
    public static final String EXTRA_CRADLE_GETINFO = "CRADLE_INFO";
    /** The Cradle action for of sn tag for call back */
    public static final String EXTRA_CRADLE_GETSN = "CRADLE_SN";
    /** The Cradle action for of logpath tag for call back */
    public static final String EXTRA_CRADLE_GETLOG_PATH = "LOG_PATH";
    /** The Cradle action for of status tag for call back */
    public static final String EXTRA_CRADLE_STATUS = "STATUS";

    /** The cradle action return success. */
    public final static byte STRUCT_ERROR_MESSAGE_SUCESS = 0x00;
    /** The cradle action return receive error*/
    public final static byte STRUCT_ERROR_MESSAGE_RECEIVE_ERROR = 0x01;
    /** The cradle action return receive error*/
    public final static byte STRUCT_ERROR_MESSAGE_MD5_INVALID = 0x02;
    /** The cradle action return fileformat error*/
    public final static byte STRUCT_ERROR_MESSAGE_FILEFORMAT_INVALID = 0x03;
    /** The cradle action return storage lack error*/
    public final static byte STRUCT_ERROR_MESSAGE_STORAGE_LACK = 0x04;
    /** The cradle action return system busy error*/
    public final static byte STRUCT_ERROR_MESSAGE_SYSTEMBUSY = 0x05;
    /** The cradle action return system error error*/
    public final static byte STRUCT_ERROR_MESSAGE_SYSTEMERROR = 0x06;
    /** The cradle action return response error error*/
    public final static byte STRUCT_ERROR_RESPONSE_FORMAT = 0x07;
    /** The cradle action return unknown error error*/
    public final static byte STRUCT_ERROR_MESSAGE_UNKNOWNERROR = 0x08;
    /** The cradle action return cradle connect fail error*/
    public final static byte STRUCT_ERROR_MESSAGE_CONNECTEDFAIL = 0x09;

    // diagnostic-device
    public static final String EXTRA_DIAG_DEVICE     = "DEVICE";
    public static final String EXTRA_DIAG_STATUS     = "STATUS";
    public static final String EXTRA_DIAG_DATA       = "DATA";
    public static final String EXTRA_DIAG_TIMESTAMP  = "TIMESTAMP";
    // diagnostic-api
    public static final String EXTRA_DIAG_TYPE       = "TYPE";
    public static final String EXTRA_DIAG_ERR        = "ERR";
    //diagnostic-notify priority
    public static final String EXTRA_DIAG_PRIORITY   = "PRIORITY";
    //
    public static final int DIAG_DEVICE_EPP          = 0;
    public static final int DIAG_DEVICE_PRINTER      = 1;
    //
    public static final int DIAG_STATUS_OK           = 0;
    public static final int DIAG_STATUS_ERROR        = -1;
    //
    public static final int DIAG_TYPE_COMM           = 0;
    //
    public static final int DIAG_PRIORITY_LOW        = 1;
    public static final int DIAG_PRIORITY_HIGH       = 10;
    public static final int DIAG_PRIORITY_SILENT     = 20;

    // Set USB device operation mode to ADB/VENUS/CDC
    public static final String USBDEV_ADB            = "adb";
    public static final String USBDEV_VENUS          = "venus";
    public static final String USBDEV_CDC            = "cdc";

    // battery switch mechanism
    public static final int SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE             = 1;
    public static final int SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE              = 2;
    public static final int SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE = 3;

    //---- private ---------------------------------------------------------------------------------
    // reboot
    private static final String REBOOT_DEVICE   = "SaioService.REBOOT_DEVICE";
    private static final String REBOOT_REASON   = "reboot_reason";

    // shutdown
    private static final String SHUTDOWN_DEVICE     = "SaioService.SHUTDOWN_DEVICE";
    private static final String SHUTDOWN_CONFIRM    = "shutdown_confirm";

    // pinentry
    private static final String SET_PINENTRY    = "SaioService.SET_PINENTRY";
    private static final String PINENTRY_EN     = "pin_enable";

    private static final String START_POLLING_CTLS_LED  = "SaioService.START_POLLING_CTLS_LED";
    private static final String STOP_POLLING_CTLS_LED   = "SaioService.STOP_POLLING_CTLS_LED";

    // power management
    private static final String SET_POWER_STATUS    = "SaioService.SET_POWER_STATUS";
    private static final String POWER_STATUS        = "POWER_STATUS";
    private static final int POWER_SLEEP            = 1;
    private static final int POWER_SUSPEND          = 2;
    private static final int POWER_WAKEUP           = 3;
    private static final int POWER_USERACTIVITY     = 4;
    //
    private static final String SCREEN_OFF_TIMEOUT          = "screen_off_timeout";
    private static final String PM_SLEEP_TIMEOUT            = "pm_sleep_timeout";
    private static final String PM_SUSPEND_TIMEOUT          = "pm_suspend_timeout";
    private static final String PM_SUSPEND_ENABLE           = "pm_suspend_enable";
    private static final String PM_POWEROFF_TIMEOUT         = "pm_poweroff_timeout";
    private static final String PM_POWEROFF_ENABLE          = "pm_poweroff_enable";
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE  = 30000;

    // keep screen on
    private static final String KEEP_SCREEN_ON      = "SaioService.KEEP_SCREEN_ON";
    private static final String SCREEN_ON_STATUS    = "SCREEN_ON_STATUS";

    // (de)activate power key
    private static final String ACTION_ACTIVATE_POWERKEY    = "SaioService.ACTIVATE_POWERKEY";
    private static final String ACTION_DEACTIVATE_POWERKEY  = "SaioService.DEACTIVATE_POWERKEY";

    // usb mode
    private static final String SET_USB_MODE    = "SaioService.SET_USBDEV_MODE";
    private static final String USB_MODE        = "usb_mode";

    // input event enable/disable
    private static final String ACTION_DEVICE_KEYBOARD      = "SaioService.DEVICE_KEYBOARD";
    private static final String ACTION_DEVICE_MOUSE         = "SaioService.DEVICE_MOUSE";
    private static final String ACTION_DEVICE_TOUCH         = "SaioService.DEVICE_TOUCH";
    private static final String ACTION_DEVICE_ACTIVATE      = "SaioService.DEVICE_ACTIVATE";
    private static final String ACTION_DEACTIVATE_TIMEOUT   = "SaioService.DEACTIVATE_TIMEOUT";

    private static final String ACTION_LATINIME_ENABLE_SOUND_ON_KEYPRESS    = "SaioService.ENABLE_SOUND_ON_KEYPRESS";
    private static final String ACTION_LATINIME_DISABLE_SOUND_ON_KEYPRESS   = "SaioService.DISABLE_SOUND_ON_KEYPRESS";

    private static final String SET_NOTIFICATION_LOCK   = "SaioService.SET_NOTIFICATION_LOCKED";
    private static final String LOCK_STATE              = "lock_state";

    private static final String AUTHORITY = "com.xac.saioservice.sharedprovider.authority";

    // battery prolong
    private static final String ACTION_SET_CHARGING_THRESHOLD = "SaioService.SET_CHARGING_THRESHOLD";
    private static final String CHARGING_THRESHOLD_ENABLE     = "charging_threshold_enable";
    private static final String CHARGING_THRESHOLD_VALUE      = "charging_threshold_value";

    // battery switch mechanism
    private static final String ACTION_SET_CPUFREQ_BY_BATTERY_TEMPERATURE = "SaioService.SET_CPUFREQ_BY_BATTERY_TEMPERATURE";
    private static final String SET_CPUFREQ_BY_BATTERY_TEMPERATURE_ENABLE = "set_cpufreq_by_battery_temperature_enable";
    private static final String ACTION_SET_CHARGING_SWITCH_MECHANISM      = "SaioService.SET_CHARGING_SWITCH_MECHANISM";
    private static final String SET_CHARGING_SWITCH_TYPE                  = "set_charging_switch_type";
    private static final String SET_CHARGING_SWITCH_ENABLE                = "set_charging_switch_enable";

    // Switch USB device mode and host mode
    private static final String ACTION_SET_USB_MODE   = "SaioService.SET_USB_MODE";
    private static final String SWITCH_USB_MODE_VALUE = "switch_usb_mode_value";

    // Set USB device operation mode to ADB/VENUS/CDC
    private static final String ACTION_SET_USB_MODE_CONFIG   = "SaioService.SET_USB_MODE_CONFIG";
    private static final String USB_MODE_CONFIG_VALUE        = "usb_mode_config_value";

    // T3
    private static final String SET_2ND_BRIGHTNESS  = "BLService.SET_2ND_BRIGHTNESS";
    private static final String GET_2ND_BRIGHTNESS  = "BLService.GET_2ND_BRIGHTNESS";
    private static final String RET_2ND_BRIGHTNESS  = "BLService.RETURN_2ND_2ND_BRIGHTNESS";
    private static final String BRIGHTNESS_LEVEL    = "brightness_level";
    //
    private static final String GET_2ND_TOUCHID = "SaioService.GET_2ND_TOUCHID";
    private static final String RET_2ND_TOUCHID = "SaioService.RETURN_2ND_TOUCHID";
    private static final String SECOND_TOUCHID  = "2nd_touch_id";
    //
    private static final String ACTION_ACTIVATE_2ND_TOUCH   = "SaioService.ACTIVATE_2ND_TOUCH";
    private static final String ACTION_DEACTIVATE_2ND_TOUCH = "SaioService.DEACTIVATE_2ND_TOUCH";

    // ACM - A3/E200CP
    private static final String SET_LED     = "SaioService.SET_LED";
    private static final String LED_ID      = "led_id";
    private static final String LED_VALUE   = "led_value";

    // cradle
    private static final String ACTION_CRADLE_API_REBOOT                    = "com.xac.cradle.api.reboot";
    private static final String ACTION_CRADLE_API_FACTORY_RESET             = "com.xac.cradle.api.reset";
    private static final String ACTION_CRADLE_API_GET_DEVICE_INFO           = "com.xac.cradle.api.getInfo";
    private static final String ACTION_CRADLE_API_GET_DEVICE_LOG            = "com.xac.cradle.api.getLog";
    private static final String ACTION_CRADLE_API_UPDATE_CRADLE_OS          = "com.xac.cradle.api.osUpdate";
    private static final String ACTION_CRADLE_API_UPDATE_CRADLE_PRINTER_FW  = "com.xac.cradle.api.fwUpdate";
    private static final String ACTION_CRADLE_API_BATCH_CONFIG              = "com.xac.cradle.api.config";
    private static final String ACTION_CRADLE_API_OPEN_CASHDRAWER_CONFIG    = "com.xac.cradle.api.openCashDrawer";
    private static final String ACTION_CRADLE_API_QUERY_CASHDRAWER_CONFIG   = "com.xac.cradle.api.queryCashDrawer";
    private static final String ACTION_CRADLE_API_CLOSE_CASHDRAWER_CONFIG   = "com.xac.cradle.api.closeCashDrawer";
    private static final String ACTION_UPDATE_APP_USAGE_PERMISSION          = "SaioService.UPATE_APP_USAGE_MODE";
    //
    private static final String CRADLE_REBOOT_DELAY_VALUE   = "Delay_time";
    private static final String CRADLE_STORE_LOG_PATH_TAG   = "Save_Log_uri";
    private static final String CRADLE_OS_IMAGE_URI_TAG     = "OS_Image_uri";
    private static final String CRADLE_FW_IMAGE_URI_TAG     = "FW_Image_uri";
    private static final String CRADLE_CONFIG_URI_TAG       = "Config_file_uri";

    // Change RTC
    private static final String ACTION_SET_SYSTEM_DATE_TIME   = "SaioService.SET_SYSTEM_DATE_TIME";
    private static final String SYSTEM_DATE_TIME_MILLISECONDS = "system_date_time_milliseconds";

    // Set Maxim Power VDD
    private static final String SET_MAXIM_POWER_VDD = "SaioService.SET_MAXIM_POWER_VDD";
    private static final String POWER_VDD_HIGH = "POWER_VDD_HIGH";
    
    private static final String ACTION_SET_TCP_SYN_RETRIES = "SaioService.SET_TCP_SYN_RETRIES";
    private static final String EXTRA_TCP_SYN_RETRIES      = "extra_tcp_syn_retries";

    //aiming switch
    private final String ACTION_ENABLE_AIMER = "SaioService.ACTION_ENABLE_AIMER";
    private final String EXTRA_ENABLE_AIMER = "extra_enable_aimer";

    //flash switch
    private final String ACTION_ENABLE_FLASH = "SaioService.ACTION_ENABLE_FLASH";
    private final String EXTRA_ENABLE_FLASH = "extra_enable_flash";

    //set aiming
    private final String ACTION_SET_AIMER = "SaioService.ACTION_SET_AIMER";
    private final String EXTRA_AIMER_DUTYCYCLE = "extra_aimer_dutyCycle";
    private final String EXTRA_AIMER_PULLHIGH = "extra_aimer_pullHigh";

    //start aimer service
    private final String ACTION_START_BCRSERVICE = "SaioService.ACTION_START_BCRSERVICE";

    //---- members ---------------------------------------------------------------------------------
    private Context mContext;
    private boolean mHasFlagKeepScreenOn = false;
    private boolean mHasDisablePowerkey = false;
    private OnSaioListener mOnSaioListener;
    private BlLevelReceiver mBlLevelReceiver;
    private OnCradleStatusListener mOnCradleStatusListener;
    private CradleStatusReceiver mCradleStatusReceiver;

    /**
     * SaioService constructor.
     *
     * @param context App context.
     * @param onSaioListener listener to get data from SaioService
     */
    public SaioService(Context context, OnSaioListener onSaioListener){
        mContext = context;
        mOnSaioListener = onSaioListener;

        mBlLevelReceiver = new BlLevelReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(RET_2ND_BRIGHTNESS);
        filter.addAction(RET_2ND_TOUCHID);
        mContext.registerReceiver(mBlLevelReceiver, filter);
    }

    public SaioService(Context context){
        mContext = context;
    }

    /**
     * The method will register OnCradleListener
     *
     * @param OnCradleStatusListener listener to get data from cradle
     */
    public void setOnCradleListener(OnCradleStatusListener onCradleStatusListener){
        mOnCradleStatusListener = onCradleStatusListener;
        mCradleStatusReceiver = new CradleStatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CRADLE_API_CALLBACK_RESPONSE);
        mContext.registerReceiver(mCradleStatusReceiver, filter);
    }

    /**
     * The method will unregister OnSaioListener.
     *
     */
    public void release(){
        if(mBlLevelReceiver != null) {
            mContext.unregisterReceiver(mBlLevelReceiver);
            mBlLevelReceiver = null;
        }
        if(mCradleStatusReceiver != null){
            mContext.unregisterReceiver(mCradleStatusReceiver);
            mCradleStatusReceiver = null;
        }
        if(mContext != null) {
            mContext = null;
        }
    }

    /**
     * The method can be used to set the brightness of the 2nd display.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     *
     * @param brightness The brightness of the 2nd display (0~1.0).
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public int set2ndDispBrightness(float brightness){
        DisplayManager displayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if(display.length == 0){
            return ERR_2ND_BRIGHTNESS_NO_SUCH_DEVICE;
        }
        if((brightness < 0)||(brightness > 1.0))
            return ERR_2ND_BRIGHTNESS_INVALID_PARAM;
        Intent blIntent = new Intent();
        blIntent.setAction(SET_2ND_BRIGHTNESS);
        blIntent.putExtra(BRIGHTNESS_LEVEL, brightness);
        mContext.sendBroadcast(blIntent);
        return 0;
    }

    /**
     * The method send request to get brightness of the 2nd display, will return by OnSaioListener.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     *
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public int get2ndDispBrightness(){
        DisplayManager displayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if(display.length == 0){
            return ERR_2ND_BRIGHTNESS_NO_SUCH_DEVICE;
        }
        Intent blIntent = new Intent();
        blIntent.setAction(GET_2ND_BRIGHTNESS);
        mContext.sendBroadcast(blIntent);
        return 0;
    }

    /**
     * The method will return secondary touch id.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     *
     * @return device id if there is no error else nonzero error code defined in class constants.
     */
    public int get2ndTouchDeviceId(){
        DisplayManager displayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if(display.length == 0){
            return ERR_2ND_TOUCH_NO_SUCH_DEVICE;
        }
        Intent tpIntent = new Intent();
        tpIntent.setAction(GET_2ND_TOUCHID);
        mContext.sendBroadcast(tpIntent);
        return 0;
    }

    /**
     * Activate the secondary touch.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     */
    public void activate2ndTouch()
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_ACTIVATE_2ND_TOUCH);
        mContext.sendBroadcast(intent);
    }

    /**
     * Deactivate the secondary touch.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     */
    public void deactivate2ndTouch()
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_DEACTIVATE_2ND_TOUCH);
        mContext.sendBroadcast(intent);
    }

    /**
     * Activate or deactivate the secondary touch if it's available.
     * <p>
     *     Note: only for E200I, T3
     * </p>
     *
     * @param on set true to activate; false to deactivate
     */
    private static void set2ndTouchActive(boolean on)
    {
        native_set2ndTouchActive((on)?1:0);
    }

    /**
     * Call this method to reboot device.
     *
     * @param reason code to pass to the kernel (e.g., "recovery") to request special boot modes, or null.
     * @return device id if there is no error else nonzero error code defined in class constants.
     */
    public int reboot(String reason){
        Intent rebootIntent = new Intent();
        rebootIntent.setAction(REBOOT_DEVICE);
        rebootIntent.putExtra(REBOOT_REASON, reason);
        mContext.sendBroadcast(rebootIntent);
        return 0;
    }

    /**
     * Shuts down the device.
     *
     * @param confirm If true, shows a shutdown confirmation dialog.
     */
    public void shutdown(boolean confirm){
        Intent shutdownIntent = new Intent();
        shutdownIntent.setAction(SHUTDOWN_DEVICE);
        shutdownIntent.putExtra(SHUTDOWN_CONFIRM, confirm);
        mContext.sendBroadcast(shutdownIntent);
    }

    /**
     * Switch antenna between internal(default) or external.
     * <p>
     *     Note: only for T3
     * </p>
     *
     * @param dist {@link #ANTENNA_INTERNAL} or {@link #ANTENNA_EXTERNAL} only
     */
    public static void switchAntenna(int dist){
        if((dist != ANTENNA_INTERNAL)&&(dist != ANTENNA_EXTERNAL))
            return;
        native_switchAntenna(dist);
    }

    /**
     * Call this method to activate/deactivate Epp.
     * <p>
     *     Note: only for 200NP
     * </p>
     * @param enabled activate or deactive Epp.
     */
    public static int setEppEnabled(boolean enabled){
        return native_setEppEnabled(enabled);
    }

    /**
     * Call this method to activate/deactivate PA.
     * <p>
     *     Note: only for AW
     * </p>
     * @param enabled activate or deactive PA.
     */
    public static int setPAEnabled(boolean enabled){
        return native_setPAEnabled(enabled);
    }
    
    /**
     * Call this method to set USB full/high speed.
     * <p>
     *     Note: only for imx6 4.4.2
     * </p>
     * @param mode USB full or high speed.
     */
    public static void setUsbSpeed(int mode){
        //int ret = 
		native_setUsbSpeed(mode);
        //if (ret != 0)
        //    return USB_NOT_SUPPORT;
        //return ret;
    }
    
    /**
     * Call this method to get USB full/high speed.
     * <p>
     *     Note: only for imx6 4.4.2
     * </p>
     */
    public static int getUsbSpeed(){
        int mode = native_getUsbSpeed();
        if ((mode != USB_FULL_SPEED)&&(mode != USB_HIGH_SPEED))
            return USB_NOT_SUPPORT;
        return mode;
    }


    /**
     * Power on Maxim.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void powerOnMaxim(int dist){
        native_powerOnMaxim(dist);
    }

    /**
     * Reset Maxim.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void resetMaxim(int dist){
        native_resetMaxim(dist);
    }

    /**
     * Power On GPS.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void powerOnGPS(int dist){
        native_powerOnGPS(dist);
    }

    /**
     * Reset GPS.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void resetGPS(int dist){
        native_resetGPS(dist);
    }

    /**
     * Heater Enable.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void setHeaterEnable(int dist){
        native_setHeaterEnable(dist);
    }

    /**
     * Power On P95.
     * <p>
     *     Note: only for Allwinner
     * </p>
     */
    public static void powerOnP95(int dist){
        native_powerOnP95(dist);
    }

    /**
     * System APP use only!!! Power on/off usb hub. It allows to power off usb hub when the only xac peripheral device connected or no device connected.
     * @param onoff set true to power on usb hub; false to power off usb hub.
     * @return zero if there are no errors.
     * <p>
     *     Note: only supported on SC20 platform.
     * </p>
     */
    public static int powerOnUsbHub(boolean onoff){

        return native_powerOnUsbHub((onoff)?1:0);
    }

    /**
     * Power on 3G module manually.
     * <p>
     *     Note: only for T3
     * </p>
     */
    public static void powerOn3Gmodule(){
        native_powerOn3Gmodule();
    }

    /**
     * Reset 3G module manually.
     */
    public static void reset3Gmodule(){
        native_reset3Gmodule();
    }

    /**
     * The method allow user to control the on-board LED on or off.
     * <p>
     *     Note: only for A3
     * </p>
     *
     * @param led_id The LED id.
     * @param enabled Indicates to turn on (true) or off (false) the LED
     */
    public void setLed(int led_id, boolean enabled){
        if(enabled)
            setLedValue(led_id,255);
        else
            setLedValue(led_id,0);
    }

    /**
     * The method allow user to adjust the on-board led brightness.
     * <p>
     *     Note: only for A3
     * </p>
     *
     * @param led_id The led id.
     * @param value Indicates LED brightness value and must be an integer between 0 and 255.
     *          [Note]: LED_SCR_SLOT/LED_KEY_PAD/LED_MSR_SLOT_E200CP are NOT able to adjust the brightness. The brightness value of these led_id should be 0 or 1.
     *          If the value greater than or equal to 1,that means to set the led_id ON.
     */
    public void setLedValue(int led_id, int value){
        Intent ledIntent = new Intent();
        ledIntent.setAction(SET_LED);
        ledIntent.putExtra(LED_ID, led_id);
        ledIntent.putExtra(LED_VALUE, value);
        mContext.sendBroadcast(ledIntent);
    }

    /**
     * The method enables or disables the PIN entry mode.
     * <p>
     *     Note: iMX6 platform is not supported. 
     * </p>
     *
     * @param enabled Indicates to enable (true) or disable (false) the PIN entry mode
     */
    public void setPinEntryModeEnabled(boolean enabled){
        if(enabled){
            // Check to see if FLAG_KEEP_SCREEN_ON is already set.  If not, set it
            int flags = ((Activity)mContext).getWindow().getAttributes().flags;
            if ((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                ((Activity)mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else {
                mHasFlagKeepScreenOn = true;
            }
            //mHasDisablePowerkey = getPowerkeyEnabled();
            //setPowerkeyEnabled(!enabled);
        }else{
            if (!mHasFlagKeepScreenOn) {
                ((Activity)mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            //setPowerkeyEnabled(mHasDisablePowerkey);
        }
        Intent pinIntent = new Intent();
        pinIntent.setAction(SET_PINENTRY);
        pinIntent.putExtra(PINENTRY_EN, enabled);
        mContext.sendBroadcast(pinIntent);
        keepScreenOn(enabled);
    }

    /**
     * The method start polling ctls leds status. Use this method,then recevie intent "SaioService.CHANGE_CTLSLed" and extra Int objects with names "ctls_led" to get ctls leds status value.
     * The value is Ctls 4 led lights correspond to a 4-bit number. Display the decimal value in 4 bit binary format using 4 leds where a 1 is represented as a lit led. The lowest bit corresponds to the first led on the left.
     * ex: 3(Decimal)-> 0011(Binary). If the obtained ctls leds status value is 3, the first and second leds on the left are lit.
     */
    public void startPollingCtlsLeds(){
        Intent StartIntent = new Intent();
        StartIntent.setAction(START_POLLING_CTLS_LED);
        mContext.sendBroadcast(StartIntent);
    }

    /**
     * The method stop polling ctls leds status.
     */
    public void stopPollingCtlsLeds(){
        Intent StopIntent = new Intent();
        StopIntent.setAction(STOP_POLLING_CTLS_LED);
        mContext.sendBroadcast(StopIntent);
    }

     /**
     * Set Maxim power vdd to high or low level.
     *
     * @param high If true, set Maxim power vdd to high level and pull the reset pin of Maxim high.
                   If false, set Maxim power vdd to low level and pull the reset pin of Maxim low.
     *
     */
    public void setMaximPowerVdd (boolean high){
        Intent setMaximPowerVddIntent = new Intent();
        setMaximPowerVddIntent.setAction(SET_MAXIM_POWER_VDD);
        setMaximPowerVddIntent.putExtra(POWER_VDD_HIGH, high);
        mContext.sendBroadcast(setMaximPowerVddIntent);
    }

    /**
     * The method will return product name
     *
     * @return product name
     *
     */
    public static String getSystemInfo(){
        Misc misc = new Misc();
        byte[] info = new byte[20];
        misc.getSystemInfo(Misc.INFO_PRODUCT, info);
        int len = info.length;
        for(int i=0; i<info.length; i++){
            if(info[i] == 0){
                len = i;
                break;
            }
        }
        String prodInfo = new String(info);
        prodInfo = prodInfo.substring(0, len);
        return prodInfo;
    }

    /**
     * For SaioService to check whether Suspend is available
     */
    private static boolean isSuspendAvailable() {
        String prodInfo = getSystemInfo();
        if (prodInfo.contains("AP-10") || prodInfo.contains("T305") || prodInfo.contains("SUD12") || prodInfo.contains("SUD7")) {
            Log.d(TAG, "AP-10/T305/SUD12/SUD7 don't support Suspend function");
            return false;
        } else {
            return true;
        }
    }

    /**
     * For SaioService to check whether PowerOff is available
     */
    private static boolean isPowerOffAvailable() {
        String prodInfo = getSystemInfo();
        if (prodInfo.contains("AP-10") || prodInfo.contains("T305") || prodInfo.contains("SUD12") || prodInfo.contains("SUD7")) {
            Log.d(TAG, "AP-10/T305/SUD12/SUD7 don't support PowerOff function");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Call this method to activate/deactivate power key.
     *
     * @param enabled activate or deactive power key.
     */
    public void setPowerkeyEnabled(boolean enabled){
        Intent intent = new Intent();
        if(enabled)
            intent.setAction(ACTION_ACTIVATE_POWERKEY);
        else
            intent.setAction(ACTION_DEACTIVATE_POWERKEY);
        mContext.sendBroadcast(intent);
    }

    /**
     * Call this method to activate/deactivate keyboard, mouse, touch.
     *
     * @param dev set keyboard, mouse or touch device.
     * @param enabled activate or deactive keyboard, mouse or touch.
     * @param timeout milliseconds. It is valid when enabled is false.
     */
    public void setInputEventEnabled(int dev, boolean enabled, int timeout){
        Intent intent = new Intent();
        switch (dev) {
            case DEV_KEYBOARD:
                intent.setAction(ACTION_DEVICE_KEYBOARD)
                    .putExtra(ACTION_DEVICE_ACTIVATE, enabled)
                    .putExtra(ACTION_DEACTIVATE_TIMEOUT, timeout);
                break;
            case DEV_MOUSE:
                intent.setAction(ACTION_DEVICE_MOUSE)
                    .putExtra(ACTION_DEVICE_ACTIVATE, enabled)
                    .putExtra(ACTION_DEACTIVATE_TIMEOUT, timeout);
                break;
            case DEV_TOUCH:
                intent.setAction(ACTION_DEVICE_TOUCH)
                    .putExtra(ACTION_DEVICE_ACTIVATE, enabled)
                    .putExtra(ACTION_DEACTIVATE_TIMEOUT, timeout);
                break;
        }   
        mContext.sendBroadcast(intent);
    }

    /**
     * The method will return current power key enable
     *
     * @return device power key enable.
     */
    public boolean getPowerkeyEnabled(){
        Method get = null;
        String value = "false";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.powerkey.enabled", "true"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value.equals("true");
    }

    /**
     * Call this method to set enable/disable LatinIME sound on keypress.
     *
     * @param enabled enable or disable LatinIME sound on keypress settings.
     */
    public void setLatinIMESoundOnKeypressEnabled(boolean enabled){
        Intent intent = new Intent();
        if(enabled)
            intent.setAction(ACTION_LATINIME_ENABLE_SOUND_ON_KEYPRESS);
        else
            intent.setAction(ACTION_LATINIME_DISABLE_SOUND_ON_KEYPRESS);
        mContext.sendBroadcast(intent);
    }

    /**
     * The method will return auto reboot status
     *
     * @return device auto reboot enabled or disabled.
     */
    public boolean isRebootEnabled(){
        Method get = null;
        boolean enabled = true;
        String value = "true";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.reboot.enable", "true"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value.equals("true");
    }

    /**
     * The method will return current reboot time
     *
     * @return device reboot time.
     */
    public String getRebootTime(){
        Method get = null;
        String value = "2:0";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.reboot.time", "2:0"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * The method will set reboot time
     *
     * @return parameter valid or not.
     */
    public boolean setRebootTime(int hour, int minute){
        boolean finalStatus = false;
        if(((hour >= 0)&&(hour <= 23))&&((minute >= 0)&&(minute <= 59))) {
            Intent intent = new Intent();
            intent.setAction(SET_REBOOT_TIME);
            intent.putExtra(REBOOT_HOUR, hour);
            intent.putExtra(REBOOT_MINUTE, minute);
            mContext.sendBroadcast(intent);
            finalStatus = true;
        }else if((hour == 99)&&(minute == 99)){
            byte[] tmpInfo = new byte[20];
            Misc misc = new Misc();
            int errno = misc.getSystemInfo(Misc.INFO_SECURE_STAT, tmpInfo);
            String tmp = new String(tmpInfo);
            if(0 == errno){
                if(tmp.substring(0, tmp.indexOf(0)).equals("Disabled")){
                    Intent intent = new Intent();
                    intent.setAction(SET_REBOOT_TIME);
                    intent.putExtra(REBOOT_HOUR, hour);
                    intent.putExtra(REBOOT_MINUTE, minute);
                    mContext.sendBroadcast(intent);
                    finalStatus = true;
                }else
                    finalStatus = false;
            }else
                finalStatus = false;
        }
        return finalStatus;
    }

    /**
     * The method will get sleep time
     *
     * @return sleep time milliseconds.
     */
    public int getSleepTime(){
        int time = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        return time;
    }

    /**
     * The method will set sleep time
     *
     * @param time the value is PM_SLEEP_TIME_15_SEC, PM_SLEEP_TIME_30_SEC,
     *       PM_SLEEP_TIME_1_MIN, PM_SLEEP_TIME_2_MIN, PM_SLEEP_TIME_5_MIN,
     *       PM_SLEEP_TIME_10_MIN, PM_SLEEP_TIME_30_MIN, PM_SLEEP_TIME_NEVER
     * @return true if success, else false indicates to passed parameter is invalid
     */
    public boolean setSleepTime(int time){
        String prodInfo = getSystemInfo();
        if ( (time==PM_SLEEP_TIME_15_SEC) ||
                (time==PM_SLEEP_TIME_30_SEC) ||
                (time==PM_SLEEP_TIME_1_MIN)  ||
                (time==PM_SLEEP_TIME_2_MIN)  ||
                (time==PM_SLEEP_TIME_5_MIN)  ||
                (time==PM_SLEEP_TIME_10_MIN) ||
                (time==PM_SLEEP_TIME_30_MIN) ||
                ((time==PM_SLEEP_TIME_NEVER) && (prodInfo.contains("T305") || prodInfo.contains("SUD12") || prodInfo.contains("SUD7"))) ) {
            Uri CONTENT_PREFERENCE_URI = Uri.parse("content://" + AUTHORITY + "/shared");
            ContentValues contentValues = new ContentValues();
            contentValues.put(PM_SLEEP_TIMEOUT, time);
            mContext.getContentResolver().insert(CONTENT_PREFERENCE_URI, contentValues);
            return true;
        } else {
            return false;
        }
    }

    /**
     * The method will return current suspend mode
     *
     * @return true if suspend mode is enabled, else suspend mode is disabled
     */
    public boolean isSuspendEnabled(){
        Method get = null;
        String value = "false";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.suspend.enable", "false"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value.equals("true");
    }

    /**
     * The method will return current suspend time
     *
     * @return suspend time milliseconds.
     */
    public int getSuspendTime(){
        Method get = null;
        String value = "0";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.suspend.timeout", "0"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return Integer.parseInt(value);
    }

    /**
     * The method will enable/disable suspend mode
     *
     * @return true if success, else false indicates to fail
     */
    public boolean setSuspendEnabled(boolean enable){
        if (isSuspendAvailable()) {
            Uri CONTENT_PREFERENCE_URI = Uri.parse("content://" + AUTHORITY + "/shared");
            ContentValues contentValues = new ContentValues();
            contentValues.put(PM_SUSPEND_ENABLE, enable);
            mContext.getContentResolver().insert(CONTENT_PREFERENCE_URI, contentValues);
            return true;
        } else {
            return false;
        }
    }

    /**
     * The method will set suspend time
     *
     * @param time the value is PM_SUSPEND_TIME_30_SEC, PM_SUSPEND_TIME_1_MIN,
     *       PM_SUSPEND_TIME_2_MIN, PM_SUSPEND_TIME_5_MIN, PM_SUSPEND_TIME_10_MIN,
     *       PM_SUSPEND_TIME_30_MIN, PM_SUSPEND_TIME_60_MIN
     * @return true if success, else false indicates to passed parameter is invalid
     */
    public boolean setSuspendTime(int time){
        if (isSuspendAvailable()) {
            if ( (time==PM_SUSPEND_TIME_30_SEC) ||
                 (time==PM_SUSPEND_TIME_1_MIN)  ||
                 (time==PM_SUSPEND_TIME_2_MIN)  ||
                 (time==PM_SUSPEND_TIME_5_MIN)  ||
                 (time==PM_SUSPEND_TIME_10_MIN) ||
                 (time==PM_SUSPEND_TIME_30_MIN) ||
                 (time==PM_SUSPEND_TIME_60_MIN) ) {
                Uri CONTENT_PREFERENCE_URI = Uri.parse("content://" + AUTHORITY + "/shared");
                ContentValues contentValues = new ContentValues();
                contentValues.put(PM_SUSPEND_TIMEOUT, time);
                mContext.getContentResolver().insert(CONTENT_PREFERENCE_URI, contentValues);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * The method will return a boolean value indicates whether the device is suspended.
     *
     * @return true if device is suspended, else false indicates the device is awake.
     */
    public boolean isSuspended(){
        Method get = null;
        String value = "false";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.suspend.state", "false"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value.equals("true");
    }

    /**
     * The method will return current poweroff mode
     *
     * @return true if poweroff mode is enabled, else poweroff mode is disabled
     */
    public boolean isPowerOffEnabled(){
        Method get = null;
        String value = "false";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.poweroff.enable", "false"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value.equals("true");
    }

    /**
     * The method will return current poweroff time
     *
     * @return poweroff time hours.
     */
    public int getPowerOffTime(){
        Method get = null;
        String value = "0";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.poweroff.timeout", "0"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return Integer.parseInt(value);
    }

    /**
     * The method will enable/disable poweroff mode
     *
     * @return true if success, else false indicates to fail
     */
    public boolean setPowerOffEnabled(boolean enable){
        if (isPowerOffAvailable()) {
            Uri CONTENT_PREFERENCE_URI = Uri.parse("content://" + AUTHORITY + "/shared");
            ContentValues contentValues = new ContentValues();
            contentValues.put(PM_POWEROFF_ENABLE, enable);
            mContext.getContentResolver().insert(CONTENT_PREFERENCE_URI, contentValues);
            return true;
        } else {
            return false;
        }
    }

    /**
     * The method will set poweroff time
     *
     * @param time the value is beteween PM_POWEROFF_TIME_MIN_HOUR to PM_POWEROFF_TIME_MAX_HOUR
     * @return true if success, else false indicates to passed parameter is invalid
     */
    public boolean setPowerOffTime(int time){
        if (isPowerOffAvailable()) {
            if  ((time>=PM_POWEROFF_TIME_MIN_HOUR) && (time <=PM_POWEROFF_TIME_MAX_HOUR)) {
                Uri CONTENT_PREFERENCE_URI = Uri.parse("content://" + AUTHORITY + "/shared");
                ContentValues contentValues = new ContentValues();
                contentValues.put(PM_POWEROFF_TIMEOUT, time);
                mContext.getContentResolver().insert(CONTENT_PREFERENCE_URI, contentValues);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * The method let system go to sleep.
     */
    public void goToSleep(){
        Intent intent = new Intent();
        intent.setAction(SET_POWER_STATUS);
        intent.putExtra(POWER_STATUS, POWER_SLEEP);
        mContext.sendBroadcast(intent);
    }

    /**
     * The method let system go to suspend.
     */
    public void goToSuspend(){
        if (isSuspendAvailable()) {
            Intent intent = new Intent();
            intent.setAction(SET_POWER_STATUS);
            intent.putExtra(POWER_STATUS, POWER_SUSPEND);
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * The method let system wakeup.
     */
    public void wakeUp(){
        Intent intent = new Intent();
        intent.setAction(SET_POWER_STATUS);
        intent.putExtra(POWER_STATUS, POWER_WAKEUP);
        mContext.sendBroadcast(intent);
    }

    /**
     * Manually report user activity to keep the device awake
     */
    public void userActivity(){
        Intent intent = new Intent();
        intent.setAction(SET_POWER_STATUS);
        intent.putExtra(POWER_STATUS, POWER_USERACTIVITY);
        mContext.sendBroadcast(intent);
    }

    /**
     * The method enables or disables keep screen on.
     * @param enable Indicates to enable (true) or disable (false) keep screen on
     */
    public void keepScreenOn(boolean enable){
        Intent intent = new Intent();
        intent.setAction(KEEP_SCREEN_ON);
        intent.putExtra(SCREEN_ON_STATUS, enable);
        mContext.sendBroadcast(intent);
    }

    /**
     * The method enables or disables usb device mtp. It only works when device is at Android mode.
     * (Computer might take a moment to identify the device.)
     *
     * @param enable Indicates to enable (true) or disable (false) usb device mtp mode.
     *
     * @return zero for sending request to set usb device mtp mode successfully else error code defined in class constants.
     */
    public int setMtpEnabled(boolean enable){
        Misc misc = new Misc();
        int status = misc.usbDeviceMode(Misc.USBDEV_MODE_QUERY);
        if (status == Misc.USBDEV_ANDROID) {
            Intent ledIntent = new Intent();
            ledIntent.setAction(SET_USB_MODE);
            if (enable) {
                ledIntent.putExtra(USB_MODE, "MTP");
            } else {
                ledIntent.putExtra(USB_MODE, "NONE");
            }
            mContext.sendBroadcast(ledIntent);
            return 0;
        } else if (status == Misc.USBDEV_DISABLE) {
            return ERR_USBDEV_MODE_DISABLE;
        } else if (status == Misc.USBDEV_SERIAL_CDC) {
            return ERR_USBDEV_MODE_CDC;
        } else {
            return ERR_USBDEV_MODE_UNKNOWN;
        }
    }

    /**
     * The method gets a value indicating the enabled or disabled status of usb device mtp mode.
     * (Switching USB debugging on/off will affect the return value. Please do not change USB debugging state before using this function.)
     *
     * @return zero if mtp mode is disabled, one if mtp mode is enabled else error code defined in class constants.
     */
    public int isMtpEnabled(){
        Misc misc = new Misc();
        int status = misc.usbDeviceMode(Misc.USBDEV_MODE_QUERY);
        if (status == Misc.USBDEV_ANDROID) {
            Method get = null;
            String unlock_value = "0";
            String usb_state = "";
            try {
                if (null == get) {
                    Class<?> cls = Class.forName("android.os.SystemProperties");
                    get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
                }
                unlock_value = (String) (get.invoke(null, new Object[]{"persist.sys.usb.data.unlock", "0"}));
                usb_state = (String) (get.invoke(null, new Object[]{"sys.usb.state", ""}));
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (unlock_value.equals("1") && usb_state.contains("mtp"))
                return 1;
            else
                return 0;

        } else if (status == Misc.USBDEV_DISABLE) {
            return ERR_USBDEV_MODE_DISABLE;
        } else if (status == Misc.USBDEV_SERIAL_CDC) {
            return ERR_USBDEV_MODE_CDC;
        } else {
            return ERR_USBDEV_MODE_UNKNOWN;
        }
    }

    /**
     * The method locks or unlocks notifications.
     * @param locked Indicates to lock (true) or unlock (false) notifications.
     */
    public void setNotificationLocked(boolean locked){
        Intent intent = new Intent();
        intent.setAction(SET_NOTIFICATION_LOCK);
        intent.putExtra(LOCK_STATE, locked);
        mContext.sendBroadcast(intent);
    }

    private class CradleStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mOnCradleStatusListener == null){
                return;
            }
            if(intent.getAction().equals(ACTION_CRADLE_API_CALLBACK_RESPONSE)){
                int cradleAction = intent.getIntExtra(SaioService.EXTRA_CRADLE_ACTION,0);
                int status = intent.getIntExtra(SaioService.EXTRA_CRADLE_STATUS,-1);
                String info = intent.getStringExtra(SaioService.EXTRA_CRADLE_GETINFO);
                String sn = intent.getStringExtra(SaioService.EXTRA_CRADLE_GETSN);
                if(cradleAction == SaioService.ACTION_CRADLE_GETINFO){
                    mOnCradleStatusListener.getDeviceStatus(status,info,sn);
                }else if(cradleAction == SaioService.ACTION_CRADLE_GETLOG){
                    mOnCradleStatusListener.getLogStatus(status);
                }else if(cradleAction == SaioService.ACTION_CRADLE_REBOOT){
                    mOnCradleStatusListener.getRebootStatus(status);
                }else if(cradleAction == SaioService.ACTION_CRADLE_RESET){
                    mOnCradleStatusListener.getResetStatus(status);
                }else if(cradleAction == SaioService.ACTION_CRADLE_PREUPGRADLE){
                    mOnCradleStatusListener.getPreUpgradleStatus(status);
                }else if(cradleAction == SaioService.ACTION_CRADLE_TRANSFER){
                    mOnCradleStatusListener.getTransferStatus(status);
                }else if(cradleAction == SaioService.ACTION_CRADLE_UPDATE){
                    mOnCradleStatusListener.getUpdateStatus(status);
                }
            }
        }
    }
    private class BlLevelReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if(mOnSaioListener == null){
                return;
            }

            if(RET_2ND_BRIGHTNESS.equals(intent.getAction())){
                float currLevel = intent.getFloatExtra(BRIGHTNESS_LEVEL, 0);
                mOnSaioListener.onBrightness(currLevel);
            }else if(RET_2ND_TOUCHID.equals(intent.getAction())){
                int touchId = intent.getIntExtra(SECOND_TOUCHID, 0);
                mOnSaioListener.onTouchId(touchId);
            }
        }
    }

    //For Cradle
    /**
     * The method will set cradle reboot time
     *
     * @param  delay_time  after delay time time out, cradle would reboot
     */
    public void cradleReboot(int delay_time){
        Intent reboot = new Intent();
        reboot.setAction(ACTION_CRADLE_API_REBOOT);
        reboot.putExtra(CRADLE_REBOOT_DELAY_VALUE, delay_time);
        mContext.sendBroadcast(reboot);
    }

    /**
     * The method will let the cradle device exec factory reset
     *
     */
    public void cradleFactoryReset(){
        Intent reset = new Intent();
        reset.setAction(ACTION_CRADLE_API_FACTORY_RESET);
        mContext.sendBroadcast(reset);
    }

    /**
     * The method will get cradle device os and fw version
     *
     *
     */
    public void cradleGetInfo(){
        Intent getInfo = new Intent();
        getInfo.setAction(ACTION_CRADLE_API_GET_DEVICE_INFO);
        mContext.sendBroadcast(getInfo);
    }

    /**
     * The method will get cradle device log
     *
     * @param  uri  it is the log file storage uri  that you want to save
     */
    public void cradleGetLog(String uri){
        Intent getlog = new Intent();
        getlog.setAction(ACTION_CRADLE_API_GET_DEVICE_LOG);
        getlog.putExtra(CRADLE_STORE_LOG_PATH_TAG, uri);
        mContext.sendBroadcast(getlog);
    }

    /**
     * The method will let cradle device update os by local image
     *
     * @param  uri  it is the image file uri that you want to upgrade
     */
    public void cradleOsUpdate(String uri){
        Intent osUpdate = new Intent();
        osUpdate.setAction(ACTION_CRADLE_API_UPDATE_CRADLE_OS);
        osUpdate.putExtra(CRADLE_OS_IMAGE_URI_TAG, uri);
        mContext.sendBroadcast(osUpdate);
    }

    /**
     * The method will let cradle device update firmware by local image
     *
     * @param  uri  it is the image file uri that you want to upgrade
     */
    public void cradleFwUpdate(String uri){
        Intent fwUpdate = new Intent();
        fwUpdate.setAction(ACTION_CRADLE_API_UPDATE_CRADLE_PRINTER_FW);
        fwUpdate.putExtra(CRADLE_FW_IMAGE_URI_TAG, uri);
        mContext.sendBroadcast(fwUpdate);
    }

    /**
     * The method will let cradle set config by the config file
     *
     * @param  uri  it is the file uri that configuration (need follow format)
     */
    public void cradleBatchConfig(String uri){
        Intent config = new Intent();
        config.setAction(ACTION_CRADLE_API_BATCH_CONFIG);
        config.putExtra(CRADLE_CONFIG_URI_TAG, uri);
        mContext.sendBroadcast(config);
    }

    /**
     * The method will use cradle device to open cash drawer
     */
    public void cradleOpenCashDrawer(){
        Intent open = new Intent();
        open.setAction(ACTION_CRADLE_API_OPEN_CASHDRAWER_CONFIG);
        mContext.sendBroadcast(open);
    }

    /**
     * The method will use cradle device to query cash drawer  status(open or close)
     */
    public void cradleQueryCashDrawer(){
        Intent open = new Intent();
        open.setAction(ACTION_CRADLE_API_QUERY_CASHDRAWER_CONFIG);
        mContext.sendBroadcast(open);
    }

    /**
     * The method will use cradle device to close cash drawer
     * it does not work, because cash drawer need manual close
     */
    public void cradleCloseCashDrawer(){
        Intent open = new Intent();
        open.setAction(ACTION_CRADLE_API_CLOSE_CASHDRAWER_CONFIG);
        mContext.sendBroadcast(open);
    }

    /**
     * The method will allow usage permission
     *  let the context  allow OP_GET_USAGE_STATS
     * <p>
     * Note: only for A3
     * </p>
     */
    public void allowUsagePermission(){
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_APP_USAGE_PERMISSION);
        intent.putExtra("uid", Binder.getCallingUid());
        intent.putExtra("pkgname", mContext.getPackageName());
        mContext.sendBroadcast(intent);
    }

    /**
     * The method will set maximum charging threshold status and value to prolong battery lifespan
     * <p>
     * Note: only for Allwinner AT-150 and SC20 platform
     * </p>
     * @param value Maximum charging threshold value, this range is 40 to 80
     * @param enable Charging threshold status, true means enable the prolong battery lifespan function
     * @return zero if there are no errors and set charging threshold value successfully
     */
    public int setChargingThreshold(int value, boolean enable){
        Toast toast;
        String prodInfo = getSystemInfo();

        if(!(prodInfo.contains("AT-150")||prodInfo.contains("AT100R")||prodInfo.contains("AT150R")||prodInfo.contains("AT170R")||prodInfo.contains("E200NPR"))){
            String msg = "setChargingThreshold() is not supported!!";
            toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
            toast.show();
            return -1;
        }

        //Check charging threshold value
        if(!(39<value && value<81)){
            String msg = value+" is out of the range!!";
            toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
            toast.show();
            return -1;
        }

        //Set charging threshold status and value to system property
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_CHARGING_THRESHOLD);
        intent.putExtra(CHARGING_THRESHOLD_VALUE, String.valueOf(value));
        intent.putExtra(CHARGING_THRESHOLD_ENABLE, String.valueOf(enable));
        mContext.sendBroadcast(intent);
        return 0;
    }

    /**
     * Set charging threshold value if it's available.
     * <p>
     * Note: only for Allwinner AT-150 and SC20 platform
     * </p>
     * @param value Maximum charging threshold value
     */
    private static int setChargingThreshold(int value){
        return native_setChargingThreshold(value);
    }

    /**
     * Call this method to switch USB device mode or host mode.
     * <p>
     *     Note: only for AP210S and AP220S
     * </p>
     * @param value of USB mode. The value of device mode is USB_DEVICE_MODE, host mode is USB_HOST_MODE.
     */
    public void switchUsbMode(int value){
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_USB_MODE);
        intent.putExtra(SWITCH_USB_MODE_VALUE, String.valueOf(value));
        mContext.sendBroadcast(intent);
    }

    /**
     * Set USB Mode if it's available.
     * <p>
     *     Note: only for AP210S and AP220S
     * </p>
     * @param value of USB mode. The value of device mode is USB_DEVICE_MODE, host mode is USB_HOST_MODE.
     * @return zero if the function succeeds; otherwise, an errno returned. Please refer to linux errno.h.
     */
    private static int setUsbMode(int value){
        return native_switchUsbMode(value);
    }

    /**
     * Call this method to set current USB device operation mode to adb/venus/cdc
     * @param mode It's USB device operation mode. The value of mode includes USBDEV_ADB, USBDEV_VENUS, USBDEV_CDC.
     */
    public void setUsbModeConfig(String mode){
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_USB_MODE_CONFIG);
        intent.putExtra(USB_MODE_CONFIG_VALUE, mode);
        mContext.sendBroadcast(intent);
    }

    /**
     * The method will return current USB device operation mode
     * @return Current USB device operation mode. The value of sys.config.usb
     */
    public String getUsbModeConfig(){
        Method get = null;
        String usb_mode = "adb";
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            usb_mode = (String) (get.invoke(null, new Object[]{"sys.usb.config", "adb"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return usb_mode;
    }

    /**
     * The method will set different charging switch status
     * @param type charging switch type. The types are defined as below:</br>
     *             SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE - Limit the CPU max frequency when battery temperature rises to 40&deg;C</br>
     *             SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE - Reduce the charging current and voltage when battery temperature is 40-45&deg;C</br>
     *             SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE - Maximum charging threshold value is 80% when battery temperature is 45-60&deg;C</br>
     * @param enabled charging switch status
     * @return 0 is sent intent successfully; -1 is the type not support.
     */
    public int setChargingSwitch(int type, boolean enabled){
        int errorResult = -1;
        if(type == SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE){
            Intent intent = new Intent();
            intent.setAction(ACTION_SET_CPUFREQ_BY_BATTERY_TEMPERATURE);
            if(enabled)
                intent.putExtra(SET_CPUFREQ_BY_BATTERY_TEMPERATURE_ENABLE, "on");
            else
                intent.putExtra(SET_CPUFREQ_BY_BATTERY_TEMPERATURE_ENABLE, "off");
            mContext.sendBroadcast(intent);
            return 0;
        }else if(type == SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE || type == SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE){
            Intent intent = new Intent();
            intent.setAction(ACTION_SET_CHARGING_SWITCH_MECHANISM);
            intent.putExtra(SET_CHARGING_SWITCH_TYPE, String.valueOf(type));
            intent.putExtra(SET_CHARGING_SWITCH_ENABLE, String.valueOf(enabled));
            mContext.sendBroadcast(intent);
            return 0;
        }else{
            Log.d(TAG,"getChargingSwitch, This type is not support!!!");
            return errorResult;
        }
    }

    /**
     * The method will get different charging switch status
     * @param type charging switch type. The types are defined as below:</br>
     *             SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE - Limit the CPU max frequency when battery temperature rises to 40&deg;C</br>
     *             SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE - Reduce the charging current and voltage when battery temperature is 40-45&deg;C</br>
     *             SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE - Maximum charging threshold value is 80% when battery temperature is 45-60&deg;C</br>
     * @return 0 is charging switch disabled; 1 is enabled; otherwise, an errno returned. Please refer to linux errno.h.
     */
    public int getChargingSwitch(int type){
        int chargingSwitchStatus;
        int errorResult = -1;
        if(type == SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE){
            Method get = null;
            String status = "off";
            try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            status = (String) (get.invoke(null, new Object[]{"persist.sys.batt.set.cpufreq", "off"}));
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if(status==null){
                chargingSwitchStatus = errorResult;
            }else{
                if(status.equals("off"))
                    chargingSwitchStatus = 0;
                else
                    chargingSwitchStatus = 1;
            }
            return chargingSwitchStatus;
        }else if(type == SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE || type == SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE){
            return native_getChargingSwitch(type);
        }else{
            Log.d(TAG,"getChargingSwitch, This type is not support!!!");
            return errorResult;
        }
    }

    /**
     * Set charging switch status if it's available.
     * @param type charging switch type. The types are defined as below:</br>
     *             SWITCH_LIMIT_CPUFREQ_BY_BATTERY_TEMPERATURE - Limit the CPU max frequency when battery temperature rises to 40&deg;C</br>
     *             SWITCH_REDUCE_CHARGING_CURRENT_AND_VOLTAGE - Reduce the charging current and voltage when battery temperature is 40-45&deg;C</br>
     *             SWITCH_LIMIT_CHARGING_CAPACITY_AT_SPECIFIED_TEMPERATURE - Maximum charging threshold value is 80% when battery temperature is 45-60&deg;C</br>
     * @param enabled charging switch status
     */
    private static int setChargingSwitchStatus(int type, boolean enabled){
        return native_setChargingSwitch(type, enabled);
    }

    /**
     * Call this method to set system date and time
     * @param calendar an abstract class, please refer to this <a href="https://developer.android.com/reference/java/util/Calendar" target="_blank">Link</a>.
     */
    public void setTime(Calendar calendar){
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_SYSTEM_DATE_TIME);
        intent.putExtra(SYSTEM_DATE_TIME_MILLISECONDS , calendar.getTimeInMillis());
        mContext.sendBroadcast(intent);
    }

    /**
     * The method will set value of tcp_syn_retries
     * @param value 1 ~ 127
     *
     */
    public void setTcpSynRetries(int value)
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_TCP_SYN_RETRIES);
        intent.putExtra(EXTRA_TCP_SYN_RETRIES, value);
        mContext.sendBroadcast(intent);
    }

    /**
     *Set Aiming switch status if it's available.
     * @param enabled Aiming switch status
     */
    public void enableAimer(final boolean enabled) {
        Intent i = new Intent();
        i.setAction(ACTION_START_BCRSERVICE);
        mContext.sendBroadcast(i);
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setAction(ACTION_ENABLE_AIMER);
                intent.putExtra(EXTRA_ENABLE_AIMER, String.valueOf(enabled));
                mContext.sendBroadcast(intent);
            }
        },300);
    }

    /**
     *Set flash switch status if it's available.
     * @param enabled flash switch status
     */
    public void enableFlash(boolean enabled) {
        Intent intent = new Intent();
        intent.setAction(ACTION_ENABLE_FLASH);
        intent.putExtra(EXTRA_ENABLE_FLASH, String.valueOf(enabled));
        mContext.sendBroadcast(intent);
    }

    /**
     *Set Aiming high and dutyCycle
     * @param dutyCycle set gpio dutyCycle , unit:ms
     * @param pullHigh set gpio high , unit:ms
     */
    public void setAimer(final int dutyCycle, final int pullHigh) {
        Intent i = new Intent();
        i.setAction(ACTION_START_BCRSERVICE);
        mContext.sendBroadcast(i);
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setAction(ACTION_SET_AIMER);
                intent.putExtra(EXTRA_AIMER_DUTYCYCLE, dutyCycle);
                intent.putExtra(EXTRA_AIMER_PULLHIGH, pullHigh);
                mContext.sendBroadcast(intent);
            }
        },300);
    }

    private static native int native_set2ndTouchActive(int onoff);

    private static native int native_switchAntenna(int dist);

    private static native int native_powerOnMaxim(int dist);

    private static native int native_resetMaxim(int dist);

    private static native int native_powerOnGPS(int dist);

    private static native int native_resetGPS(int dist);

    private static native int native_setHeaterEnable(int dist);

    private static native int native_powerOnP95(int dist);

    private static native int native_powerOn3Gmodule();

    private static native int native_reset3Gmodule();

    private static native int native_setEppEnabled(boolean enabled);

    private static native int native_setPAEnabled(boolean enabled);
    
    private static native int native_setUsbSpeed(int mode);
    
    private static native int native_getUsbSpeed();

    private static native int native_powerOnUsbHub(int onoff);

    private static native int native_setChargingThreshold(int value);

    private static native int native_switchUsbMode(int value);

    private static native int native_setChargingSwitch(int type, boolean enabled);

    private static native int native_getChargingSwitch(int type);
}
