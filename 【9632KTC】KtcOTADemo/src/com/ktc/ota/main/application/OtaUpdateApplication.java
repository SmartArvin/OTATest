package com.ktc.ota.main.application;

import java.util.LinkedList;
import java.util.List;
import com.ktc.ota.utils.LogUtil;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

/**
 * @author Arvin
 * @TODO OTA升级应用主入口
 * @Date 2019.1.24
 */
public class OtaUpdateApplication extends Application {

	private static final String TAG = "OtaUpdateApplication" ;
	private static OtaUpdateApplication instance;
	private static Context mContext;

	private List<Activity> activityList = new LinkedList();

	public static Context getContext() {
		return mContext;
	}

	public static OtaUpdateApplication getInstance() {
		if (null == instance) {
			instance = new OtaUpdateApplication();
		}
		return instance;

	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
	}

	/**
	 * 将Activity添加至Activity列表中
	 * @param activity
	 */
	public boolean addActivity(Activity activity) {
		return activityList.add(activity);
	}


	/**
	 * 遍历所有Activity并finish
	 * @param null
	 */
	public void exit() {
		for (Activity activity : activityList) {
			if (activity != null) {
				activity.finish();
			}
		}
		//System.exit(0); // 关闭JVM
	}
}