package com.example.searchtestdevice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.example.searchtestdevice.data.ContactBean;
import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.InterAddressUtil;


import android.support.v7.app.ActionBarActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Data;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
/**
 * 1.��apkΪ�豸�˵�apk����Ҫ�ȴ�������֮����ܱ�host��apk�����õ�
 * 2.Ŀǰ���Ե�Ч�����豸�˴򿪺�����host��apk����������ͬһ����������Ϣ�����Ի��ip�Ͷ˿���Ϣ(���hostҲҪ������������ҲҪ���д�apk)
 * 3.����ͨ�Ŵ����ݵ�ϸ����Ӧ���ݵ�ϸ���Ҿ�ûϸ��������
 * 4.Ȼ�����ͨ��initdata���������ݣ�setdata����������ݣ������ѵ��Ժú�δ���Ժõļ�����˵��
 *
 */
public class MainActivity extends ActionBarActivity implements OnClickListener {

	private Button waitSearchButton;
	private Context mContext;
	private ContentResolver mContentResolver;
	private ArrayList mContactList;
	private DeviceWaitingSearch deviceWaitingSearch;
	private Handler mHandler = new MyHander();
	private String mHostIp;  
	private DeviceSetting mDeviceSetting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		mContentResolver=mContext.getContentResolver();
		mDeviceSetting = new DeviceSetting(getApplicationContext());
		
		initView();
	}
	
	private void initView() {
		waitSearchButton=(Button)findViewById(R.id.bt_device);
		waitSearchButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_device:
			if(checkWifiStatus(getApplicationContext()))
				setSearch();//��ʼ��֮����ܱ�������
			break;

		default:
			break;
		}
	}

	private void setSearch() {
		Log.i("TAG", "click-device-initdata");
		if(deviceWaitingSearch != null)
			return;
		
		deviceWaitingSearch = new DeviceWaitingSearch(this, mHandler, "�����豸"){
			@Override
			public void onDeviceSearched(InetSocketAddress socketAddr) {
				mHostIp = socketAddr.getAddress().getHostAddress();
				Log.i("TAG", "-onDeviceSearched- hostIp : " + socketAddr.getAddress().getHostAddress() + ":" + socketAddr.getPort());
			}

		};
		deviceWaitingSearch.start();
	}
	
	private class MyHander extends Handler {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			
			switch (msg.what) {
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
				}
				break;
				
			case DataPackDevice.PACKET_TYPE_SEND_RECV_DATA:
				Toast.makeText(mContext, "begin to send data to client", Toast.LENGTH_LONG).show();
				byte[] dataType = new byte[]{DataPackDevice.PACKET_DATA_TYPE_DEVICE_LANG};
				String[] dataContent = new String[]{"new servcie....."};
				
				new DeviceSendDataThread(mHandler, mHostIp, dataType, dataContent).start();
				
				switch (msg.arg1) {
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_LANG:
					
					break;
					
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_AUDI:
					
					break;
				
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_NAME:
					
					break;
					
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_TIME:
					
					break;
					
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_ETIP:
					
					break;
					
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_CONT:
					
					break;
					
				case DataPackDevice.PACKET_DATA_TYPE_DEVICE_ALL:
					
					break;

				default:
					break;
				}
				
				break;

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
}
