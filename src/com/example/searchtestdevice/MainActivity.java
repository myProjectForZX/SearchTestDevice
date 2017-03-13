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
 * 1.此apk为设备端的apk，需要先打开搜索，之后才能被host的apk搜索得到
 * 2.目前调试的效果是设备端打开后，运行host的apk可以搜索到同一局域网的信息，可以获得ip和端口信息(如果host也要被搜索到，则也要运行此apk)
 * 3.具体通信传数据的细节相应数据的细节我就没细化调试了
 * 4.然后可以通过initdata获得相关数据，setdata设置相关数据，具体已调试好和未调试好的见方法说明
 *
 */
public class MainActivity extends ActionBarActivity implements OnClickListener {

	private Button waitSearchButton;
	private Context mContext;
	private ContentResolver mContentResolver;
	private ArrayList mContactList;
	private DeviceWaitingSearch deviceWaitingSearch;
	private Handler mHandler = new MyHander();
	private String mDeviceIp;  
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
				setSearch();//初始化之后才能被搜索到
			break;

		default:
			break;
		}
	}

	private void setSearch() {
		Log.i("TAG", "click-device-initdata");
		if(deviceWaitingSearch != null)
			return;
		
		deviceWaitingSearch = new DeviceWaitingSearch(this, mHandler, "中央设备"){
			@Override
			public void onDeviceSearched(InetSocketAddress socketAddr) {
				mDeviceIp = socketAddr.getAddress().getHostAddress();
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
					new DeviceSendAndRecvDataThread(mHandler, mDeviceIp, mDeviceSetting).start();
				} else if (msg.arg1 == DataPackDevice.DEVICE_NOT_CONNECTED){
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
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
