package com.tomcat.checkupdate;

import com.tomcat.checkupdate.interfaces.BaseBuilder;
import com.tomcat.checkupdate.interfaces.OnShowUiListener;
import com.tomcat.checkupdate.interfaces.ProxyDownLoadCallback;
import com.tomcat.checkupdate.interfaces.ProxyInstallCallback;

import java.io.File;

public class Builder implements BaseBuilder {
    OnShowUiListener updateUiListener = null;
    OnShowUiListener networkUiListener = null;
    ProxyDownLoadCallback proxyDownLoadCallback = null;
    ProxyInstallCallback proxyInstallCallback = null;
    boolean isForceUpdate = false;
    File parentDir = null;
    String apkName = "";


    public Builder setUpdateUiListener(OnShowUiListener updateUiListener) {
        this.updateUiListener = updateUiListener;
        return this;
    }

    public Builder setNetworkUiListener(OnShowUiListener networkUiListener) {
        this.networkUiListener = networkUiListener;
        return this;
    }

    public Builder setDownLoadProxy(ProxyDownLoadCallback proxyDownLoadCallback) {
        this.proxyDownLoadCallback = proxyDownLoadCallback;
        return this;
    }

    public Builder setInstallProxy(ProxyInstallCallback proxyInstallCallback) {
        this.proxyInstallCallback = proxyInstallCallback;
        return this;
    }

    /**
     * @param isForceUpdate 如果设为true  将禁用更新提示UI用户"否"选项 需自实现
     */
    public Builder setForceUpdate(boolean isForceUpdate) {
        this.isForceUpdate = isForceUpdate;
        return this;
    }

    public Builder setCacheDir(File parentDir) {
        this.parentDir = parentDir;
        return this;
    }

    public Builder setApkName(String apkName) {
        this.apkName = apkName;
        return this;
    }

    public CheckUpdate build() {
        return new CheckUpdate(this);
    }
}