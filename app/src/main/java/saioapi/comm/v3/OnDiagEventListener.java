package saioapi.comm.v3;

public interface OnDiagEventListener {
    /**
     * Callback method would be invoked when an event is triggered.
     * Please implement UI behavior in UI thread, not in this method.
     * <p>
     * @param event Indicates the event defined in service class constants.
     */
    void onEvent(int cmd, int event);
}
