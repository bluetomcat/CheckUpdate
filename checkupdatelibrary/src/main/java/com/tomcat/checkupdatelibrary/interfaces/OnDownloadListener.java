package com.tomcat.checkupdatelibrary.interfaces;

import java.io.File;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/9
 * 功能描述：
 */
public interface OnDownloadListener<T extends BaseVersionEntity> {
    void onDownload(File apk, String url, DownLoadListener loadListener, T data);
}
