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
					new DeviceSendAndRecvDataThread(mHandler, mDeviceIp).start();
				} else if (msg.arg1 == DataPackDevice.DEVICE_NOT_CONNECTED){
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				break;
			}
		}
	}
	
	/**
	 * 1.获取联系人的姓名和电话号码-调试ok
	 * 2.获得系统时间-调试ok
	 * 3.获得系统语言-调试ok
	 * 4.获得以太网ip-待调试，网上搜了个方法，貌似需要硬件有接了才能得到数据，待调试
	 * 5.获得系统音量-调试ok
	 */
	private void initData() {
		getContactList();//获得联系人的姓名和电话号码
		getSystemTime();//获得系统时间
		getlanguage();//获得系统语言
		getEthernetIp();//获得以太网ip
		getVoice();//获得系统音量
	}
	
	/**
	 * 1.修改联系人的姓名和电话号码-调试ok，此修改只是单个联系人的姓名和号码的方法
	 * 2.修改系统时间-调试ok
	 * 3.修改系统语言-修改是修改了，之后重新用代码获得的系统语言是改变了，但是没有像手机里面的设置修改语言那样launcher.apk等其他的重新更新语言，待调试
	 * 4.修改以太网ip-待调试
	 * 5.修改系统音量-调试ok
	 */
	private void setData(){
		updataContactData();//修改更新联系人的姓名和号码
		setSystemTime(mContext,"20170304.202800");//年月日时分秒，需要root权限才可以，注意格式
		setLanguage(0);//设置语言
		setEthernetIp();
		setVoice();
	}
	
	private void setEthernetIp() {
		
	}
	
	//设置系统时间，年月日时分秒，需要root权限才可以，如20170304.205000
	public static void setSystemTime(final Context cxt, String datetimes) {  
		// yyyyMMdd.HHmmss】  
		/* 
		 * String 
		 * cmd="busybox date  \""+bt_date1.getText().toString()+" "+bt_time1 
		 * .getText().toString()+"\""; String cmd2="busybox hwclock  -w"; 
		 */  
		try {  
			Process process = Runtime.getRuntime().exec("su");  
			//          String datetime = "20170304.205000"; // 测试的设置的时间【时间格式  
			String datetime = ""; // 测试的设置的时间【时间格式  
			datetime = datetimes.toString(); // yyyyMMdd.HHmmss】  
			DataOutputStream os = new DataOutputStream(  
					process.getOutputStream());  
			os.writeBytes("setprop persist.sys.timezone GMT\n");  
			os.writeBytes("/system/bin/date -s " + datetime + "\n");  
			os.writeBytes("clock -w\n");  
			os.writeBytes("exit\n");  
			os.flush();  
		} catch (IOException e) {  
			Toast.makeText(cxt, "请获取Root权限", Toast.LENGTH_SHORT).show();  
		}  
	}
	
	private void setVoice() {
		Log.i("TAG", "setVoice");
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		//通话音量
		int callVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, callVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//系统音量
		int systemVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//铃声音量
		int ringVolumeValue = 0;
		mAudioManager.setStreamVolume( AudioManager.STREAM_RING, ringVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//音乐音量
		int musicVolumeValue = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolumeValue, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);

		//提示声音音量
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
			//            LanguageUtils.updateLanguage(config.locale);//实际没起作用，可能需要源码下编译，或者通过源码的设置去实现
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updataContactData() {
		int id = 1;
		String phone = "2222";
		String name = "www";
		Uri dataUri = Uri.parse("content://com.android.contacts/data");//对data表的所有数据操作
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
		//通话音量
		int max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_VOICE_CALL );
		int current = mAudioManager.getStreamVolume( AudioManager.STREAM_VOICE_CALL );
		Log.i("TAG", "通话音量max="+max+",current="+current);

		//系统音量
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_SYSTEM );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_SYSTEM );
		Log.i("TAG", "系统音量max="+max+",current="+current);

		//铃声音量
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_RING );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_RING );
		Log.i("TAG", "铃声音量max="+max+",current="+current);

		//音乐音量
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
		Log.i("TAG", "音乐音量max="+max+",current="+current);

		//提示声音音量
		max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_ALARM );
		current = mAudioManager.getStreamVolume( AudioManager.STREAM_ALARM );
		Log.i("TAG", "提示声音音量max="+max+",current="+current);
	}
	
	//以太网ip地址需要硬件接入以太网才可以测试
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
	
	//小于10的数字加0
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
		Uri uri = Uri.parse("content://com.android.contacts/contacts"); //访问raw_contacts表
		Cursor cursorContact = mContentResolver.query(uri, new String[]{Data._ID}, null, null, null);  
		mContactList=new ArrayList<ContactBean>();
		while(cursorContact.moveToNext()){
			ContactBean contactBean = new ContactBean();
			//获得id并且在data中寻找数据 
			int id = cursorContact.getInt(0);
			contactBean.setId(id);
			//				buf.append("id="+id);
			uri = Uri.parse("content://com.android.contacts/contacts/"+id+"/data");
			//data1存储各个记录的总数据，mimetype存放记录的类型，如电话、email等
			Cursor cursorData = mContentResolver.query(uri, new String[]{Data.DATA1,Data.MIMETYPE}, null,null, null); 
			while(cursorData.moveToNext()){
				String data = cursorData.getString(cursorData.getColumnIndex("data1"));
				if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/name")){       //如果是名字
					contactBean.setName(data);

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/phone_v2")){  //如果是电话
					//						buf.append(",phone="+data);
					contactBean.setPhoneNum(data);
				}/*else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/email_v2")){  //如果是email

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/postal-address_v2")){ //如果是地址

				}else if(cursorData.getString(cursorData.getColumnIndex("mimetype")).equals("vnd.android.cursor.item/organization")){  //如果是组织

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
