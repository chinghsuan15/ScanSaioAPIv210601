// IDiagCallback.aidl
package com.xac.commanager;

// Declare any non-default types here with import statements

interface IDiagCallback {
    void onEvent(int dev, int cmd, int event);
}
