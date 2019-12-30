package com.ktc.ota.download;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RecoverySystem;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.Tools;


/**
 * @author Arvin
 * @TODO 启动服务执行下载任务
 * @Date 2019.2.15
 * 
 */
public class DownloadService extends Service {
	private static final String TAG = "DownloadService";
	private static Context mContext;
	
	private WeakReference<DownloadService> mDownloadService = new WeakReference<>(DownloadService.this);
	private OtaRemoteData mOtaRemoteData ;
    private DownloadTask downloadTask;
    
    private VerifyTask mVerifyTask ;
    private boolean isAllowVerify = true ;
    private int lastVerifyProgress = 0;
    
    private int mDownloadStatus = -1 ;

    private DownloadListener listener = new DownloadListener() {
    	 @Override
         public void onProgress(int progress) {
         	if (callBacks != null && callBacks.size() > 0) {
                 for (CallBack mCallBack : callBacks) {
                 	LogUtil.i(TAG, "onProgress:  "+progress);
                 	mCallBack.postDownloadProgress(progress);
                 }
         	}
         }
         
         @Override
 		public void sendDownloadInfo(DownloadInfo mDownloadInfo) {
     		if (callBacks != null && callBacks.size() > 0) {
                 for (CallBack mCallBack : callBacks) {
                	 if(mDownloadInfo != null){
                		LogUtil.i(TAG, "sendDownloadInfo:  "+mDownloadInfo.getDledLength());
                      	mCallBack.postDownloadInfo(mDownloadInfo);
                      	mDownloadStatus = mDownloadInfo.getDledStatusFlag();
                	 }
                 }
         	}
 		}

        @Override
        public void onSuccess() {
            downloadTask = null;
            
            if(mVerifyTask != null){
            	mVerifyTask.cancel(true);
            	mVerifyTask = null ;
            }
        	mVerifyTask = new VerifyTask();
        	mVerifyTask.execute();
        }

        @Override
        public void onFailed() {
            downloadTask = null;
        }

        @Override
        public void onPaused() {
            downloadTask = null;
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
        }

    };
    
    @Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
	}


	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	private DownloadBinder mBinder = new DownloadBinder();

	public class DownloadBinder extends Binder {

		/**
		 * 开始OTA包下载
		 * @return void
		 */
		public void startDownload(OtaRemoteData mOtaData) {
			if (downloadTask == null && mOtaData != null) {
				mOtaRemoteData = mOtaData;
				downloadTask = new DownloadTask(listener, getApplication());
				downloadTask.execute(mOtaRemoteData.getOtaDownloadUrl() , Tools.getDataOtaFilePath(mOtaRemoteData));
			}
		}

		/**
		 * 暂停OTA包下载
		 * @return void
		 */
		public void pauseDownload() {
			LogUtil.i(TAG, "pauseDownload");
			if (downloadTask != null) {
				downloadTask.pauseDownload();
			}
		}
		
		/**
		 * 取消OTA包下载
		 * @return void
		 */
		public void cancelDownload() {
			LogUtil.i(TAG, "cancelDownload");
			if (downloadTask != null) {
				downloadTask.cancelDownload();
			}
		}
		
		/**
		 * 开始OTA包校验
		 * @return void
		 */
		public void startVerify(){
			LogUtil.i(TAG, "startVerify");
			if(mVerifyTask != null){
            	mVerifyTask.cancel(true);
            	mVerifyTask = null ;
            }
        	mVerifyTask = new VerifyTask();
        	mVerifyTask.execute();
		}
	    
		/**
		 * 取消OTA包校验
		 * @return void
		 */
		public void cancelVerify(){
			LogUtil.i(TAG, "canclVerify");
	    	isAllowVerify = false ;
	    	if(mVerifyTask != null){
            	mVerifyTask.cancel(true);
            	mVerifyTask = null ;
            }
	    	callBackPostVerify(0, OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP);
	    }
		
		/**
		 * 安装OTA升级包
		 * @return void
		 */
		public void installDataOtaPackage(){
			if(mOtaRemoteData != null){
				String mOtaPkgName = mOtaRemoteData.getOtaPkgName() ;
				String mOtaVersion = mOtaRemoteData.getOtaPkgVersion() ;
				
				String mOtaFileName = mOtaPkgName.substring(0, mOtaPkgName.lastIndexOf("."))
						+"_"
						+mOtaVersion
						+mOtaPkgName.substring(mOtaPkgName.lastIndexOf("."), mOtaPkgName.length()) ;
				Tools.installOtaFile(DownloadService.this, OTAConfigConstant.OTAFILE.DATA_OTA_DIR + mOtaFileName) ;
			}else{
				Tools.installOtaFile(DownloadService.this, null) ;
			}
		}
		
		/**
		 * 获取DownloadService对象
		 * @return DownloadService
		 */
		public DownloadService getServiceSelf() {
            return mDownloadService.get();
        }
		
		/**
		 * 获取OTA下载及校验状态
		 * @return int
		 */
		public int getDownloadStatus(){
			return mDownloadStatus ;
		}
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
 

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    /**
     * 提供给activity的接口 因为存在一个服务绑定多个activity的情况 所以监听接口采用list装起来
     */
    public interface CallBack {
        void postDownloadProgress(int mProgress);
        void postDownloadInfo(DownloadInfo mDownloadInfo);
        void postVerifyProgress(int mVerifyProgress  , int mVerifyState);
    }
    private List<CallBack> callBacks = new LinkedList<>();
    public void registerCallBack(CallBack callBack) {
        if (callBacks != null) {
            callBacks.add(callBack);
        }
    }

    /**
     * 注销接口 false注销失败
     * @param callBack
     * @return
     */
    public boolean unRegisterCallBack(CallBack callBack) {
        if (callBacks != null && callBacks.contains(callBack)) {
            return callBacks.remove(callBack);
        }
        return false;
    }
    
    
    /**
	 * @TODO 校验已下载的OTA升级包
	 * @param null
	 */
    class VerifyTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
        	try {
        		LogUtil.i(TAG, "VerifyTask  begin");
            	if(mOtaRemoteData == null){
            		return OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL;
            	}
        		
            	isAllowVerify = true ;
            	
            	String mOtaPkgName = mOtaRemoteData.getOtaPkgName() ;
        		String mOtaVersion = mOtaRemoteData.getOtaPkgVersion() ;
        		
        		String mOtaFileName = mOtaPkgName.substring(0, mOtaPkgName.lastIndexOf("."))
        				+"_"
        				+mOtaVersion
        				+mOtaPkgName.substring(mOtaPkgName.lastIndexOf("."), mOtaPkgName.length()) ;
        		
    			RecoverySystem.verifyPackage(new File(OTAConfigConstant.OTAFILE.DATA_OTA_DIR + mOtaFileName),
    					new RecoverySystem.ProgressListener() {
					@Override
					public void onProgress(int progress) {
						lastVerifyProgress = progress ;
						publishProgress(lastVerifyProgress);
					}
				}, null);
    			if(isAllowVerify){
    				LogUtil.i(TAG, "verifyPackage is completed and it ok");
        			return OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS ;
    			}else{
    				LogUtil.i(TAG, "verifyPackage is stoped");
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
			LogUtil.i(TAG, "verifyPackage:  "+progress);
			LogUtil.i(TAG, "onProgressUpdate__isAllowVerify:  "+isAllowVerify);
			
			if(isAllowVerify){
				callBackPostVerify(progress, OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_ING);
			}else{
				callBackPostVerify(0, OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP);
			}
		}

        @Override
		protected void onPostExecute(Integer status) {
        	switch (status) {
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_FAIL:
					callBackPostVerify(0, status);
					Tools.deleteDataOtaFile();
					break;
					
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_STOP:
					if(isAllowVerify){
						callBackPostVerify(0, status);
					}
					break;
								
				case OTAConfigConstant.DownloadStatus.DOWNLOAD_VERIFY_SUCCESS:
					callBackPostVerify(lastVerifyProgress, status);
					break;
	
				default:
					break;
			}
		}
    }
    
    private void callBackPostVerify(int mVerifyProgress  , int mVerifyState){
    	if (callBacks != null && callBacks.size() > 0) {
			for (CallBack mCallBack : callBacks) {
				mDownloadStatus = mVerifyState ;
				mCallBack.postVerifyProgress(mVerifyProgress , mVerifyState);
			}
		}
    }
    
}
