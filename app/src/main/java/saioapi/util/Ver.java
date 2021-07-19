//
//	Saio API, version class
//
package saioapi.util;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

/**
 * This class provides methods to get Saio version services.
 */
public class Ver
{
    static
    {
        //
        //	Load the corresponding library
        //
        System.loadLibrary("SaioUtil");
    }
    //
    //	Constants (Error code and Events)
    //
    /** The inputted parameters are not valid. */
    public static final int ERR_INVALID_PARAM       = 0x0000E000;
    
    /** The info is not found for the given type. */
    public static final int ERR_NOT_FOUND           = 0x0000E002;
    
    /** libSaioBase. */
    public static final short LIB_SAIO_BASE         = 0x00;
    
    /** libSaioReader. */
    public static final short LIB_SAIO_READER       = 0x01;
    
    /** libSaioPhone. */
    public static final short LIB_SAIO_PHONE        = 0x02;
    
    /** libSaioVer. */
    public static final short LIB_SAIO_VER          = 0x03;
    
    /** libSaioUtil. */
    public static final short LIB_SAIO_UTIL         = 0x03;
    
    /** All Saio library version. */
    public static final short LIB_SAIO_ALL          = 0xFF;

    /** v2 ComManager */
    public static final short COM_API_V2            = 2;

    /** v3 ComManager */
    public static final short COM_API_V3            = 3;

    //	
    //	Methods
    //
    
    /**
     * The method retrieves the Saio library version.
     * <br />The information table:
     * <table border=1>
     *   <thead><tr><th>Class Constants</th><th>Specific Information</th></tr></thead>
     *   <tbody>
     *     <tr><td>LIB_SAIO_BASE</td><td>The version of Saio Base library</td></tr>
     *     <tr><td>LIB_SAIO_READER</td><td>The version of Saio Reader library</td></tr>
     *     <tr><td>LIB_SAIO_PHONE</td><td>The version of Saio Phone library</td></tr>
     *     <tr><td>LIB_SAIO_VER</td><td>The version of Saio Util library (libSaioVer.so is renamed to libSaioUtil.so)</td></tr>
     *     <tr><td>LIB_SAIO_UTIL</td><td>The version of Saio Util library</td></tr>
     *     <tr><td>LIB_SAIO_ALL</td><td>All library version</td></tr>
     *   </tbody>
     * </table>
     * 
     * @param context The context of your application
     * @param type The system information to be retrieved defined in above table.
     * @param info The data array (minimum 256 bytes) to receive the Saio library version.
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public static int getSaioVersion(Object context, int type, byte[] info){
        return getSaioVersion(((Context) context).getPackageName(), type, info);
    }
    
    /**
     * The method retrieves the Saio library version.
     * <br />The information table:
     * <table border=1>
     *   <thead><tr><th>Class Constants</th><th>Specific Information</th></tr></thead>
     *   <tbody>
     *     <tr><td>LIB_SAIO_BASE</td><td>The version of Saio Base library</td></tr>
     *     <tr><td>LIB_SAIO_READER</td><td>The version of Saio Reader library</td></tr>
     *     <tr><td>LIB_SAIO_PHONE</td><td>The version of Saio Phone library</td></tr>
     *     <tr><td>LIB_SAIO_VER</td><td>The version of Saio Util library (libSaioVer.so is renamed to libSaioUtil.so)</td></tr>
     *     <tr><td>LIB_SAIO_UTIL</td><td>The version of Saio Util library</td></tr>
     *     <tr><td>LIB_SAIO_ALL</td><td>All library version</td></tr>
     *   </tbody>
     * </table>
     * 
     * @param packagename The package name of your application
     * @param type The system information to be retrieved defined in above table.
     * @param info The data array (minimum 256 bytes) to receive the Saio library version.
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public static native int getSaioVersion(String packagename, int type, byte[] info);
    
    /**
     * The method retrieves the U-boot env version.
     * 
     * @param info The data array (minimum 256 bytes) to receive the U-boot env version.
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public static native int getUBootEnvVersion(byte[] info);
   
    /**
     * The method retrieves the U-boot env custom settings.
     *
     * @param info The data array (minimum 256 bytes) to receive the custom settings.
     * @return zero if there is no error else nonzero error code defined in class constants.
     */
    public static int getUBootEnvCustomSettings(byte[] info) {
        int iRtn = ERR_NOT_FOUND;

        try {
            iRtn = getCustomSettings(info);
        }catch (Exception e){
            e.printStackTrace();
            iRtn = ERR_NOT_FOUND;
        }finally {
            return iRtn;
        }
    }

    private static native int getCustomSettings(byte[] info);

    /**
     * The method retrieves the app package name, version code and version name.
     *
     * @param context The activity context.
     * @return string contain full app package name, version code and version name.
     */
    public static List<AppInfo> getInstalledAppInfo(Context context) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> rList = pm.queryIntentActivities(mainIntent, 0);
        List<AppInfo> appList = new ArrayList<AppInfo>();
       
        for(ResolveInfo r : rList) {
            if (!isDuplicated(appList, r))
                appList.add(new AppInfo(r, pm));
        }
        return appList;
    }

    /**
     * This method retrieves the supported ComManager APIs version.
     *
     * @return the version number of ComManager APIs.
     */
    public static int getComApiVersion() {
        Method get = null;
        String value = "false";
        int ret;
        try {
            if (null == get) {
                Class<?> cls = Class.forName("android.os.SystemProperties");
                get = cls.getDeclaredMethod("get", new Class<?>[]{String.class, String.class});
            }
            value = (String) (get.invoke(null, new Object[]{"persist.sys.commanager.v3.enabled", "false"}));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        ret = value.equals("true")? COM_API_V3:COM_API_V2;
        return ret;
    }

    public static class AppInfo {
        public String mVersionName;
        public String mVersionCode;
        public String mPackageName;

        public AppInfo(ResolveInfo rInfo, PackageManager pm){
            mPackageName = rInfo.activityInfo.packageName;
            try {
                mVersionName = pm.getPackageInfo(mPackageName, PackageManager.GET_ACTIVITIES).versionName;
                mVersionCode = String.valueOf(pm.getPackageInfo(mPackageName, PackageManager.GET_ACTIVITIES).versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isDuplicated(List<AppInfo> list, ResolveInfo rInfo) {
        for (int i=0; i<list.size(); i++) {
            if (list.get(i).mPackageName.equals(rInfo.activityInfo.packageName))
                return true;
        }
        return false;
    }
}
