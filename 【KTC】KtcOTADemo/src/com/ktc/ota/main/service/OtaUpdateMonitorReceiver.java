package com.ktc.ota.main.service;

import java.io.File;
import java.io.IOException;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.systemupdate.activitys.NotifyUpdateResultAcitivity;

import com.ktc.ota.utils.DeviceInfoUtil;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;

/**
 * @author Arvin
 * @TODO 静态广播监听器
 * @Date 2019.1.25
 */
public class OtaUpdateMonitorReceiver extends BroadcastReceiver{
    private final static String TAG = "OtaUpdateMonitorReceiver";
    private static final String STR_BOOT_COMPLETED = "android.intent.action.STR_BOOT_COMPLETED";
    
    private Context mContext ;
    
    @Override
    public void onReceive(final Context mContext, Intent intent) {
    	this.mContext = mContext ;
        String action = intent.getAction();
        LogUtil.i(TAG, "action：  " + action);

        switch (action) {
			case Intent.ACTION_BOOT_COMPLETED:
			case STR_BOOT_COMPLETED:
			case ConnectivityManager.CONNECTIVITY_ACTION:
			case Intent.ACTION_MEDIA_MOUNTED:
				final boolean isMonitorRunning = Tools.isServiceWorking(mContext, OtaUpdateMonitorService.class.getName());
				final boolean isKeepAliveRunning = Tools.isServiceWorking(mContext, OtaKeepAliveService.class.getName());
				if(!isMonitorRunning && !isKeepAliveRunning){
					new Handler().postDelayed(new Runnable() {
						
						@Override
						public void run() {
							checkUpdateFlag(mContext);
							Tools.deleteDataOtaFile();
							checkServices(mContext, isMonitorRunning, isKeepAliveRunning);
						}
					}, 3500);
				}else{
					checkServices(mContext, isMonitorRunning, isKeepAliveRunning);
				}
				
				break;
				
			default:
				break;
		}
    }
    
    /**
     * 校验Srvice是否alive
     * @param context
     * @param isMonitorRunning
     * @param isKeepAliveRunning
     */
    private void checkServices(Context context , boolean isMonitorRunning , boolean isKeepAliveRunning){
    	if(!isMonitorRunning){
			LogUtil.i(TAG, "OtaUpdateMonitorService   isNotServiceWorking");
			Intent mIntent = new Intent(context, OtaUpdateMonitorService.class);
			context.startService(mIntent);
    	}else{
    		LogUtil.i(TAG, "OtaUpdateMonitorService   isServiceWorking");
    	}
		
		if(!isKeepAliveRunning){
			LogUtil.i(TAG, "OtaKeepAliveService   isNotServiceWorking");
    		Intent mIntent = new Intent(context, OtaKeepAliveService.class);
    		context.startService(mIntent);
    	}else{
    		LogUtil.i(TAG, "OtaKeepAliveService   isServiceWorking");
    	}
    }

    /**
     * 检测升级状态标志并提示
     * @param mContext
     */
    private void checkUpdateFlag(Context mContext){
		try {
			boolean isOtaUpdateSuccess = Tools.isOtaUpdateSuccess() ;
			if(isOtaUpdateSuccess){
				DeviceInfoUtil.setRequestType("1");
				Intent mIntent = new Intent();
				mIntent.putExtra(OTAConfigConstant.ActionKey.KEY_OTA_UPDATE_FLAG , isOtaUpdateSuccess);
				mIntent.setClass(mContext, NotifyUpdateResultAcitivity.class);
				mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
				mContext.startActivity(mIntent);
				
				reloadUserApps();
			}
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
		
		//开机后删除升级标志位log/删除下载记录
		Tools.deleteRecoveryFlag();
    }
    
    /**
     * OTA升级成功后执行userapp预装
     */
	private void reloadUserApps() {
		LogUtil.i(TAG, "----reloadUserApps--");
		try {
			Runtime.getRuntime().exec("pm clear com.ktc.launcher");
			Runtime.getRuntime().exec("pm clear com.mstar.tv.tvplayer.ui");
			final File userAppDir = new File("/system/preinstall/");
			if (userAppDir != null && userAppDir.exists()
					&& userAppDir.listFiles().length > 0) {
				final File[] apps = userAppDir.listFiles();
				if (apps != null && apps.length > 0) {
					new Thread(new Runnable() {

						@Override
						public void run() {
							if (apps.length > 0) {
								for (File file : apps) {
									String cmd = "pm install -r "
											+ file.getAbsolutePath();
									try {
										Runtime.getRuntime().exec(cmd);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}).start();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
}


