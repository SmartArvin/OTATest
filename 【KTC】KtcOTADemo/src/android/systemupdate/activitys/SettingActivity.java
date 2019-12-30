package android.systemupdate.activitys;

import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.download.DownloadInfo;
import com.ktc.ota.download.DownloadService;
import com.ktc.ota.download.OtaDataRequestThread;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.DeviceInfoUtil;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;
import com.ktc.ota.views.MarqueeTextView;
import com.ktc.ota.views.RoundProgressBar;

import android.systemupdate.service.R;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;


/**
 * @author Arvin
 * @TODO 进入该页面不检测USB本地升级，直接检测网络升级配置
 * @Date 2019.1.24
 * 
 */
public class SettingActivity extends Activity {
	private static final String TAG = "SettingActivity";
	private Context mContext;
	
	private TextView txt_title ;
	private MarqueeTextView txt_cur_version_name , txt_cur_date_size;
    
	private ProgressBar progress_check ;
	private TextView txt_info_status ;
	
	private LinearLayout ly_check_btn;
	private RoundProgressBar progress_update; 
	private Button btn_cancel , btn_close;
	
	private OtaRemoteData mOtaRemoteData ;
	private int workState = -1;
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_check);
		OtaUpdateApplication.getInstance().addActivity(this);
		
		mContext = this ;
		
		findViews();
		registerNetMonitorReceiver();
	}
	
	private void findViews() {
    	txt_title = (TextView) findViewById(R.id.remote_update_title_txt);
    	txt_cur_version_name = (MarqueeTextView) findViewById(R.id.remote_update_info_version_name);
    	txt_cur_date_size = (MarqueeTextView) findViewById(R.id.remote_update_info_date_size);
		
		progress_check = (ProgressBar) findViewById(R.id.remote_update_check_progress);
		txt_info_status = (TextView) findViewById(R.id.remote_update_status_info);
		
		ly_check_btn = (LinearLayout) findViewById(R.id.remote_update_btn_rl);
		btn_cancel = (Button) findViewById(R.id.remote_update_btn_cancel);
		progress_update = (RoundProgressBar) findViewById(R.id.remote_update_progress);
		btn_close = (Button) findViewById(R.id.remote_update_btn_close);
		
		updateTitleInfo(false);
		
		setOnClickListener();
    }
	
	private void updateTitleInfo(boolean hasRemote){
		if(hasRemote && mOtaRemoteData != null){
			txt_cur_version_name.setText(getString(R.string.tip_update_zip_version) + mOtaRemoteData.getOtaPkgVersion());
			txt_cur_date_size.setText(getString(R.string.tip_update_zip_length) + Tools.formatSize(Long.parseLong(mOtaRemoteData.getOtaPkgLength())));
		}else{
			txt_cur_version_name.setText(getString(R.string.tip_system_version) + DeviceInfoUtil.getSystemVersion());
			txt_cur_date_size.setText(getString(R.string.tip_system_builddate) + Tools.formatUnixTime(DeviceInfoUtil.getSystemBuildTime()));	
		}
	}
	
	 private void setOnClickListener(){
	    	btn_cancel.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View view) {
					LogUtil.i(TAG, "btn_cancel----workState:  "+workState);
					switch (workState) {
					//################   Check	#####################
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_SUCCESS:
						if(mOtaRemoteData != null){
							startDownloadOta();
						}else{
							exit();
						}
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_EXCEPTION:
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE:
						startRemoteCheckThread();
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET:
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_SPACE:
						exit();
						break;
						
					//################   Download	##################### 
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING:
						pauseDownloadOta() ;
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
						startVerify();
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
						startDownloadOta();
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED:
						startDownloadOta();
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
						startDownloadOta();
						break;
						
					//varify
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING :
						cancelVerify();
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
						//不做动作，自动升级
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP :
						startVerify();
						break ;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
						startVerify();
						break ;
						
					default:
						break;
					}
				}
			});
	    	
	    	btn_close.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View view) {
					LogUtil.i(TAG, "btn_close----workState:  "+workState);
					switch (workState) {//刷新UI
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING:
							exit();
							break;
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED:
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
							cancelDownloadOta();
							Tools.deleteDataOtaFile();
							exit();
							break;
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING :
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP :
						case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
							cancelVerify();
							Tools.deleteDataOtaFile();
							exit();
							break ;
						default:
							exit();
							break;
					}
				}
			});
	    }
	
	@Override
	protected void onResume() {
		super.onResume();
		Tools.changeToStorage();
		resetProgressBar() ;
		startRemoteCheckThread();
		
		bindDownloadService();
	}
	
	/**
     * @TODO 重置进度条状态
     * @param null
     */
    private void resetProgressBar(){
    	progress_update.setProgress(0);
    	progress_update.setMax(100);
    	progress_update.invalidate();
    }
	
	Handler UiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			try {
				workState = msg.what ;
				LogUtil.i(TAG, "UiHandler_workState：  "+workState);
				switch (workState) {
				//########################  Check   ############################
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_ING:
						updateTitleInfo(false);
						
						progress_check.setVisibility(View.VISIBLE);
						txt_info_status.setText(getString(R.string.tip_update_checking));
						
						ly_check_btn.setVisibility(View.INVISIBLE);
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_SUCCESS:
						mOtaRemoteData = null ;
						Bundle mBundle = msg.getData();
						mOtaRemoteData = (OtaRemoteData) mBundle.getParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA);
						if(mOtaRemoteData != null && !TextUtils.isEmpty(mOtaRemoteData.getOtaPkgVersion())){
							updateTitleInfo(true);
							if(Tools.hasDataOtaFile(mOtaRemoteData) && Tools.isTopCurRunningPackage(new String[]{SettingActivity.class.getName()})){
								//从暂停状态恢复
								startDownloadOta();
							}else{
								progress_check.setVisibility(View.GONE);
								txt_info_status.setText(getString(R.string.tip_remote_find_newversion));
								resetProgressBar();
								
								btn_cancel.setText(getString(R.string.str_btn_update_now));
								btn_close.setText(getString(R.string.str_btn_update_notyet));
								btn_cancel.requestFocus();
								ly_check_btn.setVisibility(View.VISIBLE);
							}
						}else{
							sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
						}
						
						if(!Tools.isServiceWorking(mContext , DownloadService.class.getName())){
							Intent mIntent = new Intent(mContext , DownloadService.class);
				            startService(mIntent);
						}
						
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_EXCEPTION:
						updateTitleInfo(false);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_check_fail));
						resetProgressBar();
						
						btn_cancel.setText(getString(R.string.str_btn_check_again));
						btn_close.setText(getString(R.string.str_btn_close));
						btn_cancel.requestFocus();
						ly_check_btn.setVisibility(View.VISIBLE);
						break ;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE:
						updateTitleInfo(false);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_version_latest));
						resetProgressBar();
						
						btn_cancel.setText(getString(R.string.str_btn_check_again));
						btn_close.setText(getString(R.string.str_btn_close));
						btn_cancel.requestFocus();
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET:
						updateTitleInfo(false);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_version_latest));
						resetProgressBar();
						
						btn_cancel.setText(getString(R.string.str_btn_update_cancel));
						btn_close.setText(getString(R.string.str_btn_close));
						btn_cancel.requestFocus();
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
					case OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_SPACE:
						Bundle mBundle1 = msg.getData();
						OtaRemoteData mOtaRemoteData1 = (OtaRemoteData) mBundle1.getParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA);
						String tipMsg = getString(R.string.title_nospace_notify);
						if(mOtaRemoteData1 != null){
							tipMsg = String.format(getString(R.string.tip_update_nospace), 
									Tools.formatSize(Long.parseLong(mOtaRemoteData1.getOtaPkgLength())),
									Tools.formatSize(Tools.getDataFreeSize()));
						}
						updateTitleInfo(false);
						
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(tipMsg);
						resetProgressBar();
						
						btn_cancel.setText(getString(R.string.str_btn_update_cancel));
						btn_close.setText(getString(R.string.str_btn_close));
						btn_cancel.requestFocus();
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
						
						
					//######################### Download #############################	
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING:
						DownloadInfo mDownloadInfo = (DownloadInfo) msg.getData().getSerializable(OTAConfigConstant.ActionKey.KEY_DOWNLOAD_INFO);
						downloadAgain = true ;
						
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_downloaded) + Tools.formatSize(mDownloadInfo.getDledLength()));
						progress_update.setProgress(mDownloadInfo.getDledProgress());
						if(mDownloadInfo.getDledProgress() == 1){
							updateTitleInfo(true);
						}
						
						btn_cancel.setText(getString(R.string.str_btn_download_pause));
						btn_close.setText(getString(R.string.str_btn_download_bg));
						if(!btn_cancel.hasFocus() && !btn_close.hasFocus()){
							btn_cancel.requestFocus();
						}
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_download_success));
						
						btn_cancel.setText(getString(R.string.str_btn_update_now));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
						DownloadInfo mDownloadInfo1 = (DownloadInfo) msg.getData().getSerializable(OTAConfigConstant.ActionKey.KEY_DOWNLOAD_INFO);
						
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						if(mDownloadInfo1 != null && mDownloadInfo1.getDledLength() > 0){
							txt_info_status.setText(getString(R.string.tip_download_pause) + Tools.formatSize(mDownloadInfo1.getDledLength()));
						}else{
							txt_info_status.setText(getString(R.string.tip_download_pause) + Tools.formatSize(Tools.getFileSize(Tools.getDataOtaFile())));
						}
						
						btn_cancel.setText(getString(R.string.str_btn_download_goon));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED://同步检测一下网络状态
						cancelDownloadOta();
						
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						if(!Tools.isNetWorkConnected(mContext)){
							UiHandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET) ;
						}else{
							if(downloadAgain){
								startDownloadOta();
								downloadAgain = false ;
							}else{
								txt_info_status.setText(getString(R.string.tip_download_fail));
								btn_cancel.setText(getString(R.string.str_btn_download_again));
								btn_close.setText(getString(R.string.str_btn_update_notyet));
								ly_check_btn.setVisibility(View.VISIBLE);
							}
						}
						break;
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
						cancelDownloadOta();
						Tools.deleteDataOtaFile();
						
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_download_stop));
						
						btn_cancel.setText(getString(R.string.str_btn_download_again));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
						
					//######################### Verify #############################		
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING:
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_verifying));
						progress_update.setProgress(msg.arg1);
						if(msg.arg1 == 1){
							updateTitleInfo(true);
						}
						
						btn_cancel.setText(getString(R.string.str_btn_verify_cancle));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						if(!btn_cancel.hasFocus() && !btn_close.hasFocus()){
							btn_cancel.requestFocus();
						}
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
						
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_verify_success));
						
						ly_check_btn.setVisibility(View.INVISIBLE);
						installDataOtaPackage();
						break;
								
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_verify_stop));
						progress_update.setProgress(msg.arg1);
						
						btn_cancel.setText(getString(R.string.str_btn_verify_again));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						ly_check_btn.setVisibility(View.VISIBLE);
						break;
						
					case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
						updateTitleInfo(true);
						progress_check.setVisibility(View.GONE);
						txt_info_status.setText(getString(R.string.tip_update_verify_fail));
						progress_update.setProgress(msg.arg1);
						
						btn_cancel.setText(getString(R.string.str_btn_verify_again));
						btn_close.setText(getString(R.string.str_btn_update_notyet));
						ly_check_btn.setVisibility(View.VISIBLE);
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
    
    public void startRemoteCheckThread() {
		if(Tools.isNetWorkConnected(mContext)){
			UiHandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_ING);
			requestRemoteOta();
		} else {
			UiHandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET);
		}
	}
	
	/**
	 * @TODO 启用线程执行远程服务端OTA数据请求
	 * @param null
	 */
    private void requestRemoteOta(){
		try {
			HandlerThread handlerThread = new HandlerThread("RemoteOTA");
			handlerThread.start();
			Handler mHandlerRemote = new Handler(handlerThread.getLooper());
			mHandlerRemote.post(new OtaDataRequestThread(mContext , DeviceInfoUtil.getRemoteUris(mContext) , UiHandler));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
     * @TODO 开始或暂停OTA升级包校验
     * @param toVerify
     * 
     */
    private void startVerify() {
    	if(mDownloadBinder != null){
			mDownloadBinder.startVerify();
		}
	}
    
    private void cancelVerify(){
    	if(mDownloadBinder != null){
			mDownloadBinder.cancelVerify();
		}
    }
    
    private void installDataOtaPackage(){
    	if(mDownloadBinder != null){
			mDownloadBinder.installDataOtaPackage();
		}
    }
    
    /**
     * @TODO 绑定DownloadService
     * @param null
     */
    private void bindDownloadService(){
		Intent mIntent = new Intent(SettingActivity.this , DownloadService.class);
		if(!Tools.isServiceWorking(mContext, DownloadService.class.getName())){
			startService(mIntent);
		}
        bindService(mIntent , mServiceConnection, Context.BIND_AUTO_CREATE);
	}

    /**
     * @TODO DownloadService信息回传接口
     * @param null
     */
	private DownloadService.CallBack mDownloadCallBack = new DownloadService.CallBack() {

		@Override
		public void postDownloadInfo(DownloadInfo mDownloadInfo) {
			workState = mDownloadInfo.getDledStatusFlag() ;
			LogUtil.i(TAG, "mDownloadCallBack:  "+workState);
			Message msg ;
			Bundle mBundle ;
			switch (workState) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING:
					msg = UiHandler.obtainMessage(workState);
					mBundle = new Bundle();
					mBundle.putSerializable(OTAConfigConstant.ActionKey.KEY_DOWNLOAD_INFO, mDownloadInfo);
					msg.setData(mBundle);
					UiHandler.sendMessage(msg);
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
					UiHandler.sendEmptyMessage(workState);
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
					msg = UiHandler.obtainMessage(workState);
					mBundle = new Bundle();
					mBundle.putSerializable(OTAConfigConstant.ActionKey.KEY_DOWNLOAD_INFO, mDownloadInfo);
					msg.setData(mBundle);
					UiHandler.sendMessage(msg);
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED://同步检测一下网络状态
					UiHandler.sendEmptyMessage(workState);
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
					UiHandler.sendEmptyMessage(workState);
					break;
				default:
					break;
			}
		}

		@Override
		public void postDownloadProgress(int mProgress) {
			if(progress_update != null){
				progress_update.setProgress(mProgress);
			}
		}

		@Override
		public void postVerifyProgress(int mVerifyProgress , int mVerifyState) {
			LogUtil.i(TAG, "postVerifyProgress:  "+ mVerifyProgress + " : " +mVerifyState);
			Message msg ;
			switch (mVerifyState) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING:
					msg = new Message();
					msg.what = mVerifyState ;
					msg.arg1 = mVerifyProgress ;
					UiHandler.sendMessage(msg);
					
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
					msg = UiHandler.obtainMessage(mVerifyState);
					UiHandler.sendMessage(msg);
					break;
							
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
					msg = UiHandler.obtainMessage(mVerifyState);
					msg.arg1 = mVerifyProgress ;
					UiHandler.sendMessage(msg);
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
					msg = UiHandler.obtainMessage(mVerifyState);
					msg.arg1 = mVerifyProgress ;
					UiHandler.sendMessage(msg);
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
	
	private void pauseDownloadOta(){
		if(mDownloadBinder != null){
			mDownloadBinder.pauseDownload();
		}
	}
	
	private void cancelDownloadOta(){
		if(mDownloadBinder != null){
			mDownloadBinder.cancelDownload();
		}
	}
	
	/**
	 * @TODO 注册网络状态变化广播接收器
	 * @param context
	 */
	private void registerNetMonitorReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(otaNetMonitorReceiver, intentFilter);
	}
	
	/**
	 * 定义网络状态变化监听器
	 */
	private BroadcastReceiver otaNetMonitorReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context mContext, Intent mIntent) {
			String mAction = mIntent.getAction() ;
			switch (mAction) {
				case ConnectivityManager.CONNECTIVITY_ACTION:
					if(Tools.isNetWorkConnected(mContext)){
						startRemoteCheckThread();
					}else{
						UiHandler.sendEmptyMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_NET);
						pauseDownloadOta();
						
						Tools.switchIconState(mContext, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL) ;
					}
					break;
				default:
					break;
			}
		}
		
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		cancelVerify();
		unregisterReceiver(otaNetMonitorReceiver);
		unbindService(mServiceConnection);
		if (mDownloadService != null) {
    		mDownloadService.unRegisterCallBack(mDownloadCallBack);
        }
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		exit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private void exit(){
		Tools.backToHome();
	}

}
