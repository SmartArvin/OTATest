package com.ktc.ota.utils;


/**
 * @author Arvin
 * @TODO OTA升级属性配置
 * @Date 2019.1.24
 * 
 */
public class OTAConfigConstant {
	
	//配置信息
    public static class CONFIG{
        public static final int MAX_TIP_COUNT = 3 ;
    }

	//OTA文件名称及文件地址
    public static class OTAFILE{
        public static final String LOCAL_OTA_NAME = "update.zip";
        public static final String DATA_OTA_DIR = "/data/ktcOta/";
    }
    
    //远程文件下载过程中状态标志位
    public static class DownloadStatus{
    	public static final int DOWNLOAD_STATUS_DOWNLOADING = 0x10;
    	public static final int DOWNLOAD_STATUS_SUCCESS = 0x11;
    	public static final int DOWNLOAD_STATUS_PAUSED = 0x12;
    	public static final int DOWNLOAD_STATUS_FAILED = 0x13;
    	public static final int DOWNLOAD_STATUS_CANCELED = 0x14;
    	
    	//下载完成后标记校验状态
    	public static final int DOWNLOAD_VERIFY_ING = 0x15;
    	public static final int DOWNLOAD_VERIFY_FAIL = 0x16;
    	public static final int DOWNLOAD_VERIFY_SUCCESS = 0x17;
    	public static final int DOWNLOAD_VERIFY_STOP = 0x18;
    }
    
    //action传递数据时的key
    public static class MSGKey{
    	public static final int MSG_REMOTE_REQUEST_STATE_ING = 0x21 ;
    	public static final int MSG_REMOTE_REQUEST_STATE_NO_NET = 0x22 ;
    	public static final int MSG_REMOTE_REQUEST_STATE_NO_SPACE = 0x23 ;
        public static final int MSG_REMOTE_OTA_DATA_SUCCESS = 0x24;
        public static final int MSG_REMOTE_OTA_DATA_EXCEPTION = 0x25;
        public static final int MSG_REMOTE_OTA_DATA_NO_RESPONSE = 0x26;
    }
    
    //action传递数据时的key
    public static class ActionKey{
        public static final String SERVICE_INTENT_EXTRA = "SERVICE_INTENT_EXTRA";
        public static final String KEY_REMOTE_OTA_DATA = "KEY_REMOTE_OTA_DATA";//标记远程OTA升级信息
        
        public static final String KEY_OTA_UPDATE_FLAG = "KEY_OTA_UPDATE_FLAG";//标记OTA升级成功|失败
        public static final String KEY_OTA_INFO = "KEY_OTA_INFO";//标记OTA升级包大小
        
        public static final String KEY_DOWNLOAD_INFO = "KEY_DOWNLOAD_INFO";//OTA升级包下载信息
    }
    
    //主入口组件标志
    public static class ICONSTATE{
    	public static final int ICONSTATE_NORMAL = 0x30;
    	public static final int ICONSTATE_UPDATE = 0x31;
    }
}
