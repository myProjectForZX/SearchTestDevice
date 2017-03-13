package com.example.searchtestdevice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.example.searchtestdevice.data.ContactBean;
import com.example.searchtestdevice.data.InterAddressUtil;
import com.example.searchtestdevice.data.Log;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract.Contacts.Data;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class DeviceSetting {
	private static final String TAG = "deviceSetting";
	private Context mContext;
	private ContentResolver mContentResolver;
	
	public DeviceSetting(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
	}
	
	// ����ϵͳʱ�䣬������ʱ���룬��ҪrootȨ�޲ſ��ԣ���20170304.205000
	public void setSystemTime(String datetimes) {
		// yyyyMMdd.HHmmss��
		try {
			Process process = Runtime.getRuntime().exec("su");
			// String datetime = "20170304.205000"; // ���Ե����õ�ʱ�䡾ʱ���ʽ
			String datetime = ""; // ���Ե����õ�ʱ�䡾ʱ���ʽ
			datetime = datetimes.toString(); // yyyyMMdd.HHmmss��
			DataOutputStream os = new DataOutputStream(
					process.getOutputStream());
			os.writeBytes("setprop persist.sys.timezone GMT\n");
			os.writeBytes("/system/bin/date -s " + datetime + "\n");
			os.writeBytes("clock -w\n");
			os.writeBytes("exit\n");
			os.flush();
		} catch (IOException e) {
			Toast.makeText(mContext, "���ȡRootȨ��", Toast.LENGTH_SHORT).show();
		}
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
	
	public String getLanguage() {
		return null;
	}
	
	public void setEthernetIp(String ip) {
		
	}
	
	public String getEthernetIp() {
		return null;
	}
	
	public void setVoice(int streamType, int streamValue) {
		Log.i(TAG, "-------------> setVoice");
		AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		mAudioManager.setStreamVolume(streamType, streamValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
	}
	
	public String getVoice(int streamType) {
		String result = null;
		Log.i(TAG, "-------------> getVoice");
		AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		int max = mAudioManager.getStreamMaxVolume( streamType );
		int current = mAudioManager.getStreamVolume( streamType );
		
		return result;
	}

	public void updataContactData(String newName, String number) {
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
	}
	
	public void getContactList() {
		Uri uri = Uri.parse("content://com.android.contacts/contacts"); //����raw_contacts��
		Cursor cursorContact = mContentResolver.query(uri, new String[]{Data._ID}, null, null, null);  
		ArrayList<String> mContactList = new ArrayList<String>();
		
		while(cursorContact.moveToNext()){
			ContactBean contactBean = new ContactBean();
			//���id������data��Ѱ������ 
			int id = cursorContact.getInt(0);
			contactBean.setId(id);
			//				buf.append("id="+id);
			uri = Uri.parse("content://com.android.contacts/contacts/"+id+"/data");
			//data1�洢������¼�������ݣ�mimetype��ż�¼�����ͣ���绰��email��
			Cursor cursorData = mContentResolver.query(uri, new String[]{Data.DATA1,Data.MIMETYPE}, null,null, null); 
			while(cursorData.moveToNext()){
				String data = cursorData.getString(cursorData.getColumnIndex("data1"));
				if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/name")){       //���������
					contactBean.setName(data);

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/phone_v2")){  //����ǵ绰
					//						buf.append(",phone="+data);
					contactBean.setPhoneNum(data);
				}/*else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/email_v2")){  //�����email

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/postal-address_v2")){ //����ǵ�ַ

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/organization")){  //�������֯

				}*/

			}
			//mContactList.add(contactBean);
			if(cursorData!=null){
				cursorData.close();
			}
		}
		if(cursorContact!=null){
			cursorContact.close();
		}
	}
	
	//С��10�����ּ�0
	private String getStr(int value) {
		String valueStr;
		if(value<10){
			valueStr ="0"+ String.valueOf(value);
		}else{
			valueStr = String.valueOf(value);
		}
		return valueStr;
	}
}
