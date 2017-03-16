package com.example.searchtestdevice;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import android.os.Handler;

public class DeviceRecvDataThread extends Thread {
	private final String TAG = "DeviceSendAndRecvDataThread";
	
	private Handler mHandler;
	private ServerSocket serverSocket;
	private static final int SERVER_SOCKET_PORT = 9001;
	private String hostIp;
	private static final int RECEIVE_TIME_OUT = 10 * 60 * 1000;
	private boolean isRunning = true;
	
	public DeviceRecvDataThread(Handler handler, String ip, DeviceSetting ds) {
		mHandler = handler;
		hostIp = ip;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		try {
			serverSocket = new ServerSocket(SERVER_SOCKET_PORT);
			while(isRunning) {
				serverSocket.setSoTimeout(RECEIVE_TIME_OUT);
				Socket deviceSocket = serverSocket.accept();
				
				String remoteIp = deviceSocket.getInetAddress().getHostAddress();
				Log.i(TAG, "------------------->  remoteIp : " + remoteIp + "   hostIp : " + hostIp + "   isEqual : " + remoteIp.equals(hostIp));
				
				if(!remoteIp.equals(hostIp)) {
					deviceSocket.close();
					continue;
				}
				
				if(true == parseData(deviceSocket)) {
					isRunning = false;
				}
				
				deviceSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG, "---------------------> wrong : e " + e);
		} finally {
			try {
				if (serverSocket != null) {
					serverSocket.close();
					serverSocket = null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
    @SuppressWarnings("resource")
	public boolean parseData(Socket socket) {
        boolean result = false;
        if (socket != null && socket.isConnected()) {
        	if(!socket.isInputShutdown()) {
        		DataInputStream dIn = null;
				try {
					dIn = new DataInputStream(socket.getInputStream());
					int length = dIn.readInt();
					if(length > 0) {
						byte[] message = new byte[length];
						dIn.readFully(message, 0, message.length);
						
						result = DataPackDevice.parseServiceSocktPackage(message, mHandler);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if(dIn != null)
							dIn.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
        }
        return result;
    }
}
