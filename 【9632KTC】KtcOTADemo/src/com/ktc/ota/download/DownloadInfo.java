package com.ktc.ota.download;

import java.io.Serializable;

/**
 * @author Arvin
 * @TODO 当前正在现在的OTA升级包信息
 * @Date 2019.1.24
 */
public class DownloadInfo implements Serializable{
	
    private String dlUrl;
    private String dledFilePath ;
    private Long dledLength ;
    private int dledProgress ;
    private int dledStatusFlag ;
    
	public DownloadInfo() {
		super();
	}
	
	public DownloadInfo(String dlUrl , String dledFilePath,
			Long dledLength, int dledProgress, int dledStatusFlag) {
		super();
		this.dlUrl = dlUrl;
		this.dledFilePath = dledFilePath;
		this.dledLength = dledLength;
		this.dledProgress = dledProgress;
		this.dledStatusFlag = dledStatusFlag;
	}
	
	public String getDlUrl() {
		return dlUrl;
	}
	
	public void setDlUrl(String dlUrl) {
		this.dlUrl = dlUrl;
	}
	
	public String getDledFilePath() {
		return dledFilePath;
	}
	
	public void setDledFilePath(String dledFilePath) {
		this.dledFilePath = dledFilePath;
	}
	
	
	public Long getDledLength() {
		return dledLength;
	}
	
	public void setDledLength(Long dledLength) {
		this.dledLength = dledLength;
	}
	
	
	public int getDledProgress() {
		return dledProgress;
	}
	
	public void setDledProgress(int dledProgress) {
		this.dledProgress = dledProgress;
	}
	
	
	public int getDledStatusFlag() {
		return dledStatusFlag;
	}
	
	public void setDledStatusFlag(int dledStatusFlag) {
		this.dledStatusFlag = dledStatusFlag;
	}
    
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "DownloadInfo:  "+ dlUrl  
				+" : "+ dledFilePath +" : "+ dledLength 
				+" : "+ dledProgress +" : "+ dledStatusFlag ;
	}
}
