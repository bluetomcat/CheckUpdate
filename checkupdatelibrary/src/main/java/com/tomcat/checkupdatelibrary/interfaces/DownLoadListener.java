package com.tomcat.checkupdatelibrary.interfaces;

public interface DownLoadListener {
    void onStart();
    void onProgress(int progress);
    boolean onSuccess();
    void onFailed(Throwable e);
    void onCanceled();
}
