package com.ktc.ota.main.service;

import android.net.ConnectivityManager;
import android.os.IBinder;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.Tools;


/**
 * @author Arvin
 * @TODO 双服务守护下载进程，实现进程保活
 * @Date 2019.2.14
 */
public class OtaKeepAliveService extends Service {
	private static final String TAG = "OtaKeepAliveService";
	private static Context mContext;
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return Service.START_NOT_STICKY;
		}
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		
		checkService();
		registerMonitorReceiver(mContext);
	}
	
	
	/**
	 * @TODO 注册系统时间变化广播接收器(每分钟)
	 * @param context
	 */
	private void registerMonitorReceiver(Context context) {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(Intent.ACTION_TIME_TICK);
		context.registerReceiver(TimeTickReceiver, intentFilter);
		
		IntentFilter intentFilterUsb = new IntentFilter();
        intentFilterUsb.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilterUsb.addDataScheme("file");
        context.registerReceiver(usbMountReceiver, intentFilterUsb);
	}
	
	/**
	 * 定义网络状态变化监听器
	 */
	private BroadcastReceiver TimeTickReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context mContext, Intent mIntent) {
			String mAction = mIntent.getAction() ;
			switch (mAction) {
				case ConnectivityManager.CONNECTIVITY_ACTION:
				case Intent.ACTION_TIME_TICK:
					checkService();
					break;
					
				default:
					break;
			}
		}
		
	};
	
	/**
	 * 定义USB挂载成功广播监听
	 */
	private BroadcastReceiver usbMountReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            	checkService();
            } 
        }
    };
	
	private void checkService(){
		if(!Tools.isServiceWorking(mContext, OtaUpdateMonitorService.class.getName())){
			LogUtil.i(TAG, "OtaUpdateMonitorService   isNotServiceWorking");
    		Intent intent = new Intent(mContext, OtaUpdateMonitorService.class);
            mContext.startService(intent);
    	}else{
    		LogUtil.i(TAG, "OtaUpdateMonitorService   isServiceWorking");
    	}
	}
	
    
	@Override
	public void onDestroy() {
		mContext.unregisterReceiver(TimeTickReceiver);
		mContext.unregisterReceiver(usbMountReceiver);
		super.onDestroy();
	}
	
}