package com.example.searchtestdevice.data;

import java.lang.reflect.Method;
import java.util.Locale;

import android.app.backup.BackupManager;
import android.content.res.Configuration;
import android.util.Log;

public class LanguageUtils {
	public static void updateLanguage(Locale locale) {
		Log.i("TAG", "updateLanguage-locale="+locale);
		try {
			Object objIActMag, objActMagNative;

			Class clzIActMag = Class.forName("android.app.IActivityManager");

			Class clzActMagNative = Class
					.forName("android.app.ActivityManagerNative");

			//amn = ActivityManagerNative.getDefault(); 
			Method mtdActMagNative$getDefault = clzActMagNative
					.getDeclaredMethod("getDefault");

			objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);

			// objIActMag = amn.getConfiguration(); 
			Method mtdIActMag$getConfiguration = clzIActMag
					.getDeclaredMethod("getConfiguration");

			Configuration config = (Configuration) mtdIActMag$getConfiguration
					.invoke(objIActMag);

			// set the locale to the new value 
			config.locale = locale;

			//�־û�  config.userSetLocale = true; 
			Class clzConfig = Class
					.forName("android.content.res.Configuration");
			java.lang.reflect.Field userSetLocale = clzConfig
					.getField("userSetLocale");
			userSetLocale.set(config, true);

			// �˴���Ҫ����Ȩ��:android.permission.CHANGE_CONFIGURATION
			// �����µ��� onCreate();
			Class[] clzParams = { Configuration.class };

			// objIActMag.updateConfiguration(config);
			Method mtdIActMag$updateConfiguration = clzIActMag
					.getDeclaredMethod("updateConfiguration", clzParams);

			mtdIActMag$updateConfiguration.invoke(objIActMag, config);

			BackupManager.dataChanged("com.android.providers.settings");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
