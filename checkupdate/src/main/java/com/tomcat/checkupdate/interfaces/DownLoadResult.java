package com.tomcat.checkupdate.interfaces;

import java.io.File;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface DownLoadResult {
    void onSuccess(File file);

    void onFailed(Throwable e);

    void onCanceled();
}
