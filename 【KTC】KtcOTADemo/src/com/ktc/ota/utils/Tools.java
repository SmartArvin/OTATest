package com.ktc.ota.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.ktc.ota.bean.DiskData;
import com.ktc.ota.bean.OtaRemoteData;
import com.ktc.ota.main.application.OtaUpdateApplication;
import com.mstar.android.storage.MStorageManager;
import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tv.TvTimerManager;
import com.mstar.android.tvapi.common.TvManager;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.RecoverySystem;
import android.os.StatFs;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.systemupdate.service.R;
import android.text.TextUtils;
import android.view.KeyEvent;

/**
 * @author Arvin
 * @TODO 封装工具类
 * @Date 2019.1.23
 */
public class Tools {
	private static final String TAG = "Tools";

	// 内部存储挂载节点
	private static final String INTERNAL_STORAGE_PATH = "/storage/emulated/0";
	
	//读取recovery中升级状态记录
	private static File RECOVERY_DIR = new File("/cache/recovery");
	private static File UPDATE_FLAG_FILE = new File(RECOVERY_DIR , "last_install");
	private static final String COMMAND_FLAG_SUCCESS = "1";

	
	/**
	 * 格式化文件大小(KB;MB;GB)
	 * @param length
	 * @return String
	 */
	public static String formatSize(long length) {
		if (length < 1024) {
			return String.valueOf(length) + "B";
		} else if (length / 1024 > 0 && length / 1024 / 1024 == 0) {
			return String.valueOf(length / 1024) + "KB";
		} else if (length / 1024 / 1024 > 0) {
			return String.valueOf(length / 1024 / 1024) + "MB";
		} else {
			return String.valueOf(length / 1024 / 1024 / 1024) + "GB";
		}
	}
	
	/**
	 * 判断网络是否连接
	 * @param context
	 * @return
	 */
	public static boolean isNetWorkConnected(Context context) {
		ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isAvailable()
				&& networkInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 读取升级完成状态标志位(1：成功)
	 * @return boolean
	 */
	public static boolean isOtaUpdateSuccess() {
		try {
			String command = readFlagCommand();
			if (command != null) {
				LogUtil.i(TAG, "readFlagCommand:  "+command);
				String path;
				String flag;
				int nIndex = command.indexOf('\n');
				path = command.substring(0, nIndex);
				flag = command.substring(nIndex + 1, nIndex + 2);

				LogUtil.i(TAG, "path:  "+path);

				LogUtil.i(TAG, "flag:  "+flag);
				if (flag.equals(COMMAND_FLAG_SUCCESS)) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.i(TAG, "isOtaUpdateSuccess_e:  "+e.toString());
		}
		return false;
	}

	/**
	 * 读取文件内容
	 * @return String
	 */
	private static String readFlagCommand() {
		if (UPDATE_FLAG_FILE.exists()) {
			char[] buf = new char[128];
			int readCount = 0;
			try {
				FileReader reader = new FileReader(UPDATE_FLAG_FILE);
				readCount = reader.read(buf, 0, buf.length);
				LogUtil.d(TAG, "readCount = " + readCount + " buf.length = "
						+ buf.length);
				if (-1 == readCount) {
					LogUtil.d(TAG, "no chars read,file is empty!!");
					return null;
				}
			} catch (IOException e) {
				LogUtil.e(TAG, "can not read /cache/recovery/flag!");
			}

			StringBuilder sBuilder = new StringBuilder();
			for (int i = 0; i < readCount; i++) {
				if (buf[i] == 0) {
					break;
				}
				sBuilder.append(buf[i]);
			}
			return sBuilder.toString();
		} else {
			return null;
		}
	}
	
	/**
	 * @TODO 安装已下载的OTA升级包
	 * @param mContext  otaFilePath
	 */
	public static void installOtaFile(Context mContext , String otaFilePath) {
		if(otaFilePath == null || TextUtils.isEmpty(otaFilePath)){
			LogUtil.i(TAG, "installPackage invalid");
			return ;
		}
		try {
			File otaFile = new File(otaFilePath) ;
			if(otaFile != null && otaFile.exists()){
				LogUtil.i(TAG, "It will install package");
				RecoverySystemUtils.installPackage(mContext, otaFile);
			}else{
				LogUtil.i(TAG, otaFilePath+"   isNotExist");
			}
		} catch (IOException e) {
			LogUtil.i(TAG, "installPackage Error:  " + e.toString());
			e.printStackTrace();
		}
	}
	
	/**
	 * @TODO 删除cache/recovery中生成的校验文件以及升级后状态标志位信息
	 * @param null
	 */
	public static void deleteRecoveryFlag(){
		LogUtil.i(TAG , "------------deleteRecoveryFlag----------");
		if(RECOVERY_DIR != null && RECOVERY_DIR.isDirectory() && RECOVERY_DIR.listFiles().length > 0){
			for(File tmpFile : RECOVERY_DIR.listFiles()){
				tmpFile.delete();
			}
		}
	}
	
	/**
	 * @TODO 获取已下载的OTA升级包下载的文件大小
	 * @return long
	 */
	public static long getDataOtaFileLength(OtaRemoteData mOtaRemoteData){
		if(mOtaRemoteData != null){
			String mOtaFilePath = getDataOtaFilePath(mOtaRemoteData) ;
			if(!TextUtils.isEmpty(mOtaFilePath)){
				File mDataOtaFile = new File(mOtaFilePath);
				if(mDataOtaFile != null && mDataOtaFile.exists()){
					return mDataOtaFile.length();
				}
			}
		}
		
		return 0 ;
	}
	
	/**
	 * @TODO 根据OTA网络数据获取本地已下载的文件路径
	 * @return String
	 */
	public static String getDataOtaFilePath(OtaRemoteData mOtaRemoteData){
		if(mOtaRemoteData != null){
			String mOtaPkgName = mOtaRemoteData.getOtaPkgName() ;
			String mOtaVersion = mOtaRemoteData.getOtaPkgVersion() ;
			
			String mOtaFileName = mOtaPkgName.substring(0, mOtaPkgName.lastIndexOf("."))
					+"_"
					+mOtaVersion
					+mOtaPkgName.substring(mOtaPkgName.lastIndexOf("."), mOtaPkgName.length()) ;
			return OTAConfigConstant.OTAFILE.DATA_OTA_DIR + mOtaFileName;
		}
		return null ;
	}
	
	/**
	 * @TODO 删除/data/ktcOta/中已下载的升级包文件
	 * @param null
	 *
	 */
	public static void deleteDataOtaFile(){
		deleteRecoveryFlag();
		LogUtil.i(TAG, "-------deleteDataOtaFile-----");
		File mDataOtaDir = new File(OTAConfigConstant.OTAFILE.DATA_OTA_DIR);
		if(mDataOtaDir.isDirectory() && mDataOtaDir.listFiles().length > 0){
			for(File tmpFile : mDataOtaDir.listFiles()){
				LogUtil.i(TAG, "getName:  "+tmpFile.getName());
				if(getFileExName(tmpFile.getName()).equals("zip")){
					LogUtil.i(TAG, "deleteDataOtaFile:  "+tmpFile.getAbsolutePath());
					tmpFile.delete();
				}
			}
		}
	}
	
	/**
	 * @TODO 获取/data/ktcOta/中已下载的升级包文件
	 * @param null
	 *
	 */
	public static File getDataOtaFile(){
		LogUtil.i(TAG, "-------getDataOtaFile-----");
		File mDataOtaDir = new File(OTAConfigConstant.OTAFILE.DATA_OTA_DIR);
		if(mDataOtaDir.isDirectory() && mDataOtaDir.listFiles().length > 0){
			for(File tmpFile : mDataOtaDir.listFiles()){
				LogUtil.i(TAG, "getName:  "+tmpFile.getName());
				if(getFileExName(tmpFile.getName()).equals("zip")){
					return tmpFile;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * @TODO 判断/data/ktcOta/update.zip中已下载的文件
	 * @param null
	 *
	 */
	public static boolean hasDataOtaFile(OtaRemoteData mOtaRemoteData){
		if(mOtaRemoteData != null){
			String mOtaDataFilePath = getDataOtaFilePath(mOtaRemoteData);
			if(!TextUtils.isEmpty(mOtaDataFilePath)){
				File mDataOtaFile = new File(mOtaDataFilePath);
				if(mDataOtaFile != null && mDataOtaFile.exists()){
					return true ;
				}
			}
		}
		return false ;
	}

	
	/*
    * 获取文件扩展名
    * */
    private static String getFileExName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }
	
	/**
	 * 获取当前data分区剩余空间大小
	 * @return long
	 */
	public static long getDataFreeSize() {
		StatFs sf = new StatFs("/data");
		long blockSize = sf.getBlockSize();
		long availCount = sf.getAvailableBlocks();

		return availCount * blockSize;
	}

	/**
	 * 获取指定文件大小
	 * @param filePath
	 * @return long
	 */
	public static long getFileSize(String filePath) {
		File f = new File(filePath);
		if (f.exists()) {
			return f.length();
		}
		return 0;
	}
	
	/**
	 * 获取指定文件大小
	 * @param File
	 * @return long
	 */
	public static long getFileSize(File file) {
		if (file != null && file.exists()) {
			return file.length();
		}
		return 0;
	}

	
	/**
	 * 格式化Unix格式时间
	 * @param unixTime
	 * @return
	 */
	public static String formatUnixTime(String unixTime) {
		if(unixTime == null || unixTime.equals("")){
			return "";
		}
		Long timestamp = Long.parseLong(unixTime) * 1000;
		String date = new java.text.SimpleDateFormat("yyyy-MM-dd")
				.format(new java.util.Date(timestamp));
		return date;
	}

	/**
	 * TODO 获取已挂载的第一个U盘路径(eg:/mnt/usb/sda1/)
	 * 
	 * @param Context
	 * @return DiskData
	 */
	public static DiskData getFirstUsbDisk(Context mContext) {
		MStorageManager storageManager = MStorageManager.getInstance(mContext);
		String[] volumes = storageManager.getVolumePaths();
		for (String mVolumePath : volumes) {
			LogUtil.i(TAG, "mVolumePath:  "+mVolumePath);
			// filter sdcard
			if(mVolumePath.equals("/mnt/sdcard") || mVolumePath.equals(INTERNAL_STORAGE_PATH)){
				File sdFile = new File(mVolumePath);
				File emulatedFile = new File(INTERNAL_STORAGE_PATH);
				boolean hasSd = false ;
				boolean hasEmulatedDir = false ;
				if(emulatedFile != null && emulatedFile.exists()){
					File[] sdFiles = sdFile.listFiles();
					hasEmulatedDir = sdFiles != null && sdFiles.length > 0 ;
				}
				LogUtil.i(TAG, "hasEmulatedDir: "+hasEmulatedDir);
				
				if(sdFile != null && sdFile.exists()){
					File[] sdFiles = sdFile.listFiles();
					hasSd = sdFiles != null && sdFiles.length > 0 ;
					if(hasSd){
						LogUtil.i(TAG, "hasSd: "+hasSd);
						if(hasEmulatedDir){
							continue ;
						}else{//usb作SD
							DiskData mDiskData = new DiskData();
							mDiskData.setPath(mVolumePath.endsWith("/") ? mVolumePath
									: mVolumePath + "/");
							mDiskData.setName(mContext.getResources().getString(
									R.string.str_inner_storage));
							
							return mDiskData ;
						}
					}
				}
				continue;
			}

			String state = storageManager.getVolumeState(mVolumePath);
			if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
				continue;
			} else {
				DiskData mDiskData = new DiskData();
				String volumeLabel = storageManager.getVolumeLabel(mVolumePath);
				if (volumeLabel != null) {
					String[] tempVolumeLabel = volumeLabel.split(" ");
					volumeLabel = "";
					for (int j = 0; j < tempVolumeLabel.length; j++) {
						if (j != tempVolumeLabel.length - 1) {
							volumeLabel += tempVolumeLabel[j] + " ";
							continue;
						}
						volumeLabel += tempVolumeLabel[j];
					}
				}
				if (StringEncoder.isUTF8(mVolumePath)) {
					mVolumePath = StringEncoder.convertString(mVolumePath,
							"UTF-8");
				} else {
					mVolumePath = StringEncoder.convertString(mVolumePath,
							"GBK");
				}

				if (mVolumePath.equals(INTERNAL_STORAGE_PATH)) {
					mDiskData.setPath(mVolumePath.endsWith("/") ? mVolumePath
							: mVolumePath + "/");
					mDiskData.setName(mContext.getResources().getString(
							R.string.str_inner_storage));
				} else {
					mDiskData.setPath(mVolumePath.endsWith("/") ? mVolumePath
							: mVolumePath + "/");
					if (null == volumeLabel) {
						mDiskData.setName(mContext.getResources().getString(
								R.string.str_storage_default));
					} else {
						mDiskData.setName(volumeLabel);
					}
				}
				
				LogUtil.i(TAG, "getFirstUsbDisk:  "+mDiskData.getPath());
				return mDiskData;
			}
		}
		return null;
	}

	/**
	 * @TODO 获取当前系统USB磁盘挂载列表
	 * @param Context
	 * @return List<DiskData>
	 */
	public static List<DiskData> getUsbDiskList(Context mContext) {
		List<DiskData> deviceList = new ArrayList<DiskData>();
		List<DiskData> srcList = new ArrayList<DiskData>();

		MStorageManager storageManager = MStorageManager.getInstance(mContext);
		String[] volumes = storageManager.getVolumePaths();
		if (volumes == null || (volumes.length == 0)) {
			return null;
		}

		File file = new File("proc/mounts");
		if (!file.exists() || file.isDirectory()) {
			file = null;
		}

		DiskData mInterData = null;
		for (int i = 0; i < volumes.length; ++i) {
			String state = storageManager.getVolumeState(volumes[i]);
			if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
				continue;
			} else {
				DiskData mDiskData = new DiskData();
				String path = volumes[i];
				String[] pathPartition = path.split("/");
				String label = pathPartition[pathPartition.length - 1];
				String volumeLabel = storageManager.getVolumeLabel(path);
				if (volumeLabel != null) {
					String[] tempVolumeLabel = volumeLabel.split(" ");
					volumeLabel = "";
					for (int j = 0; j < tempVolumeLabel.length; j++) {
						if (j != tempVolumeLabel.length - 1) {
							volumeLabel += tempVolumeLabel[j] + " ";
							continue;
						}
						volumeLabel += tempVolumeLabel[j];
					}
				}
				if (StringEncoder.isUTF8(path)) {
					path = StringEncoder.convertString(path, "UTF-8");
				} else {
					path = StringEncoder.convertString(path, "GBK");
				}
				if (path.equals(INTERNAL_STORAGE_PATH)) {
					mInterData = new DiskData();
					mInterData.setPath(path);
					mInterData.setName(mContext.getResources().getString(
							R.string.str_inner_storage));
				} else {
					mDiskData.setPath(path);
					if (null == volumeLabel) {
						mDiskData.setName(mContext.getResources().getString(
								R.string.str_storage_default));
					} else {
						mDiskData.setName(volumeLabel);
					}
					srcList.add(mDiskData);
				}
			}
		}
		deviceList.addAll(getCollectList(srcList));
		if (mInterData != null) {
			deviceList.add(mInterData);
		}
		return deviceList;
	}

	private static Comparator<DiskData> comparator = new Comparator<DiskData>() {

		@Override
		public int compare(DiskData lData, DiskData rData) {

			String lName = lData.getName();
			String rName = rData.getName();
			if (lName != null && rName != null) {
				Collator collator = Collator.getInstance(Locale.CHINA);
				return collator.compare(lName.toLowerCase(),
						rName.toLowerCase());

			} else {
				return 0;
			}

		}
	};

	private static List<DiskData> getCollectList(List<DiskData> src) {
		try {
			Collections.sort(src, comparator);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return src;
	}
	
	
	/**
	 * 判断某一服务是否正在运行
	 * @param mContext
	 * @param serviceName
	 * @return boolean
	 */
	public static boolean isServiceWorking(Context mContext, String serviceName) {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> mServiceList = am.getRunningServices(50);
        if (mServiceList.size() <= 0) {
        	LogUtil.i(TAG, serviceName + "   isNotServiceWorking");
            return false;
        }
        
        for (RunningServiceInfo mServiceInfo : mServiceList) {
            String mName = mServiceInfo.service.getClassName().toString();
            if (mName.contains(serviceName)) {
            	LogUtil.i(TAG, serviceName + "   isServiceWorking");
                return true ;
            }
        }
        LogUtil.i(TAG, serviceName + "   isNotServiceWorking");
        return false;
    }
	
	
	/**
	* 判断当前栈顶界面是否为某进程
	* TODO 判断当前是否为pkgName
	* @throws IOException 
	* @return boolean 
	*/
	public static boolean isTopCurRunningPackage(String[] nameList) {
		if (nameList != null && nameList.length > 0) {
			String result = getTopRunningPackage();
			for(String tmpPkg : nameList){
				LogUtil.i(TAG, "tmpPkg:  "+tmpPkg);
				if (result != null && result.contains(tmpPkg)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 获取当前前台进程的名称
	 * @return String
	 * 
	 */
	public static String getTopRunningPackage(){
		try{
			String cmd ="dumpsys activity activities";
			String filter ;
			if (android.os.Build.VERSION.SDK_INT >= 26) {
				filter = "mResumedActivity";
			} else {
				filter = "mFocusedActivity";
			}
			String result = command(cmd , filter);
			LogUtil.i(TAG, "result:  "+result);
			return result ;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	       
	       
	/**
	* 
	* TODO 运行shell命令过滤指定信息
	* @param cmd 
	* @param filter 
	* @return String
	* @throws IOException 
	*/
	private static String command(String cmd, String filter) {
		Process process = null ;
		InputStream stream = null; 
		InputStreamReader reader = null;
		BufferedReader bufferedReader = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			stream = process.getInputStream();
			if (stream != null) {
				reader = new InputStreamReader(stream);
				bufferedReader = new BufferedReader(reader, 1024);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.contains(filter)) {
						break;
					}
				}
				bufferedReader.close();
				reader.close();
				stream.close();
				process.destroy();
				return line;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}finally{
			try {
				if(bufferedReader != null){
					bufferedReader.close();
				}
				if(reader != null ){
					reader.close() ;
				}
				if(stream != null){
					stream.close();
				}
				if(process != null){
					process.destroy();
				}
			} catch (Exception e) {
				e.printStackTrace() ;
			}
		}
		return null;
	}
	
	/**
	 * 动态改变入口icon图标
     * @param modeType ICONSTATE_NORMAL：为默认图标及名称  ICONSTATE_UPDATE：为用普通图标
     */
	public static void switchIconState(Context mContext, int modeType) {
		LogUtil.i(TAG, "switchIcon:  "+modeType);
		try {
			PackageManager pm = mContext.getPackageManager();
			
			// 要跟manifest的activity-alias 的name保持一致
			ComponentName normalComponent = new ComponentName(mContext , "android.systemupdate.activitys.icon_normal");
			ComponentName updateComponent = new ComponentName(mContext , "android.systemupdate.activitys.icon_update");

			if(modeType == OTAConfigConstant.ICONSTATE.ICONSTATE_NORMAL){//normal
				switchComponentState(pm, normalComponent, true) ;
				switchComponentState(pm, updateComponent, false) ;
			}else if(modeType == OTAConfigConstant.ICONSTATE.ICONSTATE_UPDATE){//update
				switchComponentState(pm, normalComponent, false) ;
				switchComponentState(pm, updateComponent, true) ;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * 切换组件状态
     * @param componentName
     */
    private static void switchComponentState(PackageManager pm , ComponentName componentName , boolean enable) {
    	int state = pm.getComponentEnabledSetting(componentName);
    	if(enable){
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return;
            }
            pm.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
    	}else{
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                return;
            }
            pm.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
    	}
    }
    
    
    /**
     * 切换至Storage信源
     * @return void
     */
    public static void changeToStorage() {
        final TvCommonManager mTvCommonManager = TvCommonManager.getInstance();
        int curSource = mTvCommonManager.getCurrentTvInputSource();
        if (mTvCommonManager != null) {
            if (curSource != TvCommonManager.INPUT_SOURCE_STORAGE) {
                Runnable sourceRunnable = new Runnable() {
                    @Override
                    public void run() {
                    	try {
                    		mTvCommonManager.setInputSource(TvCommonManager.INPUT_SOURCE_STORAGE);
                    		TvTimerManager.getInstance().setOffTimerEnable(false);
                    		TvTimerManager.getInstance().setSleepTimeMode(
                    				TvTimerManager.SLEEP_TIME_OFF);
                    		TvManager.getInstance().setTvosCommonCommand(
                    				"SetAutoSleepOffStatus");
						} catch (Exception e) {
							e.printStackTrace() ;
							LogUtil.i(TAG, "changeToStorage_e:  "+e.toString());
						}
                    }
                };
                new Thread(sourceRunnable).start();
            }
        }
    }
    
    /**
     * 是否属于Storage信源
     * @return void
     */
    public static boolean isCurStorage(){
    	final TvCommonManager mTvCommonManager = TvCommonManager.getInstance();
        int curSource = mTvCommonManager.getCurrentTvInputSource();
        if (curSource == TvCommonManager.INPUT_SOURCE_STORAGE) {
        	return true ;
        }
        return false ;
    }
    
    /**
     * 判断当前系统是否可响应Home键
     * 说明：OOBE和搜台界面禁止响应
     * @param mContext
     * @return boolean
     */
    public static boolean isHomeKeyEnable(Context mContext ){
    	try {
			int flag = Settings.System.getInt(mContext.getContentResolver(), "home_hot_key_disable") ;
			if(flag == 1){//disable
				return false;
			}
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return true ;
    }
    
    /**
     * 回退至主页Home 
     */
    public static void backToHome() {
        new Thread() {
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.toString());
                }
            }
        }.start();
        
        OtaUpdateApplication.getInstance().exit();
    }
}
