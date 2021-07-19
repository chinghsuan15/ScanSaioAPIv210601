package saioapi.util;

public interface OnCradleStatusListener {

    /**
     * Callback method would be invoked when cradle get information response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result ofcradle.
     * @param info os & firmware version of cradle.
     * @param sn serial number of cradle.
     */
    void getDeviceStatus(int status,String info,String sn);
    /**
     * Callback method would be invoked when cradle execute reboot response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getRebootStatus(int status);
    /**
     * Callback method would be invoked when cradle execute reset response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getResetStatus(int status);
    /**
     * Callback method would be invoked when cradle execute get cradle log response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getLogStatus(int status);
    /**
     * Callback method would be invoked when cradle execute update cradle OS pre-upgrade response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getPreUpgradleStatus(int status);
    /**
     * Callback method would be invoked when cradle execute update cradle OS transfer data to cradle response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getTransferStatus(int status);
    /**
     * Callback method would be invoked when cradle start update cradle OS response
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param status action execute result of cradle.
     */
    void getUpdateStatus(int status);
}
