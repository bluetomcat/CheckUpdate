package com.tomcat.checkupdate.interfaces;

import java.io.File;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/9
 * 功能描述：
 */
public interface OnDownloadListener<T extends BaseVersionEntity> {
    void onDownload(File apk, String url, DownLoadResult downLoadResult, T data);
}
