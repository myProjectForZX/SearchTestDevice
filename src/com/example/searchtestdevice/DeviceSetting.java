package com.example.searchtestdevice;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;

import com.example.searchtestdevice.data.ContactBean;
import com.example.searchtestdevice.data.DataPackDevice;
import com.example.searchtestdevice.data.Log;

import com.android.internal.app.LocalePicker;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract.Contacts.Data;

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
	
	private Locale[] supportLocales = new Locale[] {
			Locale.SIMPLIFIED_CHINESE,
			Locale.TRADITIONAL_CHINESE,
			Locale.US
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
	
	//时间格式
	//year-month-day-hour-minute-second
	//2010-12-12-23-59-59
	public boolean setSystemTime(String datetimes) {
		boolean result = true;
		if(datetimes == null || datetimes.isEmpty()) {
			return false;
		}
		
		String[] timeStrings = datetimes.split("-"); 
		
		if(timeStrings == null || timeStrings.length != 6)
			return false;
		
		int year = Integer.valueOf(timeStrings[0]);
		int month = Integer.valueOf(timeStrings[1]);
		int day = Integer.valueOf(timeStrings[2]);
		int hour = Integer.valueOf(timeStrings[3]);
		int minute = Integer.valueOf(timeStrings[4]);
		int second = Integer.valueOf(timeStrings[5]);
		
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second);
		
		Log.e(TAG, "---------- year : " + year + " month : " + month + " day : " + day + " hour : " + hour + " minute : " + minute + " sec : " + second);
		
		long when = c.getTimeInMillis();
		
		if(when / 1000 < Integer.MAX_VALUE) {
			//need android system compile
			((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).setTime(when);
		}
		
		return result;
	}

	//时间格式
	//year-month-day-hour-minute-second
	//2010-12-12-23-59-59
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
		sb.append("-");
		String monthStr = getStr(month);
		String dayStr = getStr(day);
		String hourStr = getStr(hour);
		String minuteStr = getStr(minute);
		String secondStr = getStr(second);

		sb.append(monthStr);
		sb.append("-");
		sb.append(dayStr);
		sb.append("-");
		sb.append(hourStr);
		sb.append("-");
		sb.append(minuteStr);
		sb.append("-");
		sb.append(secondStr);
		return sb.toString();
	}

	private Locale getSettingLocale(String language) {
		Locale result = null;
		if(language == null || language.isEmpty())
			return null;
		
		for(int i = 0; i < supportLanguage.length; ++i) {
			if(language.equals(supportLanguage[i][1])){
				result = supportLocales[i];
			}
		}
		
		Log.e(TAG, "------------->getSettingLocale : " + result);
		return result;
	}
	
	public boolean setLanguage(String language) {
		boolean result = false;
		Log.i(TAG, "setLanguage-language="+language);
		
		if(language == null || language.isEmpty())
			return result;
		Locale locale = getSettingLocale(language);
		
		if(locale != null) {
			//need android system compile
			LocalePicker.updateLocale(locale);
		} else 
			result = false;
		return result;
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
		Log.e(TAG, "--------------------> setEthernetIp  : " + ip);
		return result;
	}
	
	public String getEthernetIp() {
		return getLocalIpAddress();
	}
	
	public boolean setVoice(String typeValue) {
		boolean result = false;
		Log.i(TAG, "-------------> setVoice typeValue :  " + typeValue);
		if(typeValue == null && typeValue.isEmpty()) {
			return result;
		}
		
		AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		int type = -1;
		int value = -1;
		
		String[] valueStrings = typeValue.split(":");
		
		if(valueStrings != null && valueStrings.length == 2) {
			for(int i = 0; i < supportAudio.length; ++i) {
				if(valueStrings[0].equals(supportAudio[i][1])) {
					type = Integer.valueOf(supportAudio[i][0]);
					value = Integer.valueOf(valueStrings[1]);
				}
			}
		}
		
		Log.e(TAG, "-----------------> type : " + type + " value : " + value);
		mAudioManager.setStreamVolume(type, value, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
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

	//联系人格式
	//ID:名字:号码+ID:名字:号码
	public boolean setContactData(String value) {
		boolean result = true;
		Log.e(TAG, "-----------------> value " + value);
		
		if(value == null || value.isEmpty())
			return false;
		
		String[] contactNeedChange = value.split("\\+");
		
		if(contactNeedChange == null)
			return false;
		
		for(int i = 0; i < contactNeedChange.length; ++i) {
			String[] contact = contactNeedChange[i].split(":");
			if(contact == null || contact.length != 3)
				return false;
			
			int id = Integer.valueOf(contact[0]);
			String name = contact[1];
			String phoneNumber = contact[2];
			
			Log.e(TAG, "--------------> id : " + id + " name : " + name + " phoneNumber : " + phoneNumber);
			
			Uri dataUri = Uri.parse("content://com.android.contacts/data");
			ContentValues phoneValues = new ContentValues();
			phoneValues.put("data1", phoneNumber);
			mContentResolver.update(dataUri, phoneValues, "mimetype=? and raw_contact_id=?", new String[]{"vnd.android.cursor.item/phone_v2",id+""});
			
			//名字暂时先不做修改
			ContentValues nameValue = new ContentValues();
			nameValue.put("data1", name);
			mContentResolver.update(dataUri, nameValue, "mimetype=? and raw_contact_id=?", new String[]{"vnd.android.cursor.item/name",id+""});
		}
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
