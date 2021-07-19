// IComManagerService.aidl
package com.xac.commanager;
import com.xac.commanager.IComManagerServiceCallback;
import com.xac.commanager.IDiagCallback;
// Declare any non-default types here with import statements

interface IComManagerService {
   void setListener(IComManagerServiceCallback callback);
   int open(int dev);
   int connect(int dev, int baud, int data_size, int stop_bit, int parity, int flow_control, int protocol, in byte[] extra);
   int write(int dev, in byte[] data, int len, int timeout);
   int read(int dev, out byte[] data, int len, int timeout);
   int close(int dev);
   boolean isOpened(int dev);
   int lastError(int dev);
   int status(int dev);
   int getCts(int dev);
   int setRts(int dev, int rts);
   void setDiagListener(IDiagCallback callback);
   int diagWrite(int dev, int cmd, in byte[] data);
   int diagRead(int dev, out byte[] data, int len);
   int resetPort(int dev);
}