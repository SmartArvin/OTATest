package android.systemupdate.activitys;

import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;

import android.systemupdate.service.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


/**
 * @author Arvin
 * @TODO 远程OTA升级提示
 * @Date 2019.2.14
 */
public class NotifyRemoteUpdateActivity extends Activity{
	private String TAG = "NotifyRemoteUpdateActivity";
	private Context mContext;
	
	private TextView title , sub_title;
	private Button btn_ok , btn_cancel;
	
	private Bundle mBundle =null ;
	private OtaRemoteData mOtaRemoteData ;
	private String otaPkgName ;
	private String otaPkgLength ;
	private String otaPkgVersion ;
	private String otaLevelFlag ;
	private String otaDownloadUrl ;
	private String otaAnswerCode ;
	private String otaForceFlag ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notify_remote_update);
		OtaUpdateApplication.getInstance().addActivity(this);
		
		mContext = this;
		
		Intent mIntent = getIntent();
		if(mIntent != null){
			mBundle = mIntent.getExtras();
		}
		if(mBundle == null){
			finish() ;
		}else{
			mOtaRemoteData = (OtaRemoteData) mBundle.getParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA);
			otaPkgName = mOtaRemoteData.getOtaPkgName();
			otaPkgLength = mOtaRemoteData.getOtaPkgLength();
			otaPkgVersion = mOtaRemoteData.getOtaPkgVersion();
			otaLevelFlag = mOtaRemoteData.getOtaLevelFlag();
			otaDownloadUrl = mOtaRemoteData.getOtaDownloadUrl();
			otaAnswerCode = mOtaRemoteData.getOtaAnswerCode();
			otaForceFlag = mOtaRemoteData.getOtaForceFlag();
			
			findView();
		}
	}
	
	private void findView() {
		title = (TextView) findViewById(R.id.notify_remote_update_title) ;
		sub_title = (TextView) findViewById(R.id.notify_remote_update_title_sub);
		btn_ok = (Button) findViewById(R.id.notify_remote_update_ok);
		btn_cancel = (Button) findViewById(R.id.notify_remote_update_cancel);
        
		sub_title.setText(getString(R.string.tip_update_zip_version)+otaPkgVersion+"\n"+getString(R.string.tip_update_zip_length)+Tools.formatSize(Long.parseLong(otaPkgLength)));
		btn_cancel.requestFocus();
		
		
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					if(mBundle != null){
						Intent mIntent = new Intent();
						mIntent.setClass(mContext , SettingActivity.class);
						startActivity(mIntent);	
					}
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
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
}
