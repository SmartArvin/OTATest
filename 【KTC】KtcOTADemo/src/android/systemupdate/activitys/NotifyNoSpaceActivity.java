package android.systemupdate.activitys;

import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.systemupdate.service.R;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Arvin
 * @TODO 下载存储空间不足
 * @Date 2019.1.24
 * 
 */
public class NotifyNoSpaceActivity extends Activity{

	private static final String TAG = "StorageMemeryIsNotEnoughActivity";
    private static final boolean DEBUG = true;

    private TextView title_sub ;
    private Button btn_ok;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_space);
        OtaUpdateApplication.getInstance().addActivity(this);
        
        title_sub = (TextView) findViewById(R.id.no_space_info_title_sub);
        btn_ok = (Button) findViewById(R.id.no_space_cancel);
        
        Intent intent = getIntent();
        Long mOtaFileLength  = intent.getLongExtra(OTAConfigConstant.ActionKey.KEY_OTA_INFO, 0);
        Long mDataFreeLength = Tools.getDataFreeSize() ;

        String msg = String.format(getString(R.string.tip_update_nospace), Tools.formatSize(mOtaFileLength), Tools.formatSize(mDataFreeLength));
        title_sub.setText(msg);
        btn_ok.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
    }

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}
    
}

