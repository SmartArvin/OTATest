package android.systemupdate.activitys;

import com.ktc.ota.main.application.OtaUpdateApplication;
import com.ktc.ota.utils.Tools;
import android.systemupdate.service.R;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Arvin
 * @TODO 后台下载并校验完成后弹出提示请用户安装
 * @Date 2019.2.14
 */
public class NotifyInstallAndRebootActivity extends Activity{
	private String TAG = "NotifyInstallAndRebootActivity";
	private Context mContext;
	
	private TextView title , sub_title;
	private Button btn_ok , btn_cancel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notify_install_reboot);
		OtaUpdateApplication.getInstance().addActivity(this);
		
		mContext = this;
		
		findView();
	}
	
	private void findView() {
		title = (TextView) findViewById(R.id.notify_install_reboot_title);
		sub_title = (TextView) findViewById(R.id.notify_install_reboot_title_sub);
		btn_ok = (Button) findViewById(R.id.notify_install_reboot_ok);
		btn_cancel = (Button) findViewById(R.id.notify_install_reboot_cancel);
		
		btn_cancel.requestFocus();
        
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Tools.installOtaFile(mContext, Tools.getDataOtaFile().getAbsolutePath());
			}
		});
		
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Tools.deleteDataOtaFile();
				finish();
			}
		});
	}
	

	@Override
	protected void onStop() {
		super.onStop();
	}
	
}
