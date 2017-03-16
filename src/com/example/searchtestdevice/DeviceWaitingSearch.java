package com.example.searchtestdevice;


import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;


import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;
/**
 */
public abstract class DeviceWaitingSearch extends Thread {
    private final String TAG = "DeviceWaitingSearch";
 
    private static final int DEVICE_FIND_PORT = 9000;
    private static final int RECEIVE_TIME_OUT = 5000; 
    private static final int TOTAL_RECEIVE_TIME_OUT = 10 * 60 * 1000; 
    private Context mContext;
    private String mDeviceName;
    private Handler mHandler;
 
    public DeviceWaitingSearch(Context context, Handler hander, String name) {
        mContext = context;
        mDeviceName = name;
        mHandler = hander;
    }
 
    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(DEVICE_FIND_PORT);
            byte[] data = new byte[4096];
            DatagramPacket pack = new DatagramPacket(data, data.length);
            while (true) {
            	socket.setSoTimeout(TOTAL_RECEIVE_TIME_OUT); 
                socket.receive(pack);
                Log.i(TAG, "-------> receive req package");
                if (DataPackDevice.parseDatagramPacket(pack, DataPackDevice.PACKET_TYPE_FIND_HOST_REQ, mHandler)) {
                	Log.i(TAG, "-------> receive req package right");
                    byte[] sendData = DataPackDevice.packData(DataPackDevice.PACKET_TYPE_FIND_DEVICE_RSP, null, null);   
                    DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, pack.getAddress(), pack.getPort());
                    Log.i(TAG, "-------> send respone package"); 
                    socket.send(sendPack);
                    socket.setSoTimeout(RECEIVE_TIME_OUT);
                    Log.i(TAG, "-------> wait for host send password");
                    
                    try {
                        socket.receive(pack);
                        
                        byte[] result;
                        String[] resultString;
                        Message msg = new Message();
                        msg.what = DataPackDevice.DEVICE_FIND;
                        
                        if (DataPackDevice.parseDatagramPacket(pack, DataPackDevice.PACKET_TYPE_FIND_HOST_CHK, mHandler)) {
                        	Log.i(TAG, "-------> password is right");
                        	result = new byte[]{DataPackDevice.PACKET_DATA_TYPE_DEVICE_RESULT, DataPackDevice.PACKET_DATA_TYPE_DEVICE_NAME};
                        	resultString = new String[]{DataPackDevice.PACKET_CHK_RESULT_OK, mDeviceName};
                            onDeviceSearched((InetSocketAddress) pack.getSocketAddress());
                            msg.arg1 = DataPackDevice.DEVICE_CONNECTED;
                        } else {
                        	Log.i(TAG, "-------> password is bad");
                        	result = new byte[]{DataPackDevice.PACKET_DATA_TYPE_DEVICE_RESULT};
                        	resultString = new String[]{DataPackDevice.PACKET_CHK_RESULT_BAD};
                        	msg.arg1 = DataPackDevice.DEVICE_NOT_CONNECTED;
                        }
                        
                        byte[] sendCHKData = DataPackDevice.packData(DataPackDevice.PACKET_TYPE_FIND_DEVICE_CHK, result, resultString);   
                        DatagramPacket sendCHKPack = new DatagramPacket(sendCHKData, sendCHKData.length, pack.getAddress(), pack.getPort());
                        Log.i(TAG, "-------> send result package"); 
                        socket.send(sendCHKPack);
                        
                        if(mHandler != null) {
                        	mHandler.sendMessage(msg);
                        }
                        
                        if(msg.arg1 == DataPackDevice.DEVICE_CONNECTED)
                        	break;
                    } catch (SocketTimeoutException e) {
                    }
                }
            }
        } catch (SocketTimeoutException e) {
        	
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
 
    /**
     */
    public abstract void onDeviceSearched(InetSocketAddress socketAddr);
}
