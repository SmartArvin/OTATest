package com.ktc.ota.download;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.utils.DeviceInfoUtil;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.OkHttpClientUtil;
import com.ktc.ota.utils.SharedPreferencesUtil;
import com.ktc.ota.utils.SharedPreferencesUtil.ContentValue;
import com.ktc.ota.utils.Tools;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

/**
 * @author Arvin
 * @TODO 远程OTA数据请求线程
 * @Date 2019.2.14
 * 
 */
public class OtaDataRequestThread implements Runnable {
	
	private static final String TAG = "OtaDataRequestThread";
    private Context context;
    private Handler handler;
    private String mRemoteFormalUrl ;
    private String mRemoteBackUrl ;
    
    public OtaDataRequestThread(Context context , String[] mRemoteUrls , Handler handler) {
        this.context = context;
        this.handler = handler;
        if(mRemoteUrls != null && mRemoteUrls.length > 1){
        	this.mRemoteFormalUrl = mRemoteUrls[0] ;
            this.mRemoteBackUrl = mRemoteUrls[1] ;
        }
    }
    
    @Override
    public void run() {
        try {
        	getRemoteOtaInfo(mRemoteFormalUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @TODO 获取远程OTA升级包信息
	 * @param strUrl
	 * @return void
	 */
	private void getRemoteOtaInfo(final String strUrl) {
		if(strUrl == null || TextUtils.isEmpty(strUrl)){
			LogUtil.i(TAG, "警告：网络已连接，远程升级地址异常！  ");
			Message msg = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_EXCEPTION);
			if (msg != null) {
				handler.sendMessageDelayed(msg, 2000);
			}
		}
		
		OkHttpClient mOkHttpClient ;
		if(strUrl.contains("https:")){
			mOkHttpClient = OkHttpClientUtil.getUnsafeOkHttpClient();
		}else{
			mOkHttpClient = new OkHttpClient();
		}
		Request.Builder requestBuilder = new Request.Builder().url(strUrl);
		requestBuilder.method("GET", null);
		Request request = requestBuilder.build();
		Call mcall = mOkHttpClient.newCall(request);
		
		LogUtil.i(TAG, "提示：网络已连接，正在检测升级！");
		mcall.enqueue(new Callback() {
			@Override
			public void onResponse(Call call, Response response)
					throws IOException {
				String defaultValue = "";
				if (response != null) {
					OtaRemoteData mOtaRemoteData = new OtaRemoteData();
					mOtaRemoteData.setOtaPkgLength(response.header(
							"OtaPackageLength", "0"));
					mOtaRemoteData.setOtaPkgName(response.header("OtaPackageName",
							defaultValue));
					mOtaRemoteData.setOtaPkgVersion(response.header(
							"OtaPackageVersion", defaultValue));
					mOtaRemoteData.setOtaLevelFlag(response.header("OtaUpdateLevel",
							defaultValue));
					mOtaRemoteData.setOtaDownloadUrl(response.header("OtaPackageUri",
							defaultValue));

					mOtaRemoteData.setOtaAnswerCode(response.header("answer", defaultValue));
					mOtaRemoteData.setOtaForceFlag(response.header("is_important",
							defaultValue));
					
					LogUtil.i(TAG , "远程OTA升级信息:  "+mOtaRemoteData.toString());
					
					if(mOtaRemoteData != null && (mOtaRemoteData.getOtaDownloadUrl() != null || TextUtils.isEmpty(mOtaRemoteData.getOtaDownloadUrl()))){
						if(mOtaRemoteData.getOtaAnswerCode().equals("1")){//正常升级流程
							//检测内存空间
							boolean lowSpace = Tools.getDataFreeSize() < Long.parseLong(mOtaRemoteData.getOtaPkgLength());
							if(lowSpace){
								LogUtil.i(TAG, "警告：网络已连接，已检测到升级包但data空间不足！");
								Message msg0 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_REQUEST_STATE_NO_SPACE);
								if (msg0 != null) {
									Bundle mBundle = new Bundle();
									mBundle.putParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA, mOtaRemoteData);
									msg0.setData(mBundle);
									handler.sendMessageDelayed(msg0, 2000);
								}
							}else{
								LogUtil.i(TAG, "恭喜：网络已连接，已检测到可用升级包！");
								Message msg1 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_SUCCESS);
								if (msg1 != null) {
									Bundle mBundle = new Bundle();
									mBundle.putParcelable(OTAConfigConstant.ActionKey.KEY_REMOTE_OTA_DATA, mOtaRemoteData);
									msg1.setData(mBundle);
									handler.sendMessageDelayed(msg1 , 2000);
									
									Tools.switchIconState(context, OTAConfigConstant.ICONSTATE.ICONSTATE_UPDATE);
								}
							}
						}else if(mOtaRemoteData.getOtaAnswerCode().equals("2")){//上传升级成功标志后的回传信息
							Message msg2 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
							if (msg2 != null) {
								handler.sendMessageDelayed(msg2 , 2000);
								//复位请求类型记录
								DeviceInfoUtil.setRequestType("0");
								
								Tools.switchIconState(context, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL);
							}
						}else{
							Message msg3 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
							if (msg3 != null) {
								handler.sendMessageDelayed(msg3 , 2000);
								//启用备用服务器
								if(!strUrl.equals(mRemoteBackUrl)){
									getRemoteOtaInfo(mRemoteBackUrl);
								}
								Tools.switchIconState(context, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL);
							}
							LogUtil.i(TAG, "警告：网络已连接，远程升级检测未发现升级包！");
						}
					} else {
						Message msg4 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_NO_RESPONSE);
						if (msg4 != null) {
							handler.sendMessageDelayed(msg4 , 2000);
							//启用备用服务器
							if(!strUrl.equals(mRemoteBackUrl)){
								getRemoteOtaInfo(mRemoteBackUrl);
							}
							Tools.switchIconState(context, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL);
						}
						LogUtil.i(TAG, "警告：网络已连接，远程升级检测未发现升级包！");
					}
				}else{
					LogUtil.i(TAG, "警告：网络已连接，远程升级检测未发现升级包！");
					//启用备用服务器
					if(!strUrl.equals(mRemoteBackUrl)){
						getRemoteOtaInfo(mRemoteBackUrl);
					}
					Tools.switchIconState(context, OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL);
				}
			}

			@Override
			public void onFailure(Call call, IOException e) {
				LogUtil.i(TAG, "警告：网络已连接，远程升级检测异常！  "+e.toString());
				Message msg5 = handler.obtainMessage(OTAConfigConstant.MSGKey.MSG_REMOTE_OTA_DATA_EXCEPTION);
				if (msg5 != null) {
					handler.sendMessageDelayed(msg5, 2000);
				}
				//启用备用服务器
				if(!strUrl.equals(mRemoteBackUrl)){
					getRemoteOtaInfo(mRemoteBackUrl);
				}
			}
		});
	}
	
}
