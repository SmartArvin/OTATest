package android.systemupdate.activitys;

import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.Tools;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.systemupdate.service.R;
import android.widget.TextView;

/**
 * @author Arvin
 * @TODO 下载完成后强制升级
 * @Date 2019.1.24
 * 
 */
public class NotifyForceUpdateActivity extends Activity{

	private static final String TAG = "NotifyForceUpdateActivity";

	private TextView title ;
    private TextView title_sub ;
    
    private static final int MSG_COUNT_DOWN = 0x01 ;
    private int MAX_COUNT_DOWN = 5 ;
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what ;
			if(what == MSG_COUNT_DOWN){
				if(MAX_COUNT_DOWN == 0){
					Tools.installOtaFile(NotifyForceUpdateActivity.this, Tools.getDataOtaFile().getAbsolutePath());
					finish() ;
				}else{
					title_sub.setText(getString(R.string.tip_force_update , MAX_COUNT_DOWN));
					MAX_COUNT_DOWN -- ;
					sendEmptyMessageDelayed(MSG_COUNT_DOWN, 1000);
				}
			}
		}
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_force_update);
        OtaUpdateApplication.getInstance().addActivity(this);
        
        title = (TextView) findViewById(R.id.force_update_info_title);
        title_sub = (TextView) findViewById(R.id.force_update_info_title_sub);
        
        mHandler.sendEmptyMessage(MSG_COUNT_DOWN);
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
	
	 @Override
	protected void onStop() {
		if(mHandler.hasMessages(MSG_COUNT_DOWN)){
			mHandler.removeMessages(MSG_COUNT_DOWN);
		}
		super.onStop();
	}
}

