package com.tomcat.checkupdate.interfaces;

import java.io.File;

public interface ProxyDownLoadCallback<T extends BaseVersionEntity> {
    void onDownload(File apk, String url, DownLoadResult loadListener, T data);
}
