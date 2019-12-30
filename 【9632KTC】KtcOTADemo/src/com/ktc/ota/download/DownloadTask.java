package com.ktc.ota.download;

import android.content.Context;
import android.os.AsyncTask;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import com.ktc.ota.utils.LogUtil;
import com.ktc.ota.utils.OTAConfigConstant;
import com.ktc.ota.utils.OkHttpClientUtil;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Arvin
 * @TODO 异步任务执行下载
 * @Date 2019.2.15
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
	private static final String TAG = "DownloadTask";

	private long otaFileLength ;//远程OTA升级包大小(KB)
	private String downloadUrl ;
	private String downloadLocalPath ;
	private long downloadedLength ;
	
	private DownloadListener listener;
	private boolean isCanceled = false;
	private boolean isPaused = false;
	private int lastProgress = 0;

	public DownloadTask(DownloadListener listener, Context context) {
		this.listener = listener;
	}

	@Override
	protected Integer doInBackground(String... params) {
		InputStream inputStream = null;
		RandomAccessFile savedFile = null;
		Response response = null ;
		File file = null;

		try {
			downloadedLength = 0L ;// 已下载长度
			downloadUrl = params[0] ;
			downloadLocalPath = params[1] ;
			LogUtil.i(TAG, "downloadLocalPath: " + downloadLocalPath);
			
			File fileDir = new File(OTAConfigConstant.OTAFILE.DATA_OTA_DIR);
			if(!fileDir.exists()){
				fileDir.mkdir();
			}
			
			file = new File(downloadLocalPath);
			if (file.exists()) {
				downloadedLength = file.length();
			}
			
			listener.sendDownloadInfo(new DownloadInfo(downloadUrl, downloadLocalPath, 
					downloadedLength, lastProgress, 
					OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING));
			
			otaFileLength = getContentLength(downloadUrl);
			LogUtil.i(TAG,"otaFileLength:  "+otaFileLength);
			
			if (otaFileLength == 0) {
				return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED;
			} else if (otaFileLength == downloadedLength) {
				return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS;
			}
			
			OkHttpClient client ;
			if(downloadUrl.contains("https:")){
				LogUtil.i(TAG,"-------------client:  https:------------");
				client = OkHttpClientUtil.getUnsafeOkHttpClient();
			}else{
				client = new OkHttpClient();
			}
			Request request = new Request.Builder()
					.addHeader("RANGE", "bytes=" + downloadedLength + "-")
					.url(downloadUrl).build();
			response = client.newCall(request).execute();
			
			if (response != null) {
				inputStream = response.body().byteStream();
				savedFile = new RandomAccessFile(file, "rw");
				savedFile.seek(downloadedLength);
				byte[] b = new byte[1024];
				int total = 0;
				int len;
				while ((len = inputStream.read(b)) != -1) {
					if (isCanceled) {
						return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED;
					} else if (isPaused) {
						return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED;
					} else {
						total += len;
						savedFile.write(b, 0, len);
						int progress = (int) ((total + downloadedLength) * 100 / otaFileLength);
						publishProgress(progress);
					}
				}
				return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS;
			}
		} catch (Exception e0) {
			e0.printStackTrace();
			LogUtil.i(TAG,"----e0:  "+e0.toString());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (savedFile != null) {
					savedFile.close();
				}
				if(response != null){
					response.close();
				}
				if (isCanceled && file != null) {
					file.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
				LogUtil.i(TAG,"----e1:  "+e.toString());
			}
		}
		
		LogUtil.i(TAG,"-------OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED-----");
		return OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		int progress = values[0];
		if (progress > lastProgress) {
			listener.onProgress(progress);
			lastProgress = progress;
			LogUtil.i(TAG, "onProgressUpdate:  "+lastProgress);
			listener.sendDownloadInfo(new DownloadInfo(downloadUrl, downloadLocalPath, otaFileLength * lastProgress / 100, lastProgress, OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING));
		}
	}

	@Override
	protected void onPostExecute(Integer status) {
	    listener.sendDownloadInfo(new DownloadInfo(downloadUrl, downloadLocalPath, otaFileLength * lastProgress / 100 , lastProgress, status));
	    
		switch (status) {
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_SUCCESS:
				listener.onSuccess();
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_PAUSED:
				listener.onPaused();
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_FAILED:
				listener.onFailed();
				break;
			case OTAConfigConstant.DownloadStatus.DOWNLOAD_STATUS_CANCELED:
				listener.onCanceled();
				break;
			default:
				break;
		}
	}

	/**
	 * 暂停下载
	 */
	public void pauseDownload() {
		isPaused = true;
	}

	/**
	 * 取消下载
	 */
	public void cancelDownload() {
		isCanceled = true;
	}

	private long getContentLength(String downloadUrl){
		try {
			LogUtil.i(TAG,"getContentLength__downloadUrl:   "+downloadUrl);
			OkHttpClient client ;
			if(downloadUrl.contains("https:")){
				client = OkHttpClientUtil.getUnsafeOkHttpClient();
			}else{
				client = new OkHttpClient();
			}
			Request request = new Request.Builder().url(downloadUrl).build();
			Response response = client.newCall(request).execute();
			if (response != null && response.isSuccessful()) {
				long contentLength = response.body().contentLength();
				response.body().close();
				LogUtil.i(TAG,"getContentLength__contentLength:   "+contentLength);
				return contentLength;
			}
		} catch (Exception e) {
			LogUtil.i(TAG,"getContentLength__Exception:   "+e.toString());
			e.printStackTrace();
		}
		return 0;
	}
}
