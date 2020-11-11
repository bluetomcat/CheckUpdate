package com.tomcat.checkupdatelibrary.interfaces;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface DownLoadResult {
    void onSuccess();

    void onFailed(Throwable e);

    void onCanceled();
}
