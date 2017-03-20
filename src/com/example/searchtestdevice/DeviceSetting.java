package com.example.searchtestdevice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.searchtestdevice.data.ContactBean;
import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import android.R.integer;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract.Contacts.Data;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class DeviceSetting {
	private static final String TAG = "deviceSetting";
	private Context mContext;
	private ContentResolver mContentResolver;
	
	//实际locale   客户端显示名称   displayname
	private String[][] supportLanguage = new String[][] {
		{Locale.SIMPLIFIED_CHINESE.toString(), "简体","中文 (简体)"},
		{Locale.TRADITIONAL_CHINESE.toString(), "繁体", "中文 (繁体)"},
		{Locale.ENGLISH.toString(), "英语", "English (United States)"}
	};
	
	private String[][] supportAudio = new String[][] {
		{String.valueOf(AudioManager.STREAM_SYSTEM), "系统"},	
		{String.valueOf(AudioManager.STREAM_ALARM), "闹钟"},
		{String.valueOf(AudioManager.STREAM_MUSIC), "媒体"}
	};
	
	public DeviceSetting(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
	}
	
	public boolean setSystemTime(String datetimes) {
		boolean result = true;
		try {
			Process process = Runtime.getRuntime().exec("su");
			String datetime = ""; 
			datetime = datetimes.toString(); 
			DataOutputStream os = new DataOutputStream(
					process.getOutputStream());
			os.writeBytes("setprop persist.sys.timezone GMT\n");
			os.writeBytes("/system/bin/date -s " + datetime + "\n");
			os.writeBytes("clock -w\n");
			os.writeBytes("exit\n");
			os.flush();
		} catch (IOException e) {
			result = false;
			Toast.makeText(mContext, "need root", Toast.LENGTH_SHORT).show();
		}
		return result;
	}

	public String getSystemTime() {
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		int second = c.get(Calendar.SECOND);
		StringBuffer sb = new StringBuffer();
		sb.append(year);
		String monthStr = getStr(month);
		String dayStr = getStr(day);
		String hourStr = getStr(hour);
		String minuteStr = getStr(minute);
		String secondStr = getStr(second);

		sb.append(monthStr);
		sb.append(dayStr);
		sb.append(hourStr);
		sb.append(minuteStr);
		sb.append(secondStr);
		return sb.toString();
	}

	public void setLanguage(String language) {
		Log.i(TAG, "setLanguage-language="+language);
		
		try {
			Configuration config = mContext.getResources().getConfiguration();
			DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
			config.locale = Locale.ENGLISH;
			mContext.getResources().updateConfiguration(config, dm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//数据格式
	//当前语言:xx:yy
	//xx  yy 为剩下的支持的语言
	public String getLanguage() {
		Locale currentLocale = Locale.getDefault();
		StringBuffer sb = new StringBuffer();
		
		int i = 0;
		if(currentLocale.getLanguage().toString().startsWith("en")) {
			sb.append(supportLanguage[2][1]);
			i = 2;
		} else if (currentLocale.toString().equals("zh_CN")) {
			sb.append(supportLanguage[0][1]);
			i = 0;
		} else if (currentLocale.toString().equals("zh_TW")) {
			sb.append(supportLanguage[1][1]);
			i = 1;
		}
		
		sb.append(":");
		
		for(int j = 0; j < supportLanguage.length; ++j) {
			if(j == i)
				continue;
			sb.append(supportLanguage[j][1] + ":");
		}
		
		Log.e(TAG, "------------> label : " + currentLocale.getDisplayName());
		
		if(sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public boolean setEthernetIp(String ip) {
		boolean result = true;
		return result;
	}
	
	public String getEthernetIp() {
		return getLocalIpAddress();
	}
	
	
	
	public boolean setVoice(String typeValue) {
		boolean result = true;
		Log.i(TAG, "-------------> setVoice");
		AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		//mAudioManager.setStreamVolume(streamType, streamValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
		return result;
	}
	
	//voice 数据格式
	//类型:当前音量大小:最大音量大小+....
	public String getVoice(int streamType) {
		String result = null;
		Log.i(TAG, "-------------> getVoice");
		
		AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < supportAudio.length; ++i) {
			int type = Integer.valueOf(supportAudio[i][0]);
			int current = mAudioManager.getStreamVolume(type);
			int max = mAudioManager.getStreamMaxVolume(type);
			
			sb.append(supportAudio[i][1]);
			sb.append(":");
			sb.append(current);
			sb.append(":");
			sb.append(max);
			sb.append("+");
		}
		
		if(sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public boolean setContactData(String value) {
		boolean result = true;
		/*
		int id = 1;
		String phone = "2222";
		String name = "www";
		Uri dataUri = Uri.parse("content://com.android.contacts/data");//��data����������ݲ���
		ContentValues phoneValues = new ContentValues();
		phoneValues.put("data1", phone);
		mContentResolver.update(dataUri, phoneValues, "mimetype=? and raw_contact_id=?", new String[]{"vnd.android.cursor.item/phone_v2",id+""});
		ContentValues nameValue = new ContentValues();
		nameValue.put("data1", name);
		mContentResolver.update(dataUri, nameValue, "mimetype=? and raw_contact_id=?", new String[]{"vnd.android.cursor.item/name",id+""});
		*/
		return result;
	}
	
	//数据结构
	//ID:NAME:NUMBER+ID:NAME:NUMBER...
	public String getContactList() {
		StringBuffer sb = new StringBuffer();
		
		Uri uri = Uri.parse("content://com.android.contacts/contacts");
		Cursor cursorContact = mContentResolver.query(uri, new String[]{Data._ID}, null, null, null);  
		
		while(cursorContact.moveToNext()){
			ContactBean contactBean = new ContactBean();
			int id = cursorContact.getInt(0);
			contactBean.setId(id);

			uri = Uri.parse("content://com.android.contacts/contacts/"+id+"/data");
			Cursor cursorData = mContentResolver.query(uri, new String[]{Data.DATA1,Data.MIMETYPE}, null,null, null); 
			while(cursorData.moveToNext()){
				String data = cursorData.getString(cursorData.getColumnIndex("data1"));
				if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/name")){
					contactBean.setName(data);
				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/phone_v2")){
					contactBean.setPhoneNum(data);
				}
			}
			
			sb.append(id);
			sb.append(":");
			sb.append(contactBean.getName());
			sb.append(":");
			sb.append(contactBean.getPhoneNum());
			sb.append("+");
			
			if(cursorData!=null){
				cursorData.close();
			}
		}
		if(cursorContact!=null){
			cursorContact.close();
		}
		
		if(sb.length()>1) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	private String getStr(int value) {
		String valueStr;
		if(value<10){
			valueStr ="0"+ String.valueOf(value);
		}else{
			valueStr = String.valueOf(value);
		}
		return valueStr;
	}
	
	public String getDeviceName() {
		return DataPackDevice.DEVICE_NAME;
	}
	
	//获取ip地址
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					
					if(inetAddress instanceof Inet6Address)
						continue;
					
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.i(TAG, "WifiPreference IpAddress-" + ex.toString());
		}
		return null;
	}
}
