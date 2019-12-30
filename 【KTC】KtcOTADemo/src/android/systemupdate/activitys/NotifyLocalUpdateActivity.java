package android.systemupdate.activitys;

import com.ktc.ota.bean.DiskData;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;
import android.systemupdate.service.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Arvin
 * @TODO 本地OTA升级提示
 * @Date 2019.2.14
 */
public class NotifyLocalUpdateActivity extends Activity{
	private String TAG = "NotifyLocalUpdateActivity";
	private Context mContext;
	
	private DiskData mUsbDisk ;
	
	private TextView title , sub_title;
	private Button btn_ok , btn_cancel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notify_local_update);
		OtaUpdateApplication.getInstance().addActivity(this);
		
		mContext = this;
		
		findView();
		initData();
		registerUSBMountReceiver(mContext);
	}
	
	private void findView() {
		title = (TextView) findViewById(R.id.notify_local_update_title);
		sub_title = (TextView) findViewById(R.id.notify_local_update_title_sub);
		btn_ok = (Button) findViewById(R.id.notify_local_update_ok);
		btn_cancel = (Button) findViewById(R.id.notify_local_update_cancel);
		btn_cancel.requestFocus();
        
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					Intent mIntent = new Intent();
					mIntent.setClass(mContext , LocalCheckActivity.class);
					startActivity(mIntent);
					finish();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void initData() {
		try {
			mUsbDisk = Tools.getFirstUsbDisk(mContext);
			if(mUsbDisk != null){
				Long mOtaFileSize = Tools.getFileSize(mUsbDisk.getPath() + OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME) ;
				String otaFilePath = mUsbDisk.getName()+"/"+ OTAConfigConstant.OTAFILE.LOCAL_OTA_NAME ;
				String otaFileSize = Tools.formatSize(mOtaFileSize);
				sub_title.setText(getString(R.string.tip_update_zip_path) + otaFilePath + "\n"+getString(R.string.tip_update_zip_length) + otaFileSize);
				
				btn_ok.setEnabled(mOtaFileSize > 0 ? true : false );
			}
		} catch (Exception e) {
			finish();
		}
	}
	
	/**
	 * @TODO 注册监听器
	 * @param context
	 */
	private void registerUSBMountReceiver(Context context) {
		IntentFilter intentFilterUsb = new IntentFilter();
        intentFilterUsb.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilterUsb.addDataScheme("file");
        context.registerReceiver(usbMountReceiver, intentFilterUsb);
	}
	
	
	private BroadcastReceiver usbMountReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_MEDIA_EJECT)){
            	String unMountPath = intent.getData().getPath();
            	String curDiskPath = null ;
            	if(mUsbDisk != null){
            		curDiskPath = mUsbDisk.getPath();
            	}
            	
            	if(unMountPath.replaceAll("/", "").equals(curDiskPath.replaceAll("/", ""))){
            		OtaUpdateApplication.getInstance().exit() ;
            	}
            }
        }
    };
	

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(usbMountReceiver);
		super.onDestroy();
	}
	
}
