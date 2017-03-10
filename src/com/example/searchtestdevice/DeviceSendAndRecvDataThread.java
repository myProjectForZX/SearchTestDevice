package com.example.searchtestdevice;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import android.os.Handler;

public class DeviceSendAndRecvDataThread extends Thread{
	private final String TAG = "DeviceSendAndRecvDataThread";
	
	private Handler mHandler;
	private ServerSocket serverSocket;
	private static final int SERVER_SOCKET_PORT = 9001;
	private String hostIp;
	
	public DeviceSendAndRecvDataThread(Handler handler, String ip) {
		mHandler = handler;
		hostIp = ip;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		try {
			serverSocket = new ServerSocket(SERVER_SOCKET_PORT);
			while(true) {
				Socket deviceSocket = serverSocket.accept();
				
				String remoteIp = deviceSocket.getInetAddress().getHostAddress();
				Log.i(TAG, "------------------->  remoteIp : " + remoteIp + "   hostIp : " + hostIp + "   isEqual : " + remoteIp.equals(hostIp));
				
				//非目的地的ip不做处理。
				if(!remoteIp.equals(hostIp))
					continue;
				
				receiveData(deviceSocket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG, "---------------------> wrong");
		}
	}
	
    @SuppressWarnings("resource")
	public byte[] receiveData(Socket socket) {
        byte[] data = null;
        if (socket != null && socket.isConnected()) {
        	if(!socket.isInputShutdown()) {
        		DataInputStream dIn = null;
				try {
					dIn = new DataInputStream(socket.getInputStream());
					int length = dIn.readInt();
					if(length > 0) {
						byte[] message = new byte[length];
						dIn.readFully(message, 0, message.length);
						
						DataPackDevice.parseServiceSocktPackage(message, mHandler);
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
        } else {
            data = new byte[1];
        }
        return data;
    }
    
    private void sendMessage(Socket socket, byte packType, byte[]dataType, String[]dataContent) {
    	if(socket == null)
    		return;
    	
    	DataOutputStream dOut = null;
    	byte[] data = DataPackDevice.packData(packType, dataType, dataContent);
    	
		try {
			dOut = new DataOutputStream(socket.getOutputStream());
			if (dOut != null) {
				dOut.writeInt(data.length);
				dOut.write(data);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
