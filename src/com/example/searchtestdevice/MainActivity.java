package com.example.searchtestdevice;

import java.net.InetSocketAddress;

import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "mainDevice";
	private Button waitSearchButton;
	private Context mContext;
	private DeviceWaitingSearch deviceWaitingSearch;
	private Handler mHandler = new MyHander();
	private String mHostIp;  
	private DeviceSetting mDeviceSetting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		mDeviceSetting = new DeviceSetting(getApplicationContext());
		
		initView();
	}
	
	private void initView() {
		waitSearchButton = (Button)findViewById(R.id.bt_device);
		waitSearchButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_device:
			if(mDeviceSetting != null && mDeviceSetting.getEthernetStatus())
				setSearch();
			break;

		default:
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		
		if(keyCode == KeyEvent.KEYCODE_F10) {
			if(mDeviceSetting != null && mDeviceSetting.getEthernetStatus())
				setSearch();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void setSearch() {
		Log.i(TAG, "click-device-initdata");
		if(deviceWaitingSearch != null)
			return;
		
		deviceWaitingSearch = new DeviceWaitingSearch(mContext, mHandler, DataPackDevice.DEVICE_NAME){
			@Override
			public void onDeviceSearched(InetSocketAddress socketAddr) {
				mHostIp = socketAddr.getAddress().getHostAddress();
				Log.i("TAG", "-onDeviceSearched- hostIp : " + socketAddr.getAddress().getHostAddress() + ":" + socketAddr.getPort());
			}

		};
		deviceWaitingSearch.start();
		waitSearchButton.setEnabled(false);
	}
	
	private class MyHander extends Handler {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			
			switch (msg.what) {
			case DataPackDevice.PACKET_DATA_TYPE_DEVICE_QUIT:
				deviceWaitingSearch = null;
				waitSearchButton.setEnabled(true);
				break;
				
			case DataPackDevice.DEVICE_FIND:
				if(msg.arg1 == DataPackDevice.DEVICE_CONNECTED) {
					//if connect with host, close deviceWaitSearch
					//adn run tcp 
					deviceWaitingSearch = null;
					waitSearchButton.setEnabled(false);
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.udp_connect_success), Toast.LENGTH_SHORT).show();
					
					//after connect with host, run tcp connect.
					new DeviceRecvDataThread(mHandler, mHostIp, mDeviceSetting).start();
				} else if (msg.arg1 == DataPackDevice.DEVICE_NOT_CONNECTED){
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
					deviceWaitingSearch = null;
					waitSearchButton.setEnabled(false);
				}
				break;
				
			case DataPackDevice.PACKET_TYPE_SEND_RECV_DATA:
				Toast.makeText(mContext, "begin to send data to client", Toast.LENGTH_LONG).show();

				if(mDeviceSetting == null)
					return;
				
				if(msg.arg1 == DataPackDevice.PACKET_DATA_TYPE_DEVICE_ALL) {
					sendAllData();
				} else {
					boolean isDefault = false;
					byte[] dataType = new byte[]{DataPackDevice.PACKET_DATA_TYPE_DEVICE_SETIING_RESULT};
					boolean result = false;
					String value = (String)msg.obj;
					
					switch (msg.arg1) {
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_LANG:
						result = mDeviceSetting.setLanguage(value);
						break;
						
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_AUDI:
						result = mDeviceSetting.setVoice(value);
						break;
					
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_NAME:
						//not support set device name.
						result = true;
						break;
						
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_TIME:
						result = mDeviceSetting.setSystemTime(value);
						break;
						
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_ETIP:
						result = mDeviceSetting.setEthernetIp(value);
						break;
						
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_CONT:
						result = mDeviceSetting.setContactData(value);
						break;
					
					case DataPackDevice.PACKET_DATA_TYPE_DEVICE_QUIT:
						deviceWaitingSearch = null;
						waitSearchButton.setEnabled(true);
						dataType = new byte[]{DataPackDevice.PACKET_DATA_TYPE_DEVICE_QUIT};
						result = true;
						break;

					default:
						isDefault = true;
						break;
					}
					
					if(!isDefault) {
						String[] dataContent = new String[]{
								(result ? DataPackDevice.PACKET_CHK_RESULT_OK : DataPackDevice.PACKET_CHK_RESULT_BAD)
						};
						
						new DeviceSendDataThread(mHandler, mHostIp, dataType, dataContent).start();
					}
				}

			default:
				break;
			}
		}
	}

    private boolean checkWifiStatus(Context context) {
    	boolean result = false;
    	ConnectivityManager wm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = wm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(!ni.isConnected()) {
        	Toast.makeText(context, context.getResources().getString(R.string.please_open_wifi), Toast.LENGTH_SHORT).show();
        } else {
        	result = true;
        }
        return result;
    }
    
    private void sendAllData() {
    	byte[] dataType = new byte[] {
    		DataPackDevice.PACKET_DATA_TYPE_DEVICE_NAME,
    		DataPackDevice.PACKET_DATA_TYPE_DEVICE_LANG,
    		DataPackDevice.PACKET_DATA_TYPE_DEVICE_AUDI,
			DataPackDevice.PACKET_DATA_TYPE_DEVICE_TIME,
			DataPackDevice.PACKET_DATA_TYPE_DEVICE_ETIP,
			DataPackDevice.PACKET_DATA_TYPE_DEVICE_CONT
    	};
    	
    	String[] dataContent = new String[dataType.length];
    	
    	//device name
    	dataContent[0] = DataPackDevice.DEVICE_NAME;
    	
    	//device lang
    	dataContent[1] = mDeviceSetting.getLanguage();
    	
    	//device audio
    	dataContent[2] = mDeviceSetting.getVoice(0);
    	
    	//device time
    	dataContent[3] = mDeviceSetting.getSystemTime();
    	
    	//device ip address
    	dataContent[4] = mDeviceSetting.getEthernetIp();
    	
    	//device contact
    	dataContent[5] = mDeviceSetting.getContactList();
    	
    	new DeviceSendDataThread(mHandler, mHostIp, dataType, dataContent).start();
    }
}
