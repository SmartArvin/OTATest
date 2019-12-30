package com.ktc.ota.main.service;

import java.util.Calendar;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.systemupdate.activitys.NotifyForceUpdateActivity;
import android.systemupdate.activitys.NotifyInstallAndRebootActivity;
import android.systemupdate.activitys.NotifyLocalUpdateActivity;
import android.systemupdate.activitys.NotifyNoSpaceActivity;
import android.systemupdate.activitys.NotifyRemoteUpdateActivity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import com.ktc.ota.bean.DiskData;
import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.download.DownloadInfo;
import com.ktc.ota.download.DownloadService;
import com.ktc.ota.download.OtaDataRequestThread;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.DeviceInfoUtil;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;


/**
 * @author Arvin
 * @TODO 监听各类广播执行升级检测
 * @Date 2019.2.14
 */
public class OtaUpdateMonitorService extends Service {
	private static final String TAG = "OtaUpdateMonitorService";
	private static Context mContext;
	private OtaRemoteData mOtaRemoteData ;
	private boolean downloadAgain = true ;
	
	private DownloadService mDownloadService;
    private DownloadService.DownloadBinder mDownloadBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        	LogUtil.i(TAG , "----onServiceConnected----");
            mDownloadBinder = (DownloadService.DownloadBinder) service;
            
            mDownloadService = mDownloadBinder.getServiceSelf();
            mDownloadService.registerCallBack(mDownloadCallBack);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	if (mDownloadService != null) {
        		mDownloadService.unRegisterCallBack(mDownloadCallBack);
            }
        	LogUtil.i(TAG, "警告：服务器连接断开!");
        }
    };
	
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
		
		bindDownloadService();
		checkOtaUpdateFile();
		registerMonitorReceiver(mContext);
	}
	
	/**
	 * 检测本地和远程OTA升级包状态
	 * 无本地升级文件则检测远程网络升级
	 */
	private void checkOtaUpdateFile(){
		if(!checkLocalOtaUpdate()){
			startCheckRemoteOtaUpdate();
		}
	}

	/**
	 * 检测是否存在本地升级文件(如果当前已处于OTA升级界面，则不提示)
	 * @return boolean
	 */
	private boolean checkLocalOtaUpdate(){
		try {
			if(/*!Tools.isHomeKeyEnable(mContext) 
					|| */Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
    			return false;
    		}
			
			DiskData mUsbDisk = Tools.getFirstUsbDisk(mContext);
			if(mUsbDisk != null){
				LogUtil.i(TAG, "------checkLocalOtaUpdate:  "+mUsbDisk.getPath());
				LogUtil.i(TAG, "------checkLocalOtaUpdate_getFileSize:  "+Tools.getFileSize(mUsbDisk.getPath() + OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME));
			}
			
	    	if(mUsbDisk != null && Tools.getFileSize(mUsbDisk.getPath() + OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME) > 0){
	    		Intent mIntent = new Intent();
				mIntent.setClass(mContext, NotifyLocalUpdateActivity.class);
				mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
				mContext.startActivity(mIntent);
	    		return true;
	    	}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	/**
	 * @TODO 启动新线程检测远程网络升级配置
	 * @param null
	 */
	private void startCheckRemoteOtaUpdate() {
		if(Tools.isNetWorkConnected(mContext)){
			if(((getDownloadStatus() != OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING) 
					&& (getDownloadStatus() != OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING))){
				try {
					callBackhandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_ING);
					HandlerThread handlerThread = new HandlerThread("ServiceRemoteOTA");
					handlerThread.start();
					Handler HandlerUpdata = new Handler(handlerThread.getLooper());
					HandlerUpdata.post(new OtaDataRequestThread(mContext , DeviceInfoUtil.getRemoteUris(mContext) , callBackhandler));
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}else{
    		callBackhandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET);
    		if(mDownloadBinder != null){
    			mDownloadBinder.pauseDownload();
    			mDownloadBinder.cancelVerify() ;
    		}
    		
    		Tools.switchIconState(mContext, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL) ;
    	}
	}
	
	/**
	 * 获取当前下载或校验状态
	 * @return int
	 */
	private int getDownloadStatus(){
		int mDownloadStatus = -1 ;
		if(mDownloadBinder != null){
			mDownloadStatus = mDownloadBinder.getDownloadStatus() ;
		}
		LogUtil.i(TAG, "mDownloadStatus:  "+mDownloadStatus);
		return mDownloadStatus ;
	}
	
	Handler callBackhandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			try {
				LogUtil.i(TAG, "callBackhandler:  "+msg.what);
				switch (msg.what) {
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_ING:
						//正在请求远程OTA配置中
						LogUtil.i(TAG, "提示：网络已连接，正在启动升级检测！");
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_SUCCESS:
						//获取已请求的OTA升级包信息;打开升级提示弹窗
						try {
							Bundle mBundle = msg.getData() ;
							mOtaRemoteData = null ;
							if(mBundle != null){
								mOtaRemoteData = (OtaRemoteData) mBundle.getParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA);
								if(mOtaRemoteData != null){
									LogUtil.i(TAG, "getOtaForceFlag:  "+mOtaRemoteData.getOtaForceFlag());
									if(mOtaRemoteData.getOtaForceFlag().equals("0")){//normal update
										if(Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
											if(getDownloadStatus() == OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED
													|| getDownloadStatus() == OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED){
												startDownloadOta();
											}
							    		}else /*if(Tools.isHomeKeyEnable(mContext))*/{
							    			int tipCount = Integer.parseInt(DeviceInfoUtil.getRemoteTipCount());
											LogUtil.i(TAG, "tipCount:  "+tipCount);
											if(tipCount < OTAConfigConstant.CONFIG.MAX_TIP_COUNT){
												Intent mIntent = new Intent();
												mIntent.setClass(mContext , NotifyRemoteUpdateActivity.class);
												mIntent.putExtras(msg.getData());
												mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
												mContext.startActivity(mIntent);
												
												DeviceInfoUtil.setRemoteTipCount(String.valueOf(tipCount + 1)) ;
											}
							    		}
										mOtaRemoteData = null;
									}else if(mOtaRemoteData.getOtaForceFlag().equals("1")){//force update;when download over to tip
										startDownloadOta();
									}
								}else{
									sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
								}
							}
						} catch (Exception e) {
							LogUtil.i(TAG, "e:  "+e.toString());
							sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
						}
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_EXCEPTION:
						//网络请求失败
						break ;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE:
						//远程无OTA升级文件
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET:
						//无网络
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_SPACE:
						//本地data空间不足,打开空间不足提示
						Bundle mBundle1 = msg.getData();
						OtaRemoteData mOtaRemoteData1 = (OtaRemoteData) mBundle1.getParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA);
						
						Intent mIntent1 = new Intent();
						mIntent1.setClass(mContext , NotifyNoSpaceActivity.class);
						mIntent1.putExtra(OTAConfigConstant.ActionKey.KEY_OTA_INFO , Long.parseLong(mOtaRemoteData1.getOtaPkgLength()));
						mIntent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
						mContext.startActivity(mIntent1);
						break;
					default:
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
    };
	
	/**
	 * @TODO 注册网络状态变化及USB挂载状态变化广播接收器
	 * @param context
	 */
	private void registerMonitorReceiver(Context context) {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(Intent.ACTION_TIME_TICK);
		context.registerReceiver(otaNetMonitorReceiver, intentFilter);
		
		IntentFilter intentFilterUsb = new IntentFilter();
        intentFilterUsb.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilterUsb.addDataScheme("file");
        context.registerReceiver(usbMountReceiver, intentFilterUsb);
	}
	
	/**
	 * 定义网络状态变化监听器
	 */
	private BroadcastReceiver otaNetMonitorReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context mContext, Intent mIntent) {
			String mAction = mIntent.getAction() ;
			LogUtil.i(TAG, "mAction:  "+mAction);
			switch (mAction) {
				case ConnectivityManager.CONNECTIVITY_ACTION:
					if(!Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
						OtaUpdateApplication.getInstance().exit();
					}
					startCheckRemoteOtaUpdate();
					break;
				case Intent.ACTION_TIME_TICK://时钟侦听
					try {
						if(canResetCount()){
							DeviceInfoUtil.setRemoteTipCount("0");
						}
						checkOtaKeepAlive();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
	
				default:
					break;
			}
		}
		
	};
	

	/**
	 * 双进程守护服务，实现service保活
	 * @param void
	 */
	private void checkOtaKeepAlive(){
		if(!Tools.isServiceWorking(mContext, OtaKeepAliveService.class.getName())){
			LogUtil.i(TAG, "OtaKeepAliveService   isNotServiceWorking");
			
    		Intent intent = new Intent(mContext, OtaKeepAliveService.class);
            mContext.startService(intent);
    	}else{
    		LogUtil.i(TAG, "OtaKeepAliveService   isServiceWorking");
    	}
	}
	
	/**
	 * 当前日期为5:10:15:20:25:30时重置提示次数，重新提示用户升级
	 * @return boolean
	 */
	private static boolean canResetCount() {
		Calendar mCalendar = Calendar.getInstance();
		int day = mCalendar.get(Calendar.DAY_OF_MONTH);
		LogUtil.i(TAG, "canResetCount__day:  (day % 5)  "+(day % 5));
		if((day % 5) == 0){//5:10:15:20:25:30
			return true ;
		}
		
		return false ;
	}
	
	/**
	 * 定义USB挂载成功广播监听
	 */
	private BroadcastReceiver usbMountReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            	checkLocalOtaUpdate();
            } 
        }
    };
    
	/**
     * @TODO 绑定DownloadService
     * @param null
     */
    private void bindDownloadService(){
		Intent mIntent = new Intent(mContext , DownloadService.class);
		if(!Tools.isServiceWorking(mContext, DownloadService.class.getName())){
			startService(mIntent);
		}
        bindService(mIntent , mServiceConnection, Context.BIND_AUTO_CREATE);
	}

    /**
     * @TODO DownloadService信息回传接口(含下载状态、校验状态)
     * @param null
     */
	private DownloadService.CallBack mDownloadCallBack = new DownloadService.CallBack() {

		@Override
		public void postDownloadInfo(DownloadInfo mDownloadInfo) {
			int workState = mDownloadInfo.getDledStatusFlag() ;
			LogUtil.i(TAG, "mDownloadCallBack:  "+workState);
			switch (workState) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING:
					if(isForceUpdate()){
						downloadAgain = true ;
						LogUtil.i(TAG , "强制升级---下载进度：  "+mDownloadInfo.getDledProgress());
					}
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 自动下载成功！");
					}
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 自动下载暂停！");
					}
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED:
					if(isForceUpdate()){//再次请求下载
						LogUtil.i(TAG , "强制升级--- 自动下载失败！");
						if(downloadAgain){
							startDownloadOta();
							downloadAgain = false ;
						}
					}
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 自动下载取消！");
						Tools.deleteDataOtaFile();
					}
					break;
				default:
					break;
			}
		}

		@Override
		public void postDownloadProgress(int mProgress) {
		}

		@Override
		public void postVerifyProgress(int mVerifyProgress , int mVerifyState) {
			LogUtil.i(TAG, "mVerifyState:  "+mVerifyState);
			switch (mVerifyState) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 升级包校验中：  "+mVerifyProgress);
					}
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
					//提示倒计时升级
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 升级包校验成功! ");
						if(!Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
							LogUtil.i(TAG , "启动NotifyForceUpdateActivity");
							Intent mIntent = new Intent();
					    	mIntent.setClass(mContext, NotifyForceUpdateActivity.class);
					    	mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
					    	startActivity(mIntent);
					    	mOtaRemoteData = null;
						}else{
							if(mDownloadBinder != null){
								LogUtil.i(TAG , "强制升级---installDataOtaPackage");
								mDownloadBinder.installDataOtaPackage();
								mOtaRemoteData = null;
							}
						}
					}else if(!Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
						//下载完成，是否立即升级？
						Intent mIntent = new Intent();
				    	mIntent.setClass(mContext, NotifyInstallAndRebootActivity.class);
				    	mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ;
				    	startActivity(mIntent);
				    	mOtaRemoteData = null;
					}
					break;
							
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 升级包校验停止! ");
					}
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
					if(isForceUpdate()){
						LogUtil.i(TAG , "强制升级--- 升级包校验失败! ");
					}
					break;
				default:
					break;
			}
		}
    };
    
    
	/**
	 * @TODO 启动远程OTA升级包下载
	 * 说明：获取已下载进度
	 * 等于0时重新下载；
	 * 大于0且小于线上包时判断版本是否一致，一致则继续下载，否则删除旧版本重新下载新版本；
	 * 等于线上包大小时判断版本是否一致，一致则开始校验，否则删除旧版本重新下载新版本；
	 * 大于线上包时删除旧版本重新下载新版本
	 */
	private void startDownloadOta(){
		if(mDownloadBinder != null && mOtaRemoteData != null){
			long dataOtaLength = Tools.getDataOtaFileLength(mOtaRemoteData);
			long mOtaPkgLength = Long.parseLong(mOtaRemoteData.getOtaPkgLength()) ;
			LogUtil.i(TAG, "dataOtaLength:  "+dataOtaLength);
			LogUtil.i(TAG, "getOtaPkgLength:  "+Long.parseLong(mOtaRemoteData.getOtaPkgLength()));
			
			if(dataOtaLength < mOtaPkgLength){
				if(dataOtaLength <= 0){
					mDownloadBinder.startDownload(mOtaRemoteData);
				}else{
					if(Tools.getDataOtaFile().getName().contains(mOtaRemoteData.getOtaPkgVersion())){
						mDownloadBinder.startDownload(mOtaRemoteData);
					}else{
						Tools.deleteDataOtaFile();
						mDownloadBinder.startDownload(mOtaRemoteData);
					}
				}
			}else if(dataOtaLength == mOtaPkgLength){
				if(Tools.getDataOtaFile().getName().contains(mOtaRemoteData.getOtaPkgVersion())){
					startVerify();
				}else{
					Tools.deleteDataOtaFile();
					mDownloadBinder.startDownload(mOtaRemoteData);
				}
			}else{
				Tools.deleteDataOtaFile();
				mDownloadBinder.startDownload(mOtaRemoteData);
			}
		}
	}
	
	/**
     * @TODO 开始或暂停OTA升级包校验
     */
    private void startVerify() {
    	if(mDownloadBinder != null){
			mDownloadBinder.startVerify();
		}
	}
	
    /**
     * 是否为强制升级
     * @return
     */
    private boolean isForceUpdate(){
    	if(mOtaRemoteData != null && mOtaRemoteData.getOtaForceFlag().equals("1")){
    		return true ;
    	}
    	return false ;
    }
    
	@Override
	public void onDestroy() {
		mContext.unregisterReceiver(otaNetMonitorReceiver);
		unbindService(mServiceConnection);
		if (mDownloadService != null) {
    		mDownloadService.unRegisterCallBack(mDownloadCallBack);
        }
		super.onDestroy();
	}
	
}