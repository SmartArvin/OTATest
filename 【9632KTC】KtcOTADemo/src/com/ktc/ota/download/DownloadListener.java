package com.ktc.ota.download;


/**
 * @author Arvin
 * @TODO 下载监听器接口
 * @Date 2019.2.15
 */
public interface DownloadListener {
	/**
     * @TODO 回传下载信息
     * @param DownloadInfo
     */
    void sendDownloadInfo(DownloadInfo mDownloadInfo);
    
    /**
     * @TODO 更新下载进度
     * @param progress
     */
    void onProgress(int progress);
    
    /**
     * @TODO 下载成功
     */
    void onSuccess();
    
    /**
     * @TODO 下载失败
     */
    void onFailed();
    
    /**
     * @TODO 下载暂停
     */
    void onPaused();
    
    /**
     * @TODO 下载取消，删除已下载文件
     */
    void onCanceled();
}
