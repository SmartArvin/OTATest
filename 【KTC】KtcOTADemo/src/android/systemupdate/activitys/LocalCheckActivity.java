package android.systemupdate.activitys;

import java.io.File;
import com.ktc.ota.bean.DiskData;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;
import com.ktc.ota.views.MarqueeTextView;
import com.ktc.ota.views.RoundProgressBar;

import android.systemupdate.service.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RecoverySystem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * @author Arvin
 * @TODO 本地USB升级入口
 * @Date 2019.1.24
 * 
 */
public class LocalCheckActivity extends Activity {
	private String TAG = "LocalCheckActivity";
	private Context mContext;
	private RoundProgressBar mProgressBar;

	private LinearLayout local_check_btn_rl ;
	private Button mBtnClose;
	private Button mBtnCancel;
	private TextView mText_title , mText_status;
	private MarqueeTextView mText_path , mText_size ;
	
	private File mLocalOtaFile;
	private Long mOtaFileSize;

	private VerifyTask mVerifyTask ;
	private int workStatus = -1 ;
	private boolean isAllowInstall = true;
	
    //标记正在校验或已提示的U盘路径
	private String checkingPath = null ;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_local_check);
		OtaUpdateApplication.getInstance().addActivity(this);
		
		mContext = this;

		initViews();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Tools.changeToStorage();
	}

	private void initViews() {
		mText_title = (TextView) findViewById(R.id.local_check_tip);
		mText_path = (MarqueeTextView) findViewById(R.id.local_check_ota_text_path);
		mText_size = (MarqueeTextView) findViewById(R.id.local_check_ota_text_size);
		mText_status = (TextView) findViewById(R.id.local_check_ota_text_status);

		local_check_btn_rl = (LinearLayout) findViewById(R.id.local_check_btn_rl);
		mProgressBar = (RoundProgressBar) findViewById(R.id.local_check_progress);
		mBtnCancel = (Button) findViewById(R.id.local_check_btn_cancel);
		mBtnClose = (Button) findViewById(R.id.local_check_btn_close);

		mBtnCancel.requestFocus();
		mBtnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				switch (workStatus) {
				case MSG_LOCAL_OTA_EXIST:
					startVerify();
					break;
				case MSG_LOCAL_OTA_NOEXIST:
					exit();		
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING:
					cancelVerify();
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
					startVerify();
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
					startVerify();
					break;
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
					startVerify();
					break;

				default:
					exit();
					break;
				}
			}
		});

		mBtnClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				cancelVerify();
				exit();
			}
		});

		getLocalOtaInfo();
		UIHandler.sendEmptyMessage(mOtaFileSize > 0  ?  MSG_LOCAL_OTA_EXIST : MSG_LOCAL_OTA_NOEXIST);
		
		registerUSBMountReceiver(mContext);
	}
	
	private void getLocalOtaInfo(){
		try {
			DiskData mUsbDisk = Tools.getFirstUsbDisk(mContext);
			if(mUsbDisk != null){
				mLocalOtaFile = new File(mUsbDisk.getPath()
						+ OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME);
				mOtaFileSize = Tools.getFileSize(mLocalOtaFile.getAbsolutePath());
				
				checkingPath = mUsbDisk.getPath() ;
				mText_path.setText(getString(R.string.tip_update_zip_path) + mUsbDisk.getName()+"/"+OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME);
				mText_size.setText(getString(R.string.tip_update_zip_length) + Tools.formatSize(mOtaFileSize));
			}
		} catch (Exception e) {
			mOtaFileSize = 0L;
		}
	}

	private void startVerify() {
		try {
			isAllowInstall = true ;
			resetProgressBar();
			
			getLocalOtaInfo();
			
			if (mOtaFileSize > 0) {
				UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING);
				if(mVerifyTask != null){
					mVerifyTask.cancel(true);
					mVerifyTask = null ;
				}
				mVerifyTask = new VerifyTask();
				mVerifyTask.execute();
			} else {
				UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL);
			}
		} catch (Exception e) {
			UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL);
		}
	}

	
	private void cancelVerify(){
		workStatus = -1 ;
		isAllowInstall = false ;
		if(mVerifyTask != null){
			mVerifyTask.cancel(true);
			mVerifyTask = null ;
		}
		UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP);
		Tools.deleteRecoveryFlag();
	}

	private void resetProgressBar() {
		mProgressBar.setMax(100);
		mProgressBar.setProgress(0);
		mProgressBar.invalidate();
	}
	
	private static final int MSG_LOCAL_OTA_EXIST = 0x01 ;
	private static final int MSG_LOCAL_OTA_NOEXIST = 0x02 ;
	
	
	Handler UIHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			workStatus = msg.what ;
			LogUtil.i(TAG, "UIHandler_workStatus:   "+workStatus);
			switch (workStatus) {
			case MSG_LOCAL_OTA_EXIST:
				mText_status.setText(getString(R.string.tip_local_hasfile));
				mText_status.setVisibility(View.VISIBLE);
				resetProgressBar();
				
				mBtnCancel.setText(getString(R.string.str_btn_update_now));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				mBtnCancel.requestFocus();
				local_check_btn_rl.setVisibility(View.VISIBLE);
				break;
				
			case MSG_LOCAL_OTA_NOEXIST:
				getLocalOtaInfo();
				mText_status.setText(getString(R.string.tip_local_nofile));
				mText_status.setVisibility(View.VISIBLE);
				resetProgressBar();
				
				mBtnCancel.setText(getString(R.string.str_btn_update_cancel));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				mBtnClose.requestFocus();
				local_check_btn_rl.setVisibility(View.VISIBLE);
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING:
				mText_status.setText(getString(R.string.tip_update_verifying));
				mText_status.setVisibility(View.VISIBLE);		
				
				mBtnCancel.setText(getString(R.string.str_btn_verify_cancle));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				mBtnCancel.requestFocus();
				local_check_btn_rl.setVisibility(View.VISIBLE);
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
				mText_status.setText(getString(R.string.tip_update_verify_success));
				mText_status.setVisibility(View.VISIBLE);
				
				mBtnCancel.setText(getString(R.string.str_btn_verify_again));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				local_check_btn_rl.setVisibility(View.INVISIBLE);
			
				Tools.installOtaFile(mContext, mLocalOtaFile.getPath());
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
				resetProgressBar();
				mText_status.setText(getString(R.string.tip_update_verify_stop));
				mText_status.setVisibility(View.VISIBLE);
				
				mBtnCancel.setText(getString(R.string.str_btn_verify_again));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				mBtnCancel.requestFocus();
				local_check_btn_rl.setVisibility(View.VISIBLE);
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
				mText_status.setText(getString(R.string.tip_update_verify_fail));
				mText_status.setVisibility(View.VISIBLE);
				resetProgressBar();
				
				mBtnCancel.setText(getString(R.string.str_btn_verify_again));
				mBtnClose.setText(getString(R.string.str_btn_update_notyet));
				mBtnCancel.requestFocus();
				local_check_btn_rl.setVisibility(View.VISIBLE);
				break;
				

			default:
				break;
			}
		}
    };
    
	class VerifyTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
        	try {
    			RecoverySystem.verifyPackage(mLocalOtaFile,
    					new RecoverySystem.ProgressListener() {
    						@Override
    						public void onProgress(int progress) {
    							LogUtil.i(TAG, "verifyPackage = " + progress);
								publishProgress(progress);
    						}
    					}, null);
    			
    			if(isAllowInstall){
        			return OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS ;
    			}else{
        			return OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP ;
    			}
    		} catch (Exception e) {
    			LogUtil.i(TAG, "verifyPackage Error:  "+e.toString());
    			e.printStackTrace();
    			return OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL;
    			
    		}
        }
        
        @Override
    	protected void onProgressUpdate(Integer... values) {
        	int progress = values[0] ;
        	mProgressBar.setProgress(isAllowInstall ? progress : 0);

        	if(isAllowInstall){
                if(progress >= 100 && !Tools.isTopCurRunningPackage(new String[]{mContext.getPackageName()})){
                	UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS);
                }
			}else{
				UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP);
			}
        }

        @Override
        protected void onPostExecute(Integer status) {
        	switch (status) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
					UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL);
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
					break;
								
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
					UIHandler.sendEmptyMessage(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS);
					break;
	
				default:
					break;
			}
		}
    }
	
	/**
	 * @TODO 注册监听器
	 * @param context
	 */
	private void registerUSBMountReceiver(Context context) {
		IntentFilter intentFilterUsb = new IntentFilter();
        intentFilterUsb.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilterUsb.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilterUsb.addDataScheme("file");
        context.registerReceiver(usbMountReceiver, intentFilterUsb);
	}
	
	
	private BroadcastReceiver usbMountReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            	getLocalOtaInfo();
        		UIHandler.sendEmptyMessage(mOtaFileSize > 0  ?  MSG_LOCAL_OTA_EXIST : MSG_LOCAL_OTA_NOEXIST);
            } else if(action.equals(Intent.ACTION_MEDIA_EJECT)){
            	String unMountPath = intent.getData().getPath();
            	if(unMountPath.replaceAll("/", "").equals(checkingPath.replaceAll("/", ""))){
            		cancelVerify();
            		OtaUpdateApplication.getInstance().exit() ;
            	}
            }
        }
    };

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		cancelVerify();
		if(UIHandler.hasMessages(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS)){
			UIHandler.removeMessages(OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS);
		}
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		exit();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(usbMountReceiver);
		super.onDestroy();
	}

	private void exit(){
		Tools.backToHome();
	}
}
