package com.ktc.ota.utils;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * @author Arvin
 * @TODO ToastUtils
 * @Date 2019.1.24
 * 
 */
public class ToastUtil {

	private volatile static Toast sToast;
	
    public static void showToast(Context context, String msg){
    	showToast(context, msg, 0);
    }

    public static void showToast(Context context, String msg, int time){
    	if (TextUtils.isEmpty(msg))
            return;
    	
        if (time <= 0){
            time = Toast.LENGTH_SHORT;
        }
        if (sToast == null) {
            sToast = Toast.makeText(context, msg, time);
        }
        sToast.setText(msg);
        sToast.show();
    }
}
