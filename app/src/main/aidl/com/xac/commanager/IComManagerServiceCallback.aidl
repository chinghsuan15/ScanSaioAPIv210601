// IComManagerServiceCallback.aidl
package com.xac.commanager;

// Declare any non-default types here with import statements

interface IComManagerServiceCallback {
    void onEvent(int dev, int event);
}
