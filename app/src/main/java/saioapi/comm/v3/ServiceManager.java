package saioapi.comm.v3;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.xac.commanager.IComManagerService;
import com.xac.commanager.IComManagerServiceCallback;
import com.xac.commanager.IDiagCallback;

class ServiceManager {
    private String TAG = "CM-ServiceManager";
    private IComManagerService mService;
    private int mOpenPort = -1;

    /**
     * The listener that receives notifications when an COMM event is triggered.
     */
    private OnComEventListener mOnComEventListener = null;
    private OnDiagEventListener mOnDiagEventListener = null;

    private Uri URI_COMMANAGER_SERVICE = Uri.parse("content://com.xac.commanager.ipcprovider.authority/ipc/");
    private Context mContext;
    private long mTimeStamp;

    private void getCurrentBinder(){
        long timeStamp = System.currentTimeMillis();
        Cursor c = mContext.getContentResolver().query(URI_COMMANAGER_SERVICE,new String[] {"isReconected"},null,new String[] {"commanager"},null);
        try {
            if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                timeStamp = c.getLong(0);
            }
            c.close();
        }catch(Exception e){
            Log.e(TAG,"timeStamp e ="+e);
        }
        if(mTimeStamp != timeStamp) {
            mTimeStamp = timeStamp;
            Cursor cur = mContext.getContentResolver().query(URI_COMMANAGER_SERVICE, new String[]{"service"}, null, new String[]{"commanager"}, null);
            try {
                IBinder binder = getBinder(cur);
                if (binder != null) {
                    IComManagerService service;
                    service = IComManagerService.Stub.asInterface(binder);
                    //Log.i(TAG, "mService=" + mService + " service=" + IComManagerService.Stub.asInterface(binder));
                    if (service != null) {
                        if (mService != service) {
                            mService = service;
                        }
                        if (mOpenPort != -1) {
                            service.open(mOpenPort);
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "e =" + e);
            }
            cur.close();
        }
    }
    //public ServiceManager(Context context,OnComEventListener listener,OnDiagEventListener diaglistener)
    public ServiceManager(Context context)
    {
        /*Intent intent = new Intent();
        intent.setAction("ComManagerService.SERVICE.REMOTE");
        intent.setPackage("com.xac.commanager");
        context.bindService(intent, mConnection, context.BIND_AUTO_CREATE);*/
        mContext = context;
        getCurrentBinder();
        //mOnComEventListener = listener;
        //mOnDiagEventListener = diaglistener;


    }
    /**
     * Register a callback to be invoked when a Com event is triggered.
     * @param listener The callback that will be invoked.
     */
    public void setOnComEventListener(OnComEventListener listener)
    {
        mOnComEventListener = listener;
        try {
            getCurrentBinder();
            if(mService != null ) {
                mService.setListener(mListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Register a callback to be invoked when a Diag event is triggered.
     * @param listener The callback that will be invoked.
     */
    public void setOnDiagEventListener(OnDiagEventListener listener)
    {
        mOnDiagEventListener = listener;
        try {
            getCurrentBinder();
            if(mService != null) {
                Log.d(TAG,"setDiagListener");
                mService.setDiagListener(mDiagListener);
            }
        }catch(RemoteException e){
            Log.d(TAG,"e ="+e);
        }
    }


    private static final IBinder getBinder(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        extras.setClassLoader(BinderCursor.BinderParcelable.class.getClassLoader());
        BinderCursor.BinderParcelable w = extras.getParcelable("commanager");
        if(w != null)
            return w.mBinder;
        else
            return null;
    }

    /*private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"============!!!onServiceConnected!!!==============");
            mService = IComManagerService.Stub.asInterface(service);
            try {
                mService.setListener(mListener);
                mService.setDiagListener(mDiagListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };*/

    /**CallBack*/
    private IComManagerServiceCallback.Stub mListener = new IComManagerServiceCallback.Stub() {
        @Override
        public void onEvent(int dev, int event) throws RemoteException {
            if (null != mOnComEventListener) {
                mOnComEventListener.onEvent(event);
            }
        }
    };

    /**CallBack*/
    private IDiagCallback.Stub mDiagListener = new IDiagCallback.Stub() {
        @Override
        public void onEvent(int dev, int cmd, int event) throws RemoteException {
            if (null != mOnDiagEventListener) {
                mOnDiagEventListener.onEvent(cmd, event);
            }
        }
    };

    public int open(int dev_id) {
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.open(dev_id);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (ret != -1){
            mOpenPort = dev_id;
        }
        return ret;
    }

    public int close() {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.close(mOpenPort);
            mOpenPort = -1;
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int connect(int baud, int data_size, int stop_bit, int parity, int flow_control, int protocol, byte[] extra) {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.connect(mOpenPort, baud, data_size, stop_bit, parity, flow_control, protocol, extra);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int write(byte[] data, int len, int timeout) {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.write(mOpenPort, data, len, timeout);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int read(byte[] data, int len, int timeout) {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.read(mOpenPort, data, len, timeout);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int diagWrite(int dev, int cmd, byte[] data) {
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.diagWrite(dev,cmd,data);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int diagRead(int dev, byte[] data,int len) {
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.diagRead(dev,data,len);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public boolean isOpened() {
        boolean ret = false;
        if (mOpenPort != -1)
            ret = true;

        return ret;
    }

    public int lastError() {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.lastError(mOpenPort);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int status() {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.status(mOpenPort);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int getCts() {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.getCts(mOpenPort);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int setRts(int rts) {
        if (mOpenPort == -1)
            return ComManager.ERR_NOT_OPEN;
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.setRts(mOpenPort,rts);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public int resetPort(int dev) {
        int ret = -1;
        try {
            getCurrentBinder();
            ret = mService.resetPort(dev);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }
}
