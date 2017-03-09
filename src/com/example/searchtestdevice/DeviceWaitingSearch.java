package com.example.searchtestdevice;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

import com.example.searchtestdevice.data.DataPack;
import com.example.searchtestdevice.data.Log;
/**
 * 设备等待搜索类
 */
public abstract class DeviceWaitingSearch extends Thread {
    private final String TAG = "DeviceWaitingSearch";
 
    private static final int DEVICE_FIND_PORT = 9000;
    private static final int RECEIVE_TIME_OUT = 60 * 1000; // 接收超时时间，应小于等于主机的超时时间1500
 
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
                // 等待主机的搜索
                socket.receive(pack);
                Log.i(TAG, "-------> receive req package");
                if (DataPack.parseDatagramPacket(pack, DataPack.PACKET_TYPE_FIND_HOST_REQ, mHandler)) {
                	//第一次反馈一个回应包 表示设备端接收到发送过来的请求  host端接收到后发送密码过来即可。
                	Log.i(TAG, "-------> receive req package right");
                    byte[] sendData = DataPack.packData(DataPack.PACKET_TYPE_FIND_DEVICE_RSP, null, null);   
                    DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, pack.getAddress(), pack.getPort());
                    Log.i(TAG, "-------> send respone package"); 
                    socket.send(sendPack);
                    socket.setSoTimeout(RECEIVE_TIME_OUT);
                    Log.i(TAG, "-------> wait for host send password");
                    
                    try {
                        socket.receive(pack);
                        
                        byte[] result;
                        String[] resultString;
                        
                        if (DataPack.parseDatagramPacket(pack, DataPack.PACKET_TYPE_FIND_HOST_CHK, mHandler)) {
                        	Log.i(TAG, "-------> password is right");
                        	result = new byte[]{DataPack.PACKET_DATA_TYPE_DEVICE_RESULT, DataPack.PACKET_DATA_TYPE_DEVICE_NAME};
                        	resultString = new String[]{DataPack.PACKET_CHK_RESULT_OK, mDeviceName};
                        	
                            onDeviceSearched((InetSocketAddress) pack.getSocketAddress());
                        } else {
                        	Log.i(TAG, "-------> password is bad");
                        	result = new byte[]{DataPack.PACKET_DATA_TYPE_DEVICE_RESULT};
                        	resultString = new String[]{DataPack.PACKET_CHK_RESULT_BAD};
                        }
                        
                        byte[] sendCHKData = DataPack.packData(DataPack.PACKET_TYPE_FIND_DEVICE_CHK, result, resultString);   
                        DatagramPacket sendCHKPack = new DatagramPacket(sendCHKData, sendCHKData.length, pack.getAddress(), pack.getPort());
                        Log.i(TAG, "-------> send result package"); 
                        socket.send(sendCHKPack);
                        break;
                    } catch (SocketTimeoutException e) {
                    }
                    socket.setSoTimeout(0); // 连接超时还原成无穷大，阻塞式接收
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
 
    /**
     * 当设备被发现时执行
     */
    public abstract void onDeviceSearched(InetSocketAddress socketAddr);
}
