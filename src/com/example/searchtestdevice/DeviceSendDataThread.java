package com.example.searchtestdevice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import android.os.Handler;

public class DeviceSendDataThread extends Thread{
	private final String TAG = DeviceSendDataThread.class.getSimpleName();
	private Handler mHandler;
	private String mHostIp;
	private final int HOST_SERVER_IP = 9002;
	private DataOutputStream dataOutputStream;
	private byte[] mDataType;
	private String[] mDataContent;
	
	public DeviceSendDataThread(Handler handler, String ip, byte[]dataType, String[] dataContent) {
		this.mHandler = handler;
		this.mHostIp = ip;
		if(dataType == null || dataContent == null)
			return;
		this.mDataType = dataType.clone();
		this.mDataContent = dataContent.clone();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		Socket socket = null;
		try {
			socket = new Socket(mHostIp, HOST_SERVER_IP);
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (socket == null)
			return;

		if (socket.isConnected()) {
			if (!socket.isOutputShutdown()) {
				sendMessage(
						dataOutputStream,
						DataPackDevice.PACKET_TYPE_SEND_RECV_DATA,
						mDataType,
						mDataContent);
			}
		}
		
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	  private void sendMessage(DataOutputStream dOut, byte packType, byte[]dataType, String[]dataContent) {
	    	if(dOut == null)
	    		return;
	    	
	    	byte[] data = DataPackDevice.packData(packType, dataType, dataContent);
	    	
			try {
				Log.e(TAG, "--------------------> sendMessage getOutPutStream dOut : " + dOut);
				if (dOut != null) {
					Log.e(TAG, "--------------------> sendMessage length : " + data.length);
					dOut.writeInt(data.length);
					dOut.write(data);
					dOut.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "-------------------> sendMessage error : " + e.toString());
				e.printStackTrace();
			} finally {
				try {
					if (dOut != null)
						dOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	    }
}
