package com.ktc.ota.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.ktc.ota.utils.SharedPreferencesUtil.ContentValue;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;


/**
 * @author Arvin
 * @TODO 获取设备基础信息
 * @Date 2019.1.24
 */
public class DeviceInfoUtil {
	private static final String TAG = "DeviceInfoUtils";
	private static OtaUrlBody mOtaUrlBody ;
	
	/**
	 * 获取本机正式/备用服务器请求地址
	 * @param mContext
	 * @return String[]{正式请求 , 备用请求}
	 */
	public static String[] getRemoteUris(Context mContext) {
		try {
			constructOtaUrlBody();
			if(mOtaUrlBody != null && mOtaUrlBody.toString().length() > 0){
				LogUtil.v(TAG, "getRemoteUris__mOtaUrlBody：  " + mOtaUrlBody);
				
				String uriHeadFormal = "http://" + mOtaUrlBody.getUrlHost();
				String uriHeadBack = "http://" + mOtaUrlBody.getUrlBackHost();
				
				String uriBody = "/Otaservlet/index?model=" + mOtaUrlBody.getProductName() 
						+ "&product=" + mOtaUrlBody.getCustomerName()
						+ "&boardtype=" + mOtaUrlBody.getBoardType() 
						+ "_" + mOtaUrlBody.getBoardType_ota()
						+ "_" + mOtaUrlBody.getBoardType_Timezone() 
						+ "_" + mOtaUrlBody.getBoardType_Language()
						+ "_" + mOtaUrlBody.getBoardType_Reserved() 
						+ "_" + mOtaUrlBody.getBoardMemory() 
						+ "_" + mOtaUrlBody.getLcd_Density() 
						+ "&version=" + mOtaUrlBody.getSystemVersion()
						+ "&sdanum=" + getSDANum_Ini() 
						+ "&macaddress=" + getMac()
						+ "&request_type=" + getRequestType();
				
				LogUtil.v(TAG, "uriHeadFormal=" + uriHeadFormal);
				LogUtil.v(TAG, "uriHeadBack=" + uriHeadBack);
				LogUtil.v(TAG, "uriBody=" + uriBody);
				return new String[]{uriHeadFormal + uriBody , uriHeadBack + uriBody};
			}
		} catch (Exception e) {
			LogUtil.v(TAG, "getRemoteUris__Exception:   " + e.toString());
		}
		
		return null;
	}
	
	/**
	 * 封装本机软硬件信息
	 * @return OtaUrlBody
	 */
	private static OtaUrlBody constructOtaUrlBody() {
		mOtaUrlBody = null;
		mOtaUrlBody = new OtaUrlBody();

		String filePath;
		String value = "";
		Properties props = new Properties();
		InputStream in = null;
		try {
			if (Build.VERSION.SDK_INT >= 26) {
				filePath = "/vendor/build.prop";
			} else {
				filePath = "/system/build.prop";
			}
			in = new BufferedInputStream(new FileInputStream(filePath));
			props.load(in);
			
			mOtaUrlBody.setUrlHost(refactStr(props.getProperty("ro.product.ota.host")));
			mOtaUrlBody.setUrlBackHost(refactStr(props.getProperty("ro.product.ota.host2")));
			mOtaUrlBody.setProductName(refactStr(props.getProperty("ktc.ota.model")));
			mOtaUrlBody.setCustomerName(refactStr(props.getProperty("ktc.ota.customer")));
			mOtaUrlBody.setBoardType(refactStr(props.getProperty("ktc.board.type")));
			mOtaUrlBody.setBoardType_ota(getBoardType_ota());
			
			String mTimeZone = props.getProperty("persist.sys.timezone");
			mOtaUrlBody.setBoardType_Timezone(mTimeZone.substring(mTimeZone.indexOf("/") + 1).replaceAll(";", "")
					.replaceAll(" ", "").trim());
			
			mOtaUrlBody.setBoardType_Language(refactStr(props.getProperty("persist.sys.language")));
			mOtaUrlBody.setBoardType_Reserved(refactStr(props.getProperty("ktc.board.reserved")));
			mOtaUrlBody.setBoardMemory(refactStr(props.getProperty("ktc.board.memory")));
			mOtaUrlBody.setLcd_Density(refactStr(props.getProperty("ro.sf.lcd_density")));
			mOtaUrlBody.setSystemVersion(refactStr(props.getProperty("ro.product.version")));

			in.close();

		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(in != null){
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		return mOtaUrlBody;
	}
	
	/**
	 * 获取本机有线mac地址
	 * @return String
	 */
	private static String getMac() {
		String str_mac = "";
		try {
			str_mac = TvManager.getInstance().getEnvironment("ethaddr");
		} catch (TvCommonException e) {
			e.printStackTrace();
		}
		if (str_mac != null) {
			str_mac = str_mac.replaceAll(":", "");
		}
		return str_mac;
	}

	/**
	 * 获取网络请求类型(本机记忆的)
	 * @return String
	 */
	public static String getRequestType() {
		String mOtaRequestType = SharedPreferencesUtil.getInstance().getString("mOtaRequestType") ;
		return mOtaRequestType == null ? "0" : mOtaRequestType;
	}

	/**
	 * 更新网络请求类型(本机记忆的)
	 * @return String
	 */
	public static void setRequestType(String value) {
		SharedPreferencesUtil.getInstance().putValues(new ContentValue("mOtaRequestType", value));
	}
	
	/**
	 * 获取网络升级提示次数(本机记忆的)
	 * @return String
	 */
	public static String getRemoteTipCount() {
		String mOtaRemoteTipCount = SharedPreferencesUtil.getInstance().getString("mOtaRemoteTipCount") ;
		return mOtaRemoteTipCount == null ? "0" : mOtaRemoteTipCount;
	}

	/**
	 * 更新网络请求类型(本机记忆的)
	 * @return String
	 */
	public static void setRemoteTipCount(String value) {
		SharedPreferencesUtil.getInstance().putValues(new ContentValue("mOtaRemoteTipCount", value));
	}
	
	/**
	 * Get BOARD_NO from Supernova by ktc.sn.ota.board.type
	 * @return String
	 */
    private static String getBoardType_ota(){
    	String boardType = SystemProperties.get("ktc.sn.ota.board.type");
    	boardType = boardType.replaceAll("\"", "");
    	boardType = boardType.replaceAll(";", "");
    	boardType = boardType.replaceAll(" ", "");
    	
    	return boardType;
    }

    /**
	 * 获取软件SDA单号
	 * @return String
	 */
	private static String getSDANum_Ini() {
		String sdaNum = getIniProp("PRODUCT_SDA_NO");
		if(sdaNum == null || TextUtils.isEmpty(sdaNum)){
			String serialNum = getBuildProp("ro.product.serial") ;
			if(serialNum != null && !TextUtils.isEmpty(serialNum)){
				if(serialNum.contains("SDA") && !serialNum.contains("SDA12345")){
					sdaNum = serialNum ;
				}
			}
		}
		return sdaNum;
	}

	/**
	 * 获取build.prop指定key的值
	 * @param key
	 * @return String
	 */
	public static String getBuildProp(String key){
		try {
			if (Build.VERSION.SDK_INT >= 26) {
				return getKeyValue("/vendor/build.prop", key);
			} else {
				return getKeyValue("/system/build.prop", key);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";
	}
	
	/**
	 * 获取Customer_1.ini指定key的值
	 * @param key
	 * @return String
	 */
	public static String getIniProp(String key){
		try {
			if (android.os.Build.VERSION.SDK_INT >= 26) {
				return getKeyValue("tvconfig/config/model/Customer_1.ini", key);
			} else {
				return getKeyValue("/config/model/Customer_1.ini", key);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";
	}
	
	
	/**
	 * 获取build.prop(区分8.0及其他)指定key的值
	 * @param file , key
	 * @return String
	 */
	private static String getKeyValue(String file, String key) {
		String value = "";
		Properties props = new Properties();
		InputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			props.load(in);
			value = props.getProperty(key);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!TextUtils.isEmpty(value)) {
			if (value.contains(";")) {
				String[] array = value.split(";");
				if (array[0].length() > 0) {
					value = array[0];
				}
			}
			//timezone is special
			if(key.equals("persist.sys.timezone")){
				return value ;
			}
			
			return refactStr(value);
		}
		return "";
	}
	
	/**
	 * 重构字符串(删除空格及其他特殊字符)
	 * @param str
	 * @return String
	 */
	private static String refactStr(String str){
		if (str == null || str.length() == 0) {
			str = "";
		}
		return str.replaceAll("\"", "").replaceAll(";", "")
					.replaceAll(" ", "").trim();
	}
	
	
	/**
	 * @TODO 获取当前系统的时区
	 * @return String
	 */
	public static String getSystemTimeZone(){
		String mTimeZone = getBuildProp("persist.sys.timezone");
		return mTimeZone.substring(mTimeZone.indexOf("/") + 1).replaceAll(";", "")
				.replaceAll(" ", "").trim();
	}
	
	/**
	 * @TODO 获取当前系统的语言类型
	 * @return String
	 */
	public static String getSystemLanguage(){
		return getBuildProp("persist.sys.language");
	}
	
	/**
	 * @TODO 获取当前系统的内存大小
	 * @return String
	 */
	public static String getSystemMemory(){
		return getBuildProp("ktc.board.memory");
	}
	
	/**
	 * @TODO 获取当前系统的软件版本
	 * @return String
	 */
	public static String getSystemVersion(){
		return getBuildProp("ro.product.version");
	}
	
	/**
	 * @TODO 获取当前系统的编译时间
	 * @return String
	 */
	public static String getSystemBuildTime(){
		try {
			if (Build.VERSION.SDK_INT >= 26) {
				return getBuildProp("ro.vendor.build.date.utc");
			} else {
				return getBuildProp("ro.build.date.utc");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";
	}
	
	
	static class OtaUrlBody {
		
		private String 
		urlHost ,
		urlBackHost ,
		productName , customerName , 
		boardType , boardType_ota ,
		boardType_Timezone , boardType_Language ,
		boardType_Reserved , boardMemory ,
		lcd_Density , systemVersion;
		
		public String getUrlHost() {
			return urlHost;
		}

		public void setUrlHost(String urlHost) {
			this.urlHost = urlHost;
		}
		
		public String getUrlBackHost() {
			return urlBackHost;
		}

		public void setUrlBackHost(String urlBackHost) {
			this.urlBackHost = urlBackHost;
		}

		public String getProductName() {
			return productName;
		}

		public void setProductName(String productName) {
			this.productName = productName;
		}

		public String getCustomerName() {
			return customerName;
		}

		public void setCustomerName(String customerName) {
			this.customerName = customerName;
		}

		public String getBoardType() {
			return boardType;
		}

		public void setBoardType(String boardType) {
			this.boardType = boardType;
		}

		public String getBoardType_ota() {
			return boardType_ota;
		}

		public void setBoardType_ota(String boardType_ota) {
			this.boardType_ota = boardType_ota;
		}

		public String getBoardType_Timezone() {
			return boardType_Timezone;
		}

		public void setBoardType_Timezone(String boardType_Timezone) {
			this.boardType_Timezone = boardType_Timezone;
		}

		public String getBoardType_Language() {
			return boardType_Language;
		}

		public void setBoardType_Language(String boardType_Language) {
			this.boardType_Language = boardType_Language;
		}

		public String getBoardType_Reserved() {
			return boardType_Reserved;
		}

		public void setBoardType_Reserved(String boardType_Reserved) {
			this.boardType_Reserved = boardType_Reserved;
		}

		public String getBoardMemory() {
			return boardMemory;
		}

		public void setBoardMemory(String boardMemory) {
			this.boardMemory = boardMemory;
		}

		public String getLcd_Density() {
			return lcd_Density;
		}

		public void setLcd_Density(String lcd_Density) {
			this.lcd_Density = lcd_Density;
		}

		public String getSystemVersion() {
			return systemVersion;
		}

		public void setSystemVersion(String systemVersion) {
			this.systemVersion = systemVersion;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return urlHost + ";" + urlBackHost + ";" + productName + ";" + customerName + ";"
					+ boardType + ";" + boardType_ota + ";" + boardType_Timezone + ";" 
					+ boardType_Language + ";" + boardType_Reserved + ";" + boardMemory
					+ ";" + lcd_Density + ";" + systemVersion;
		}
	}
}
