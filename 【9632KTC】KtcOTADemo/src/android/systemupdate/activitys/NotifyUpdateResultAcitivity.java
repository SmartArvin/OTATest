package android.systemupdate.activitys;

import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.systemupdate.service.R;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Arvin
 * @TODO 提示升级结果
 * @Date 2019.2.11
 */
public class NotifyUpdateResultAcitivity extends Activity {
	private static String TAG = "UpdateResultAcitivity";

	private ImageView img ;
	private TextView sub_text ;
	private Button btn_ok ;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_result);
        OtaUpdateApplication.getInstance().addActivity(this);
        
        initData() ;
	}
	
	private void initData(){
		img = (ImageView) findViewById(R.id.update_result_img);
        sub_text= (TextView) findViewById(R.id.update_result_title_sub);
        btn_ok = (Button) findViewById(R.id.update_result_ok);
        
		Intent mIntent = getIntent();
        boolean isUpdateSuccess = mIntent.getBooleanExtra(OTAConfigConstant.ActionKey.KEY_OTA_UPDATE_FLAG , false);
        
        LogUtil.d(TAG, "updateflag = " + isUpdateSuccess);
		if(isUpdateSuccess) {//升级成功
			img.setBackgroundResource(R.drawable.ic_happy);
			sub_text.setText(getString(R.string.tip_update_result_success));
	    }else{//升级失败
	    	img.setBackgroundResource(R.drawable.ic_sad);
	    	sub_text.setText(getString(R.string.tip_update_result_failed));
        }
		
		btn_ok.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}

