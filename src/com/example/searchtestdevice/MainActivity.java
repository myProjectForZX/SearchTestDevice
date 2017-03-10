package com.example.searchtestdevice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.example.searchtestdevice.data.ContactBean;
import com.example.searchtestdevice.data.DataPack;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		mContentResolver=mContext.getContentResolver();
		
		initView();
		initData();
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
				Log.i("TAG", "-onDeviceSearched-�����ߣ�����������" + socketAddr.getAddress().getHostAddress() + ":" + socketAddr.getPort());
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
			case DataPack.DEVICE_FIND:
				if(msg.arg1 == DataPack.DEVICE_CONNECTED) {
					//if connect with host, close deviceWaitSearch
					//adn run tcp 
					deviceWaitingSearch = null;
					waitSearchButton.setEnabled(false);
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.udp_connect_success), Toast.LENGTH_SHORT).show();
				} else if (msg.arg1 == DataPack.DEVICE_NOT_CONNECTED){
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				break;
			}
		}
	}
	
	/**
	 * 1.��ȡ��ϵ�˵������͵绰����-����ok
	 * 2.���ϵͳʱ��-����ok
	 * 3.���ϵͳ����-����ok
	 * 4.�����̫��ip-�����ԣ��������˸�������ò����ҪӲ���н��˲��ܵõ����ݣ�������
	 * 5.���ϵͳ����-����ok
	 */
	private void initData() {
		getContactList();//�����ϵ�˵������͵绰����
		getSystemTime();//���ϵͳʱ��
		getlanguage();//���ϵͳ����
		getEthernetIp();//�����̫��ip
		getVoice();//���ϵͳ����
	}
	
	/**
	 * 1.�޸���ϵ�˵������͵绰����-����ok�����޸�ֻ�ǵ�����ϵ�˵������ͺ���ķ���
	 * 2.�޸�ϵͳʱ��-����ok
	 * 3.�޸�ϵͳ����-�޸����޸��ˣ�֮�������ô����õ�ϵͳ�����Ǹı��ˣ�����û�����ֻ�����������޸���������launcher.apk�����������¸������ԣ�������
	 * 4.�޸���̫��ip-������
	 * 5.�޸�ϵͳ����-����ok
	 */
	private void setData(){
		updataContactData();//�޸ĸ�����ϵ�˵������ͺ���
		setSystemTime(mContext,"20170304.202800");//������ʱ���룬��ҪrootȨ�޲ſ��ԣ�ע���ʽ
		setLanguage(0);//��������
		setEthernetIp();
		setVoice();
	}
	
	private void setEthernetIp() {
		
	}
	
	//����ϵͳʱ�䣬������ʱ���룬��ҪrootȨ�޲ſ��ԣ���20170304.205000
	public static void setSystemTime(final Context cxt, String datetimes) {  
		// yyyyMMdd.HHmmss��  
		/* 
		 * String 
		 * cmd="busybox date  \""+bt_date1.getText().toString()+" "+bt_time1 
		 * .getText().toString()+"\""; String cmd2="busybox hwclock  -w"; 
		 */  
		try {  
			Process process = Runtime.getRuntime().exec("su");  
			//          String datetime = "20170304.205000"; // ���Ե����õ�ʱ�䡾ʱ���ʽ  
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
			Toast.makeText(cxt, "���ȡRootȨ��", Toast.LENGTH_SHORT).show();  
		}  
	}
	
	private void setVoice() {
		Log.i("TAG", "setVoice");
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		//ͨ������
		int callVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, callVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//ϵͳ����
		int systemVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//��������
		int ringVolumeValue = 0;
		mAudioManager.setStreamVolume( AudioManager.STREAM_RING, ringVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//��������
		int musicVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//��ʾ��������
		int alarmVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,alarmVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
	}
	
	int ZH=0;
	int EN=1;
	private void setLanguage(int language) {
		Log.i("TAG", "setLanguage-language="+language);
		try {
			Configuration config = getResources().getConfiguration();
			DisplayMetrics dm = getResources().getDisplayMetrics();
			if (language == ZH) {
				config.locale = Locale.ENGLISH;
			} else {
				config.locale = Locale.SIMPLIFIED_CHINESE;
			}
			getResources().updateConfiguration(config, dm);
			SharedPreferences sp = getSharedPreferences("userinfo", 0);
			SharedPreferences.Editor editor=sp.edit();
			editor.putInt("locale", language);
			editor.commit();
			//            LanguageUtils.updateLanguage(config.locale);//ʵ��û�����ã�������ҪԴ���±��룬����ͨ��Դ�������ȥʵ��
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updataContactData() {
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
	
	private void getVoice() {
		Log.i("TAG", "getvoice");
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//ͨ������
		int max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_VOICE_CALL );
		int current = mAudioManager.getStreamVolume( AudioManager.STREAM_VOICE_CALL );
		Log.i("TAG", "ͨ������max="+max+",current="+current);

		//ϵͳ����
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_SYSTEM );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_SYSTEM );
		Log.i("TAG", "ϵͳ����max="+max+",current="+current);

		//��������
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_RING );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_RING );
		Log.i("TAG", "��������max="+max+",current="+current);

		//��������
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
		Log.i("TAG", "��������max="+max+",current="+current);

		//��ʾ��������
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_ALARM );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_ALARM );
		Log.i("TAG", "��ʾ��������max="+max+",current="+current);
	}
	
	//��̫��ip��ַ��ҪӲ��������̫���ſ��Բ���
	private void getEthernetIp() {
		String macAddress=InterAddressUtil.getMacAddress();
		Log.i("TAG", "macAddress="+macAddress);
	}
	
	private void getlanguage() {
		Locale locale = getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		Log.i("TAG", "getlanguage-language="+language);
	}
	
	private void getSystemTime() {
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH)+1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		int second = c.get(Calendar.SECOND);
		StringBuffer sb = new StringBuffer();
		sb.append(year);
		String monthStr = getStr(month);
		String dayStr=	getStr(day);
		String hourStr=	getStr(hour);
		String minuteStr=	getStr(minute);
		String secondStr=	getStr(second);

		sb.append(monthStr);
		sb.append(dayStr) ;
		sb.append(hourStr);
		sb.append(minuteStr);
		sb.append(secondStr);
		Log.i("TAG", "sb="+sb);

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
	
	private void getContactList() {
		Uri uri = Uri.parse("content://com.android.contacts/contacts"); //����raw_contacts��
		Cursor cursorContact = mContentResolver.query(uri, new String[]{Data._ID}, null, null, null);  
		mContactList=new ArrayList<ContactBean>();
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
			mContactList.add(contactBean);
			if(cursorData!=null){
				cursorData.close();
			}
		}
		if(cursorContact!=null){
			cursorContact.close();
		}
		Log.i("TAG", "mContactList="+mContactList.toString());
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
