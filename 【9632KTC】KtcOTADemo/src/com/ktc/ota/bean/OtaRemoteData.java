package com.ktc.ota.bean;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * mOtaPackageLength:  568075448
 * mOtaPackageName:  update.zip
 * mOtaPackageVersion:  V1.1.0
 * mOtaUpdateLevel:  未知
 * mTargetURI:  http://ktcota2test.oss-cn-hangzhou.aliyuncs.com/848DTMB/ktc_en/update.zip
 * mOtaAnswer:  1
 * mOtaForce:  0
*/

//远程OTA信息：  update.zip:481427694:V1.1.0:http://ktcota2test.oss-cn-hangzhou.aliyuncs.com/358DVB/ktc/update.zip:未知:1:1
/**
 * @author Arvin
 * @TODO 远程OTA升级包信息
 * @Date 2019.1.24
 */
public class OtaRemoteData implements Parcelable{

	private String otaPkgName ;
	private String otaPkgLength ;
	private String otaPkgVersion ;
	private String otaLevelFlag ;
	private String otaDownloadUrl ;
	private String otaAnswerCode ;
	private String otaForceFlag ;
	
	
	public OtaRemoteData() {
		super();
	}

	public OtaRemoteData(String otaPkgName, String otaPkgLength,
			String otaPkgVersion, String otaLevelFlag, String otaDownloadUrl,
			String otaAnswerCode, String otaForceFlag) {
		super();
		this.otaPkgName = otaPkgName;
		this.otaPkgLength = otaPkgLength;
		this.otaPkgVersion = otaPkgVersion;
		this.otaLevelFlag = otaLevelFlag;
		this.otaDownloadUrl = otaDownloadUrl;
		this.otaAnswerCode = otaAnswerCode;
		this.otaForceFlag = otaForceFlag;
	}
	
	public String getOtaPkgName() {
		return otaPkgName;
	}
	public void setOtaPkgName(String otaPkgName) {
		this.otaPkgName = otaPkgName;
	}
	public String getOtaPkgLength() {
		return otaPkgLength;
	}
	public void setOtaPkgLength(String otaPkgLength) {
		this.otaPkgLength = otaPkgLength;
	}
	public String getOtaPkgVersion() {
		return otaPkgVersion;
	}
	public void setOtaPkgVersion(String otaPkgVersion) {
		this.otaPkgVersion = otaPkgVersion;
	}
	public String getOtaLevelFlag() {
		return otaLevelFlag;
	}
	public void setOtaLevelFlag(String otaLevelFlag) {
		this.otaLevelFlag = otaLevelFlag;
	}
	public String getOtaDownloadUrl() {
		return otaDownloadUrl;
	}
	public void setOtaDownloadUrl(String otaDownloadUrl) {
		this.otaDownloadUrl = otaDownloadUrl;
	}
	public String getOtaAnswerCode() {
		return otaAnswerCode;
	}
	public void setOtaAnswerCode(String otaAnswerCode) {
		this.otaAnswerCode = otaAnswerCode;
	}
	public String getOtaForceFlag() {
		return otaForceFlag;
	}
	public void setOtaForceFlag(String otaForceFlag) {
		this.otaForceFlag = otaForceFlag;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(otaPkgName);
        dest.writeString(otaPkgLength);
        dest.writeString(otaPkgVersion);
        dest.writeString(otaDownloadUrl);
        dest.writeString(otaLevelFlag);
        dest.writeString(otaAnswerCode);
        dest.writeString(otaForceFlag);
    }

    public static final Parcelable.Creator<OtaRemoteData> CREATOR = new Parcelable.Creator<OtaRemoteData>() {
        @Override
        public OtaRemoteData createFromParcel(Parcel source) {
        	OtaRemoteData file = new OtaRemoteData();
            file.otaPkgName = source.readString();
            file.otaPkgLength = source.readString();
            file.otaPkgVersion = source.readString();
            file.otaDownloadUrl = source.readString();
            file.otaLevelFlag = source.readString() ;
            file.otaAnswerCode = source.readString() ;
            file.otaForceFlag = source.readString();
            return file;
        }

        @Override
        public OtaRemoteData[] newArray(int size) {
            return new OtaRemoteData[size];
        }
    };
    
	@Override
	public String toString() {
		return "OtaRemoteData：  " + otaPkgName +":"+ otaPkgLength 
				+":"+ otaPkgVersion +":"+ otaDownloadUrl 
				+":"+ otaLevelFlag +":"+ otaAnswerCode 
				+":"+ otaForceFlag;
	}
}
